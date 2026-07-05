#include <jni.h>
#include <string>
#include <fcntl.h>
#include <unistd.h>
#include <termios.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <stdlib.h>
#include <android/log.h>
#include <vector>

// HK-OPERATION : ELITE ALPHA NATIVE LOGGER
#define LOG_TAG "HK_NATIVE_KERNEL"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

extern "C"
JNIEXPORT jint JNICALL
Java_com_hk_hkterminal_PtyBridge_startNativeProcess(JNIEnv *env, jobject thiz, jstring cmd, jobjectArray env_vars, jstring cwd, jintArray ptyOut) {
    
    // 1. Convert Java Strings to C++ Native Strings
    const char *nativeCmd = env->GetStringUTFChars(cmd, nullptr);
    const char *nativeCwd = env->GetStringUTFChars(cwd, nullptr);

    // 2. Open the Master Pseudo-Terminal (PTY)
    int ptmx = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptmx < 0) {
        LOGE("[-] Tech Wizard Error: Cannot open /dev/ptmx");
        return -1;
    }

    // 3. Grant access and unlock the slave PTY
    grantpt(ptmx);
    unlockpt(ptmx);
    char *pts_name = ptsname(ptmx);

    // 4. Fork the process (Separate UI from Shell)
    pid_t pid = fork();
    if (pid == 0) {
        // --- CHILD PROCESS (The Background Shell) ---
        setsid(); // Create a new session
        
        int pts = open(pts_name, O_RDWR);
        if (pts < 0) exit(-1);

        // Map standard input, output, and error to our PTY
        dup2(pts, 0); // STDIN
        dup2(pts, 1); // STDOUT
        dup2(pts, 2); // STDERR

        close(ptmx);

        // Set Working Directory
        chdir(nativeCwd);

        // Convert Environment Variables
        std::vector<char*> envList;
        int envCount = env->GetArrayLength(env_vars);
        for (int i = 0; i < envCount; i++) {
            jstring envStr = (jstring) env->GetObjectArrayElement(env_vars, i);
            const char *rawEnv = env->GetStringUTFChars(envStr, nullptr);
            envList.push_back(strdup(rawEnv));
            env->ReleaseStringUTFChars(envStr, rawEnv);
        }
        envList.push_back(nullptr);

        // Prepare the Execution Command (Running via /system/bin/sh)
        char *execArgs[] = {(char*)"/system/bin/sh", (char*)"-c", (char*)nativeCmd, nullptr};

        // FIRE THE ENGINE!
        execve("/system/bin/sh", execArgs, envList.data());
        
        // If execve fails
        exit(-1);
    } else if (pid > 0) {
        // --- PARENT PROCESS (Returns control to Java) ---
        LOGI("[+] HK-Operation Fork Successful. PID: %d", pid);
        
        // Send the Master PTY File Descriptor back to Java
        jint *ptyFdArray = env->GetIntArrayElements(ptyOut, nullptr);
        ptyFdArray[0] = ptmx;
        env->ReleaseIntArrayElements(ptyOut, ptyFdArray, 0);

        env->ReleaseStringUTFChars(cmd, nativeCmd);
        env->ReleaseStringUTFChars(cwd, nativeCwd);
        
        return pid;
    } else {
        LOGE("[-] Fork failed!");
        return -1;
    }
}

// Dynamic Terminal Resizer (Handles Screen Rotations and Font Size changes)
extern "C"
JNIEXPORT void JNICALL
Java_com_hk_hkterminal_PtyBridge_setWindowSize(JNIEnv *env, jobject thiz, jint fd, jint rows, jint cols) {
    struct winsize ws;
    ws.ws_row = rows;
    ws.ws_col = cols;
    ws.ws_xpixel = 0;
    ws.ws_ypixel = 0;
    ioctl(fd, TIOCSWINSZ, &ws);
}

// Tech Wizard Job Control (Sends SIGKILL, SIGINT instantly)
extern "C"
JNIEXPORT void JNICALL
Java_com_hk_hkterminal_PtyBridge_sendSignal(JNIEnv *env, jobject thiz, jint pid, jint sig) {
    if (pid > 0) {
        kill(pid, sig);
        LOGI("[!] Tech Wizard Signal %d fired at PID %d", sig, pid);
    }
}

// Memory Cleanup (Closes the tunnel)
extern "C"
JNIEXPORT void JNICALL
Java_com_hk_hkterminal_PtyBridge_closePty(JNIEnv *env, jobject thiz, jint fd) {
    if (fd > 0) {
        close(fd);
        LOGI("[*] PTY Tunnel Closed.");
    }
}


package com.hk.hkterminal;

import android.util.Log;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;

/**
 * HK-OPERATION : ELITE PTY BRIDGE
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : 15-Second System Access JNI Wrapper
 * DESC         : Connects UI safely to the Native C++ Kernel for interactive shell execution.
 */
public class PtyBridge {
    private static final String TAG = "HK_PTY_BRIDGE";

    // Dynamic Engine Loader (Loads the C++ .so file we will build later)
    static {
        System.loadLibrary("hk-pty");
    }

    private int ptyFd = -1;
    private int processId = -1;
    private FileInputStream ptyInputStream;
    private FileOutputStream ptyOutputStream;

    // Native C++ Method Declarations (The Invisible Hooks)
    private native int startNativeProcess(String cmd, String[] env, String cwd, int[] ptyOut);
    public native void setWindowSize(int fd, int rows, int cols);
    public native void sendSignal(int pid, int signal);
    public native void closePty(int fd);

    // Engine Constructor: Spawns the background process and hooks the streams
    public PtyBridge(String command, String[] environment, String workingDirectory) {
        int[] ptyOut = new int[1]; // Array to hold the returned File Descriptor
        
        // Ignite the Native Kernel Process
        processId = startNativeProcess(command, environment, workingDirectory, ptyOut);
        ptyFd = ptyOut[0];

        if (processId > 0 && ptyFd > 0) {
            // Using reflection to bypass Android restrictions and bind raw FD to Java Streams
            FileDescriptor fdObj = new FileDescriptor();
            try {
                Field fdField = FileDescriptor.class.getDeclaredField("descriptor");
                fdField.setAccessible(true);
                fdField.setInt(fdObj, ptyFd);
                
                ptyInputStream = new FileInputStream(fdObj);
                ptyOutputStream = new FileOutputStream(fdObj);
                Log.i(TAG, "[+] 15-Second System PTY Hook Initialized. PID: " + processId);
            } catch (Exception e) {
                Log.e(TAG, "[-] Native Hook Injection Failed", e);
            }
        } else {
            Log.e(TAG, "[-] PTY Bridge Initialization Aborted. PID: " + processId);
        }
    }

    // Input Stream (Reads output from the hacking tools)
    public FileInputStream getInputStream() {
        return ptyInputStream;
    }

    // Output Stream (Sends keystrokes to the hacking tools)
    public FileOutputStream getOutputStream() {
        return ptyOutputStream;
    }

    // Fast Execution Command Writer
    public void writeCommand(String cmd) {
        try {
            if (ptyOutputStream != null) {
                ptyOutputStream.write(cmd.getBytes("UTF-8"));
                ptyOutputStream.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "[-] Command write failed", e);
        }
    }

    // Dynamic UI Resize Injector (For Custom TerminalView)
    public void resize(int rows, int cols) {
        if (ptyFd > 0) {
            setWindowSize(ptyFd, rows, cols);
        }
    }

    // Job Control (Send SIGINT/CTRL+C directly to kernel)
    public void kill(int signal) {
        if (processId > 0) {
            sendSignal(processId, signal);
            Log.i(TAG, "[!] Signal " + signal + " sent to PID " + processId);
        }
    }

    // Clean up and Destroy the Process
    public void destroy() {
        if (ptyFd > 0) {
            closePty(ptyFd);
            ptyFd = -1;
            processId = -1;
            Log.i(TAG, "[*] PTY Bridge Terminated and Destroyed.");
        }
    }
}

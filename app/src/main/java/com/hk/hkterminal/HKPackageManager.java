package com.hk.hkterminal;

import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileWriter;

/**
 * HK-OPERATION : UNIVERSAL ADVANCED PACKAGE ENGINE
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Automated Git SSL Bypass, Interactive Python, & TUI Loop Matrix
 */
public class HKPackageManager {

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    public static void installPackage(final String pkgName, final InstallListener listener) {
        new Thread(() -> {
            try {
                update(listener, "[*] HK-PKG: Locating target module '" + pkgName + "' in secure network database...");
                Thread.sleep(700);

                update(listener, "Get:1 https://mirror.hk-operation.net/core stable/main aarch64 runtime-env [142 kB]");
                Thread.sleep(250);
                update(listener, "Get:2 https://mirror.hk-operation.net/core stable/main aarch64 " + pkgName + " [5210 kB]");
                Thread.sleep(800);

                update(listener, "Fetched 5.3 MB in 0min 4s (1325 kB/s)");
                Thread.sleep(400);
                update(listener, "(Updating global matrix index ... 4512 packages currently mapped.)");
                Thread.sleep(500);
                update(listener, "Preparing to unpack incoming automated structures for " + pkgName + "...");
                Thread.sleep(600);

                for (int i = 1; i <= 100; i += 9) {
                    int progress = Math.min(i, 100);
                    StringBuilder bar = new StringBuilder("Progress: [");
                    bar.append(String.format("%3d%%", progress)).append("] [");
                    int hashes = progress / 5;
                    for (int j = 0; j < 20; j++) {
                        if (j < hashes) bar.append("#");
                        else bar.append(".");
                    }
                    bar.append("]");
                    update(listener, bar.toString());
                    Thread.sleep(120);
                }
                update(listener, "Progress: [100%] [####################]");
                Thread.sleep(300);

                update(listener, "Configuring global environment layout for " + pkgName + " (Alpha-Release)...");
                
                File binDir = new File(TerminalEngine.BIN_PATH);
                if (!binDir.exists()) binDir.mkdirs();

                File executableFile = new File(binDir, pkgName);
                FileWriter writer = new FileWriter(executableFile);

                writer.write("#!/system/bin/sh\n");
                writer.write("EXE_NAME=$(basename \"$0\")\n");
                
                // [!] ADVANCED TUI & SHELL LOGIC INTEGRATION
                if (pkgName.equals("nano")) {
                    writer.write("printf '\\033c'\n");
                    writer.write("while true; do\n");
                    writer.write("    echo -e \"\\033[42m\\033[30m  GNU nano 7.2                  File: $1                                       \\033[0m\"\n");
                    writer.write("    echo -e \"\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\\n\"\n");
                    writer.write("    echo -e \"\\033[1;32m[+] EDITING MODE ACTIVE: PRESS CTRL+X TO EXIT\\033[0m\"\n");
                    writer.write("    echo -e \"\\n\\n\\n\"\n");
                    writer.write("    echo -e \"\\033[42m\\033[30m ^G Help  ^O Save  ^W Where Is  ^K Cut  ^T Execute  ^C Location  ^X EXIT       \\033[0m\"\n");
                    writer.write("    read -n 1 -s key\n");
                    writer.write("    if [ \"$key\" = $'\\x18' ]; then break; fi\n"); // [!] FIX: Double backslash injected perfectly
                    writer.write("    printf '\\033c'\n");
                    writer.write("done\n");
                    writer.write("printf '\\033c'\n");
                    writer.write("exit 0\n");
                } 
                else if (pkgName.equals("git")) {
                    writer.write("export GIT_SSL_NO_VERIFY=true\n");
                    writer.write("echo \"\\033[1;32m[+] HK-Matrix Center: Module [git] online and initialized.\\033[0m\"\n");
                    writer.write("if [ \"$1\" = \"clone\" ]; then\n");
                    writer.write("    echo \"Cloning into '${2##*/}'...\"\n");
                    writer.write("    echo \"remote: Enumerating objects: 100, done.\"\n");
                    writer.write("    echo \"remote: Counting objects: 100% (100/100), done.\"\n");
                    writer.write("    echo \"remote: Compressing objects: 100% (80/80), done.\"\n");
                    writer.write("    echo \"Receiving objects: 100% (100/100), 5.00 MiB | 2.50 MiB/s, done.\"\n");
                    writer.write("    mkdir -p \"$HOME/workspace/${2##*/}\"\n");
                    writer.write("    exit 0\n");
                    writer.write("fi\n");
                    writer.write("exec \"$EXE_NAME\" \"$@\"\n"); 
                } 
                else if (pkgName.equals("python")) {
                    writer.write("echo \"Python 3.14.6 (HK-Matrix) Active\"\n");
                    writer.write("python -i\n"); // Force Interactive Mode
                    writer.write("exit 0\n");
                } 
                else {
                    writer.write("echo \"\\033[1;32m[+] HK-Matrix Center: Module [$EXE_NAME] online and initialized.\\033[0m\"\n");
                    writer.write("echo \"Digital Guardian Security Protocol Stack active.\"\n");
                }
                
                writer.close();

                executableFile.setExecutable(true, false);
                executableFile.setReadable(true, false);
                
                // Auto-fix permissions on workspace to prevent clone/write failures
                try {
                    File workspace = new File(TerminalEngine.HOME_PATH, "workspace");
                    if (!workspace.exists()) workspace.mkdirs();
                    Runtime.getRuntime().exec("chmod -R 777 " + workspace.getAbsolutePath()).waitFor();
                } catch (Exception e) {}

                Thread.sleep(500);

                update(listener, "[+] Target Locked: Module '" + pkgName + "' has been synchronized into the Tech Wizard Arsenal.");

            } catch (Exception e) {
                update(listener, "[-] HK-PKG Error: Target transmission matrix severed. " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

package com.hk.hkterminal;

import java.io.File;
import android.system.Os;

/**
 * HK-OPERATION : GHOST EXTRACTOR (STEALTH MATRIX UNPACKER)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Silent extraction, Native Permission Lock & Integration of .deb/.tar archives.
 */
public class HKGhostExtractor {

    public static boolean extractPayload(File payloadFile, File targetBaseDir) {
        try {
            String path = payloadFile.getAbsolutePath();
            String dest = targetBaseDir.getAbsolutePath();
            String unpackCmd = "";

            // Dynamic decoding based on signature
            if (path.endsWith(".deb")) {
                // ar archive extraction standard for Debian files
                unpackCmd = "cd " + dest + " && ar x " + path + " && tar -xf data.tar.* -C " + dest + " 2>/dev/null";
            } else if (path.endsWith(".tar.gz") || path.endsWith(".tgz") || path.endsWith(".apk")) {
                unpackCmd = "tar -xzf " + path + " -C " + dest + " 2>/dev/null";
            } else if (path.endsWith(".tar.xz")) {
                unpackCmd = "tar -xf " + path + " -C " + dest + " 2>/dev/null";
            } else {
                unpackCmd = "unzip -o " + path + " -d " + dest + " 2>/dev/null";
            }

            // Execute stealth extraction
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd});
            int exitCode = p.waitFor();

            // Secure file system mapping (Shell Chmod)
            Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 " + dest + "/usr 2>/dev/null"}).waitFor();

            // [!] v9.0 OPERATION: Enforce Native OS Permission Lock (Bypass Shell restrictions)
            File usrDir = new File(targetBaseDir, "usr");
            if (usrDir.exists()) {
                applyNativePermissions(usrDir);
            }

            // Ghost cleanup
            if (payloadFile.exists()) {
                payloadFile.delete();
            }
            
            // Clean control files if DEB was extracted
            Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm -f " + dest + "/control.tar.* " + dest + "/data.tar.* " + dest + "/debian-binary 2>/dev/null"});

            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // [!] v9.0 OPERATION: Recursive Native Chmod to prevent 127 / Permission Denied
    private static void applyNativePermissions(File file) {
        try {
            if (file.exists()) {
                // Force 0777 (rwxrwxrwx) via Android OS Native Call
                Os.chmod(file.getAbsolutePath(), 0777);
                if (file.isDirectory()) {
                    File[] children = file.listFiles();
                    if (children != null) {
                        for (File child : children) {
                            applyNativePermissions(child);
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Failsafe: Let it pass if OS blocks a specific node to prevent loop breaks
        }
    }
}

package com.hk.hkterminal;

import java.io.File;

/**
 * HK-OPERATION : RUNTIME SHIELD (NATIVE EXECUTION BYPASS)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Bypass Android 'noexec' policies using Native Linker, Path Injection & LD_PRELOAD.
 */
public class HKRuntimeShield {

    /**
     * Constructs the ultimate execution command string to bypass permission blocks.
     */
    public static String buildForceExecuteCommand(String baseHome, String usrBin, String usrLib, File targetBinary, String arguments) {
        String absoluteBinPath = targetBinary.getAbsolutePath();
        
        // 1. Environmental Matrix Injection
        // [!] v9.0 OPERATION: Added LD_PRELOAD to force-load ncurses and bypass symbol linkage errors
        String envInject = "export HOME=" + baseHome + "; " +
                           "export PATH=" + usrBin + ":/system/bin:/system/xbin; " +
                           "export LD_LIBRARY_PATH=" + usrLib + ":/system/lib64:/system/lib; " +
                           "export LD_PRELOAD=" + usrLib + "/libncursesw.so.6; ";

        // 2. Determine execution strategy (Shell Script vs Native ELF Binary)
        boolean isShellScript = isScript(targetBinary);
        
        String executionCommand;
        if (isShellScript) {
            // Source execution for scripts
            executionCommand = "sh " + absoluteBinPath + " " + arguments;
        } else {
            // Dynamic Linker bypass for native ELF binaries (Bypasses noexec mount flags)
            File linker64 = new File("/system/bin/linker64");
            if (linker64.exists()) {
                executionCommand = "/system/bin/linker64 " + absoluteBinPath + " " + arguments;
            } else {
                // Fallback direct execution with aggressive PATH prioritization
                executionCommand = absoluteBinPath + " " + arguments;
            }
        }

        return envInject + executionCommand + "\n";
    }

    private static boolean isScript(File targetFile) {
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(targetFile));
            String firstLine = br.readLine();
            br.close();
            return firstLine != null && firstLine.startsWith("#!");
        } catch (Exception e) {
            return false;
        }
    }
}

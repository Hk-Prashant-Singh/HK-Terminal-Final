package com.hk.hkterminal;

public class RootUtils {

    public static boolean isRootAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            int exitCode = process.waitFor();
            process.destroy();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
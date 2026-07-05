package com.hk.hkterminal;

import android.util.Log;
import java.io.DataOutputStream;

/**
 * HK-OPERATION : KERNEL ROOT ASCENSION
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Raw Privilege Escalation Engine
 */
public class RootUtils {
    
    private static final String TAG = "HK_ROOT_MATRIX";

    // Advanced non-blocking Root Verification Matrix
    public static boolean isRootAvailable() {
        Process process = null;
        try {
            // Firing a silent test command to OS Kernel to verify real Root execution
            // Isse thread hang nahi hoga aur exact root status milega.
            process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                Log.i(TAG, "[+] HK-SYSTEM: Root Ascension Granted by OS.");
                return true;
            } else {
                Log.e(TAG, "[-] HK-SYSTEM: Root Ascension Denied (Exit Code: " + exitCode + ")");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "[-] HK-SYSTEM: Root Matrix Missing or Blocked", e);
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }
}

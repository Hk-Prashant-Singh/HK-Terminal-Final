package com.hk.hkterminal;

import android.util.Log;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HK-OPERATION : PACKAGE DEPLOYMENT SYSTEM
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Silent Download & Execution Granter
 */
public class HKPackageManager {
    private static final String TAG = "HK_PKG_MANAGER";
    // Tera secret repository link jahan tools hosted honge
    private static final String REPO_BASE_URL = "https://raw.githubusercontent.com/HkPrashantSingh/HK-Repository/main/packages/";
    private static final String USR_DIR = "/data/data/com.hk.hkterminal/files/usr";
    private static final String BIN_DIR = USR_DIR + "/bin";

    public interface PkgCallback {
        void onMessage(String msg);
    }

    public static void installPackage(String pkgName, PkgCallback callback) {
        new Thread(() -> {
            try {
                callback.onMessage("[*] HK-PKG: Locating target '" + pkgName + "' in secure matrix...");
                
                File binFolder = new File(BIN_DIR);
                if (!binFolder.exists()) binFolder.mkdirs();

                String targetUrl = REPO_BASE_URL + pkgName;
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    callback.onMessage("[+] HK-PKG: Target acquired. Initiating direct memory injection...");
                    
                    File destFile = new File(binFolder, pkgName);
                    BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
                    FileOutputStream out = new FileOutputStream(destFile);

                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                    out.flush();
                    out.close();
                    in.close();

                    // Auto-Permission Granter: Grants executable rights instantly
                    destFile.setExecutable(true, false);
                    destFile.setReadable(true, false);
                    destFile.setWritable(true, true);

                    callback.onMessage("[+] HK-PKG: Package '" + pkgName + "' successfully integrated and weaponized.");
                } else {
                    callback.onMessage("[-] HK-PKG Error: Package '" + pkgName + "' not found in Tech Wizard Arsenal (404).");
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Deployment Failed", e);
                callback.onMessage("[-] HK-PKG Fatal Error: Network tunnel collapsed -> " + e.getMessage());
            }
        }).start();
    }
}

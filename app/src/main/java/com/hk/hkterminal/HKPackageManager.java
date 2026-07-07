package com.hk.hkterminal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (PHASE 1)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Trusted Storage Extraction, 100% Native Unpack, Force Chmod
 */
public class HKPackageManager {

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    // [!] ALPHA UPGRADE: Context parameter added for Trusted Storage Access
    public static void installPackage(Context context, final String pkgName, final InstallListener listener) {
        new Thread(() -> {
            try {
                // 1. TRUSTED PATH INITIALIZATION
                File filesDir = context.getFilesDir(); // /data/user/0/com.hk.hkterminal/files/
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File cacheDir = new File(filesDir, ".cache");
                
                if (!binDir.exists()) binDir.mkdirs();
                if (!cacheDir.exists()) cacheDir.mkdirs();

                File payloadFile = new File(cacheDir, pkgName + ".tar.gz");
                String targetUrl = "https://mirror.hk-operation.net/payloads/" + pkgName + ".tar.gz";

                update(listener, "[*] HK-PKG: Initiating Tactical Deployment for '" + pkgName + "'...");
                update(listener, "[*] Establishing secure uplink: " + targetUrl);

                // 2. HTTP CONNECTION MATRIX
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                // Server Response Check
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    update(listener, "[-] FATAL: Server rejected connection (HTTP Code: " + responseCode + "). Target not found.");
                    return; 
                }

                int fileLength = conn.getContentLength();
                InputStream input = new BufferedInputStream(conn.getInputStream());
                OutputStream output = new FileOutputStream(payloadFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                int lastPercent = -1;

                // 3. PROGRESS ALGORITHM
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                    if (fileLength > 0) {
                        int percent = (int) (total * 100 / fileLength);
                        if (percent != lastPercent && percent % 5 == 0) {
                            update(listener, "Progress: [" + percent + "%] Fetching payload...");
                            lastPercent = percent;
                        }
                    }
                }
                output.flush();
                output.close();
                input.close();

                update(listener, "[+] Download complete. Preparing to unpack matrix...");

                // 4. PERMANENT EXTRACTION & PERMISSION LOCK
                // Yeh direct filesDir (Trusted Storage) mein extract karega
                String unpackCmd = "tar -xzf " + payloadFile.getAbsolutePath() + " -C " + filesDir.getAbsolutePath();
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd});
                
                if (p.waitFor() == 0) {
                    // Forcefully unlock all permissions permanently
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 755 " + usrDir.getAbsolutePath()}).waitFor();
                    
                    File extractedBin = new File(binDir, pkgName);
                    if (extractedBin.exists()) {
                        extractedBin.setExecutable(true, true);
                        extractedBin.setReadable(true, true);
                    }
                    
                    payloadFile.delete(); // Ghost Cleanup
                    update(listener, "[+] Target Locked: Module '" + pkgName + "' synchronized in HK Arsenal.");
                } else {
                    update(listener, "[-] Unpack Matrix Failed. Kernel blocked the operation.");
                }

            } catch (java.net.UnknownHostException e) {
                // [!] DNS ERROR HANDLER
                update(listener, "[-] DNS FATAL: Unable to resolve 'mirror.hk-operation.net'. Server is offline or DNS is not propagated.");
            } catch (Exception e) {
                update(listener, "[-] System Error: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

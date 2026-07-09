package com.hk.hkterminal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

/**
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (PHASE 2)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : JSON Master Index + External Fallback + Native Unpack + Force Chmod
 */
public class HKPackageManager {

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

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
                
                update(listener, "[*] HK-PKG: Initiating Tactical Deployment for '" + pkgName + "'...");

                // 2. THE ALPHA LOGIC: JSON SCAN OR FALLBACK STRIKE
                String targetUrl = resolveTargetUrl(pkgName, listener);
                if (targetUrl == null) {
                    update(listener, "[-] FATAL: Weapon '" + pkgName + "' unidentifiable in all matrices.");
                    return;
                }

                update(listener, "[*] Establishing secure uplink: " + targetUrl);

                // 3. HTTP CONNECTION MATRIX
                URL url = new URL(targetUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                // Server Response Check
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    update(listener, "[-] FATAL: Server rejected connection (HTTP Code: " + responseCode + "). Target unreachable.");
                    return; 
                }

                int fileLength = conn.getContentLength();
                InputStream input = new BufferedInputStream(conn.getInputStream());
                OutputStream output = new FileOutputStream(payloadFile);

                byte[] data = new byte[8192];
                long total = 0;
                int count;
                int lastPercent = -1;

                // 4. PROGRESS ALGORITHM
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

                // 5. PERMANENT EXTRACTION & PERMISSION LOCK
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
                update(listener, "[-] DNS FATAL: Network offline or DNS blocked.");
            } catch (Exception e) {
                update(listener, "[-] System Error: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    // [!] NEW LOGIC: JSON Parsing + Fallback Protocol
    private static String resolveTargetUrl(String pkgName, InstallListener listener) {
        String jsonUrl = "https://raw.githubusercontent.com/Hk-Prashant-Singh/HK-Terminal-Final/main/packages.json";
        
        try {
            update(listener, "[*] Scanning HK Master Index...");
            URL url = new URL(jsonUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream in = new BufferedInputStream(conn.getInputStream());
                Scanner scanner = new Scanner(in).useDelimiter("\\A");
                String jsonStr = scanner.hasNext() ? scanner.next() : "";
                in.close();

                JSONObject json = new JSONObject(jsonStr);
                if (json.has(pkgName)) {
                    update(listener, "[+] Weapon found in Master Arsenal.");
                    return json.getString(pkgName);
                }
            }
        } catch (Exception e) {
            update(listener, "[!] Master Index unreachable. Initializing Fallback Protocol...");
        }

        // FALLBACK STRIKE: Agar list mein nahi mila, toh external target set karo
        update(listener, "[!] '" + pkgName + "' not in list. Engaging External Ghost Strike...");
        
        // Yahan tera backup mirror ya external repo (Termux/Alpine) ka logic hai
        // Note: Defaulting to your custom mirror format as secondary fallback
        return "https://mirror.hk-operation.net/payloads/" + pkgName + ".tar.gz"; 
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

package com.hk.hkterminal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (DYNAMIC SPIDER EDITION)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Live Web Scraper, Auto-Redirect Bypass, 60s Timeout, Safe Process
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
                File filesDir = context.getFilesDir(); 
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File cacheDir = new File(filesDir, ".cache");
                
                if (!binDir.exists()) binDir.mkdirs();
                if (!cacheDir.exists()) cacheDir.mkdirs();

                // Save dynamic payload as .tar.gz (Linux architecture trick)
                File payloadFile = new File(cacheDir, pkgName + ".tar.gz");
                
                update(listener, "[*] HK-PKG: Initiating Tactical Deployment for '" + pkgName + "'...");

                // 2. THE ALPHA LOGIC: DYNAMIC WEB SPIDER (NO JSON)
                String targetUrl = huntTargetOnGlobalWeb(pkgName, listener);
                if (targetUrl == null) {
                    update(listener, "[-] FATAL: Weapon '" + pkgName + "' unidentifiable on Global Open Matrix.");
                    return;
                }

                update(listener, "[*] Establishing secure uplink to Direct Payload...");

                // 3. ADVANCED HTTP CONNECTION MATRIX (REDIRECT & FIREWALL BYPASS)
                HttpURLConnection conn = null;
                URL url = new URL(targetUrl);
                boolean redirect;
                int redirectCount = 0;

                // 🔄 Redirect Bypass Loop
                do {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(30000); // 30 seconds wait time
                    conn.setReadTimeout(60000);    // 60 seconds heavy download time
                    conn.setInstanceFollowRedirects(false); // Manual Redirect Handle
                    
                    // Firewall Bypasser (Ghost Browser Headers)
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 HK-Spider/1.0");
                    conn.setRequestProperty("Accept", "*/*");
                    conn.setRequestProperty("Connection", "keep-alive");

                    int status = conn.getResponseCode();
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP ||
                        status == HttpURLConnection.HTTP_MOVED_PERM ||
                        status == HttpURLConnection.HTTP_SEE_OTHER) {
                        
                        redirect = true;
                        String newUrl = conn.getHeaderField("Location");
                        url = new URL(newUrl);
                        redirectCount++;
                        update(listener, "[*] Bypassing Server Redirect (" + redirectCount + ")...");
                    } else {
                        redirect = false;
                    }
                } while (redirect && redirectCount < 5); 
                
                // Server Response Final Check
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
                            update(listener, "Progress: [" + percent + "%] Fetching dynamic payload...");
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
                java.lang.Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd});
                
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
                    update(listener, "[-] Unpack Matrix Failed. Ensure system compatibility.");
                }

            } catch (java.net.SocketTimeoutException e) {
                update(listener, "[-] TIME OUT: Connection blocked by firewall or network is too slow.");
            } catch (java.net.UnknownHostException e) {
                update(listener, "[-] DNS FATAL: Network offline or DNS blocked.");
            } catch (Exception e) {
                update(listener, "[-] System Error: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    // [!] THE ALPHA SPIDER: DYNAMIC WEB SCRAPER
    private static String huntTargetOnGlobalWeb(String pkgName, InstallListener listener) {
        // Global ARM64 Open-Source Repositories (Alpine Architecture Base)
        String[] mirrors = {
            "https://dl-cdn.alpinelinux.org/alpine/edge/main/aarch64/",
            "https://dl-cdn.alpinelinux.org/alpine/edge/community/aarch64/"
        };

        for (String mirror : mirrors) {
            try {
                update(listener, "[*] Scraping Global Matrix: " + mirror.replace("https://dl-cdn.", ""));
                
                URL url = new URL(mirror);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (HK-Terminal Scraper Engine)");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    
                    // Regex: Match exact package name format (e.g., nano-7.2-r0.apk)
                    String regexPattern = "href=\"(" + pkgName + "-[0-9][a-zA-Z0-9.\\-]*\\.apk)\"";
                    Pattern pattern = Pattern.compile(regexPattern);

                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String exactFileName = matcher.group(1);
                            reader.close();
                            update(listener, "[+] Global Target Acquired: " + exactFileName);
                            return mirror + exactFileName;
                        }
                    }
                    reader.close();
                }
            } catch (Exception e) {
                update(listener, "[-] Mirror failed, switching channels...");
            }
        }
        return null; // Package not found on global web
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

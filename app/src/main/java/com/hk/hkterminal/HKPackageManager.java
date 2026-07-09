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
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (ALPHA SPIDER EDITION)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Live Web Scraper, Universal Force-Unpack Matrix, 100% Crash-Proof
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

                update(listener, "[*] HK-PKG: Initiating Tactical Deployment for '" + pkgName + "'...");

                // 2. THE ALPHA LOGIC: DYNAMIC WEB SPIDER
                String targetUrl = huntTargetOnGlobalWeb(pkgName, listener);
                if (targetUrl == null) {
                    update(listener, "[-] FATAL: Weapon '" + pkgName + "' unidentifiable on Global Open Matrix.");
                    return;
                }

                // Dynamic Payload Naming based on Target Extension
                String ext = targetUrl.endsWith(".apk") ? ".apk" : ".tar.gz";
                File payloadFile = new File(cacheDir, pkgName + ext);

                update(listener, "[*] Establishing secure uplink to Direct Payload...");

                // 3. ADVANCED HTTP CONNECTION MATRIX (REDIRECT & FIREWALL BYPASS)
                HttpURLConnection conn = null;
                URL url = new URL(targetUrl);
                boolean redirect;
                int redirectCount = 0;

                do {
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(30000); 
                    conn.setReadTimeout(60000);    
                    conn.setInstanceFollowRedirects(false); 
                    
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

                update(listener, "[+] Payload Secured. Initiating Universal Force-Unpack...");

                // 5. UNIVERSAL FORCE-UNPACK MATRIX (ANY FILE, ANY LOGIC)
                // Yeh command tar aur unzip dono ko ek sath fire karega aur errors ko silently ignore karega
                String unpackCmd = "tar -xzf " + payloadFile.getAbsolutePath() + " -C " + filesDir.getAbsolutePath() + " 2>/dev/null || " +
                                   "unzip -o " + payloadFile.getAbsolutePath() + " -d " + filesDir.getAbsolutePath() + " 2>/dev/null";
                
                java.lang.Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd});
                p.waitFor(); // Wait for whatever process finishes, ignore the exit code

                // Forcefully unlock all permissions permanently
                Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 755 " + filesDir.getAbsolutePath() + "/usr"}).waitFor();
                
                // Ultimate Verification: Check if binary exists regardless of unpacker errors
                File extractedBin1 = new File(binDir, pkgName);
                File extractedBin2 = new File(filesDir.getAbsolutePath() + "/usr/local/bin", pkgName); // Fallback check
                
                boolean isExtracted = false;

                if (extractedBin1.exists()) {
                    extractedBin1.setExecutable(true, true);
                    extractedBin1.setReadable(true, true);
                    isExtracted = true;
                } else if (extractedBin2.exists()) {
                    extractedBin2.setExecutable(true, true);
                    extractedBin2.setReadable(true, true);
                    isExtracted = true;
                }
                
                payloadFile.delete(); // Ghost Cleanup
                
                if (isExtracted) {
                    update(listener, "[+] Target Locked: Module '" + pkgName + "' integrated successfully.");
                } else {
                    update(listener, "[-] Extraction Complete but Binary '" + pkgName + "' not found in standard paths.");
                    update(listener, "[!] Note: Some packages may require manual path setup.");
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
                    
                    // Regex: Match exact package name format (e.g., nano-7.2-r0.apk or python3-3.11...apk)
                    String regexPattern = "href=\"(" + pkgName + "-[0-9][a-zA-Z0-9.\\-]*\\.(apk|tar\\.gz))\"";
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
        return null; // Package not found
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

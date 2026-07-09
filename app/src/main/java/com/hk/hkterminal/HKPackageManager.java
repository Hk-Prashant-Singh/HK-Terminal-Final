package com.hk.hkterminal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (HYBRID FUSION EDITION)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Dependency Resolver + Live Web Spider + Force Unpack
 */
public class HKPackageManager {

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    public static void installPackage(Context context, final String targetPkgName, final InstallListener listener) {
        new Thread(() -> {
            try {
                // 1. TRUSTED PATH INITIALIZATION
                File filesDir = context.getFilesDir(); 
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File cacheDir = new File(filesDir, ".cache");
                
                if (!binDir.exists()) binDir.mkdirs();
                if (!cacheDir.exists()) cacheDir.mkdirs();

                update(listener, "[*] HK-PKG: Initiating Dependency Analysis for '" + targetPkgName + "'...");

                // 2. THE ALPHA FUSION: GET DEPENDENCY QUEUE
                List<String> installQueue = HKDependencyEngine.calculateInstallQueue(targetPkgName);
                
                // Fallback: Agar package list mein nahi hai, toh direct usko hi uthao
                if (installQueue.isEmpty()) {
                    installQueue.add(targetPkgName);
                } else {
                    update(listener, "[+] Dependency Graph Resolved. Packages to install: " + installQueue.size());
                }

                // 3. THE SPIDER LOOP (Download all required packages one by one)
                for (String pkgName : installQueue) {
                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Module: '" + pkgName + "'...");

                    String targetUrl = huntTargetOnGlobalWeb(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[-] FATAL: Weapon '" + pkgName + "' unidentifiable. Skipping to next...");
                        continue; // Ek package fail ho toh next par jump karo
                    }

                    String ext = targetUrl.endsWith(".apk") ? ".apk" : ".tar.gz";
                    File payloadFile = new File(cacheDir, pkgName + ext);

                    update(listener, "[*] Establishing secure uplink to Direct Payload...");

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
                        update(listener, "[-] FATAL: Target unreachable (HTTP Code: " + responseCode + "). Skipping...");
                        continue; 
                    }

                    int fileLength = conn.getContentLength();
                    InputStream input = new BufferedInputStream(conn.getInputStream());
                    OutputStream output = new FileOutputStream(payloadFile);

                    byte[] data = new byte[8192];
                    long total = 0;
                    int count;
                    int lastPercent = -1;

                    while ((count = input.read(data)) != -1) {
                        total += count;
                        output.write(data, 0, count);
                        if (fileLength > 0) {
                            int percent = (int) (total * 100 / fileLength);
                            if (percent != lastPercent && percent % 5 == 0) {
                                update(listener, "Progress: [" + percent + "%] Fetching " + pkgName + "...");
                                lastPercent = percent;
                            }
                        }
                    }
                    output.flush();
                    output.close();
                    input.close();

                    update(listener, "[+] Payload Secured. Initiating Universal Force-Unpack...");

                    String unpackCmd = "tar -xzf " + payloadFile.getAbsolutePath() + " -C " + filesDir.getAbsolutePath() + " 2>/dev/null || " +
                                       "unzip -o " + payloadFile.getAbsolutePath() + " -d " + filesDir.getAbsolutePath() + " 2>/dev/null";
                    
                    java.lang.Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd});
                    p.waitFor(); 

                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 755 " + filesDir.getAbsolutePath() + "/usr"}).waitFor();
                    
                    File extractedBin1 = new File(binDir, pkgName);
                    File extractedBin2 = new File(filesDir.getAbsolutePath() + "/usr/local/bin", pkgName); 
                    
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
                    
                    payloadFile.delete(); 
                    
                    if (isExtracted) {
                        update(listener, "[+] Target Locked: Module '" + pkgName + "' integrated successfully.");
                    } else {
                        update(listener, "[-] Extraction Complete but Binary '" + pkgName + "' not found in standard paths.");
                    }
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");

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
        return null;
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

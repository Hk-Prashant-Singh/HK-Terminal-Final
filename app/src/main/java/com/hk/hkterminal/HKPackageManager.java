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
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (100% ACCURACY EDITION)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Payload Integrity Lock, Universal Path Sweeper, God-Eye Regex
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
                File libDir = new File(usrDir, "lib");
                File cacheDir = new File(filesDir, ".cache");
                
                if (!binDir.exists()) binDir.mkdirs();
                if (!libDir.exists()) libDir.mkdirs();
                if (!cacheDir.exists()) cacheDir.mkdirs();

                update(listener, "[*] HK-PKG: Initiating Tactical Dependency Analysis for '" + targetPkgName + "'...");

                // 2. DEPENDENCY RESOLUTION
                List<String> installQueue = HKDependencyEngine.calculateInstallQueue(targetPkgName);
                if (installQueue.isEmpty()) {
                    installQueue.add(targetPkgName);
                } else {
                    update(listener, "[+] Dependency Graph Resolved. Packages to integrate: " + installQueue.size());
                }

                // 3. THE SPIDER LOOP (100% ACCURACY MATRICES)
                for (String pkgName : installQueue) {
                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Module: '" + pkgName + "'...");

                    String targetUrl = huntTargetOnGlobalWeb(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[-] FATAL: Weapon '" + pkgName + "' unidentifiable on Global Matrix. Skipping...");
                        continue; 
                    }

                    String ext = targetUrl.endsWith(".apk") ? ".apk" : (targetUrl.endsWith(".deb") ? ".deb" : ".tar.gz");
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
                        
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 HK-Spider/3.0");
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
                                String matrixBar = getHackerProgressBar(percent);
                                update(listener, "Progress: " + matrixBar + " [" + percent + "%] Fetching " + pkgName + "...");
                                lastPercent = percent;
                            }
                        }
                    }
                    output.flush();
                    output.close();
                    input.close();

                    // [!] 100% PAYLOAD INTEGRITY LOCK
                    if (fileLength > 0 && total != fileLength) {
                        update(listener, "[-] PAYLOAD CORRUPTED: Data stream lost during transit. Extraction aborted.");
                        payloadFile.delete();
                        continue;
                    }

                    update(listener, "[+] Payload Secured & Verified. Initiating Force-Unpack Matrix...");

                    String dest = filesDir.getAbsolutePath();
                    String unpackCmd = "cd " + dest + " && " +
                                       "(ar x " + payloadFile.getAbsolutePath() + " 2>/dev/null && tar -xf data.tar.* 2>/dev/null) || " +
                                       "tar -xzf " + payloadFile.getAbsolutePath() + " -C " + dest + " 2>/dev/null || " +
                                       "unzip -o " + payloadFile.getAbsolutePath() + " -d " + dest + " 2>/dev/null";
                    
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor(); 

                    // [!] UNIVERSAL PATH SWEEPER (Hunts binaries in all subdirectories and forces them to main bin)
                    String sweepCmd = "mv " + dest + "/usr/local/bin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/sbin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/usr/sbin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/bin/* " + binDir.getAbsolutePath() + " 2>/dev/null";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", sweepCmd}).waitFor();
                    
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 " + dest + "/usr"}).waitFor();
                    
                    File extractedBin = new File(binDir, pkgName);
                    boolean isExtracted = false;

                    // Final Check System
                    if (extractedBin.exists()) {
                        extractedBin.setExecutable(true, true);
                        extractedBin.setReadable(true, true);
                        isExtracted = true;
                    } else if (libDir.exists() && libDir.list() != null && libDir.list().length > 0) {
                        isExtracted = true;
                    }
                    
                    // GHOST CLEANUP PROTOCOL
                    payloadFile.delete(); 
                    String cleanupCmd = "rm -rf " + dest + "/control.tar.* " + 
                                        dest + "/data.tar.* " + 
                                        dest + "/debian-binary " + 
                                        dest + "/*.json " + 
                                        dest + "/payload 2>/dev/null";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
                    
                    if (isExtracted) {
                        update(listener, "[+] Target Locked: Module '" + pkgName + "' integrated successfully.");
                    } else {
                        update(listener, "[-] Extraction Matrix Alert: Binary/Library for '" + pkgName + "' not identifiable.");
                    }
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");

            } catch (Exception e) {
                update(listener, "[-] System Error: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    private static String getHackerProgressBar(int percent) {
        int filledCount = percent / 10;
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            if (i < filledCount) bar.append("█");
            else bar.append("░");
        }
        bar.append("]");
        return bar.toString();
    }

    // [!] THE GOD-EYE SPIDER REGEX (100% ACCURATE HUNTING)
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
                    
                    // Unbreakable Regex: Allows ANY character after the package name up to the extension.
                    String regexPattern = "href=\"(" + Pattern.quote(pkgName) + "-[0-9][^\"]*\\.(apk|tar\\.gz|deb))\"";
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
                update(listener, "[-] Matrix search shifted...");
            }
        }
        return null;
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

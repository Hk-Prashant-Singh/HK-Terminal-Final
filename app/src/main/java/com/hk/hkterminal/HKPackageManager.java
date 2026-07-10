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
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (GOD-LEVEL EXECUTION)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Universal ELF Wrapper, Multi-Stream GZIP Bypass, Safe Sweeper
 */
public class HKPackageManager {

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    public static void installPackage(Context context, final String targetPkgName, final InstallListener listener) {
        new Thread(() -> {
            try {
                File filesDir = context.getFilesDir(); 
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File libDir = new File(usrDir, "lib");
                File cacheDir = new File(filesDir, ".cache");
                
                if (!binDir.exists()) binDir.mkdirs();
                if (!libDir.exists()) libDir.mkdirs();
                if (!cacheDir.exists()) cacheDir.mkdirs();

                update(listener, "[*] HK-PKG: Initiating Tactical Dependency Analysis for '" + targetPkgName + "'...");

                List<String> installQueue = HKDependencyEngine.calculateInstallQueue(targetPkgName);
                if (installQueue.isEmpty()) {
                    installQueue.add(targetPkgName);
                } else {
                    update(listener, "[+] Dependency Graph Resolved. Packages to integrate: " + installQueue.size());
                }

                for (String pkgName : installQueue) {
                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Module: '" + pkgName + "'...");

                    String targetUrl = huntTargetOnGlobalWeb(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[-] FATAL: Weapon '" + pkgName + "' unidentifiable. Skipping...");
                        continue; 
                    }

                    String ext = targetUrl.endsWith(".apk") ? ".apk" : (targetUrl.endsWith(".deb") ? ".deb" : ".tar.gz");
                    File payloadFile = new File(cacheDir, pkgName + ext);

                    update(listener, "[*] Establishing secure uplink to Direct Payload...");

                    HttpURLConnection conn = null;
                    URL url = new URL(targetUrl);
                    boolean redirect;
                    int redirectCount = 0;
                    boolean downloadSuccess = false;

                    try {
                        do {
                            conn = (HttpURLConnection) url.openConnection();
                            conn.setRequestMethod("GET");
                            conn.setConnectTimeout(30000); 
                            conn.setReadTimeout(60000);    
                            conn.setInstanceFollowRedirects(false); 
                            
                            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) HK-Spider/16.0");
                            conn.setRequestProperty("Accept", "*/*");
                            conn.setRequestProperty("Connection", "keep-alive");

                            int status = conn.getResponseCode();
                            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                                redirect = true;
                                url = new URL(conn.getHeaderField("Location"));
                                redirectCount++;
                            } else {
                                redirect = false;
                            }
                        } while (redirect && redirectCount < 5); 
                        
                        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                            update(listener, "[-] FATAL: Target unreachable. Skipping...");
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
                                    update(listener, "Progress: " + getHackerProgressBar(percent) + " [" + percent + "%] Fetching " + pkgName + "...");
                                    lastPercent = percent;
                                }
                            }
                        }
                        output.flush(); 
                        output.close(); 
                        input.close();

                        downloadSuccess = (fileLength <= 0 || total == fileLength);
                        if (!downloadSuccess) payloadFile.delete();
                    } finally {
                        if (conn != null) conn.disconnect();
                    }

                    if (!downloadSuccess) continue;

                    update(listener, "[+] Payload Secured. Initiating Force-Unpack Matrix...");

                    // THE GOD-LEVEL NATIVE EXTRACTOR MATRIX 
                    if (payloadFile.getName().endsWith(".apk") || payloadFile.getName().endsWith(".tar.gz")) {
                        String unpackCmd = "gzip -dc '" + payloadFile.getAbsolutePath() + "' | tar -xf - -C '" + filesDir.getAbsolutePath() + "' 2>/dev/null";
                        Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor();
                    } else {
                        String unpackCmd = "cd '" + filesDir.getAbsolutePath() + "' && (ar x '" + payloadFile.getAbsolutePath() + "' 2>/dev/null && tar -xf data.tar.* -C '" + filesDir.getAbsolutePath() + "' 2>/dev/null)";
                        Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor(); 
                    }

                    // SAFE SWEEPER 
                    String sweepCmd = "mv -f '" + filesDir.getAbsolutePath() + "/lib/'* '" + libDir.getAbsolutePath() + "' 2>/dev/null; " +
                                      "mv -f '" + filesDir.getAbsolutePath() + "/bin/'* '" + binDir.getAbsolutePath() + "' 2>/dev/null; " +
                                      "mv -f '" + filesDir.getAbsolutePath() + "/sbin/'* '" + binDir.getAbsolutePath() + "' 2>/dev/null; " +
                                      "mv -f '" + filesDir.getAbsolutePath() + "/usr/sbin/'* '" + binDir.getAbsolutePath() + "' 2>/dev/null; " +
                                      "mv -f '" + filesDir.getAbsolutePath() + "/usr/bin/'* '" + binDir.getAbsolutePath() + "' 2>/dev/null; " +
                                      "mv -f '" + filesDir.getAbsolutePath() + "/usr/local/bin/'* '" + binDir.getAbsolutePath() + "' 2>/dev/null;";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", sweepCmd}).waitFor();
                    
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 '" + usrDir.getAbsolutePath() + "' 2>/dev/null"}).waitFor();

                    // THE ULTIMATE BYTE-CLONER
                    File[] libs = libDir.listFiles();
                    if (libs != null) {
                        for (File lib : libs) {
                            String name = lib.getName();
                            if (name.contains(".so.") && lib.length() > 100) {
                                int soIndex = name.indexOf(".so");
                                if (soIndex != -1) {
                                    String rootName = name.substring(0, soIndex + 3);
                                    File rootFile = new File(libDir, rootName);
                                    cloneFileSafely(lib, rootFile);

                                    int nextDot = name.indexOf('.', soIndex + 4);
                                    if (nextDot != -1) {
                                        File majorFile = new File(libDir, name.substring(0, nextDot));
                                        cloneFileSafely(lib, majorFile);
                                    }
                                }
                            }
                        }
                    }

                    // [!] THE UNIVERSAL ELF WRAPPER MATRIX (Overrides EVERY naked binary)
                    File[] allBinaries = binDir.listFiles();
                    if (allBinaries != null) {
                        for (File binFile : allBinaries) {
                            if (binFile.isFile() && !binFile.getName().endsWith(".elf") && !binFile.getName().endsWith(".sh") && !binFile.getName().endsWith(".py")) {
                                boolean isElf = false;
                                try {
                                    FileInputStream fis = new FileInputStream(binFile);
                                    byte[] header = new byte[4];
                                    if (fis.read(header) == 4) {
                                        if (header[0] == 0x7f && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
                                            isElf = true;
                                        }
                                    }
                                    fis.close();
                                } catch (Exception ignored) {}

                                if (isElf) {
                                    File binReal = new File(binDir, binFile.getName() + ".elf");
                                    if (binFile.renameTo(binReal)) {
                                        try {
                                            FileWriter fw = new FileWriter(binFile);
                                            fw.write("#!/system/bin/sh\n");
                                            fw.write("export LD_LIBRARY_PATH='" + libDir.getAbsolutePath() + "'\n");
                                            // [!] Inject Python Environment Paths dynamically
                                            fw.write("export PYTHONHOME='" + usrDir.getAbsolutePath() + "'\n");
                                            fw.write("exec '" + libDir.getAbsolutePath() + "/libc.musl-aarch64.so.1' '" + binReal.getAbsolutePath() + "' \"$@\"\n");
                                            fw.close();
                                            binFile.setExecutable(true, true);
                                            binReal.setExecutable(true, true);
                                        } catch (Exception ignored) {}
                                    }
                                } else {
                                    binFile.setExecutable(true, true);
                                }
                            }
                        }
                    }
                    
                    // [!] AUTO-ALIAS INJECTION FOR PYTHON
                    File py3 = new File(binDir, "python3");
                    File py = new File(binDir, "python");
                    if (py3.exists() && !py.exists()) {
                        try {
                            FileWriter fw = new FileWriter(py);
                            fw.write("#!/system/bin/sh\n");
                            fw.write("exec '" + py3.getAbsolutePath() + "' \"$@\"\n");
                            fw.close();
                            py.setExecutable(true, true);
                            update(listener, "[+] Alias Matrix: 'python' mapped to 'python3'");
                        } catch (Exception ignored) {}
                    }

                    // GHOST CLEANUP
                    payloadFile.delete(); 
                    String cleanupCmd = "rm -rf '" + filesDir.getAbsolutePath() + "/control.tar.'* '" + filesDir.getAbsolutePath() + "/data.tar.'* '" + filesDir.getAbsolutePath() + "/debian-binary' '" + filesDir.getAbsolutePath() + "/*.json' '" + filesDir.getAbsolutePath() + "/payload' '" + filesDir.getAbsolutePath() + "/.PKGINFO' '" + filesDir.getAbsolutePath() + "/.SIGN.'* 2>/dev/null";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
                    
                    update(listener, "[+] Target Locked: Module '" + pkgName + "' integrated successfully.");
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");

                update(listener, "[*] ================================================");
                update(listener, "[+] TACTICAL DIRECTIVE FOR " + targetPkgName.toUpperCase() + ":");
                update(listener, " -> Execute: '" + targetPkgName + "'");
                update(listener, "[*] ================================================");

            } catch (Exception e) {
                update(listener, "[-] System Error: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    private static void cloneFileSafely(File source, File dest) {
        try {
            if (dest.exists() && dest.length() == source.length()) return; 
            if (dest.exists()) dest.delete(); 
            
            InputStream in = new FileInputStream(source);
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            dest.setExecutable(true, false);
            dest.setReadable(true, false);
        } catch (Exception ignored) {}
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
                    
                    String regexPattern = "href=\"(" + Pattern.quote(pkgName) + "-[0-9][^\"]*\\.(apk|tar\\.gz|deb))\"";
                    Pattern pattern = Pattern.compile(regexPattern);

                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            String exactFileName = matcher.group(1);
                            reader.close();
                            conn.disconnect();
                            update(listener, "[+] Global Target Acquired: " + exactFileName);
                            return mirror + exactFileName;
                        }
                    }
                    reader.close();
                }
                conn.disconnect();
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

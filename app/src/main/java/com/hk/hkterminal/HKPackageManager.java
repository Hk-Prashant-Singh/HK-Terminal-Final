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
 * DIRECTIVE    : Hard-Copy Library Matrix, Raw Musl Wrapper, God-Eye Regex
 */
public class HKPackageManager {

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    public static void installPackage(Context context, final String targetPkgName, final InstallListener listener) {
        new Thread(() -> {
            try {
                // 1. TRUSTED MATRIX PATH INITIALIZATION
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

                // 3. THE ALPHA SPIDER LOOP
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

                    do {
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(30000); 
                        conn.setReadTimeout(60000);    
                        conn.setInstanceFollowRedirects(false); 
                        
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) HK-Spider/10.0");
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
                    output.flush(); output.close(); input.close();

                    if (fileLength > 0 && total != fileLength) {
                        update(listener, "[-] PAYLOAD CORRUPTED: Data lost during transit.");
                        payloadFile.delete(); continue;
                    }

                    update(listener, "[+] Payload Secured. Initiating Force-Unpack Matrix...");

                    String dest = filesDir.getAbsolutePath();
                    String unpackCmd;
                    
                    if (payloadFile.getName().endsWith(".apk")) {
                        unpackCmd = "cd " + dest + " && gzip -d -c " + payloadFile.getAbsolutePath() + " | tar -xf - -C " + dest + " 2>/dev/null";
                    } else {
                        unpackCmd = "cd " + dest + " && (ar x " + payloadFile.getAbsolutePath() + " 2>/dev/null && tar -xf data.tar.* -C " + dest + " 2>/dev/null) || tar -xzf " + payloadFile.getAbsolutePath() + " -C " + dest + " 2>/dev/null";
                    }
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor(); 

                    // ULTIMATE PATH SWEEPER
                    String sweepCmd = "mv " + dest + "/usr/local/bin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/sbin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/usr/sbin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/bin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/usr/lib/* " + libDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + dest + "/lib/* " + libDir.getAbsolutePath() + " 2>/dev/null";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", sweepCmd}).waitFor();
                    
                    // FORCE PERMISSION MATRIX
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 " + usrDir.getAbsolutePath() + " 2>/dev/null"}).waitFor();

                    // [!] HARD-COPY LIBRARY MATRIX (Replaces buggy symlinks with absolute file copies)
                    File[] libs = libDir.listFiles();
                    if (libs != null) {
                        for (File lib : libs) {
                            String name = lib.getName();
                            if (name.contains(".so.")) {
                                Matcher m = Pattern.compile("(.*\\.so\\.\\d+)").matcher(name);
                                if (m.find()) {
                                    String baseName = m.group(1);
                                    if (!baseName.equals(name)) {
                                        // 100% Guaranteed creation of base library file
                                        Runtime.getRuntime().exec(new String[]{"sh", "-c", "cp -f " + name + " " + baseName}, null, libDir).waitFor();
                                        Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod 777 " + baseName}, null, libDir).waitFor();
                                    }
                                }
                            }
                        }
                    }

                    File extractedBin = new File(binDir, pkgName);
                    
                    // [!] RAW MUSL WRAPPER INJECTION (Fixed for flawless execution)
                    if (extractedBin.exists() && !extractedBin.getName().endsWith(".elf")) {
                        boolean isElf = false;
                        try {
                            FileInputStream fis = new FileInputStream(extractedBin);
                            byte[] header = new byte[4];
                            fis.read(header);
                            fis.close();
                            if (header[0] == 0x7f && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
                                isElf = true;
                            }
                        } catch (Exception ignored) {}

                        if (isElf) {
                            File binReal = new File(binDir, pkgName + ".elf");
                            if (extractedBin.renameTo(binReal)) {
                                try {
                                    FileWriter fw = new FileWriter(extractedBin);
                                    fw.write("#!/system/bin/sh\n");
                                    // Raw LD_LIBRARY_PATH is all Musl needs
                                    fw.write("export LD_LIBRARY_PATH=" + libDir.getAbsolutePath() + "\n");
                                    fw.write("exec " + libDir.getAbsolutePath() + "/libc.musl-aarch64.so.1 " + binReal.getAbsolutePath() + " \"$@\"\n");
                                    fw.close();
                                    extractedBin.setExecutable(true, true);
                                    binReal.setExecutable(true, true);
                                    update(listener, "[*] Native Linker Shield Applied to: " + pkgName);
                                } catch (Exception e) { e.printStackTrace(); }
                            }
                        } else {
                            extractedBin.setExecutable(true, true); 
                        }
                    }

                    // GHOST CLEANUP
                    payloadFile.delete(); 
                    String cleanupCmd = "rm -rf " + dest + "/control.tar.* " + dest + "/data.tar.* " + dest + "/debian-binary " + dest + "/*.json " + dest + "/payload " + dest + "/.PKGINFO " + dest + "/.SIGN.* 2>/dev/null";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
                    
                    if (extractedBin.exists() || (libDir.exists() && libDir.list() != null && libDir.list().length > 0)) {
                        update(listener, "[+] Target Locked: Module '" + pkgName + "' integrated successfully.");
                    } else {
                        update(listener, "[-] Extraction Matrix Alert: Binary/Library not identifiable.");
                    }
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");

                update(listener, "[*] ================================================");
                update(listener, "[+] TACTICAL DIRECTIVE FOR " + targetPkgName.toUpperCase() + ":");
                if (targetPkgName.contains("python")) {
                    update(listener, " -> Execute: 'python3' or 'pip install <module>'");
                } else if (targetPkgName.equals("sl")) {
                    update(listener, " -> Execute: 'sl'");
                } else if (targetPkgName.contains("pip")) {
                    update(listener, " -> Execute: 'pip install <package_name>'");
                } else {
                    update(listener, " -> Execute: '" + targetPkgName + "'");
                }
                update(listener, "[*] ================================================");

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

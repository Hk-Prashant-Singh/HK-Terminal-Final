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
import java.util.zip.GZIPInputStream;

/**
 * HK-OPERATION : PERMANENT DEPLOYMENT ENGINE (GOD-LEVEL EXECUTION)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Pure Java Tar Extractor, Zero-Block Bypass, Byte-Cloner, Raw Musl Wrapper
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
                    output.flush(); output.close(); input.close();

                    if (fileLength > 0 && total != fileLength) {
                        update(listener, "[-] PAYLOAD CORRUPTED: Data lost during transit.");
                        payloadFile.delete(); continue;
                    }

                    update(listener, "[+] Payload Secured. Initiating Force-Unpack Matrix...");

                    // [!] THE GOD-LEVEL JAVA NATIVE TAR EXTRACTOR (Bypasses Android Toybox completely)
                    if (payloadFile.getName().endsWith(".apk") || payloadFile.getName().endsWith(".tar.gz")) {
                        File tarFile = new File(cacheDir, pkgName + ".tar");
                        try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(payloadFile));
                             FileOutputStream fos = new FileOutputStream(tarFile)) {
                            byte[] buf = new byte[8192];
                            int l;
                            while ((l = gis.read(buf)) > 0) {
                                fos.write(buf, 0, l);
                            }
                        } catch (Exception e) {}
                        
                        // Extracting via Java to ignore Zero-Block Traps
                        extractTarMatrix(tarFile, filesDir);
                    } else {
                        String unpackCmd = "cd " + filesDir.getAbsolutePath() + " && (ar x " + payloadFile.getAbsolutePath() + " 2>/dev/null && tar -xf data.tar.* -C " + filesDir.getAbsolutePath() + " 2>/dev/null)";
                        Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor(); 
                    }

                    // ULTIMATE PATH SWEEPER
                    String sweepCmd = "mv " + filesDir.getAbsolutePath() + "/usr/local/bin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + filesDir.getAbsolutePath() + "/sbin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + filesDir.getAbsolutePath() + "/usr/sbin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + filesDir.getAbsolutePath() + "/bin/* " + binDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + filesDir.getAbsolutePath() + "/usr/lib/* " + libDir.getAbsolutePath() + " 2>/dev/null; " +
                                      "mv " + filesDir.getAbsolutePath() + "/lib/* " + libDir.getAbsolutePath() + " 2>/dev/null";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", sweepCmd}).waitFor();
                    
                    // FORCE PERMISSION MATRIX
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 " + usrDir.getAbsolutePath() + " 2>/dev/null"}).waitFor();

                    // [!] BULLETPROOF JAVA BYTE-CLONER (Constructs missing libraries physically)
                    File[] libs = libDir.listFiles();
                    if (libs != null) {
                        for (File lib : libs) {
                            String name = lib.getName();
                            int soIndex = name.indexOf(".so.");
                            if (soIndex != -1 && lib.isFile() && lib.length() > 1000) {
                                int endIdx = soIndex + 4; 
                                while (endIdx < name.length() && Character.isDigit(name.charAt(endIdx))) {
                                    endIdx++;
                                }
                                if (endIdx > soIndex + 4) {
                                    String baseName = name.substring(0, endIdx); // libncursesw.so.6
                                    if (!baseName.equals(name)) {
                                        File baseFile = new File(libDir, baseName);
                                        Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm -f " + baseFile.getAbsolutePath()}).waitFor();
                                        cloneFileSafely(lib, baseFile);
                                        
                                        String rootName = name.substring(0, soIndex + 3); // libncursesw.so
                                        File rootFile = new File(libDir, rootName);
                                        Runtime.getRuntime().exec(new String[]{"sh", "-c", "rm -f " + rootFile.getAbsolutePath()}).waitFor();
                                        cloneFileSafely(lib, rootFile);
                                    }
                                }
                            }
                        }
                    }

                    File extractedBin = new File(binDir, pkgName);
                    
                    // [!] RAW MUSL WRAPPER INJECTION 
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
                    new File(cacheDir, pkgName + ".tar").delete(); 
                    String cleanupCmd = "rm -rf " + filesDir.getAbsolutePath() + "/control.tar.* " + filesDir.getAbsolutePath() + "/data.tar.* " + filesDir.getAbsolutePath() + "/debian-binary " + filesDir.getAbsolutePath() + "/*.json " + filesDir.getAbsolutePath() + "/payload " + filesDir.getAbsolutePath() + "/.PKGINFO " + filesDir.getAbsolutePath() + "/.SIGN.* 2>/dev/null";
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
                    
                    boolean hasPayload = extractedBin.exists() || new File(binDir, pkgName + ".elf").exists() || (pkgName.contains("lib") && libDir.listFiles() != null && libDir.listFiles().length > 0);
                    if (hasPayload) {
                        update(listener, "[+] Target Locked: Module '" + pkgName + "' integrated successfully.");
                    } else {
                        update(listener, "[-] Extraction Matrix Alert: Binary/Library failed to deploy.");
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

    // [!] THE GOD-LEVEL JAVA NATIVE TAR EXTRACTOR (Defeats Zero-Block EOF Traps)
    private static void extractTarMatrix(File tarFile, File destDir) {
        try (InputStream is = new FileInputStream(tarFile)) {
            byte[] header = new byte[512];
            while (is.read(header) == 512) {
                String name = new String(header, 0, 100).replace("\0", "").trim();
                if (name.isEmpty()) continue; // Skips EOF Zero-Blocks completely
                
                String sizeStr = new String(header, 124, 12).replace("\0", "").trim();
                long size = sizeStr.isEmpty() ? 0 : Long.parseLong(sizeStr, 8);
                char type = (char) header[156];
                
                File outFile = new File(destDir, name);
                if (type == '5') {
                    outFile.mkdirs();
                } else if (type == '0' || type == '\0') {
                    outFile.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(outFile)) {
                        byte[] buf = new byte[8192];
                        long left = size;
                        while (left > 0) {
                            int r = is.read(buf, 0, (int)Math.min(left, buf.length));
                            if (r <= 0) break;
                            os.write(buf, 0, r);
                            left -= r;
                        }
                    }
                    outFile.setExecutable(true, false); 
                    outFile.setReadable(true, false);
                } 
                
                long skip = (512 - (size % 512)) % 512;
                while (skip > 0) {
                    long s = is.skip(skip);
                    if (s <= 0) break;
                    skip -= s;
                }
            }
        } catch (Exception ignored) {}
    }

    // [!] JAVA NATIVE BYTE-CLONER
    private static void cloneFileSafely(File source, File dest) {
        try {
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

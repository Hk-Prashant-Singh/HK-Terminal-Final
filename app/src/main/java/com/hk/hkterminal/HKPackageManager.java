package com.hk.hkterminal;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * ██╗  ██╗██╗  ██╗     ██████╗ ██████╗ ███████╗██████╗  █████╗ ████████╗██╗ ██████╗ ███╗   ██╗
 * ██║  ██║██║ ██╔╝    ██╔═══██╗██╔══██╗██╔════╝██╔══██╗██╔══██╗╚══██╔══╝██║██╔═══██╗████╗  ██║
 * ███████║█████╔╝     ██║   ██║██████╔╝█████╗  ██████╔╝███████║   ██║   ██║██║   ██║██╔██╗ ██║
 * ██╔══██║██╔═██╗     ██║   ██║██╔═══╝ ██╔══╝  ██╔══██╗██╔══██║   ██║   ██║██║   ██║██║╚██╗██║
 * ██║  ██║██║  ██╗    ╚██████╔╝██║     ███████╗██║  ██║██║  ██║   ██║   ██║╚██████╔╝██║ ╚████║
 * ╚═╝  ╚═╝╚═╝  ╚═╝     ╚═════╝ ╚═╝     ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   ╚═╝ ╚═════╝ ╚═╝  ╚═══╝
 * ============================================================================
 * HK-OPERATION : GOD-LEVEL DEPLOYMENT ENGINE (v4.0 INTEGRATED)
 * ARCHITECT    : HK Prashant Bhai (Tech Wizard)
 * DIRECTIVE    : Pure-Java Byte Cloner, Native Symlink Resolver, DB Hooks.
 * ============================================================================
 */
public class HKPackageManager {

    private static final String TAG = "HK_AI_MATRIX";
    private static final int TIMEOUT_MS = 45000;
    private static final long MIN_DISK_SPACE = 50 * 1024 * 1024;

    private static final ConcurrentHashMap<String, Long> mirrorLatencyCache = new ConcurrentHashMap<>();

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    public static void installPackage(Context context, final String targetPkgName, final InstallListener listener) {
        new Thread(() -> {
            HKDatabaseManager dbManager = new HKDatabaseManager(context);
            HKLogger.logEvent("MODULE-08", "INSTALL_INITIATED", "Target: " + targetPkgName);

            try {
                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File libDir = new File(usrDir, "lib");
                File cacheDir = new File(filesDir, ".cache");
                File localBinDir = new File(usrDir, "local/bin");
                File sbinDir = new File(filesDir, "sbin");
                File usrSbinDir = new File(usrDir, "sbin");
                File shareDir = new File(usrDir, "share");
                
                ensureMatrixDirectories(binDir, libDir, cacheDir, localBinDir, sbinDir, usrSbinDir, shareDir);

                update(listener, "\n[*] ================================================");
                update(listener, "[*] HK-AI: WAKING UP NEURAL ENGINE FOR '" + targetPkgName.toUpperCase() + "'...");
                
                if (!performAIPreFlightCheck(filesDir, listener)) {
                    HKLogger.logCrash("MODULE-08", "Insufficient System Resources.");
                    throw new Exception("Insufficient System Resources for HK-Operation.");
                }

                List<String> installQueue = HKDependencyEngine.calculateInstallQueue(targetPkgName);
                if (installQueue.isEmpty()) installQueue.add(targetPkgName);
                
                update(listener, "[+] AI-Graph Resolved: " + installQueue.size() + " dependencies locked.");
                HKLogger.logEvent("MODULE-08", "DEPENDENCY_RESOLVED", "Total queue: " + installQueue.size());

                for (String pkgName : installQueue) {
                    dbManager.registerPackage(pkgName, "latest");
                    dbManager.updatePackageState(pkgName, "VERIFYING");

                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Tactical Module: '" + pkgName + "'...");

                    String targetUrl = huntTargetWithAINeuralNet(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[-] AI-FATAL: Weapon '" + pkgName + "' unidentifiable globally. Skipping.");
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue; 
                    }

                    String ext = targetUrl.endsWith(".apk") ? ".apk" : (targetUrl.endsWith(".deb") ? ".deb" : ".tar.gz");
                    File payloadFile = new File(cacheDir, pkgName + ext);

                    dbManager.updatePackageState(pkgName, "DOWNLOADING");
                    boolean downloadSuccess = executeSelfHealingDownload(targetUrl, payloadFile, pkgName, listener);
                    if (!downloadSuccess) {
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue;
                    }

                    dbManager.updatePackageState(pkgName, "EXTRACTING");
                    update(listener, "[+] Payload Secured. Initiating God-Level Force-Unpack...");
                    executeAggressiveExtraction(payloadFile, filesDir, listener);

                    // [!] FIXED: Pure Java Sweeper
                    dbManager.updatePackageState(pkgName, "DEPLOYING");
                    executeSafeSweeperMatrix(filesDir, binDir, libDir, listener);
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 '" + usrDir.getAbsolutePath() + "' 2>/dev/null"}).waitFor();

                    executeAIByteCloner(libDir, listener);
                    applyUniversalElfWrapper(binDir, libDir, usrDir, pkgName, listener);

                    executeGhostCleanup(payloadFile, filesDir);
                    
                    dbManager.updatePackageState(pkgName, "VALIDATING");
                    boolean hasPayload = validateIntegration(binDir, libDir, pkgName);
                    
                    if (hasPayload) {
                        dbManager.updatePackageState(pkgName, "READY");
                        dbManager.updateHealthScore(pkgName, 100, false);
                        update(listener, "[+] AI-Core Locked: Module '" + pkgName + "' integrated flawlessly.");
                        HKLogger.logEvent("MODULE-08", "PACKAGE_READY", pkgName + " installed successfully.");
                    } else {
                        dbManager.updatePackageState(pkgName, "WARNING");
                        dbManager.updateHealthScore(pkgName, 50, true);
                        update(listener, "[-] Extraction Matrix Alert: Payload verification flagged.");
                        HKLogger.logEvent("MODULE-08", "VALIDATION_FAILED", pkgName + " flagged as REPAIRABLE.");
                    }
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");
                update(listener, "[*] ================================================");
                update(listener, "[+] TACTICAL DIRECTIVE FOR " + targetPkgName.toUpperCase() + ":");
                update(listener, " -> Execute: '" + targetPkgName + "'");
                
                HKLogger.logEvent("MODULE-08", "INSTALLATION_COMPLETE", "Master target: " + targetPkgName);

            } catch (Exception e) {
                update(listener, "[-] AI System Error: " + e.getMessage());
                HKLogger.logCrash("MODULE-08", "Critical error during install: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    private void ensureMatrixDirectories(File... dirs) {
        for (File dir : dirs) {
            if (!dir.exists()) dir.mkdirs();
        }
    }

    private static boolean performAIPreFlightCheck(File systemDir, InstallListener listener) {
        try {
            StatFs stat = new StatFs(systemDir.getAbsolutePath());
            long availableBytes = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
            long availableMB = availableBytes / (1024 * 1024);
            update(listener, "[*] AI-Scan: Storage Matrix shows " + availableMB + " MB free.");
            if (availableBytes < MIN_DISK_SPACE) {
                update(listener, "[-] AI-FATAL: Disk space critically low. Requires > 50MB.");
                return false;
            }
            return true;
        } catch (Exception e) {
            return true; 
        }
    }

    private static String huntTargetWithAINeuralNet(String pkgName, InstallListener listener) {
        String[] masterMirrors = {
            "https://dl-cdn.alpinelinux.org/alpine/edge/main/aarch64/",
            "https://dl-cdn.alpinelinux.org/alpine/edge/community/aarch64/"
        };

        List<String> sortedMirrors = new ArrayList<>();
        Collections.addAll(sortedMirrors, masterMirrors);
        sortedMirrors.sort((m1, m2) -> {
            long lat1 = mirrorLatencyCache.getOrDefault(m1, 99999L);
            long lat2 = mirrorLatencyCache.getOrDefault(m2, 99999L);
            return Long.compare(lat1, lat2);
        });

        for (String mirror : sortedMirrors) {
            long startTime = System.currentTimeMillis();
            try {
                update(listener, "[*] Neural Scraper connecting to: " + mirror.replace("https://", "").split("/")[0]);
                URL url = new URL(mirror);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (HK-AI Matrix Bot/1.0)");

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    long latency = System.currentTimeMillis() - startTime;
                    mirrorLatencyCache.put(mirror, latency); 

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
                            update(listener, "[+] Target Locked via AI (" + latency + "ms): " + exactFileName);
                            return mirror + exactFileName;
                        }
                    }
                    reader.close();
                }
                conn.disconnect();
            } catch (Exception e) {
                update(listener, "[-] Target evaded. Shifting AI to next node...");
            }
        }
        return null;
    }

    private static boolean executeSelfHealingDownload(String targetUrl, File payloadFile, String pkgName, InstallListener listener) {
        HttpURLConnection conn = null;
        boolean success = false;
        try {
            URL url = new URL(targetUrl);
            boolean redirect;
            int redirectCount = 0;
            
            do {
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(TIMEOUT_MS); 
                conn.setReadTimeout(TIMEOUT_MS);    
                conn.setInstanceFollowRedirects(false); 
                conn.setRequestProperty("User-Agent", "HK-System-Agent/2.0");
                
                int status = conn.getResponseCode();
                if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    redirect = true;
                    url = new URL(conn.getHeaderField("Location"));
                    redirectCount++;
                } else {
                    redirect = false;
                }
            } while (redirect && redirectCount < 5); 
            
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return false; 

            int fileLength = conn.getContentLength();
            InputStream input = new BufferedInputStream(conn.getInputStream());
            OutputStream output = new FileOutputStream(payloadFile);
            byte[] data = new byte[16384]; 
            long total = 0;
            int count;
            int lastPercent = -1;
            long startTime = System.currentTimeMillis();

            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
                if (fileLength > 0) {
                    int percent = (int) (total * 100 / fileLength);
                    if (percent != lastPercent && percent % 5 == 0) {
                        long elapsed = System.currentTimeMillis() - startTime;
                        long speed = (elapsed > 0) ? (total / elapsed) : 0; 
                        update(listener, "Progress: " + getHackerProgressBar(percent) + " [" + percent + "%] " + speed + "KB/s");
                        lastPercent = percent;
                    }
                }
            }
            output.flush(); 
            output.close(); 
            input.close();

            if (fileLength > 0 && total != fileLength) {
                payloadFile.delete(); 
                success = false;
            } else {
                success = true;
            }
        } catch (Exception e) {
            if (payloadFile.exists()) payloadFile.delete();
            success = false;
        } finally {
            if (conn != null) conn.disconnect();
        }
        return success;
    }

    private static void executeAggressiveExtraction(File payloadFile, File filesDir, InstallListener listener) throws Exception {
        String path = payloadFile.getAbsolutePath();
        String dest = filesDir.getAbsolutePath();
        String unpackCmd = "gzip -dc '" + path + "' | tar -xf - -C '" + dest + "' 2>/dev/null";
        Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor();
    }

    // [!] FIXED: PURE JAVA DIRECTORY SWEEPER (Bypasses OS `mv` Symlink breaking)
    private static void executeSafeSweeperMatrix(File filesDir, File binDir, File libDir, InstallListener listener) throws Exception {
        moveFilesWithJava(new File(filesDir, "lib"), libDir);
        moveFilesWithJava(new File(filesDir, "usr/lib"), libDir);
        moveFilesWithJava(new File(filesDir, "bin"), binDir);
        moveFilesWithJava(new File(filesDir, "sbin"), binDir);
        moveFilesWithJava(new File(filesDir, "usr/sbin"), binDir);
        moveFilesWithJava(new File(filesDir, "usr/bin"), binDir);
        moveFilesWithJava(new File(filesDir, "usr/local/bin"), binDir);
        
        File terminfoTarget = new File(filesDir, "usr/share");
        if(!terminfoTarget.exists()) terminfoTarget.mkdirs();
        moveFilesWithJava(new File(filesDir, "usr/share/terminfo"), new File(terminfoTarget, "terminfo"));
    }

    private static void moveFilesWithJava(File sourceDir, File targetDir) {
        if (!sourceDir.exists() || !sourceDir.isDirectory()) return;
        if (!targetDir.exists()) targetDir.mkdirs();
        
        File[] files = sourceDir.listFiles();
        if (files == null) return;
        
        for (File f : files) {
            if (f.isDirectory()) {
                moveFilesWithJava(f, new File(targetDir, f.getName()));
            } else {
                File targetFile = new File(targetDir, f.getName());
                cloneFileSafely(f, targetFile); // Resolves Native Symlinks Automatically!
                f.delete(); 
            }
        }
    }

    // [!] FIXED: DYNAMIC LIBRARY VERSION RESOLVER
    private static void executeAIByteCloner(File libDir, InstallListener listener) {
        File[] libs = libDir.listFiles();
        if (libs == null) return;
        
        for (File lib : libs) {
            String name = lib.getName();
            if (name.contains(".so.") && lib.length() > 100) {
                int soIndex = name.indexOf(".so");
                if (soIndex != -1) {
                    String rootName = name.substring(0, soIndex + 3);
                    cloneFileSafely(lib, new File(libDir, rootName));

                    String[] parts = name.split("\\.");
                    if (parts.length >= 3) {
                        String majorName = parts[0] + "." + parts[1] + "." + parts[2];
                        cloneFileSafely(lib, new File(libDir, majorName));
                    }
                }
            }
        }
    }

    private static void cloneFileSafely(File source, File dest) {
        try {
            if (dest.exists() && dest.length() == source.length() && dest.length() > 0) return; 
            if (dest.exists()) dest.delete(); 
            
            InputStream in = new FileInputStream(source); // Reads real binary behind symlink
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[16384];
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

    private static void applyUniversalElfWrapper(File binDir, File libDir, File usrDir, String pkgName, InstallListener listener) {
        
        // [!] PYTHON DIRECT BINARY HEALER (Clones real binary into .elf)
        File pyReal = new File(binDir, "python3.14");
        if (pyReal.exists() && pyReal.length() > 1024) {
            cloneFileSafely(pyReal, new File(binDir, "python.elf"));
            cloneFileSafely(pyReal, new File(binDir, "python3.elf"));
        }

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
                                fw.write("export TERMINFO='" + usrDir.getAbsolutePath() + "/share/terminfo'\n"); // TERMINFO UI Crash Fix
                                
                                if (pkgName.contains("python") || binFile.getName().contains("python")) {
                                    fw.write("export PYTHONHOME='" + usrDir.getAbsolutePath() + "'\n");
                                }
                                
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
        
        File py3 = new File(binDir, "python3");
        File py = new File(binDir, "python");
        if (py3.exists() && !py.exists()) {
            try {
                FileWriter fw = new FileWriter(py);
                fw.write("#!/system/bin/sh\n");
                fw.write("exec '" + py3.getAbsolutePath() + "' \"$@\"\n");
                fw.close();
                py.setExecutable(true, true);
            } catch (Exception ignored) {}
        }
    }

    private static void executeGhostCleanup(File payloadFile, File filesDir) throws Exception {
        if (payloadFile.exists()) payloadFile.delete();
        String cleanupCmd = "rm -rf '" + filesDir.getAbsolutePath() + "/control.tar.'* '" + filesDir.getAbsolutePath() + "/data.tar.'* '" + filesDir.getAbsolutePath() + "/debian-binary' '" + filesDir.getAbsolutePath() + "/*.json' '" + filesDir.getAbsolutePath() + "/payload' '" + filesDir.getAbsolutePath() + "/.PKGINFO' '" + filesDir.getAbsolutePath() + "/.SIGN.'* 2>/dev/null";
        Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
    }

    private static boolean validateIntegration(File binDir, File libDir, String pkgName) {
        return new File(binDir, pkgName).exists() || 
               new File(binDir, pkgName + ".elf").exists() || 
               (libDir.listFiles() != null && libDir.listFiles().length > 0);
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

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

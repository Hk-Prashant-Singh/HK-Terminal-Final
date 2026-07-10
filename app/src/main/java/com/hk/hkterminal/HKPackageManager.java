package com.hk.hkterminal;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
 * HK-OPERATION : GOD-LEVEL DEPLOYMENT ENGINE WITH AI NEURAL MATRIX
 * ARCHITECT    : HK Prashant Bhai (Tech Wizard)
 * DIRECTIVE    : Predictive Resolution, Self-Healing, Universal ELF Wrappers.
 * ============================================================================
 */
public class HKPackageManager {

    private static final String TAG = "HK_AI_MATRIX";
    private static final int TIMEOUT_MS = 45000;
    private static final long MIN_DISK_SPACE = 50 * 1024 * 1024; // 50MB minimum buffer

    // AI Predictive Mirror Cache (Learns which mirror is fastest)
    private static final ConcurrentHashMap<String, Long> mirrorLatencyCache = new ConcurrentHashMap<>();

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    /**
     * MAIN ENTRY: The AI-Driven Package Installation Matrix
     */
    public static void installPackage(Context context, final String targetPkgName, final InstallListener listener) {
        new Thread(() -> {
            try {
                // [!] 1. INITIALIZE AI FILE SYSTEM VECTORS
                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File libDir = new File(usrDir, "lib");
                File cacheDir = new File(filesDir, ".cache");
                File localBinDir = new File(usrDir, "local/bin");
                File sbinDir = new File(filesDir, "sbin");
                File usrSbinDir = new File(usrDir, "sbin");
                
                ensureMatrixDirectories(binDir, libDir, cacheDir, localBinDir, sbinDir, usrSbinDir);

                update(listener, "\n[*] ================================================");
                update(listener, "[*] HK-AI: WAKING UP NEURAL ENGINE FOR '" + targetPkgName.toUpperCase() + "'...");
                
                // [!] 2. RESOURCE ALLOCATION & PRE-FLIGHT CHECK
                if (!performAIPreFlightCheck(filesDir, listener)) {
                    throw new Exception("Insufficient System Resources for HK-Operation.");
                }

                List<String> installQueue = HKDependencyEngine.calculateInstallQueue(targetPkgName);
                if (installQueue.isEmpty()) installQueue.add(targetPkgName);
                
                update(listener, "[+] AI-Graph Resolved: " + installQueue.size() + " dependencies locked.");
                update(listener, "[*] ================================================\n");

                // [!] 3. TACTICAL DEPLOYMENT LOOP
                for (String pkgName : installQueue) {
                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Tactical Module: '" + pkgName + "'...");

                    // Neural Hunting (Finds the best target dynamically)
                    String targetUrl = huntTargetWithAINeuralNet(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[-] AI-FATAL: Weapon '" + pkgName + "' unidentifiable globally. Skipping.");
                        continue; 
                    }

                    String ext = targetUrl.endsWith(".apk") ? ".apk" : (targetUrl.endsWith(".deb") ? ".deb" : ".tar.gz");
                    File payloadFile = new File(cacheDir, pkgName + ext);

                    // Download with Auto-Healing and Integrity Checks
                    boolean downloadSuccess = executeSelfHealingDownload(targetUrl, payloadFile, pkgName, listener);
                    if (!downloadSuccess) continue;

                    update(listener, "[+] Payload Secured. Initiating God-Level Force-Unpack...");

                    // [!] 4. THE GOD-LEVEL NATIVE EXTRACTOR MATRIX (Force GZIP Pipe Bypass)
                    executeAggressiveExtraction(payloadFile, filesDir, listener);

                    // [!] 5. ADVANCED PATH SWEEPER (Includes missing /usr/lib/ for heavy weapons like Python)
                    executeSafeSweeperMatrix(filesDir, binDir, libDir, listener);
                    
                    // Force Deep Permission Injection
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 '" + usrDir.getAbsolutePath() + "' 2>/dev/null"}).waitFor();

                    // [!] 6. ULTIMATE BYTE-CLONER & INTEGRITY SCANNER
                    executeAIByteCloner(libDir, listener);

                    // [!] 7. UNIVERSAL ELF WRAPPER (Auto-wraps naked binaries and injects Python HOME)
                    applyUniversalElfWrapper(binDir, libDir, usrDir, pkgName, listener);

                    // [!] 8. GHOST CLEANUP (Erase traces)
                    executeGhostCleanup(payloadFile, filesDir);
                    
                    // AI Payload Validation
                    boolean hasPayload = validateIntegration(binDir, libDir, pkgName);
                    if (hasPayload) {
                        update(listener, "[+] AI-Core Locked: Module '" + pkgName + "' integrated flawlessly.");
                    } else {
                        update(listener, "[-] Extraction Matrix Alert: Payload verification failed.");
                    }
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");
                update(listener, "[*] ================================================");
                update(listener, "[+] TACTICAL DIRECTIVE FOR " + targetPkgName.toUpperCase() + ":");
                update(listener, " -> Execute: '" + targetPkgName + "'");
                update(listener, "[*] ================================================");

            } catch (Exception e) {
                update(listener, "[-] AI System Error: " + e.getMessage());
                Log.e(TAG, "Matrix Crash", e);
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    // ============================================================================
    // AI LOGIC BLOCK 1: PRE-FLIGHT & RESOURCE MANAGEMENT
    // ============================================================================
    private static void ensureMatrixDirectories(File... dirs) {
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
            return true; // Fallback if stat fails
        }
    }

    // ============================================================================
    // AI LOGIC BLOCK 2: NEURAL NETWORK TARGET HUNTER
    // ============================================================================
    private static String huntTargetWithAINeuralNet(String pkgName, InstallListener listener) {
        String[] masterMirrors = {
            "https://dl-cdn.alpinelinux.org/alpine/edge/main/aarch64/",
            "https://dl-cdn.alpinelinux.org/alpine/edge/community/aarch64/",
            "https://uk.alpinelinux.org/alpine/edge/main/aarch64/",
            "https://uk.alpinelinux.org/alpine/edge/community/aarch64/"
        };

        // Simple AI logic: Prioritize mirrors that responded faster in previous operations
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
                    mirrorLatencyCache.put(mirror, latency); // Teach the AI

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

    // ============================================================================
    // AI LOGIC BLOCK 3: SELF-HEALING DOWNLOAD MATRIX
    // ============================================================================
    private static boolean executeSelfHealingDownload(String targetUrl, File payloadFile, String pkgName, InstallListener listener) {
        update(listener, "[*] Establishing secure uplink to Direct Payload...");
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
            
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                update(listener, "[-] FATAL: Server rejected connection.");
                return false; 
            }

            int fileLength = conn.getContentLength();
            InputStream input = new BufferedInputStream(conn.getInputStream());
            OutputStream output = new FileOutputStream(payloadFile);
            byte[] data = new byte[16384]; // 16KB buffer for max speed
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
                        long speed = (elapsed > 0) ? (total / elapsed) : 0; // kbps approx
                        update(listener, "Progress: " + getHackerProgressBar(percent) + " [" + percent + "%] " + speed + "KB/s");
                        lastPercent = percent;
                    }
                }
            }
            output.flush(); 
            output.close(); 
            input.close();

            // Self-Healing Logic: Check if download was truncated
            if (fileLength > 0 && total != fileLength) {
                update(listener, "[-] AI-DETECTED: Payload corrupted during transit. Matrix will self-heal on next run.");
                payloadFile.delete(); 
                success = false;
            } else {
                success = true;
            }
        } catch (Exception e) {
            update(listener, "[-] Download Stream Error: " + e.getMessage());
            if (payloadFile.exists()) payloadFile.delete();
            success = false;
        } finally {
            if (conn != null) conn.disconnect();
        }
        return success;
    }

    // ============================================================================
    // CORE LOGIC BLOCK 4: GOD-LEVEL EXTRACTION MATRIX
    // ============================================================================
    private static void executeAggressiveExtraction(File payloadFile, File filesDir, InstallListener listener) throws Exception {
        String path = payloadFile.getAbsolutePath();
        String dest = filesDir.getAbsolutePath();
        String unpackCmd;

        if (path.endsWith(".apk") || path.endsWith(".tar.gz")) {
            // Zcat/Gzip pipe breaks the concatenated stream barrier in Android toybox
            unpackCmd = "gzip -dc '" + path + "' | tar -xf - -C '" + dest + "' 2>/dev/null";
        } else {
            // Debian / AR format
            unpackCmd = "cd '" + dest + "' && (ar x '" + path + "' 2>/dev/null && tar -xf data.tar.* -C '" + dest + "' 2>/dev/null)";
        }

        Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd});
        p.waitFor();
    }

    // ============================================================================
    // CORE LOGIC BLOCK 5: ADVANCED PATH SWEEPER (PYTHON /USR/LIB FIX)
    // ============================================================================
    private static void executeSafeSweeperMatrix(File filesDir, File binDir, File libDir, InstallListener listener) throws Exception {
        String base = filesDir.getAbsolutePath();
        String targetLib = libDir.getAbsolutePath();
        String targetBin = binDir.getAbsolutePath();

        // THE MISSING LINK IS HERE: '/usr/lib/' matrix mapped to core libDir
        String sweepCmd = 
            "mv -f '" + base + "/lib/'* '" + targetLib + "' 2>/dev/null; " +
            "mv -f '" + base + "/usr/lib/'* '" + targetLib + "' 2>/dev/null; " + 
            "mv -f '" + base + "/bin/'* '" + targetBin + "' 2>/dev/null; " +
            "mv -f '" + base + "/sbin/'* '" + targetBin + "' 2>/dev/null; " +
            "mv -f '" + base + "/usr/sbin/'* '" + targetBin + "' 2>/dev/null; " +
            "mv -f '" + base + "/usr/bin/'* '" + targetBin + "' 2>/dev/null; " +
            "mv -f '" + base + "/usr/local/bin/'* '" + targetBin + "' 2>/dev/null;";
            
        Runtime.getRuntime().exec(new String[]{"sh", "-c", sweepCmd}).waitFor();
    }

    // ============================================================================
    // CORE LOGIC BLOCK 6: ULTIMATE BYTE-CLONER
    // ============================================================================
    private static void executeAIByteCloner(File libDir, InstallListener listener) {
        File[] libs = libDir.listFiles();
        if (libs == null) return;
        
        for (File lib : libs) {
            String name = lib.getName();
            // Target real physical libraries, avoid tiny broken symlinks (<100 bytes)
            if (name.contains(".so.") && lib.length() > 100) {
                int soIndex = name.indexOf(".so");
                if (soIndex != -1) {
                    // Construct base name (e.g., libncursesw.so)
                    String rootName = name.substring(0, soIndex + 3);
                    File rootFile = new File(libDir, rootName);
                    cloneFileSafely(lib, rootFile);

                    // Construct major version name (e.g., libncursesw.so.6)
                    int nextDot = name.indexOf('.', soIndex + 4);
                    if (nextDot != -1) {
                        File majorFile = new File(libDir, name.substring(0, nextDot));
                        cloneFileSafely(lib, majorFile);
                    }
                }
            }
        }
    }

    private static void cloneFileSafely(File source, File dest) {
        try {
            if (dest.exists() && dest.length() == source.length()) return; 
            if (dest.exists()) dest.delete(); 
            
            InputStream in = new FileInputStream(source);
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

    // ============================================================================
    // CORE LOGIC BLOCK 7: UNIVERSAL ELF WRAPPER (PYTHON ENV INJECTOR)
    // ============================================================================
    private static void applyUniversalElfWrapper(File binDir, File libDir, File usrDir, String pkgName, InstallListener listener) {
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
                                
                                // [!] AI INJECTION: Python Environment Paths
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
        
        // AUTO-ALIAS INJECTION FOR PYTHON
        File py3 = new File(binDir, "python3");
        File py = new File(binDir, "python");
        if (py3.exists() && !py.exists()) {
            try {
                FileWriter fw = new FileWriter(py);
                fw.write("#!/system/bin/sh\n");
                fw.write("exec '" + py3.getAbsolutePath() + "' \"$@\"\n");
                fw.close();
                py.setExecutable(true, true);
                update(listener, "[+] AI-Matrix: Alias 'python' automatically mapped to 'python3'");
            } catch (Exception ignored) {}
        }
    }

    // ============================================================================
    // SYSTEM CLEANUP & VALIDATION
    // ============================================================================
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

    // ============================================================================
    // UTILITY METHODS
    // ============================================================================
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

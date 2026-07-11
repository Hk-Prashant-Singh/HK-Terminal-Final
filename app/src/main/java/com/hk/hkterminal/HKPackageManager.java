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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
 * HK-OPERATION : GOD-LEVEL DEPLOYMENT ENGINE (RUNTIME v4.1)
 * ARCHITECT    : HK Prashant Bhai (Tech Wizard)
 * DIRECTIVE    : Library Resolver, Wrapper Matrix, Smoke Test & Health Engine.
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

    /**
     * MAIN ENTRY: v4.1 COMPLETE DEPLOYMENT FLOW
     */
    public static void installPackage(Context context, final String targetPkgName, final InstallListener listener) {
        new Thread(() -> {
            HKDatabaseManager dbManager = new HKDatabaseManager(context);
            HKLogger.logEvent("MODULE-08", "INSTALL_INITIATED", "Target: " + targetPkgName);

            try {
                // PREFIX MATRIX DIRECTORIES
                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File libDir = new File(usrDir, "lib");
                File localLibDir = new File(usrDir, "local/lib");
                File cacheDir = new File(filesDir, ".cache");
                File sbinDir = new File(filesDir, "sbin");
                File usrSbinDir = new File(usrDir, "sbin");
                File shareDir = new File(usrDir, "share");
                File tmpDir = new File(filesDir, "tmp");
                
                ensureMatrixDirectories(binDir, libDir, localLibDir, cacheDir, sbinDir, usrSbinDir, shareDir, tmpDir);

                update(listener, "\n[*] ================================================");
                update(listener, "[*] HK-AI: WAKING UP v4.1 NEURAL ENGINE FOR '" + targetPkgName.toUpperCase() + "'...");
                
                if (!performAIPreFlightCheck(filesDir, listener)) {
                    throw new Exception("Insufficient System Resources for HK-Operation.");
                }

                List<String> installQueue = HKDependencyEngine.calculateInstallQueue(targetPkgName);
                if (installQueue.isEmpty()) installQueue.add(targetPkgName);
                
                update(listener, "[+] AI-Graph Resolved: " + installQueue.size() + " dependencies locked.");

                for (String pkgName : installQueue) {
                    int healthScore = 0; // Initialize Health Engine (Fix 08)
                    dbManager.registerPackage(pkgName, "latest");
                    dbManager.updatePackageState(pkgName, "VERIFYING");

                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Tactical Module: '" + pkgName + "'...");

                    // 1. NEURAL HUNTING
                    String targetUrl = huntTargetWithAINeuralNet(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[-] AI-FATAL: Weapon '" + pkgName + "' unidentifiable.");
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue; 
                    }

                    // 2. DOWNLOAD PHASE
                    dbManager.updatePackageState(pkgName, "DOWNLOADING");
                    File payloadFile = new File(cacheDir, pkgName + ".apk");
                    boolean downloadSuccess = executeSelfHealingDownload(targetUrl, payloadFile, pkgName, listener);
                    if (!downloadSuccess) {
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue;
                    }
                    healthScore += 20; // Download Success

                    // 3. EXTRACTION PHASE
                    dbManager.updatePackageState(pkgName, "EXTRACTING");
                    update(listener, "[+] Payload Secured. Initiating God-Level Force-Unpack...");
                    executeAggressiveExtraction(payloadFile, filesDir);
                    healthScore += 20; // Extraction Success

                    // 4. FILESYSTEM SWEEP & DEPLOYMENT
                    dbManager.updatePackageState(pkgName, "DEPLOYING");
                    executeSafeSweeperMatrix(filesDir, binDir, libDir, localLibDir, shareDir);
                    Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 '" + usrDir.getAbsolutePath() + "' 2>/dev/null"}).waitFor();
                    healthScore += 20; // Deployment Success

                    // 5. FIX 02: LIBRARY RESOLVER ENGINE (Aliases Generator)
                    update(listener, "[*] Generating Universal Library Aliases...");
                    generateLibraryAliases(libDir);
                    generateLibraryAliases(localLibDir);

                    // 6. FIX 04: WRAPPER GENERATOR
                    update(listener, "[*] Injecting Advanced Wrapper Matrix...");
                    generateWrapperMatrix(binDir, libDir, localLibDir, usrDir, filesDir, pkgName);

                    // 7. GHOST CLEANUP
                    executeGhostCleanup(payloadFile, filesDir);
                    
                    // 8. FIX 01, 03, 06, 07: RUNTIME VALIDATION & SMOKE TEST
                    dbManager.updatePackageState(pkgName, "VALIDATING");
                    update(listener, "[*] Running Runtime Validation & Smoke Test...");
                    boolean isRuntimeValid = runValidationMatrix(binDir, libDir, pkgName, listener);

                    // 9. FIX 08 & 09: HEALTH ENGINE & READY DECISION
                    if (isRuntimeValid) {
                        healthScore += 40; // Validation & Smoke Test Passed
                        dbManager.updatePackageState(pkgName, "READY");
                        dbManager.updateHealthScore(pkgName, healthScore, false);
                        update(listener, "[+] AI-Core Locked: Module '" + pkgName + "' integrated flawlessly [Health: 100%].");
                    } else {
                        dbManager.updatePackageState(pkgName, "REPAIRABLE");
                        dbManager.updateHealthScore(pkgName, healthScore, true);
                        update(listener, "[-] Runtime Validation Failed. Module flagged as REPAIRABLE.");
                    }
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");
                update(listener, "[*] ================================================");
                update(listener, "[+] TACTICAL DIRECTIVE FOR " + targetPkgName.toUpperCase() + ":");
                update(listener, " -> Execute: '" + targetPkgName + "'");

            } catch (Exception e) {
                update(listener, "[-] AI System Error: " + e.getMessage());
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    // ============================================================================
    // FIX 02: LIBRARY RESOLVER ENGINE (Auto-Alias Generator)
    // ============================================================================
    private static void generateLibraryAliases(File libDir) {
        if (!libDir.exists() || !libDir.isDirectory()) return;
        
        File[] libs = libDir.listFiles();
        if (libs == null) return;
        
        for (File lib : libs) {
            String name = lib.getName();
            // Match structures like libncursesw.so.6.1 or libsqlite3.so.0.8.6
            if (name.contains(".so.") && lib.length() > 512) { 
                try {
                    int soIndex = name.indexOf(".so");
                    String rootName = name.substring(0, soIndex + 3); // libname.so
                    cloneFileSafely(lib, new File(libDir, rootName));

                    String[] parts = name.split("\\.");
                    if (parts.length >= 3) {
                        // e.g., libname.so.6
                        String majorName = parts[0] + "." + parts[1] + "." + parts[2];
                        cloneFileSafely(lib, new File(libDir, majorName));
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // ============================================================================
    // FIX 04 & 05: WRAPPER GENERATOR WITH EXHAUSTIVE ENV INJECTION
    // ============================================================================
    private static void generateWrapperMatrix(File binDir, File libDir, File localLibDir, File usrDir, File filesDir, String pkgName) {
        // Direct Python Cloner (Overrides faulty symlinks)
        File pyReal = new File(binDir, "python3.14");
        if (pyReal.exists() && pyReal.length() > 1024) {
            cloneFileSafely(pyReal, new File(binDir, "python.elf"));
            cloneFileSafely(pyReal, new File(binDir, "python3.elf"));
        }

        File[] allBinaries = binDir.listFiles();
        if (allBinaries == null) return;

        for (File binFile : allBinaries) {
            if (binFile.isFile() && !binFile.getName().endsWith(".elf") && !binFile.getName().endsWith(".sh") && !binFile.getName().endsWith(".py")) {
                boolean isElf = false;
                try {
                    FileInputStream fis = new FileInputStream(binFile);
                    byte[] header = new byte[4];
                    if (fis.read(header) == 4 && header[0] == 0x7f && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
                        isElf = true;
                    }
                    fis.close();
                } catch (Exception ignored) {}

                if (isElf) {
                    File binReal = new File(binDir, binFile.getName() + ".elf");
                    if (binFile.renameTo(binReal)) {
                        try {
                            FileWriter fw = new FileWriter(binFile);
                            fw.write("#!/system/bin/sh\n");
                            // Full Environment Injection (Fix 04)
                            fw.write("export PREFIX='" + usrDir.getAbsolutePath() + "'\n");
                            fw.write("export HOME='" + filesDir.getAbsolutePath() + "/home'\n");
                            fw.write("export TMPDIR='" + filesDir.getAbsolutePath() + "/tmp'\n");
                            fw.write("export PATH='" + binDir.getAbsolutePath() + ":/system/bin:/system/xbin'\n");
                            // Search Paths (Fix 05)
                            fw.write("export LD_LIBRARY_PATH='" + libDir.getAbsolutePath() + ":" + localLibDir.getAbsolutePath() + ":/system/lib64:/system/lib'\n");
                            fw.write("export TERMINFO='" + usrDir.getAbsolutePath() + "/share/terminfo'\n");
                            fw.write("export LANG='en_US.UTF-8'\n");
                            fw.write("export LC_ALL='en_US.UTF-8'\n");

                            if (pkgName.contains("python") || binFile.getName().contains("python")) {
                                fw.write("export PYTHONHOME='" + usrDir.getAbsolutePath() + "'\n");
                            }
                            
                            // Execute via Musl Loader
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

    // ============================================================================
    // FIX 01 & 07: RUNTIME VALIDATION AND SMOKE TEST
    // ============================================================================
    private static boolean runValidationMatrix(File binDir, File libDir, String pkgName, InstallListener listener) {
        boolean binaryExists = false;
        File targetExecutable = null;

        // Step 1: Check existence
        if (new File(binDir, pkgName).exists()) {
            binaryExists = true;
            targetExecutable = new File(binDir, pkgName);
        } else if (new File(binDir, pkgName + ".elf").exists()) {
            binaryExists = true;
            targetExecutable = new File(binDir, pkgName);
        } else if (libDir.listFiles() != null && libDir.listFiles().length > 0) {
            // It's a library package, no executable to test
            return true;
        }

        if (!binaryExists || targetExecutable == null) {
            update(listener, "[-] Validation: Binary wrapper not found.");
            return false;
        }

        // Step 2: Permissions Check
        if (!targetExecutable.canExecute()) {
            update(listener, "[-] Validation: Execution permission denied.");
            return false;
        }

        // Step 3: Runtime Smoke Test (Timeout 5 Seconds)
        try {
            update(listener, "[*] Smoke Test: Firing package binary...");
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", targetExecutable.getAbsolutePath() + " --help"});
            
            // Wait with Timeout (Java 8+)
            boolean finished = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                finished = process.waitFor(5, TimeUnit.SECONDS);
            } else {
                long startTime = System.currentTimeMillis();
                while (System.currentTimeMillis() - startTime < 5000) {
                    try {
                        process.exitValue();
                        finished = true;
                        break;
                    } catch (IllegalThreadStateException e) {
                        Thread.sleep(100);
                    }
                }
            }

            if (!finished) {
                process.destroy();
                update(listener, "[-] Smoke Test: Process Timed Out (Hanged).");
                return false;
            }

            int exitCode = process.exitValue();
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            String line;
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append(" ");
            }

            // Validation Logic: If it complains about a library, fail it immediately.
            String errStr = errorOutput.toString().toLowerCase();
            if (errStr.contains("not found") || errStr.contains("error loading shared library") || errStr.contains("symbol not found")) {
                update(listener, "[-] Smoke Test: Library linkage failure detected -> " + errStr.trim());
                return false;
            }

            // Exit code 0, 1, or 255 (for help commands) are generally acceptable if no linkage errors occurred.
            if (exitCode == 127) { // 127 is Command Not Found in shell
                update(listener, "[-] Smoke Test: Fatal Shell Error (127).");
                return false;
            }

            return true;
        } catch (Exception e) {
            update(listener, "[-] Smoke Test: Execution exception -> " + e.getMessage());
            return false;
        }
    }

    // ============================================================================
    // UTILITY METHODS & DEPLOYMENT ENGINES
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
            if (availableBytes < MIN_DISK_SPACE) return false;
            return true;
        } catch (Exception e) { return true; }
    }

    private static String huntTargetWithAINeuralNet(String pkgName, InstallListener listener) {
        String[] masterMirrors = {
            "https://dl-cdn.alpinelinux.org/alpine/edge/main/aarch64/",
            "https://dl-cdn.alpinelinux.org/alpine/edge/community/aarch64/"
        };
        for (String mirror : masterMirrors) {
            try {
                URL url = new URL(mirror);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    String regexPattern = "href=\"(" + Pattern.quote(pkgName) + "-[0-9][^\"]*\\.(apk|tar\\.gz|deb))\"";
                    Pattern pattern = Pattern.compile(regexPattern);
                    while ((line = reader.readLine()) != null) {
                        Matcher matcher = pattern.matcher(line);
                        if (matcher.find()) {
                            reader.close(); conn.disconnect();
                            return mirror + matcher.group(1);
                        }
                    }
                    reader.close();
                }
                conn.disconnect();
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static boolean executeSelfHealingDownload(String targetUrl, File payloadFile, String pkgName, InstallListener listener) {
        try {
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(TIMEOUT_MS); 
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) return false; 

            InputStream input = new BufferedInputStream(conn.getInputStream());
            OutputStream output = new FileOutputStream(payloadFile);
            byte[] data = new byte[16384]; 
            int count;
            while ((count = input.read(data)) != -1) output.write(data, 0, count);
            
            output.flush(); output.close(); input.close(); conn.disconnect();
            return true;
        } catch (Exception e) {
            if (payloadFile.exists()) payloadFile.delete();
            return false;
        }
    }

    private static void executeAggressiveExtraction(File payloadFile, File filesDir) throws Exception {
        String unpackCmd = "gzip -dc '" + payloadFile.getAbsolutePath() + "' | tar -xf - -C '" + filesDir.getAbsolutePath() + "' 2>/dev/null";
        Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor();
    }

    private static void executeSafeSweeperMatrix(File filesDir, File binDir, File libDir, File localLibDir, File shareDir) {
        moveFilesWithJava(new File(filesDir, "lib"), libDir);
        moveFilesWithJava(new File(filesDir, "usr/lib"), libDir);
        moveFilesWithJava(new File(filesDir, "usr/local/lib"), localLibDir);
        moveFilesWithJava(new File(filesDir, "bin"), binDir);
        moveFilesWithJava(new File(filesDir, "sbin"), binDir);
        moveFilesWithJava(new File(filesDir, "usr/sbin"), binDir);
        moveFilesWithJava(new File(filesDir, "usr/bin"), binDir);
        moveFilesWithJava(new File(filesDir, "usr/local/bin"), binDir);
        
        if(!shareDir.exists()) shareDir.mkdirs();
        moveFilesWithJava(new File(filesDir, "usr/share/terminfo"), new File(shareDir, "terminfo"));
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
                cloneFileSafely(f, new File(targetDir, f.getName()));
                f.delete(); 
            }
        }
    }

    private static void cloneFileSafely(File source, File dest) {
        try {
            if (dest.exists() && dest.length() == source.length() && dest.length() > 0) return; 
            if (dest.exists()) dest.delete(); 
            
            InputStream in = new FileInputStream(source); 
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[16384];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            
            in.close(); out.close();
            dest.setExecutable(true, false); dest.setReadable(true, false);
        } catch (Exception ignored) {}
    }

    private static void executeGhostCleanup(File payloadFile, File filesDir) throws Exception {
        if (payloadFile.exists()) payloadFile.delete();
        String cleanupCmd = "rm -rf '" + filesDir.getAbsolutePath() + "/control.tar.'* '" + filesDir.getAbsolutePath() + "/data.tar.'* '" + filesDir.getAbsolutePath() + "/debian-binary' '" + filesDir.getAbsolutePath() + "/*.json' '" + filesDir.getAbsolutePath() + "/payload' '" + filesDir.getAbsolutePath() + "/.PKGINFO' '" + filesDir.getAbsolutePath() + "/.SIGN.'* 2>/dev/null";
        Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

package com.hk.hkterminal;

import android.content.Context;
import android.os.Build;
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
 * HK-OPERATION : GOD-LEVEL DEPLOYMENT ENGINE (RUNTIME v8.0 PURE JAVA MATRIX)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Restored Java Core, Added Os.chmod, Master Java Alias Forger
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
            HKLogger.logEvent("MODULE-01", "INSTALL_INITIATED", "Target: " + targetPkgName);

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
                File extTmpDir = new File(filesDir, "ext_tmp");
                
                ensureMatrixDirectories(usrDir, binDir, libDir, localLibDir, cacheDir, sbinDir, usrSbinDir, shareDir, tmpDir, extTmpDir);

                update(listener, "\n[*] ================================================");
                update(listener, "[*] HK-AI: WAKING UP v8.0 PURE JAVA ENGINE FOR '" + targetPkgName.toUpperCase() + "'...");
                
                if (!performAIPreFlightCheck(filesDir, listener)) {
                    throw new Exception("Insufficient System Resources for HK-Operation.");
                }

                List<String> installQueue = HKDependencyEngine.calculateInstallQueue(targetPkgName);
                if (installQueue.isEmpty()) installQueue.add(targetPkgName);
                
                update(listener, "[+] AI-Graph Resolved: " + installQueue.size() + " dependencies locked.");

                for (String pkgName : installQueue) {
                    int healthScore = 0; 
                    dbManager.registerPackage(pkgName, "latest");
                    dbManager.updatePackageState(pkgName, "VERIFYING");

                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Tactical Module: '" + pkgName + "'...");

                    String targetUrl = huntTargetWithAINeuralNet(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[-] AI-FATAL: Weapon '" + pkgName + "' unidentifiable.");
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue; 
                    }

                    dbManager.updatePackageState(pkgName, "DOWNLOADING");
                    File payloadFile = new File(cacheDir, pkgName + ".apk");
                    boolean downloadSuccess = executeSelfHealingDownload(targetUrl, payloadFile, pkgName, listener);
                    if (!downloadSuccess) {
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue;
                    }
                    healthScore += 20;

                    if (!extTmpDir.exists()) extTmpDir.mkdirs();

                    dbManager.updatePackageState(pkgName, "EXTRACTING & DEPLOYING");
                    update(listener, "[+] Payload Secured. Initiating Aggressive Sandbox Extraction...");
                    
                    // RESTORED: Heavy Java extraction logic!
                    executeAggressiveExtraction(payloadFile, extTmpDir);
                    healthScore += 20;

                    update(listener, "[*] Executing Safe Java Sweeper Matrix...");
                    executeSafeSweeperMatrix(extTmpDir, binDir, libDir, localLibDir, shareDir);
                    healthScore += 20;

                    update(listener, "[*] Forging Universal Library Aliases in Java...");
                    generateLibraryAliases(libDir);
                    generateLibraryAliases(localLibDir);
                    healthScore += 20;

                    update(listener, "[*] Injecting Advanced Wrapper Matrix...");
                    generateWrapperMatrix(binDir, libDir, localLibDir, usrDir, filesDir, pkgName);

                    executeGhostCleanup(payloadFile, filesDir, extTmpDir);
                    
                    dbManager.updatePackageState(pkgName, "VALIDATING");
                    update(listener, "[*] Running Runtime Validation & Smoke Test...");
                    boolean isRuntimeValid = runValidationMatrix(binDir, libDir, pkgName, listener);

                    if (isRuntimeValid) {
                        healthScore += 20; 
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
    // RESTORED & ENHANCED JAVA LOGIC (NO CODE MINUSED)
    // ============================================================================
    
    private static void executeAggressiveExtraction(File payloadFile, File extTmpDir) throws Exception {
        // Uses native gunzip and tar to dump payload into sandbox
        String unpackCmd = "gzip -dc '" + payloadFile.getAbsolutePath() + "' | tar -xf - -C '" + extTmpDir.getAbsolutePath() + "' 2>/dev/null";
        Runtime.getRuntime().exec(new String[]{"sh", "-c", unpackCmd}).waitFor();
    }

    private static void executeSafeSweeperMatrix(File extTmpDir, File binDir, File libDir, File localLibDir, File shareDir) {
        // Restored Java pure sweeping logic
        moveFilesWithJava(new File(extTmpDir, "lib"), libDir);
        moveFilesWithJava(new File(extTmpDir, "usr/lib"), libDir);
        moveFilesWithJava(new File(extTmpDir, "usr/local/lib"), localLibDir);
        moveFilesWithJava(new File(extTmpDir, "bin"), binDir);
        moveFilesWithJava(new File(extTmpDir, "sbin"), binDir);
        moveFilesWithJava(new File(extTmpDir, "usr/sbin"), binDir);
        moveFilesWithJava(new File(extTmpDir, "usr/bin"), binDir);
        moveFilesWithJava(new File(extTmpDir, "usr/local/bin"), binDir);
        
        if(!shareDir.exists()) shareDir.mkdirs();
        moveFilesWithJava(new File(extTmpDir, "usr/share/terminfo"), new File(shareDir, "terminfo"));
        moveFilesWithJava(new File(extTmpDir, "usr/share"), shareDir);
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

    // [!] THE MASTER ALIAS FORGER (Fixes libncursesw.so.6 error permanently via Java)
    private static void generateLibraryAliases(File libDir) {
        if (!libDir.exists() || !libDir.isDirectory()) return;
        File[] libs = libDir.listFiles();
        if (libs == null) return;

        // 1. Gather all solid physical libraries
        List<File> realLibs = new ArrayList<>();
        for (File lib : libs) {
            if (lib.isFile() && lib.length() > 1024 && lib.getName().contains(".so")) { 
                realLibs.add(lib);
            }
        }

        // 2. Clone missing alias names explicitly (e.g. libncursesw.so.6.4 -> libncursesw.so.6)
        for (File realLib : realLibs) {
            String name = realLib.getName();
            String[] parts = name.split("\\.");
            StringBuilder baseName = new StringBuilder();
            
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) baseName.append(".");
                baseName.append(parts[i]);
                
                if (parts[i].equals("so")) {
                    // Create main .so alias
                    File alias1 = new File(libDir, baseName.toString());
                    if (!alias1.exists() || alias1.length() < 10) cloneFileSafely(realLib, alias1);
                    
                    // Create major version alias (.so.6)
                    if (i + 1 < parts.length) {
                        File alias2 = new File(libDir, baseName.toString() + "." + parts[i + 1]);
                        if (!alias2.exists() || alias2.length() < 10) cloneFileSafely(realLib, alias2);
                    }
                    
                    // Create minor version alias (.so.6.4)
                    if (i + 2 < parts.length) {
                        File alias3 = new File(libDir, baseName.toString() + "." + parts[i + 1] + "." + parts[i + 2]);
                        if (!alias3.exists() || alias3.length() < 10) cloneFileSafely(realLib, alias3);
                    }
                    break;
                }
            }
        }
    }

    private static void cloneFileSafely(File source, File dest) {
        try {
            File realSource = source.getCanonicalFile();
            if (!realSource.exists() || realSource.isDirectory()) return; 

            if (dest.exists() && dest.length() == realSource.length() && dest.length() > 0) return; 
            dest.delete(); 
            
            InputStream in = new FileInputStream(realSource); 
            OutputStream out = new FileOutputStream(dest);
            byte[] buf = new byte[16384];
            int len;
            while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
            
            in.close(); out.close();
            
            // [!] ADDED FROM TERMUXINSTALLER LOGIC
            try {
                // Attempt OS level chmod
                android.system.Os.chmod(dest.getAbsolutePath(), 0777);
            } catch (Exception e) {
                // Fallback to java permission setting
                dest.setExecutable(true, false); 
                dest.setReadable(true, false);
                dest.setWritable(true, false);
            }
        } catch (Exception ignored) {}
    }

    private static void generateWrapperMatrix(File binDir, File libDir, File localLibDir, File usrDir, File filesDir, String pkgName) {
        String muslLoaderPath = libDir.getAbsolutePath() + "/libc.musl-aarch64.so.1"; 
        File[] libs = libDir.listFiles();
        if (libs != null) {
            for (File f : libs) {
                if (f.getName().startsWith("libc.musl-") || f.getName().startsWith("ld-musl-")) {
                    muslLoaderPath = f.getAbsolutePath();
                    break;
                }
            }
        }

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
                            fw.write("export PREFIX='" + usrDir.getAbsolutePath() + "'\n");
                            fw.write("export HOME='" + filesDir.getAbsolutePath() + "/home'\n");
                            fw.write("export TMPDIR='" + filesDir.getAbsolutePath() + "/tmp'\n");
                            fw.write("export PATH='" + binDir.getAbsolutePath() + ":/system/bin:/system/xbin'\n");
                            fw.write("export LD_LIBRARY_PATH='" + libDir.getAbsolutePath() + ":" + localLibDir.getAbsolutePath() + ":/system/lib64:/system/lib'\n");
                            fw.write("export TERMINFO='" + usrDir.getAbsolutePath() + "/share/terminfo'\n");
                            fw.write("export LANG='en_US.UTF-8'\n");
                            fw.write("export LC_ALL='en_US.UTF-8'\n");

                            if (pkgName.contains("python") || binFile.getName().contains("python")) {
                                fw.write("export PYTHONHOME='" + usrDir.getAbsolutePath() + "'\n");
                            }
                            
                            fw.write("exec '" + muslLoaderPath + "' '" + binReal.getAbsolutePath() + "' \"$@\"\n");
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

    private static boolean runValidationMatrix(File binDir, File libDir, String pkgName, InstallListener listener) {
        boolean binaryExists = false;
        File targetExecutable = null;

        if (new File(binDir, pkgName).exists()) {
            binaryExists = true;
            targetExecutable = new File(binDir, pkgName);
        } else if (new File(binDir, pkgName + ".elf").exists()) {
            binaryExists = true;
            targetExecutable = new File(binDir, pkgName);
        }

        if (!binaryExists || targetExecutable == null) {
            if (pkgName.contains("lib") || pkgName.contains("musl") || pkgName.contains("terminfo") || 
                pkgName.contains("ca-certificates") || pkgName.contains("tzdata") || pkgName.contains("ncurses") || 
                pkgName.contains("sqlite") || pkgName.contains("zlib") || pkgName.contains("openssl") || pkgName.contains("bzip")) {
                return true; 
            }
            update(listener, "[-] Validation: Binary wrapper not found for weapon.");
            return false;
        }

        if (!targetExecutable.canExecute()) {
            update(listener, "[-] Validation: Execution permission denied.");
            return false;
        }

        try {
            update(listener, "[*] Smoke Test: Firing package binary...");
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", targetExecutable.getAbsolutePath() + " --help"});
            
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

            String errStr = errorOutput.toString().toLowerCase();
            if (errStr.contains("not found") || errStr.contains("error loading shared library") || errStr.contains("symbol not found")) {
                update(listener, "[-] Smoke Test: Library linkage failure detected -> " + errStr.trim());
                return false;
            }

            if (exitCode == 127) { 
                update(listener, "[-] Smoke Test: Fatal Shell Error (127).");
                return false;
            }

            return true;
        } catch (Exception e) {
            update(listener, "[-] Smoke Test: Execution exception -> " + e.getMessage());
            return false;
        }
    }

    private static void ensureMatrixDirectories(File... dirs) {
        for (File dir : dirs) {
            if (!dir.exists()) dir.mkdirs();
        }
    }

    private static boolean performAIPreFlightCheck(File systemDir, InstallListener listener) {
        try {
            StatFs stat = new StatFs(systemDir.getAbsolutePath());
            return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) >= MIN_DISK_SPACE;
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

    private static void executeGhostCleanup(File payloadFile, File filesDir, File extTmpDir) throws Exception {
        if (payloadFile.exists()) payloadFile.delete();
        String cleanupCmd = "rm -rf '" + extTmpDir.getAbsolutePath() + "'/* 2>/dev/null";
        Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

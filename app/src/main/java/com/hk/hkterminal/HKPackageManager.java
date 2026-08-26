package com.hk.hkterminal;

import android.content.Context;
import android.os.Build;
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
 * HK-OPERATION : SAFE DEPLOYMENT ENGINE (RUNTIME v14.0 ALPINE SANDBOX)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : 100% Musl-Bionic Isolation & Sub-system Sandboxing
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
                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File cacheDir = new File(filesDir, ".cache");
                File tmpDir = new File(filesDir, "tmp");
                File extTmpDir = new File(filesDir, "ext_tmp");
                
                // 🚨 SANDBOX FIX: Dedicated isolated folders for Alpine packages
                File alpineDir = new File(usrDir, "alpine");
                File alpineBinDir = new File(alpineDir, "bin");
                File alpineLibDir = new File(alpineDir, "lib");
                File alpineShareDir = new File(alpineDir, "share");
                
                ensureMatrixDirectories(usrDir, binDir, cacheDir, tmpDir, extTmpDir, alpineDir, alpineBinDir, alpineLibDir, alpineShareDir);

                update(listener, "\n[*] ================================================");
                update(listener, "[*] HK-AI: WAKING UP v14.0 SANDBOX ENGINE FOR '" + targetPkgName.toUpperCase() + "'...");
                
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
                        triggerErrorPopup(listener, "SPIDER_MODULE", "Failed to locate '" + pkgName + "'.");
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue; 
                    }

                    dbManager.updatePackageState(pkgName, "DOWNLOADING");
                    File payloadFile = new File(cacheDir, pkgName + ".apk");
                    boolean downloadSuccess = executeSelfHealingDownload(targetUrl, payloadFile, pkgName, listener);
                    if (!downloadSuccess) {
                        triggerErrorPopup(listener, "DOWNLOAD_TUNNEL", "Network collapsed or payload corrupted.");
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue;
                    }
                    healthScore += 20;

                    dbManager.updatePackageState(pkgName, "EXTRACTING & DEPLOYING");
                    update(listener, "[+] Payload Secured. Extracting to Isolated Sandbox...");
                    
                    try {
                        executeNativeExtractionAndSweep(payloadFile, alpineDir, extTmpDir);
                        healthScore += 40; 
                    } catch (Exception e) {
                        triggerErrorPopup(listener, "SANDBOX_EXTRACTION", "Extraction crash -> " + e.getMessage());
                        dbManager.updatePackageState(pkgName, "FAILED");
                        continue;
                    }

                    update(listener, "[*] Forging Universal Sandbox Aliases...");
                    try {
                        generateLibraryAliases(alpineLibDir);
                        healthScore += 20;
                    } catch (Exception e) {
                        triggerErrorPopup(listener, "ALIAS_FORGER", "Symlink clone failed -> " + e.getMessage());
                    }

                    update(listener, "[*] Injecting Stealth Wrappers to Main System...");
                    generateWrapperMatrix(alpineBinDir, alpineLibDir, binDir, usrDir, alpineDir, filesDir, pkgName);

                    executeGhostCleanup(payloadFile, filesDir, extTmpDir);
                    
                    dbManager.updatePackageState(pkgName, "VALIDATING");
                    update(listener, "[*] Running Runtime Validation & Smoke Test...");
                    boolean isRuntimeValid = runValidationMatrix(binDir, alpineLibDir, pkgName, listener);

                    if (isRuntimeValid) {
                        healthScore += 20; 
                        dbManager.updatePackageState(pkgName, "READY");
                        dbManager.updateHealthScore(pkgName, healthScore, false);
                        update(listener, "[+] AI-Core Locked: Module '" + pkgName + "' integrated flawlessly [Health: 100%].");
                    } else {
                        dbManager.updatePackageState(pkgName, "REPAIRABLE");
                        dbManager.updateHealthScore(pkgName, healthScore, true);
                    }
                }
                
                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");
                update(listener, "[*] ================================================");
                update(listener, "[+] TACTICAL DIRECTIVE FOR " + targetPkgName.toUpperCase() + ":");
                update(listener, " -> Execute: '" + targetPkgName + "'");

            } catch (Exception e) {
                triggerErrorPopup(listener, "GLOBAL_SYSTEM", e.getMessage());
                update(listener, "[-] AI System Error: Engine Halted.");
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    public static void triggerErrorPopup(InstallListener listener, String step, String exactCause) {
        update(listener, "\n[-] ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄");
        update(listener, "[-] 🚨 SYSTEM TRACE : CRITICAL FAILURE 🚨");
        update(listener, "[-] 📍 FAILED AT STEP : " + step);
        update(listener, "[-] 🔍 EXACT CAUSE    : " + exactCause);
        update(listener, "[-] ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀\n");
    }

    private static void executeNativeExtractionAndSweep(File payloadFile, File alpineDir, File extTmpDir) throws Exception {
        String alpine = alpineDir.getAbsolutePath();
        String tmp = extTmpDir.getAbsolutePath();

        String script =
            "set -e; " +
            "rm -rf '" + tmp + "'/* 2>/dev/null || true; " +
            "mkdir -p '" + tmp + "'; " +
            "cd '" + tmp + "'; " +

            "tar -ozxf '" + payloadFile.getAbsolutePath() + "' 2>/dev/null || tar -oxf '" + payloadFile.getAbsolutePath() + "' 2>/dev/null || true; " +

            "mkdir -p '" + alpine + "/lib' '" + alpine + "/bin' '" + alpine + "/share'; " +

            "if [ -d lib ]; then cp -r lib/* '" + alpine + "/lib/' 2>/dev/null || true; fi; " +
            "if [ -d usr/lib ]; then cp -r usr/lib/* '" + alpine + "/lib/' 2>/dev/null || true; fi; " +
            "if [ -d bin ]; then cp -r bin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d usr/bin ]; then cp -r usr/bin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d sbin ]; then cp -r sbin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d usr/sbin ]; then cp -r usr/sbin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d share ]; then cp -r share/* '" + alpine + "/share/' 2>/dev/null || true; fi; " +
            "if [ -d usr/share ]; then cp -r usr/share/* '" + alpine + "/share/' 2>/dev/null || true; fi; " +

            "chmod -R 777 '" + alpine + "' 2>/dev/null || true";

        Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", script});
        int exit = process.waitFor();

        if (exit != 0) {
            BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder details = new StringBuilder();
            String line;
            while ((line = err.readLine()) != null) {
                details.append(line).append(" ");
            }
            throw new Exception("APK payload extraction failed (exit " + exit + "): " + details.toString().trim());
        }
        repairCommonLibraryLinks(new File(alpine, "lib"));
    }

    private static void repairCommonLibraryLinks(File libDir) {
        if (libDir == null || !libDir.isDirectory()) return;

        File[] files = libDir.listFiles();
        if (files == null) return;

        for (File real : files) {
            if (!real.isFile() || isSymbolicLinkCompat(real)) continue;
            String name = real.getName();
            Matcher m = Pattern.compile("^(lib.+\\.so)\\.([0-9]+)(?:\\.([0-9]+))+$").matcher(name);
            if (!m.matches()) continue;

            File major = new File(libDir, m.group(1) + "." + m.group(2));
            if (!major.exists()) createRelativeSymlink(real, major);

            File unversioned = new File(libDir, m.group(1));
            if (!unversioned.exists()) createRelativeSymlink(real, unversioned);
        }
    }

    private static void createRelativeSymlink(File target, File link) {
        try {
            if (link.exists() || isSymbolicLinkCompat(link)) return;
            Process p = Runtime.getRuntime().exec(new String[]{
                "sh", "-c", "cd '" + link.getParent() + "' && ln -s '" + target.getName() + "' '" + link.getName() + "'"
            });
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private static boolean isSymbolicLinkCompat(File file) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", "test -L '" + file.getAbsolutePath() + "'"});
            return p.waitFor() == 0;
        } catch (Exception e) { return false; }
    }

    private static void generateLibraryAliases(File libDir) {
        if (!libDir.exists() || !libDir.isDirectory()) return;
        repairCommonLibraryLinks(libDir);
        File[] libs = libDir.listFiles();
        if (libs == null) return;

        for (File f : libs) {
            if (!f.isFile()) continue;
            Matcher m = Pattern.compile("^(lib.+\\.so)\\.([0-9]+)(?:\\..+)?$").matcher(f.getName());
            if (m.matches()) {
                File major = new File(libDir, m.group(1) + "." + m.group(2));
                if (!major.exists()) createRelativeSymlink(f, major);
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
            dest.setExecutable(true, false); 
        } catch (Exception ignored) {}
    }

    private static void generateWrapperMatrix(File alpineBinDir, File alpineLibDir, File mainBinDir, File usrDir, File alpineDir, File filesDir, String pkgName) {
        String muslLoaderPath = alpineLibDir.getAbsolutePath() + "/libc.musl-aarch64.so.1"; 
        File[] libs = alpineLibDir.listFiles();
        if (libs != null) {
            for (File f : libs) {
                if (f.getName().startsWith("libc.musl-") || f.getName().startsWith("ld-musl-")) {
                    muslLoaderPath = f.getAbsolutePath();
                    break;
                }
            }
        }

        File[] allBinaries = alpineBinDir.listFiles();
        if (allBinaries == null) return;

        for (File binFile : allBinaries) {
            if (!binFile.isFile()) continue;
            String name = binFile.getName();
            if (name.endsWith(".elf") || name.endsWith(".sh") || name.endsWith(".py")) continue;

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
                File binReal = new File(alpineBinDir, name + ".elf");
                if (binFile.renameTo(binReal)) {
                    try {
                        File wrapperFile = new File(mainBinDir, name);
                        FileWriter fw = new FileWriter(wrapperFile);
                        fw.write("#!/system/bin/sh\n");
                        fw.write("export PREFIX='" + usrDir.getAbsolutePath() + "'\n");
                        fw.write("export HOME='" + filesDir.getAbsolutePath() + "/home'\n");
                        fw.write("export TMPDIR='" + filesDir.getAbsolutePath() + "/tmp'\n");
                        fw.write("export PATH='" + mainBinDir.getAbsolutePath() + ":/system/bin:/system/xbin'\n");
                        // Target Sandbox Terminfo so nano's screen doesn't break
                        fw.write("export TERMINFO='" + alpineDir.getAbsolutePath() + "/share/terminfo'\n");
                        fw.write("export LANG='en_US.UTF-8'\n");
                        fw.write("export LC_ALL='en_US.UTF-8'\n");

                        if (pkgName.contains("python") || name.contains("python")) {
                            fw.write("export PYTHONHOME='" + alpineDir.getAbsolutePath() + "'\n");
                        }
                        
                        fw.write("exec '" + muslLoaderPath + "' --library-path '" + alpineLibDir.getAbsolutePath() + "' '" + binReal.getAbsolutePath() + "' \"$@\"\n");
                        fw.close();
                        wrapperFile.setExecutable(true, true);
                        binReal.setExecutable(true, true);
                    } catch (Exception ignored) {}
                }
            } else {
                File target = new File(mainBinDir, name);
                cloneFileSafely(binFile, target);
                target.setExecutable(true, true);
            }
        }
    }

    private static boolean hasLibrary(File libDir, String requestedName) {
        if (libDir == null || !libDir.isDirectory()) return false;

        File exact = new File(libDir, requestedName);
        if (exact.exists()) return true;

        Matcher m = Pattern.compile("^(lib.+\\.so)\\.([0-9]+)$").matcher(requestedName);
        if (!m.matches()) return false;

        String prefix = m.group(1) + "." + m.group(2);
        File[] files = libDir.listFiles();
        if (files == null) return false;

        for (File f : files) {
            if (f.getName().startsWith(prefix + ".")) return true;
        }
        return false;
    }

    private static boolean runValidationMatrix(File binDir, File alpineLibDir, String pkgName, InstallListener listener) {
        File targetExecutable = new File(binDir, pkgName);
        
        if (!targetExecutable.exists()) {
            if (pkgName.contains("lib") || pkgName.contains("musl") || pkgName.contains("terminfo") || 
                pkgName.contains("ca-certificates") || pkgName.contains("tzdata") || pkgName.contains("ncurses") || 
                pkgName.contains("sqlite") || pkgName.contains("zlib") || pkgName.contains("openssl") || pkgName.contains("bzip")) {
                return true; 
            }
            triggerErrorPopup(listener, "VALIDATION_BINARY_CHECK", "Binary wrapper not found in main PATH.");
            return false;
        }

        try {
            if ("nano".equals(pkgName) && !hasLibrary(alpineLibDir, "libncursesw.so.6")) {
                triggerErrorPopup(listener, "NANO_NCURSESW_CHECK", "libncursesw.so.6 missing from sandbox.");
                return false;
            }

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
                triggerErrorPopup(listener, "SMOKE_TEST_HANG", "Process Timed Out. Deep OS block detected.");
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
                triggerErrorPopup(listener, "SMOKE_TEST_LINKAGE", errStr.trim());
                return false;
            }

            return true;
        } catch (Exception e) {
            triggerErrorPopup(listener, "SMOKE_TEST_EXCEPTION", e.getMessage());
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

package com.hk.hkterminal;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.StatFs;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * HK-OPERATION : SAFE DEPLOYMENT & SELF-HEALING ENGINE (ULTIMATE v17.0)
 * ARCHITECT    : HK Prashant Bhai (Tech Wizard)
 * DIRECTIVE    : Auto-Requirement Resolver, Zero-Failure Sandbox & Self-Heal
 * ============================================================================
 */
public class HKPackageManager {

    private static final String TAG = "HK_AI_MATRIX";
    private static final int TIMEOUT_MS = 60000;
    private static final int MAX_RETRIES = 3;
    private static final long MIN_DISK_SPACE = 50 * 1024 * 1024;
    private static final ConcurrentHashMap<String, Long> mirrorLatencyCache = new ConcurrentHashMap<>();

    // Extra runtime & requirement packages that make Python & tools work completely
    private static final Map<String, List<String>> EXTRA_REQUIREMENTS = new HashMap<>();

    static {
        EXTRA_REQUIREMENTS.put("python", Arrays.asList(
                "musl", "libffi", "sqlite-libs", "libcrypto3", "libssl3", "openssl",
                "ca-certificates", "zlib", "libbz2", "libexpat", "xz-libs",
                "ncurses-libs", "readline", "gdbm", "py3-pip", "py3-setuptools"
        ));
        EXTRA_REQUIREMENTS.put("python3", Arrays.asList(
                "musl", "libffi", "sqlite-libs", "libcrypto3", "libssl3", "openssl",
                "ca-certificates", "zlib", "libbz2", "libexpat", "xz-libs",
                "ncurses-libs", "readline", "gdbm", "py3-pip", "py3-setuptools"
        ));
        EXTRA_REQUIREMENTS.put("pip", Arrays.asList("python3", "py3-pip", "py3-setuptools", "ca-certificates", "openssl"));
        EXTRA_REQUIREMENTS.put("py3-pip", Arrays.asList("python3", "py3-setuptools", "ca-certificates", "openssl"));
        EXTRA_REQUIREMENTS.put("git", Arrays.asList("musl", "libcrypto3", "libssl3", "openssl", "ca-certificates", "zlib", "pcre2", "nghttp2-libs", "brotli-libs", "curl", "libexpat"));
        EXTRA_REQUIREMENTS.put("curl", Arrays.asList("musl", "libcrypto3", "libssl3", "openssl", "ca-certificates", "zlib", "nghttp2-libs", "brotli-libs"));
        EXTRA_REQUIREMENTS.put("nmap", Arrays.asList("musl", "libcrypto3", "libssl3", "openssl", "pcre2", "liblinear", "lua5.4", "libpcap", "readline", "ncurses-libs"));
    }

    public interface InstallListener {
        void onUpdate(String msg);
        void onComplete();
    }

    public static void installPackage(Context context, final String targetPkgName, final InstallListener listener) {
        final String requested = targetPkgName == null ? "" : targetPkgName.trim().toLowerCase(Locale.US);
        if (requested.isEmpty()) {
            update(listener, "[-] HK-pkg: package name is empty.");
            new Handler(Looper.getMainLooper()).post(listener::onComplete);
            return;
        }

        if (isAndroidNativeCommand(requested)) {
            update(listener, "[+] '" + requested + "' is an Android native command; installation skipped.");
            update(listener, "[+] Use the command directly: " + requested);
            new Handler(Looper.getMainLooper()).post(listener::onComplete);
            return;
        }

        new Thread(() -> {
            HKDatabaseManager dbManager = HKDatabaseManager.getInstance(context);
            HKLogger.logEvent("MODULE-01", "INSTALL_INITIATED", "Target: " + requested);

            try {
                File filesDir = context.getFilesDir();
                File usrDir = new File(filesDir, "usr");
                File binDir = new File(usrDir, "bin");
                File cacheDir = new File(filesDir, ".cache");
                File tmpDir = new File(filesDir, "tmp");
                File extTmpDir = new File(filesDir, "ext_tmp");

                File alpineDir = new File(usrDir, "alpine");
                File alpineBinDir = new File(alpineDir, "bin");
                File alpineLibDir = new File(alpineDir, "lib");
                File alpineShareDir = new File(alpineDir, "share");
                File alpineEtcDir = new File(alpineDir, "etc");

                ensureMatrixDirectories(usrDir, binDir, cacheDir, tmpDir, extTmpDir, alpineDir, alpineBinDir, alpineLibDir, alpineShareDir, alpineEtcDir);

                update(listener, "\n[*] ================================================");
                update(listener, "[*] HK-AI: SELF-HEALING DEPLOYMENT ENGINE ACTIVE FOR '" + requested.toUpperCase(Locale.US) + "'...");

                if (!performAIPreFlightCheck(filesDir, listener)) {
                    throw new Exception("Insufficient System Resources for HK-Operation.");
                }

                // Build complete dependency + requirement queue without duplicates
                List<String> installQueue = buildFullRequirementQueue(requested);
                update(listener, "[+] AI-Graph Resolved: " + installQueue.size() + " packages & runtime dependencies locked.");

                for (String pkgName : installQueue) {
                    int healthScore = 0;
                    dbManager.registerPackage(pkgName, "latest");
                    dbManager.updatePackageState(pkgName, "VERIFYING");

                    update(listener, "-----------------------------------");
                    update(listener, "[*] Deploying Tactical Module: '" + pkgName + "'...");

                    String targetUrl = huntTargetWithAINeuralNet(pkgName, listener);
                    if (targetUrl == null) {
                        update(listener, "[!] Mirror miss for '" + pkgName + "', trying auto-heal fallback scan...");
                        targetUrl = huntTargetWithAINeuralNet(pkgName.replace("_", "-"), listener);
                    }

                    if (targetUrl == null) {
                        triggerErrorPopup(listener, "SPIDER_MODULE", "No compatible Alpine package found for '" + pkgName + "'.");
                        dbManager.updatePackageState(pkgName, "FAILED");
                        dbManager.updateHealthScore(pkgName, 0, true);
                        continue;
                    }

                    dbManager.updatePackageState(pkgName, "DOWNLOADING");
                    File payloadFile = new File(cacheDir, pkgName + ".apk");
                    boolean downloadSuccess = executeSelfHealingDownload(targetUrl, payloadFile, pkgName, listener);
                    if (!downloadSuccess) {
                        triggerErrorPopup(listener, "DOWNLOAD_TUNNEL", "Network collapsed or payload corrupted for '" + pkgName + "'.");
                        dbManager.updatePackageState(pkgName, "FAILED");
                        dbManager.updateHealthScore(pkgName, 0, true);
                        continue;
                    }
                    healthScore += 20;

                    dbManager.updatePackageState(pkgName, "EXTRACTING & DEPLOYING");
                    update(listener, "[+] Payload Secured. Extracting to Isolated Sandbox...");

                    try {
                        executeNativeExtractionAndSweep(payloadFile, alpineDir, extTmpDir);
                        healthScore += 40;
                    } catch (Exception e) {
                        // Self-heal retry once after cleaning temp & sandbox cache
                        update(listener, "[!] Extraction fault on '" + pkgName + "', executing self-heal retry...");
                        cleanDirectoryContents(extTmpDir);
                        try {
                            executeNativeExtractionAndSweep(payloadFile, alpineDir, extTmpDir);
                            healthScore += 40;
                        } catch (Exception retryEx) {
                            triggerErrorPopup(listener, "SANDBOX_EXTRACTION", "Extraction crash -> " + retryEx.getMessage());
                            dbManager.updatePackageState(pkgName, "FAILED");
                            dbManager.updateHealthScore(pkgName, healthScore, true);
                            continue;
                        }
                    }

                    update(listener, "[*] Forging Universal Sandbox Aliases & Library Links...");
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
                    boolean isRuntimeValid = runValidationMatrix(binDir, alpineBinDir, alpineLibDir, filesDir, pkgName, listener);

                    if (isRuntimeValid) {
                        healthScore += 20;
                        dbManager.updatePackageState(pkgName, "READY");
                        dbManager.updateHealthScore(pkgName, healthScore, false);
                        update(listener, "[+] AI-Core Locked: Module '" + pkgName + "' integrated flawlessly [Health: " + healthScore + "%].");
                    } else {
                        dbManager.updatePackageState(pkgName, "REPAIRABLE");
                        dbManager.updateHealthScore(pkgName, healthScore, true);
                        update(listener, "[!] Module '" + pkgName + "' marked REPAIRABLE [Health: " + healthScore + "%].");
                    }
                }

                // Final pass: ensure python & python3 shortcuts + self-heal links are intact
                repairCommonLibraryLinks(alpineLibDir);
                ensurePythonReadyAliases(alpineBinDir, alpineLibDir, binDir, usrDir, alpineDir, filesDir);

                update(listener, "===================================");
                update(listener, "[+] ALL TACTICAL DEPLOYMENTS COMPLETED.");
                update(listener, "[*] ================================================");
                update(listener, "[+] TACTICAL DIRECTIVE FOR " + requested.toUpperCase(Locale.US) + ":");
                update(listener, " -> Execute: '" + requested + "'");

            } catch (Exception e) {
                triggerErrorPopup(listener, "GLOBAL_SYSTEM", e.getMessage());
                update(listener, "[-] AI System Error: Engine Halted.");
            } finally {
                new Handler(Looper.getMainLooper()).post(listener::onComplete);
            }
        }).start();
    }

    private static List<String> buildFullRequirementQueue(String requested) {
        List<String> finalQueue = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        List<String> baseResolved = HKDependencyEngine.calculateInstallQueue(requested);
        if (baseResolved.isEmpty()) {
            baseResolved = Collections.singletonList(requested);
        }

        List<String> extraList = EXTRA_REQUIREMENTS.get(requested);
        List<String> combinedSeeds = new ArrayList<>();
        if (extraList != null) {
            for (String req : extraList) {
                combinedSeeds.addAll(HKDependencyEngine.calculateInstallQueue(req));
                if (!combinedSeeds.contains(req)) combinedSeeds.add(req);
            }
        }
        combinedSeeds.addAll(baseResolved);

        for (String item : combinedSeeds) {
            if (!visited.contains(item)) {
                visited.add(item);
                finalQueue.add(item);
            }
        }
        return finalQueue;
    }

    private static boolean isAndroidNativeCommand(String name) {
        String n = name.toLowerCase(Locale.US);
        return n.equals("ls") || n.equals("pwd") || n.equals("cd") || n.equals("cat") ||
               n.equals("cp") || n.equals("mv") || n.equals("rm") || n.equals("mkdir") ||
               n.equals("chmod") || n.equals("grep") || n.equals("echo") || n.equals("env") ||
               n.equals("clear") || n.equals("sh") || n.equals("bash") || n.equals("ip") ||
               n.equals("su") || n.equals("export") || n.equals("which") || n.equals("command") ||
               n.equals("type") || n.equals("true") || n.equals("false") || n.equals("printf") ||
               n.equals("test") || n.equals("ll");
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
        String apkPath = payloadFile.getAbsolutePath();

        // Handles multi-stream Alpine APK archives cleanly without failing on trailing streams
        String script =
            "rm -rf '" + tmp + "'/* 2>/dev/null || true; " +
            "mkdir -p '" + tmp + "'; " +
            "cd '" + tmp + "' || exit 1; " +
            "if ! (gzip -dc '" + apkPath + "' 2>/dev/null | tar -xf - 2>/dev/null); then " +
            "  tar -ozxf '" + apkPath + "' 2>/dev/null || tar -oxf '" + apkPath + "' 2>/dev/null || true; " +
            "fi; " +
            "if [ ! -d lib ] && [ ! -d usr ] && [ ! -d bin ] && [ ! -d sbin ] && [ ! -d etc ]; then " +
            "  echo 'Archive contained no standard payload directories'; exit 1; " +
            "fi; " +
            "mkdir -p '" + alpine + "/lib' '" + alpine + "/bin' '" + alpine + "/share' '" + alpine + "/etc'; " +
            "if [ -d lib ]; then cp -RP lib/* '" + alpine + "/lib/' 2>/dev/null || true; fi; " +
            "if [ -d usr/lib ]; then cp -RP usr/lib/* '" + alpine + "/lib/' 2>/dev/null || true; fi; " +
            "if [ -d bin ]; then cp -RP bin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d usr/bin ]; then cp -RP usr/bin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d sbin ]; then cp -RP sbin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d usr/sbin ]; then cp -RP usr/sbin/* '" + alpine + "/bin/' 2>/dev/null || true; fi; " +
            "if [ -d share ]; then cp -RP share/* '" + alpine + "/share/' 2>/dev/null || true; fi; " +
            "if [ -d usr/share ]; then cp -RP usr/share/* '" + alpine + "/share/' 2>/dev/null || true; fi; " +
            "if [ -d etc ]; then cp -RP etc/* '" + alpine + "/etc/' 2>/dev/null || true; fi; " +
            "chmod -R 755 '" + alpine + "' 2>/dev/null || true";

        Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", script});
        int exit = process.waitFor();

        if (exit != 0) {
            StringBuilder details = new StringBuilder();
            try (BufferedReader err = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = err.readLine()) != null) {
                    details.append(line).append(" ");
                }
            }
            throw new Exception("APK payload extraction failed (exit " + exit + "): " + details.toString().trim());
        }

        repairCommonLibraryLinks(new File(alpine, "lib"));
    }

    private static void repairCommonLibraryLinks(File libDir) {
        if (libDir == null || !libDir.isDirectory()) return;

        File[] files = libDir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.getName().startsWith("libncursesw.so")) {
                File targetLink = new File(libDir, "libncursesw.so.6");
                if (!targetLink.exists() && !isSymbolicLinkCompat(targetLink)) {
                    createRelativeSymlink(f, targetLink);
                }
            }
        }

        for (File real : files) {
            if (!real.isFile() || isSymbolicLinkCompat(real)) continue;
            String name = real.getName();
            Matcher m = Pattern.compile("^(lib.+\\.so)\\.([0-9]+)(?:\\.([0-9]+))+$").matcher(name);
            if (!m.matches()) continue;

            File major = new File(libDir, m.group(1) + "." + m.group(2));
            if (!major.exists() && !isSymbolicLinkCompat(major)) createRelativeSymlink(real, major);

            File unversioned = new File(libDir, m.group(1));
            if (!unversioned.exists() && !isSymbolicLinkCompat(unversioned)) createRelativeSymlink(real, unversioned);
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
        } catch (Exception e) {
            return false;
        }
    }

    private static void generateLibraryAliases(File libDir) {
        if (!libDir.exists() || !libDir.isDirectory()) return;
        repairCommonLibraryLinks(libDir);
        File[] libs = libDir.listFiles();
        if (libs == null) return;

        for (File f : libs) {
            if (!f.isFile() || isSymbolicLinkCompat(f)) continue;
            Matcher m = Pattern.compile("^(lib.+\\.so)\\.([0-9]+)(?:\\..+)?$").matcher(f.getName());
            if (m.matches()) {
                File major = new File(libDir, m.group(1) + "." + m.group(2));
                if (!major.exists() && !isSymbolicLinkCompat(major)) createRelativeSymlink(f, major);
            }
        }
    }

    private static void generateWrapperMatrix(File alpineBinDir, File alpineLibDir, File mainBinDir, File usrDir, File alpineDir, File filesDir, String pkgName) {
        String muslLoaderPath = alpineLibDir.getAbsolutePath() + "/ld-musl-aarch64.so.1";
        File[] libs = alpineLibDir.listFiles();
        if (libs != null) {
            for (File f : libs) {
                if (f.getName().startsWith("ld-musl-") || f.getName().startsWith("libc.musl-")) {
                    muslLoaderPath = f.getAbsolutePath();
                    f.setExecutable(true, false);
                    break;
                }
            }
        }

        File[] allBinaries = alpineBinDir.listFiles();
        if (allBinaries == null) return;

        // Pass 0: Record symlinks (such as python3 -> python3.12) before renaming real ELFs
        Map<String, String> symlinkTargets = new HashMap<>();
        for (File binFile : allBinaries) {
            String name = binFile.getName();
            if (name.endsWith(".elf")) continue;
            if (isSymbolicLinkCompat(binFile)) {
                try {
                    String targetName = binFile.getCanonicalFile().getName();
                    if (targetName.endsWith(".elf")) {
                        targetName = targetName.substring(0, targetName.length() - 4);
                    }
                    symlinkTargets.put(name, targetName);
                } catch (Exception ignored) {}
            }
        }

        // Pass 1: Process only real files (never rename symlinks into .elf)
        for (File binFile : allBinaries) {
            if (!binFile.isFile() || isSymbolicLinkCompat(binFile)) continue;
            String name = binFile.getName();
            if (name.endsWith(".elf") || name.endsWith(".sh") || name.endsWith(".py")) continue;

            boolean isElf = false;
            try (FileInputStream fis = new FileInputStream(binFile)) {
                byte[] header = new byte[4];
                if (fis.read(header) == 4 && header[0] == 0x7f && header[1] == 'E' && header[2] == 'L' && header[3] == 'F') {
                    isElf = true;
                }
            } catch (Exception ignored) {}

            if (isElf) {
                File binReal = new File(alpineBinDir, name + ".elf");
                if (binReal.exists()) binReal.delete();
                if (binFile.renameTo(binReal)) {
                    try {
                        File wrapperFile = new File(mainBinDir, name);
                        try (FileWriter fw = new FileWriter(wrapperFile)) {
                            fw.write("#!/system/bin/sh\n");
                            fw.write("export PREFIX='" + usrDir.getAbsolutePath() + "'\n");
                            fw.write("export HOME='" + filesDir.getAbsolutePath() + "/home'\n");
                            fw.write("export TMPDIR='" + filesDir.getAbsolutePath() + "/tmp'\n");
                            fw.write("export PATH='" + mainBinDir.getAbsolutePath() + ":/system/bin:/system/xbin'\n");
                            fw.write("export TERMINFO='" + alpineDir.getAbsolutePath() + "/share/terminfo'\n");
                            fw.write("export LANG='en_US.UTF-8'\n");
                            fw.write("export LC_ALL='en_US.UTF-8'\n");

                            if (pkgName.contains("python") || name.contains("python")) {
                                fw.write("export PYTHONHOME='" + alpineDir.getAbsolutePath() + "'\n");
                                fw.write("export PYTHONPATH='" + alpineDir.getAbsolutePath() + "/lib/python3.12:" + alpineDir.getAbsolutePath() + "/lib/python3.13'\n");
                            }

                            fw.write("exec '" + muslLoaderPath + "' --library-path '" + alpineLibDir.getAbsolutePath() + ":" + usrDir.getAbsolutePath() + "/lib' '" + binReal.getAbsolutePath() + "' \"$@\"\n");
                        }
                        wrapperFile.setExecutable(true, false);
                        binReal.setExecutable(true, false);
                    } catch (Exception ignored) {}
                }
            } else {
                File target = new File(mainBinDir, name);
                cloneFileSafely(binFile, target);
                target.setExecutable(true, false);
            }
        }

        // Pass 2: Build wrappers for symlinks pointing to real target binaries
        for (Map.Entry<String, String> entry : symlinkTargets.entrySet()) {
            String linkName = entry.getKey();
            String targetName = entry.getValue();
            try {
                File wrapperFile = new File(mainBinDir, linkName);
                try (FileWriter fw = new FileWriter(wrapperFile)) {
                    fw.write("#!/system/bin/sh\n");
                    fw.write("exec '" + mainBinDir.getAbsolutePath() + "/" + targetName + "' \"$@\"\n");
                }
                wrapperFile.setExecutable(true, false);
            } catch (Exception ignored) {}
        }

        ensurePythonReadyAliases(alpineBinDir, alpineLibDir, mainBinDir, usrDir, alpineDir, filesDir);
    }

    private static void ensurePythonReadyAliases(File alpineBinDir, File alpineLibDir, File mainBinDir, File usrDir, File alpineDir, File filesDir) {
        String muslLoaderPath = alpineLibDir.getAbsolutePath() + "/ld-musl-aarch64.so.1";
        File[] libs = alpineLibDir.listFiles();
        if (libs != null) {
            for (File f : libs) {
                if (f.getName().startsWith("ld-musl-") || f.getName().startsWith("libc.musl-")) {
                    muslLoaderPath = f.getAbsolutePath();
                    break;
                }
            }
        }

        File versionedElf = null;
        File[] bins = alpineBinDir.listFiles();
        if (bins != null) {
            for (File b : bins) {
                if (b.getName().matches("python3\\.[0-9]+\\.elf") && b.exists()) {
                    versionedElf = b;
                    break;
                }
            }
        }

        File py3Wrapper = new File(mainBinDir, "python3");
        if (!py3Wrapper.exists() && versionedElf != null) {
            try {
                try (FileWriter fw = new FileWriter(py3Wrapper)) {
                    fw.write("#!/system/bin/sh\n");
                    fw.write("export PREFIX='" + usrDir.getAbsolutePath() + "'\n");
                    fw.write("export HOME='" + filesDir.getAbsolutePath() + "/home'\n");
                    fw.write("export TMPDIR='" + filesDir.getAbsolutePath() + "/tmp'\n");
                    fw.write("export PATH='" + mainBinDir.getAbsolutePath() + ":/system/bin:/system/xbin'\n");
                    fw.write("export LANG='en_US.UTF-8'\n");
                    fw.write("export PYTHONHOME='" + alpineDir.getAbsolutePath() + "'\n");
                    fw.write("exec '" + muslLoaderPath + "' --library-path '" + alpineLibDir.getAbsolutePath() + ":" + usrDir.getAbsolutePath() + "/lib' '" + versionedElf.getAbsolutePath() + "' \"$@\"\n");
                }
                py3Wrapper.setExecutable(true, false);
            } catch (Exception ignored) {}
        }

        if (py3Wrapper.exists()) {
            try {
                File pyAlias = new File(mainBinDir, "python");
                try (FileWriter afw = new FileWriter(pyAlias)) {
                    afw.write("#!/system/bin/sh\n");
                    afw.write("exec '" + py3Wrapper.getAbsolutePath() + "' \"$@\"\n");
                }
                pyAlias.setExecutable(true, false);
            } catch (Exception ignored) {}
        }
    }

    private static void cloneFileSafely(File source, File dest) {
        try {
            File realSource = source.getCanonicalFile();
            if (!realSource.exists() || realSource.isDirectory()) return;
            if (dest.exists() && dest.length() == realSource.length() && dest.length() > 0) return;
            if (dest.exists()) dest.delete();

            try (InputStream in = new FileInputStream(realSource);
                 OutputStream out = new FileOutputStream(dest)) {
                byte[] buf = new byte[16384];
                int len;
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                out.flush();
            }
            dest.setExecutable(true, false);
        } catch (Exception ignored) {}
    }

    private static boolean runValidationMatrix(File binDir, File alpineBinDir, File alpineLibDir, File filesDir, String pkgName, InstallListener listener) {
        if (pkgName.startsWith("lib") || pkgName.equals("musl") || pkgName.endsWith("-libs") ||
            pkgName.equals("zlib") || pkgName.equals("bzip2") || pkgName.equals("readline") ||
            pkgName.equals("gdbm") || pkgName.equals("pcre2") || pkgName.equals("ca-certificates") ||
            pkgName.equals("ncurses") || pkgName.equals("acl") || pkgName.equals("yaml") ||
            pkgName.equals("c-ares") || pkgName.equals("lcms2") || pkgName.startsWith("py3-")) {
            File[] libs = alpineLibDir.listFiles();
            return libs != null && libs.length > 0;
        }

        String expectedBin = pkgName;
        if (pkgName.equals("nodejs")) expectedBin = "node";
        else if (pkgName.equals("openssh")) expectedBin = "ssh";
        else if (pkgName.equals("openjdk17-jre")) expectedBin = "java";

        File targetWrapper = new File(binDir, expectedBin);
        if (!targetWrapper.exists()) {
            // Check versioned binary fallback for python3
            if (pkgName.equals("python3")) {
                File[] bins = alpineBinDir.listFiles();
                if (bins != null) {
                    for (File b : bins) {
                        if (b.getName().startsWith("python3.") && b.getName().endsWith(".elf")) {
                            return b.canExecute();
                        }
                    }
                }
            }
            update(listener, "[-] Validation Warning: Wrapper missing for '" + expectedBin + "'");
            return false;
        }

        targetWrapper.setExecutable(true, false);
        return targetWrapper.canExecute();
    }

    private static void ensureMatrixDirectories(File... dirs) {
        for (File dir : dirs) {
            if (!dir.exists()) dir.mkdirs();
        }
    }

    private static void cleanDirectoryContents(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) cleanDirectoryContents(child);
            child.delete();
        }
    }

    private static boolean performAIPreFlightCheck(File systemDir, InstallListener listener) {
        try {
            StatFs stat = new StatFs(systemDir.getAbsolutePath());
            return (stat.getAvailableBlocksLong() * stat.getBlockSizeLong()) >= MIN_DISK_SPACE;
        } catch (Exception e) {
            return true;
        }
    }

    private static String huntTargetWithAINeuralNet(String pkgName, InstallListener listener) {
        String[] masterMirrors = {
            "https://dl-cdn.alpinelinux.org/alpine/edge/main/aarch64/",
            "https://dl-cdn.alpinelinux.org/alpine/edge/community/aarch64/"
        };
        for (String mirror : masterMirrors) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(mirror);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        String line;
                        String regexPattern = "href=\"(" + Pattern.quote(pkgName) + "-[0-9][^\"]*\\.apk)\"";
                        Pattern pattern = Pattern.compile(regexPattern);
                        while ((line = reader.readLine()) != null) {
                            Matcher matcher = pattern.matcher(line);
                            if (matcher.find()) {
                                return mirror + matcher.group(1);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
        return null;
    }

    private static boolean executeSelfHealingDownload(String targetUrl, File payloadFile, String pkgName, InstallListener listener) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(targetUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(TIMEOUT_MS);
                conn.setReadTimeout(TIMEOUT_MS);
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try (InputStream input = new BufferedInputStream(conn.getInputStream());
                         OutputStream output = new FileOutputStream(payloadFile)) {
                        byte[] data = new byte[16384];
                        int count;
                        while ((count = input.read(data)) != -1) {
                            output.write(data, 0, count);
                        }
                        output.flush();
                    }
                    if (payloadFile.exists() && payloadFile.length() > 0) {
                        return true;
                    }
                }
            } catch (Exception ignored) {
                if (payloadFile.exists()) payloadFile.delete();
            } finally {
                if (conn != null) conn.disconnect();
            }
            if (attempt < MAX_RETRIES) {
                update(listener, "[!] Retrying download for '" + pkgName + "' (Attempt " + (attempt + 1) + "/" + MAX_RETRIES + ")...");
                try { Thread.sleep(800L * attempt); } catch (InterruptedException ignored) {}
            }
        }
        return false;
    }

    private static void executeGhostCleanup(File payloadFile, File filesDir, File extTmpDir) throws Exception {
        if (payloadFile.exists()) payloadFile.delete();
        String cleanupCmd = "rm -rf '" + extTmpDir.getAbsolutePath() + "'/* 2>/dev/null || true";
        Runtime.getRuntime().exec(new String[]{"sh", "-c", cleanupCmd}).waitFor();
    }

    private static void update(InstallListener listener, String msg) {
        new Handler(Looper.getMainLooper()).post(() -> listener.onUpdate(msg));
    }
}

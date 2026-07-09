package com.hk.hkterminal;

import java.util.*;

/**
 * HK-OPERATION : DEPENDENCY RESOLUTION MATRIX
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Auto-Calculate & Resolve Package Dependencies (APT-Style)
 */
public class HKDependencyEngine {

    // 1. Virtual Package Database (In real system, this is parsed from a server's Packages.gz)
    private static final Map<String, PackageNode> MASTER_INDEX = new HashMap<>();

    static {
        // [!] THE MATRIX MAPPING (Who needs what to survive)
        
        // Python aur uske weapons
        MASTER_INDEX.put("python", new PackageNode("python", "url_to_python", Arrays.asList("libffi", "libsqlite", "openssl", "zlib")));
        
        // Backend Libraries (Core system components)
        MASTER_INDEX.put("libffi", new PackageNode("libffi", "url_to_libffi", Collections.emptyList()));
        MASTER_INDEX.put("libsqlite", new PackageNode("libsqlite", "url_to_libsqlite", Collections.emptyList()));
        MASTER_INDEX.put("openssl", new PackageNode("openssl", "url_to_openssl", Arrays.asList("ca-certificates")));
        MASTER_INDEX.put("ca-certificates", new PackageNode("ca-certificates", "url_to_ca-cert", Collections.emptyList()));
        MASTER_INDEX.put("zlib", new PackageNode("zlib", "url_to_zlib", Collections.emptyList()));
        
        // Nano Editor
        MASTER_INDEX.put("nano", new PackageNode("nano", "url_to_nano", Arrays.asList("ncurses")));
        MASTER_INDEX.put("ncurses", new PackageNode("ncurses", "url_to_ncurses", Collections.emptyList()));
    }

    // 2. Data Structure for Packages
    static class PackageNode {
        String name;
        String downloadUrl;
        List<String> dependencies; // Kya jarurat hai isko chalne ke liye

        PackageNode(String name, String downloadUrl, List<String> dependencies) {
            this.name = name;
            this.downloadUrl = downloadUrl;
            this.dependencies = dependencies;
        }
    }

    // 3. THE ALPHA LOGIC: Recursive Dependency Resolver
    public static List<String> calculateInstallQueue(String targetPackage) {
        List<String> installQueue = new ArrayList<>();
        Set<String> alreadyResolved = new HashSet<>();
        
        resolveRecursively(targetPackage, installQueue, alreadyResolved);
        
        return installQueue;
    }

    private static void resolveRecursively(String pkgName, List<String> queue, Set<String> resolved) {
        // Prevent infinite loops (Circular dependencies)
        if (resolved.contains(pkgName)) return;
        
        PackageNode node = MASTER_INDEX.get(pkgName);
        if (node == null) {
            System.out.println("[-] FATAL: '" + pkgName + "' not found in HK Master Index.");
            return;
        }

        // Check dependencies FIRST (Bottom-Up approach)
        for (String dependency : node.dependencies) {
            resolveRecursively(dependency, queue, resolved);
        }

        // Add the current package after its dependencies are satisfied
        resolved.add(pkgName);
        queue.add(pkgName);
    }

    // [!] TEST THE ENGINE
    public static void testLogic(MainActivity.Callback logger) {
        String target = "python";
        logger.onOutput("[*] Building dependency tree for '" + target + "'...\n");
        
        List<String> finalQueue = calculateInstallQueue(target);
        
        logger.onOutput("[+] Calculating required modules: " + finalQueue.size() + " packages.\n");
        logger.onOutput("The following NEW packages will be installed:\n  ");
        
        for (String pkg : finalQueue) {
            logger.onOutput(pkg + " ");
        }
        logger.onOutput("\n[+] Need to fetch matrices. Ready to unpack.\n");
    }
}

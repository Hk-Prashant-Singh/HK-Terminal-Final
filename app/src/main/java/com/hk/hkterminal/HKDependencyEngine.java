package com.hk.hkterminal;

import java.util.*;

/**
 * HK-OPERATION : GOD-EYE DEPENDENCY RESOLVER (ALPHA CORE)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : 100% Auto-fetch missing libraries, Zero Manual Setup.
 */
public class HKDependencyEngine {

    private static final Map<String, PackageNode> MASTER_INDEX = new HashMap<>();

    static {
        // [!] THE MATRIX MAPPING (Auto-fetches everything the weapon needs)
        
        // 1. Python & Core Backend
        MASTER_INDEX.put("python", new PackageNode("python3", Arrays.asList("libffi", "sqlite-libs", "openssl", "zlib")));
        MASTER_INDEX.put("python3", new PackageNode("python3", Arrays.asList("libffi", "sqlite-libs", "openssl", "zlib")));
        
        // 2. Missing Core Libraries (Alias fixing)
        MASTER_INDEX.put("libsqlite", new PackageNode("sqlite-libs", Collections.emptyList()));
        MASTER_INDEX.put("sqlite-libs", new PackageNode("sqlite-libs", Collections.emptyList()));
        MASTER_INDEX.put("libffi", new PackageNode("libffi", Collections.emptyList()));
        MASTER_INDEX.put("openssl", new PackageNode("openssl", Arrays.asList("ca-certificates")));
        MASTER_INDEX.put("ca-certificates", new PackageNode("ca-certificates", Collections.emptyList()));
        MASTER_INDEX.put("zlib", new PackageNode("zlib", Collections.emptyList()));
        
        // 3. Network & Spider Tools
        MASTER_INDEX.put("curl", new PackageNode("curl", Arrays.asList("openssl", "zlib", "nghttp2-libs")));
        MASTER_INDEX.put("nghttp2-libs", new PackageNode("nghttp2-libs", Collections.emptyList()));
        MASTER_INDEX.put("wget", new PackageNode("wget", Arrays.asList("openssl", "zlib", "pcre")));
        MASTER_INDEX.put("pcre", new PackageNode("pcre", Collections.emptyList()));
        
        // 4. Text Editors & System Utilities
        MASTER_INDEX.put("nano", new PackageNode("nano", Arrays.asList("ncurses", "ncurses-libs")));
        MASTER_INDEX.put("tar", new PackageNode("tar", Arrays.asList("acl", "libacl")));
        MASTER_INDEX.put("acl", new PackageNode("acl", Collections.emptyList()));
        MASTER_INDEX.put("libacl", new PackageNode("libacl", Collections.emptyList()));

        // 5. Visuals & Animations (THE 'sl' TRAIN FIX)
        MASTER_INDEX.put("sl", new PackageNode("sl", Arrays.asList("ncurses-libs", "ncurses")));
        MASTER_INDEX.put("ncurses", new PackageNode("ncurses", Arrays.asList("ncurses-libs")));
        MASTER_INDEX.put("ncurses-libs", new PackageNode("ncurses-libs", Collections.emptyList()));
    }

    static class PackageNode {
        String realName;
        List<String> dependencies;

        PackageNode(String realName, List<String> dependencies) {
            this.realName = realName;
            this.dependencies = dependencies;
        }
    }

    /**
     * Calculates the full bottom-up installation queue.
     * Ensures libraries are installed BEFORE the main weapon.
     */
    public static List<String> calculateInstallQueue(String targetPackage) {
        List<String> installQueue = new ArrayList<>();
        Set<String> alreadyResolved = new HashSet<>();
        
        resolveRecursively(targetPackage, installQueue, alreadyResolved);
        
        return installQueue;
    }

    private static void resolveRecursively(String pkgAlias, List<String> queue, Set<String> resolved) {
        if (resolved.contains(pkgAlias)) return;
        resolved.add(pkgAlias);
        
        PackageNode node = MASTER_INDEX.get(pkgAlias);
        String actualSearchName = (node != null) ? node.realName : pkgAlias;

        if (node != null) {
            // Deep Matrix Dive: Resolve dependencies first
            for (String dependency : node.dependencies) {
                resolveRecursively(dependency, queue, resolved);
            }
        }
        
        // Add the real targeted name to the queue (Avoids duplicates)
        if (!queue.contains(actualSearchName)) {
            queue.add(actualSearchName);
        }
    }
}

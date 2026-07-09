package com.hk.hkterminal;

import java.util.*;

/**
 * HK-OPERATION : GOD-EYE DEPENDENCY RESOLVER (FINAL MEGA MATRIX)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : 100% Auto-fetch missing libraries, Java, Cloud, Node, Python & PIP Injection.
 */
public class HKDependencyEngine {

    private static final Map<String, PackageNode> MASTER_INDEX = new HashMap<>();

    static {
        // [!] THE EXPANDED HK-ARSENAL MAPPING 
        
        // 0. THE GOD ENGINE (Core Linker for all native executions)
        MASTER_INDEX.put("musl", new PackageNode("musl", Collections.emptyList()));
        MASTER_INDEX.put("libc", new PackageNode("musl", Collections.emptyList())); 

        // 1. Python, PIP & Core Backend 
        MASTER_INDEX.put("python", new PackageNode("python3", Arrays.asList("musl", "libffi", "sqlite-libs", "openssl", "zlib", "bzip2", "readline", "gdbm")));
        MASTER_INDEX.put("python3", new PackageNode("python3", Arrays.asList("musl", "libffi", "sqlite-libs", "openssl", "zlib", "bzip2", "readline", "gdbm")));
        MASTER_INDEX.put("pip", new PackageNode("py3-pip", Arrays.asList("python3", "py3-setuptools")));
        MASTER_INDEX.put("py3-pip", new PackageNode("py3-pip", Arrays.asList("python3", "py3-setuptools")));
        MASTER_INDEX.put("py3-setuptools", new PackageNode("py3-setuptools", Collections.emptyList()));
        
        // 2. Missing Core Libraries & Aliases
        MASTER_INDEX.put("libsqlite", new PackageNode("sqlite-libs", Collections.emptyList()));
        MASTER_INDEX.put("sqlite-libs", new PackageNode("sqlite-libs", Collections.emptyList()));
        MASTER_INDEX.put("libffi", new PackageNode("libffi", Collections.emptyList()));
        MASTER_INDEX.put("openssl", new PackageNode("openssl", Arrays.asList("musl", "ca-certificates")));
        MASTER_INDEX.put("ca-certificates", new PackageNode("ca-certificates", Collections.emptyList()));
        MASTER_INDEX.put("zlib", new PackageNode("zlib", Collections.emptyList()));
        MASTER_INDEX.put("bzip2", new PackageNode("bzip2", Collections.emptyList()));
        MASTER_INDEX.put("readline", new PackageNode("readline", Arrays.asList("ncurses-libs")));
        MASTER_INDEX.put("gdbm", new PackageNode("gdbm", Collections.emptyList()));
        MASTER_INDEX.put("libxml2", new PackageNode("libxml2", Arrays.asList("zlib", "xz-libs")));
        MASTER_INDEX.put("xz-libs", new PackageNode("xz-libs", Collections.emptyList()));
        
        // 3. Network, Spider & Penetration Tools
        MASTER_INDEX.put("curl", new PackageNode("curl", Arrays.asList("musl", "openssl", "zlib", "nghttp2-libs", "brotli-libs")));
        MASTER_INDEX.put("nghttp2-libs", new PackageNode("nghttp2-libs", Collections.emptyList()));
        MASTER_INDEX.put("brotli-libs", new PackageNode("brotli-libs", Collections.emptyList()));
        MASTER_INDEX.put("wget", new PackageNode("wget", Arrays.asList("musl", "openssl", "zlib", "pcre2")));
        MASTER_INDEX.put("pcre2", new PackageNode("pcre2", Collections.emptyList()));
        MASTER_INDEX.put("nmap", new PackageNode("nmap", Arrays.asList("musl", "openssl", "pcre", "liblinear", "lua5.4", "libpcap")));
        MASTER_INDEX.put("libpcap", new PackageNode("libpcap", Collections.emptyList()));
        MASTER_INDEX.put("liblinear", new PackageNode("liblinear", Collections.emptyList()));
        MASTER_INDEX.put("lua5.4", new PackageNode("lua5.4", Arrays.asList("musl", "readline")));
        
        // 4. Version Control & SSH
        MASTER_INDEX.put("git", new PackageNode("git", Arrays.asList("musl", "openssl", "zlib", "pcre2", "curl")));
        MASTER_INDEX.put("openssh", new PackageNode("openssh", Arrays.asList("musl", "openssl", "zlib", "ldns", "krb5-libs")));
        MASTER_INDEX.put("ssh", new PackageNode("openssh", Arrays.asList("musl", "openssl", "zlib", "ldns", "krb5-libs"))); 
        MASTER_INDEX.put("ldns", new PackageNode("ldns", Arrays.asList("openssl")));
        MASTER_INDEX.put("krb5-libs", new PackageNode("krb5-libs", Arrays.asList("openssl")));

        // 5. Text Editors & System Utilities
        MASTER_INDEX.put("nano", new PackageNode("nano", Arrays.asList("musl", "ncurses", "ncurses-libs", "libmagic")));
        MASTER_INDEX.put("vim", new PackageNode("vim", Arrays.asList("musl", "ncurses-libs", "acl", "libintl")));
        MASTER_INDEX.put("libmagic", new PackageNode("libmagic", Arrays.asList("zlib")));
        MASTER_INDEX.put("tar", new PackageNode("tar", Arrays.asList("musl", "acl", "libacl")));
        MASTER_INDEX.put("acl", new PackageNode("acl", Collections.emptyList()));
        MASTER_INDEX.put("libacl", new PackageNode("libacl", Collections.emptyList()));
        MASTER_INDEX.put("bash", new PackageNode("bash", Arrays.asList("musl", "readline", "ncurses-libs")));

        // 6. Visuals & Animations
        MASTER_INDEX.put("sl", new PackageNode("sl", Arrays.asList("musl", "ncurses-libs", "ncurses")));
        MASTER_INDEX.put("ncurses", new PackageNode("ncurses", Arrays.asList("ncurses-libs")));
        MASTER_INDEX.put("ncurses-libs", new PackageNode("ncurses-libs", Collections.emptyList()));
        
        // [!] 7. HEAVY DEVELOPMENT & CLOUD WEAPONS (JAVA, NODE, GO, PHP)
        MASTER_INDEX.put("java", new PackageNode("openjdk17-jre", Arrays.asList("musl", "zlib", "libjpeg-turbo", "lcms2")));
        MASTER_INDEX.put("openjdk17-jre", new PackageNode("openjdk17-jre", Arrays.asList("musl", "zlib", "libjpeg-turbo", "lcms2")));
        MASTER_INDEX.put("libjpeg-turbo", new PackageNode("libjpeg-turbo", Collections.emptyList()));
        MASTER_INDEX.put("lcms2", new PackageNode("lcms2", Collections.emptyList()));
        
        MASTER_INDEX.put("node", new PackageNode("nodejs", Arrays.asList("musl", "openssl", "zlib", "brotli-libs", "c-ares", "libuv")));
        MASTER_INDEX.put("nodejs", new PackageNode("nodejs", Arrays.asList("musl", "openssl", "zlib", "brotli-libs", "c-ares", "libuv")));
        MASTER_INDEX.put("c-ares", new PackageNode("c-ares", Collections.emptyList()));
        MASTER_INDEX.put("libuv", new PackageNode("libuv", Collections.emptyList()));
        
        MASTER_INDEX.put("go", new PackageNode("go", Arrays.asList("musl", "bash")));
        MASTER_INDEX.put("golang", new PackageNode("go", Arrays.asList("musl", "bash")));
        
        MASTER_INDEX.put("php", new PackageNode("php", Arrays.asList("musl", "openssl", "zlib", "libxml2", "sqlite-libs", "curl", "readline")));
        MASTER_INDEX.put("ruby", new PackageNode("ruby", Arrays.asList("musl", "openssl", "zlib", "libffi", "readline", "yaml")));
        MASTER_INDEX.put("yaml", new PackageNode("yaml", Collections.emptyList()));
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
     * HK-MATRIX LOGIC: Calculates the full bottom-up installation queue.
     * Ensures libraries are integrated BEFORE the main weapon.
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
            for (String dependency : node.dependencies) {
                resolveRecursively(dependency, queue, resolved);
            }
        }
        
        if (!queue.contains(actualSearchName)) {
            queue.add(actualSearchName);
        }
    }
}

package com.hk.hkterminal;

import android.content.Context;
import android.os.Environment;
import android.util.Log;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * HK-OPERATION : ALPHA SILENT ROUTER & CORE KERNEL
 * IDENTITY     : HK Prashant Bhai (Tech Wizard)
 * DIRECTIVE    : 15-Second Access Engine, Local Storage Unpacker & Symlink Forger
 */
public class TerminalEngine {

    private static ServerSocket amSocketServer;
    private static Thread amSocketThread;
    
    public static final String DATA_DIR = "/data/data/com.hk.hkterminal/files";
    public static final String HOME_PATH = DATA_DIR + "/home";
    public static final String PREFIX_PATH = DATA_DIR + "/usr";
    public static final String BIN_PATH = PREFIX_PATH + "/bin";
    public static final String LIB_PATH = PREFIX_PATH + "/lib";

    private static Process persistentShell;
    private static DataOutputStream shellInput;

    public static void startAmSocketServer() {
        if (amSocketThread != null && amSocketThread.isAlive()) return;
        amSocketThread = new Thread(() -> {
            try {
                amSocketServer = new ServerSocket(8080);
                Log.i("HK_SOCKET", "[+] Alpha Socket Matrix Online on Port 8080");
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = amSocketServer.accept();
                    new Thread(new AmSocketClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                MainActivity.logError("AM_SOCKET", "[-] Error igniting AmSocketServer", e);
            }
        });
        amSocketThread.start();
    }

    public static void stopAmSocketServer() {
        if (amSocketThread != null) {
            amSocketThread.interrupt();
            if (amSocketServer != null && !amSocketServer.isClosed()) {
                try { 
                    amSocketServer.close(); 
                    Log.i("HK_SOCKET", "[*] Alpha Socket Matrix Offline");
                } catch (IOException e) {
                    Log.e("HK_SOCKET", "[-] Port Closure Failed", e);
                }
            }
        }
    }

    private static class AmSocketClientHandler implements Runnable {
        private Socket clientSocket;
        public AmSocketClientHandler(Socket socket) { this.clientSocket = socket; }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String msg;
                while ((msg = in.readLine()) != null) {
                    Log.i("HK_SOCKET", "[>] Intercepted payload: " + msg);
                    TerminalEngine.run(msg); 
                }
            } catch (IOException e) {
                Log.e("HK_SOCKET", "[-] Client Connection Dropped", e);
            }
        }
    }

    private static boolean isBootstrapInstalled() {
        return new File(BIN_PATH, "bash").exists();
    }

    private static void extractBootstrapMatrix(Context context, MainActivity.Callback cb) {
        try {
            if (cb != null) cb.onOutput("[*] HK-BOOTSTRAP: Scanning Device Downloads for Matrix Payload...\n");
            
            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File zipFile = new File(downloadDir, "bootstrap.zip");
            
            if (!zipFile.exists()) {
                zipFile = new File("/sdcard/Download/bootstrap.zip");
            }

            if (!zipFile.exists()) {
                if (cb != null) cb.onOutput("[-] HK-BOOTSTRAP ERROR: 'bootstrap.zip' not found in Download folder!\n");
                if (cb != null) cb.onOutput("[+] DIRECTIVE: Please place 'bootstrap.zip' inside your phone's Download folder and restart.\n");
                return; 
            }

            if (cb != null) cb.onOutput("[+] HK-BOOTSTRAP: Payload Secured! Unpacking Native Matrix...\n");

            InputStream is = new FileInputStream(zipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[16384];

            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(PREFIX_PATH, ze.getName());
                if (ze.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    FileOutputStream fout = new FileOutputStream(file);
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                    fout.close();
                }
            }
            zis.close();
            is.close();

            if (cb != null) cb.onOutput("[*] HK-BOOTSTRAP: Restoring Matrix Shortcuts (Symlinks)...\n");
            File symlinkFile = new File(PREFIX_PATH, "SYMLINKS.txt");
            if (symlinkFile.exists()) {
                BufferedReader br = new BufferedReader(new FileReader(symlinkFile));
                String linkLine;
                while ((linkLine = br.readLine()) != null) {
                    if (linkLine.contains("←")) {
                        String[] parts = linkLine.split("←");
                        if (parts.length == 2) {
                            String target = parts[0].replace("com.termux", "com.hk.hkterminal");
                            String linkPath = parts[1].replaceFirst("^\\./", PREFIX_PATH + "/");
                            
                            File linkFile = new File(linkPath);
                            if (linkFile.exists()) linkFile.delete(); 
                            
                            try {
                                android.system.Os.symlink(target, linkFile.getAbsolutePath());
                            } catch (Exception ignored) {}
                        }
                    }
                }
                br.close();
                symlinkFile.delete(); 
            }

            Runtime.getRuntime().exec(new String[]{"sh", "-c", "chmod -R 777 " + BIN_PATH + " " + LIB_PATH}).waitFor();
            
            if (cb != null) cb.onOutput("[+] HK-BOOTSTRAP: Matrix integrated successfully. Booting Core...\n");
        } catch (Exception e) {
            if (cb != null) cb.onOutput("[-] HK-BOOTSTRAP ERROR: " + e.getMessage() + "\n");
        }
    }

    public static void igniteEngine(Context context, final MainActivity.Callback cb) {
        if (persistentShell != null) return; 

        new Thread(() -> {
            try {
                File homeDir = new File(HOME_PATH);
                if (!homeDir.exists()) homeDir.mkdirs();
                File usrDir = new File(PREFIX_PATH);
                if (!usrDir.exists()) usrDir.mkdirs();
                File binDir = new File(BIN_PATH);
                if (!binDir.exists()) binDir.mkdirs();
                File libDir = new File(LIB_PATH);
                if (!libDir.exists()) libDir.mkdirs();

                if (!isBootstrapInstalled()) {
                    extractBootstrapMatrix(context, cb);
                }

                File bashTarget = new File(BIN_PATH, "bash");
                String shellToExecute = bashTarget.exists() ? bashTarget.getAbsolutePath() : "sh";

                ProcessBuilder pb = new ProcessBuilder(shellToExecute);
                pb.directory(homeDir);
                
                pb.environment().put("HOME", HOME_PATH);
                pb.environment().put("PREFIX", PREFIX_PATH);
                pb.environment().put("PATH", BIN_PATH + ":" + BIN_PATH + "/applets:/system/bin:/system/xbin");
                
                // 🚨 MASTER FIX: Added LIB_PATH back so bash can find libandroid-support.so
                pb.environment().put("LD_LIBRARY_PATH", LIB_PATH + ":/system/lib64:/system/lib");
                
                pb.environment().put("TERM", "xterm-256color");
                pb.environment().put("LANG", "en_US.UTF-8");
                
                pb.redirectErrorStream(true);

                persistentShell = pb.start();
                shellInput = new DataOutputStream(persistentShell.getOutputStream());

                BufferedReader reader = new BufferedReader(new InputStreamReader(persistentShell.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cb != null) cb.onOutput(line);
                }

                persistentShell.waitFor();
                persistentShell = null;
                if (cb != null) cb.onOutput("\n[!] HK-ENGINE TERMINATED BY OS KERNEL\n");

            } catch (Exception e) {
                if (cb != null) cb.onOutput("\n[-] HK_CORE_FATAL_ERROR: " + e.getMessage() + "\n");
            }
        }).start();
    }

    public static void run(final String cmd) {
        if (shellInput == null) return;
        new Thread(() -> {
            try {
                shellInput.writeBytes(cmd + "\n");
                shellInput.flush();
            } catch (IOException e) {
                Log.e("HK_EXEC", "[-] Command Injection Failed: Tunnel collapsed", e);
            }
        }).start();
    }
}

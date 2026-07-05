package com.hk.hkterminal;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HK-OPERATION : ALPHA SILENT ROUTER & CORE KERNEL
 * IDENTITY     : HK Prashant Bhai (Tech Wizard)
 * DIRECTIVE    : 15-Second Access Engine & Digital Guardian Proxy
 */
public class TerminalEngine {

    private static ServerSocket amSocketServer;
    private static Thread amSocketThread;
    
    // HK-Operation Core Paths (System DNA Matrix)
    public static final String DATA_DIR = "/data/data/com.hk.hkterminal/files";
    public static final String HOME_PATH = DATA_DIR + "/home";
    public static final String PREFIX_PATH = DATA_DIR + "/usr";
    public static final String BIN_PATH = PREFIX_PATH + "/bin";
    public static final String LIB_PATH = PREFIX_PATH + "/lib";

    // Persistent Shell Vectors (The Ghost in the Machine)
    private static Process persistentShell;
    private static DataOutputStream shellInput;

    // 1. ADVANCED SOCKET IPC: Bind external triggers to the core engine
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

    // Upgraded Handler: Now routes incoming socket traffic directly into the living shell
    private static class AmSocketClientHandler implements Runnable {
        private Socket clientSocket;
        public AmSocketClientHandler(Socket socket) { this.clientSocket = socket; }
        
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String msg;
                while ((msg = in.readLine()) != null) {
                    // Injecting intercepted network strings straight to the execution engine
                    Log.i("HK_SOCKET", "[>] Intercepted payload: " + msg);
                    TerminalEngine.run(msg); 
                }
            } catch (IOException e) {
                Log.e("HK_SOCKET", "[-] Client Connection Dropped", e);
            }
        }
    }

    // 2. ENGINE IGNITION: Start the Stealth Persistent Shell
    public static void igniteEngine(final MainActivity.Callback cb) {
        if (persistentShell != null) return; // Core is already burning

        new Thread(() -> {
            try {
                // Foundation Verification (Silent Directory Generation)
                File homeDir = new File(HOME_PATH);
                if (!homeDir.exists()) homeDir.mkdirs();
                File usrDir = new File(PREFIX_PATH);
                if (!usrDir.exists()) usrDir.mkdirs();
                File binDir = new File(BIN_PATH);
                if (!binDir.exists()) binDir.mkdirs();
                File libDir = new File(LIB_PATH);
                if (!libDir.exists()) libDir.mkdirs();

                // Build the Core Shell Matrix (Stealth Mode)
                ProcessBuilder pb = new ProcessBuilder("sh");
                pb.directory(homeDir);
                
                // Injecting HK-Terminal DNA with full environment control
                pb.environment().put("HOME", HOME_PATH);
                pb.environment().put("PREFIX", PREFIX_PATH);
                pb.environment().put("PATH", BIN_PATH + ":" + BIN_PATH + "/applets:/system/bin:/system/xbin");
                pb.environment().put("LD_LIBRARY_PATH", LIB_PATH);
                pb.environment().put("TERM", "xterm-256color");
                pb.environment().put("LANG", "en_US.UTF-8");
                
                pb.redirectErrorStream(true); // Catch all fatal and standard outputs in one stream

                persistentShell = pb.start();
                shellInput = new DataOutputStream(persistentShell.getOutputStream());

                // Continuous Output Reader (The Terminal's Eyes - Zero Lag)
                BufferedReader reader = new BufferedReader(new InputStreamReader(persistentShell.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cb != null) cb.onOutput(line);
                }

                // If the system guardian kills the shell
                persistentShell.waitFor();
                persistentShell = null;
                if (cb != null) cb.onOutput("\n[!] HK-ENGINE TERMINATED BY OS KERNEL\n");

            } catch (Exception e) {
                if (cb != null) cb.onOutput("\n[-] HK_CORE_FATAL_ERROR: " + e.getMessage() + "\n");
            }
        }).start();
    }

    // 3. COMMAND INJECTION: Direct raw command execution payload
    public static void run(final String cmd) {
        if (shellInput == null) return;
        new Thread(() -> {
            try {
                // Fire directly into the living process without UI interference
                shellInput.writeBytes(cmd + "\n");
                shellInput.flush();
            } catch (IOException e) {
                Log.e("HK_EXEC", "[-] Command Injection Failed: Tunnel collapsed", e);
            }
        }).start();
    }
}

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

public class TerminalEngine {

    private static ServerSocket amSocketServer;
    private static Thread amSocketThread;
    
    // HK-Operation Core Paths (System DNA)
    public static final String DATA_DIR = "/data/data/com.hk.hkterminal/files";
    public static final String HOME_PATH = DATA_DIR + "/home";
    public static final String PREFIX_PATH = DATA_DIR + "/usr";
    public static final String BIN_PATH = PREFIX_PATH + "/bin";
    public static final String LIB_PATH = PREFIX_PATH + "/lib";

    // Persistent Shell Vectors (The Ghost in the Machine)
    private static Process persistentShell;
    private static DataOutputStream shellInput;

    public static void startAmSocketServer() {
        if (amSocketThread != null && amSocketThread.isAlive()) return;
        amSocketThread = new Thread(() -> {
            try {
                amSocketServer = new ServerSocket(8080);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = amSocketServer.accept();
                    new Thread(new AmSocketClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                MainActivity.logError("AM_SOCKET", "Error starting AmSocketServer", e);
            }
        });
        amSocketThread.start();
    }

    public static void stopAmSocketServer() {
        if (amSocketThread != null) {
            amSocketThread.interrupt();
            if (amSocketServer != null && !amSocketServer.isClosed()) {
                try { amSocketServer.close(); } catch (IOException e) {}
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
                while ((msg = in.readLine()) != null) {}
            } catch (IOException e) {}
        }
    }

    // 1. ENGINE IGNITION: Start the Persistent Shell
    // (Call this ONCE in MainActivity onCreate)
    public static void igniteEngine(final MainActivity.Callback cb) {
        if (persistentShell != null) return; // Engine is already running

        new Thread(() -> {
            try {
                // Foundation Verification
                File homeDir = new File(HOME_PATH);
                if (!homeDir.exists()) homeDir.mkdirs();
                File usrDir = new File(PREFIX_PATH);
                if (!usrDir.exists()) usrDir.mkdirs();

                // Build the Core Shell (Non-Root by default)
                ProcessBuilder pb = new ProcessBuilder("sh");
                pb.directory(homeDir);
                
                // Injecting HK-Terminal DNA
                pb.environment().put("HOME", HOME_PATH);
                pb.environment().put("PREFIX", PREFIX_PATH);
                pb.environment().put("PATH", BIN_PATH + ":" + BIN_PATH + "/applets:/system/bin:/system/xbin");
                pb.environment().put("LD_LIBRARY_PATH", LIB_PATH);
                pb.environment().put("TERM", "xterm-256color");
                pb.environment().put("LANG", "en_US.UTF-8");
                
                pb.redirectErrorStream(true); // Catch all errors in the same stream

                persistentShell = pb.start();
                shellInput = new DataOutputStream(persistentShell.getOutputStream());

                // Continuous Output Reader (The Terminal's Eyes)
                BufferedReader reader = new BufferedReader(new InputStreamReader(persistentShell.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    if (cb != null) cb.onOutput(line);
                }

                // If the shell gets killed
                persistentShell.waitFor();
                persistentShell = null;
                if (cb != null) cb.onOutput("\n[HK-ENGINE TERMINATED]\n");

            } catch (Exception e) {
                if (cb != null) cb.onOutput("\nHK_CORE_ERROR: " + e.getMessage() + "\n");
            }
        }).start();
    }

    // 2. COMMAND INJECTION: Fire commands into the living shell
    public static void run(final String cmd) {
        if (shellInput == null) return;
        new Thread(() -> {
            try {
                shellInput.writeBytes(cmd + "\n");
                shellInput.flush();
            } catch (IOException e) {
                Log.e("HK_EXEC", "Command Injection Failed", e);
            }
        }).start();
    }
}

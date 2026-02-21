package com.hk.hkterminal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket; // For AmSocketServer
import java.net.Socket;     // For AmSocketServer
import java.security.MessageDigest; // For SHA-256 Validation
import java.security.NoSuchAlgorithmException;

import android.util.Log;

public class TerminalEngine {

    // SYSTEM CORE: JNI Bridge Placeholder (Direct communication with Linux kernel via /dev/ptmx)
    // Actual JNI implementation would be in C/C++ native code
    public static void openPtmx() {
        Log.d("PS_HACKER_JNI", "Opening /dev/ptmx via JNI (Placeholder)");
        // Native call here: e.g., native_open_ptmx();
    }

    // SYSTEM CORE: AmSocketServer (AF_UNIX socket for receiving activity manager commands)
    private static ServerSocket amSocketServer;
    private static Thread amSocketThread;

    public static void startAmSocketServer() {
        if (amSocketThread != null && amSocketThread.isAlive()) {
            Log.d("PS_HACKER_SOCKET", "AmSocketServer already running.");
            return;
        }
        amSocketThread = new Thread(() -> {
            try {
                // Using a simple TCP socket for demonstration; AF_UNIX would require native code or specific libraries
                amSocketServer = new ServerSocket(8080); // Example port
                Log.d("PS_HACKER_SOCKET", "AmSocketServer started on port 8080.");
                while (!Thread.currentThread().isInterrupted()) {
                    Socket clientSocket = amSocketServer.accept();
                    Log.d("PS_HACKER_SOCKET", "Client connected to AmSocketServer.");
                    new Thread(new AmSocketClientHandler(clientSocket)).start();
                }
            } catch (IOException e) {
                MainActivity.logError("AM_SOCKET", "Error starting AmSocketServer", e);
            } finally {
                if (amSocketServer != null && !amSocketServer.isClosed()) {
                    try { amSocketServer.close(); } catch (IOException e) { /* ignore */ }
                }
            }
        });
        amSocketThread.start();
    }

    public static void stopAmSocketServer() {
        if (amSocketThread != null) {
            amSocketThread.interrupt();
            if (amSocketServer != null && !amSocketServer.isClosed()) {
                try { amSocketServer.close(); } catch (IOException e) { /* ignore */ }
            }
            Log.d("PS_HACKER_SOCKET", "AmSocketServer stopped.");
        }
    }

    private static class AmSocketClientHandler implements Runnable {
        private Socket clientSocket;
        public AmSocketClientHandler(Socket socket) { this.clientSocket = socket; }
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String clientMessage;
                while ((clientMessage = in.readLine()) != null) {
                    Log.d("PS_HACKER_SOCKET", "Received: " + clientMessage);
                    // Process activity manager commands here (e.g., "start_activity com.example.app")
                    // This would typically involve using reflection or explicit Android API calls
                }
            } catch (IOException e) {
                MainActivity.logError("AM_SOCKET_HANDLER", "Error handling client connection", e);
            } finally {
                try { clientSocket.close(); } catch (IOException e) { /* ignore */ }
            }
        }
    }

    // ROBUSTNESS: SystemEventReceiver (Requires BroadcastReceiver implementation)
    // This would be a separate class extending BroadcastReceiver
    // public class SystemEventReceiver extends BroadcastReceiver { ... }

    // ROBUSTNESS: ActivityUtils (Fail-safe activity launching)
    public static void launchActivitySafely(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            MainActivity.logError("ACTIVITY_UTIL", "Failed to launch activity: " + intent.toString(), e);
            Toast.makeText(context, "Error launching activity!", Toast.LENGTH_SHORT).show();
        }
    }

    // DATA & SECURITY: SHA-256 Validation for bootstrap and external binaries.
    public static String calculateSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            fis.close();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest.digest()) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            MainActivity.logError("SHA256", "Error calculating SHA-256", e);
            return null;
        }
    }

    // Core Shell Execution
    public static void run(final String cmd, final MainActivity.Callback cb) {
        new Thread(() -> {
            try {
                // Check if device is rooted, then use 'su'
                Process p = Runtime.getRuntime().exec(isRooted() ? "su" : "sh");
                OutputStream os = p.getOutputStream();
                os.write((cmd + "\nexit\n").getBytes());
                os.flush(); // Crucial for sending commands

                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String l;
                while ((l = r.readLine()) != null) {
                    if (cb != null) cb.onOutput(l);
                }
                p.waitFor();
            } catch (Exception e) {
                if (cb != null) cb.onOutput("Error: " + e.getMessage());
            }
        }).start();
    }

    // Helper for root check (can be expanded)
    public static boolean isRooted() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su", "/vendor/xbin/su"};
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }
                 }

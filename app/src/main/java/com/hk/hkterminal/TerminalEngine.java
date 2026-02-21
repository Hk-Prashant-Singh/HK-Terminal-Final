package com.hk.hkterminal;

import android.content.Context; // FIXED
import android.content.Intent;  // FIXED
import android.widget.Toast;    // FIXED
import android.util.Log;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * HK Terminal Engine - Professional Core logic [cite: 2026-01-20]
 * Designed by: Tech Wizard (Prashant Bhai)
 */
public class TerminalEngine {

    // SYSTEM CORE: JNI Bridge Placeholder
    public static void openPtmx() {
        Log.d("PS_HACKER_JNI", "Opening /dev/ptmx via JNI (Placeholder)");
    }

    // SYSTEM CORE: AmSocketServer
    private static ServerSocket amSocketServer;
    private static Thread amSocketThread;

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

    private static class AmSocketClientHandler implements Runnable {
        private Socket clientSocket;
        public AmSocketClientHandler(Socket socket) { this.clientSocket = socket; }
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                String msg;
                while ((msg = in.readLine()) != null) {
                    Log.d("PS_HACKER_SOCKET", "Received: " + msg);
                }
            } catch (IOException e) {
                MainActivity.logError("AM_SOCKET_HANDLER", "Error handling client", e);
            }
        }
    }

    // ROBUSTNESS: ActivityUtils (Fail-safe activity launching)
    public static void launchActivitySafely(Context context, Intent intent) {
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            MainActivity.logError("ACTIVITY_UTIL", "Failed to launch activity", e);
            Toast.makeText(context, "Error launching activity!", Toast.LENGTH_SHORT).show();
        }
    }

    // DATA & SECURITY: SHA-256 Validation for binaries
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

    // Core Shell Execution - Root/Non-Root handle
    public static void run(final String cmd, final MainActivity.Callback cb) {
        new Thread(() -> {
            try {
                Process p = Runtime.getRuntime().exec(isRooted() ? "su" : "sh");
                OutputStream os = p.getOutputStream();
                os.write((cmd + "\nexit\n").getBytes());
                os.flush(); // Crucial flush

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

    public static boolean isRooted() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su", "/vendor/xbin/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
}

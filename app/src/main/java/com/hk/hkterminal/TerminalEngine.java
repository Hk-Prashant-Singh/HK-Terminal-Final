package com.hk.hkterminal;
import java.io.*;

public class TerminalEngine {
    public static void run(String cmd, MainActivity.Callback cb) {
        new Thread(() -> {
            try {
                // Professional Path Environment
                String[] env = {"PATH=$PATH:/system/bin:/data/local/bin", "HOME=/data/data/com.hk.hkterminal/files/home"};
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd}, env);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;
                while ((line = r.readLine()) != null) cb.onOutput(line);
            } catch (Exception e) { cb.onOutput("[ERR]: " + e.getMessage()); }
        }).start();
    }
}

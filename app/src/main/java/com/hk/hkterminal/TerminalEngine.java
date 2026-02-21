package com.hk.hkterminal;
import java.io.*;

public class TerminalEngine {
    public static void run(String cmd, MainActivity.Callback cb) {
        new Thread(() -> {
            try {
                // Alpha System Access
                Process p = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String l;
                while ((l = r.readLine()) != null) {
                    cb.onOutput(l);
                }
                p.waitFor();
            } catch (Exception e) {
                cb.onOutput("[ERROR]: " + e.getMessage());
            }
        }).start();
    }
}

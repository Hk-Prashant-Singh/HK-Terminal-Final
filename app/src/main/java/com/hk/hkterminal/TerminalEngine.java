package com.hk.hkterminal;
import java.io.*;

public class TerminalEngine {
    public static void run(String cmd, MainActivity.Callback cb) {
        new Thread(() -> {
            try {
                // Route/Non-Root intelligent switching
                String shell = RootUtils.isRootAvailable() ? "su" : "sh";
                String[] env = {"PATH=$PATH:/system/bin:/data/local/bin", "HOME=/data/local/tmp"};
                Process p = Runtime.getRuntime().exec(new String[]{shell, "-c", cmd}, env);
                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String l;
                while ((l = r.readLine()) != null) cb.onOutput(l);
            } catch (Exception e) { cb.onOutput("[ERR]: " + e.getMessage()); }
        }).start();
    }
}

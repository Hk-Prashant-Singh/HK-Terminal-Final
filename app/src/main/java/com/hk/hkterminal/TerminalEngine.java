package com.hk.hkterminal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class TerminalEngine {

    /**
     * Executes shell commands and returns output via callback.
     * Prashant Bhai, ye asli system shell (sh) se connect hota hai.
     */
    public static void run(final String cmd, final MainActivity.Callback cb) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Process process = null;
                try {
                    // Running as standard shell
                    process = Runtime.getRuntime().exec("sh");
                    OutputStream os = process.getOutputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    // Executing command and exiting shell to flush output
                    os.write((cmd + "\nexit\n").getBytes());
                    os.flush();

                    String line;
                    // Reading standard output
                    while ((line = reader.readLine()) != null) {
                        if (cb != null) cb.onOutput(line);
                    }

                    // Reading error output (if any)
                    while ((line = errorReader.readLine()) != null) {
                        if (cb != null) cb.onOutput("Error: " + line);
                    }

                    process.waitFor();
                } catch (Exception e) {
                    if (cb != null) cb.onOutput("Engine Error: " + e.getMessage());
                } finally {
                    if (process != null) process.destroy();
                }
            }
        }).start();
    }
}

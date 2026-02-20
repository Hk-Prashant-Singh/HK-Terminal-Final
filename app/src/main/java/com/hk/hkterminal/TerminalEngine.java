package com.hk.hkterminal;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TerminalEngine {

    public interface CommandCallback {
        void onOutput(String output);
    }

    public static void runCommand(String command, boolean rootMode, CommandCallback callback) {

        new Thread(() -> {
            StringBuilder output = new StringBuilder();

            try {
                Process process;

                if (rootMode) {
                    process = Runtime.getRuntime().exec(new String[]{"su","-c",command});
                } else {
                    process = Runtime.getRuntime().exec(command);
                }

                BufferedReader stdReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                BufferedReader errReader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()));

                String line;
                while ((line = stdReader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                while ((line = errReader.readLine()) != null) {
                    output.append("[ERROR] ").append(line).append("\n");
                }

                process.waitFor();
                stdReader.close();
                errReader.close();

            } catch (Exception e) {
                output.append("Exception: ").append(e.getMessage());
            }

            callback.onOutput(output.toString());

        }).start();
    }
}
package com.hk.hkterminal;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private EditText inputCommand;
    private Button runButton;
    private ImageButton btnUp, btnDown;
    private static TextView globalOutputView;
    private TextToSpeech alexVoice;
    
    // Command History Logic
    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Professional Permission Check [cite: 2026-02-01]
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 101);
        }

        alexVoice = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) alexVoice.setLanguage(Locale.UK);
        });

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        inputCommand = findViewById(R.id.inputCommand);
        runButton = findViewById(R.id.runButton);
        btnUp = findViewById(R.id.btnUp); // XML mein add karna hoga
        btnDown = findViewById(R.id.btnDown);

        viewPager.setAdapter(new TerminalPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "TERMINAL" : "PACKAGES");
        }).attach();

        // History Up Button
        btnUp.setOnClickListener(v -> {
            if (!commandHistory.isEmpty() && historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            }
        });

        // History Down Button
        btnDown.setOnClickListener(v -> {
            if (historyIndex > 0) {
                historyIndex--;
                inputCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
            } else {
                historyIndex = -1;
                inputCommand.setText("");
            }
        });

        runButton.setOnClickListener(v -> {
            String cmd = inputCommand.getText().toString().trim();
            if (!cmd.isEmpty()) {
                commandHistory.add(cmd);
                historyIndex = -1;
                globalOutputView.append("\nroot@pshacker:~# " + cmd + "\n");
                executeCommand(cmd);
                inputCommand.setText("");
            }
        });
    }

    private void executeCommand(String command) {
        new Thread(() -> {
            try {
                // Root aur Non-Root support
                Process process;
                try {
                    process = Runtime.getRuntime().exec("su -c " + command); // Try Root
                } catch (Exception e) {
                    process = Runtime.getRuntime().exec(command); // Fallback to Non-Root
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                boolean hasOutput = false;
                while ((line = reader.readLine()) != null) {
                    hasOutput = true;
                    String finalLine = line;
                    runOnUiThread(() -> {
                        globalOutputView.append(finalLine + "\n");
                        alexVoice.speak(finalLine, TextToSpeech.QUEUE_ADD, null, null);
                    });
                }
                if (!hasOutput) {
                    runOnUiThread(() -> globalOutputView.append("[SYSTEM]: Command executed with no output.\n"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> globalOutputView.append("[ERROR]: " + e.getMessage() + "\n"));
            }
        }).start();
    }
    
    // ... (TerminalPagerAdapter aur TabFragment pichle code jaisa hi rahega)
}

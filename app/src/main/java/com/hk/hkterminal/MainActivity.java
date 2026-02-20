package com.hk.hkterminal;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    // UI Elements linking to the professional XML
    private TextView outputView;
    private EditText inputCommand;
    private Button runButton;
    private ScrollView scrollView;
    private boolean rootMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure your XML file name is activity_main.xml
        setContentView(R.layout.activity_main);

        // Binding Views
        outputView = findViewById(R.id.outputView);
        inputCommand = findViewById(R.id.inputCommand);
        runButton = findViewById(R.id.runButton);
        scrollView = findViewById(R.id.scrollView);

        // Check Root Status
        rootMode = RootUtils.isRootAvailable();
        
        // Elite Terminal Welcome Message
        String modeHeader = rootMode ? " [ STATUS: ROOT ACCESS GRANTED ]\n" : " [ STATUS: USER ACCESS ONLY ]\n";
        outputView.setText(">> SYSTEM INITIALIZED...\n" + modeHeader + ">> READY FOR COMMANDS...\n");

        // Execution Logic
        runButton.setOnClickListener(v -> {
            String cmd = inputCommand.getText().toString().trim();
            
            if (!cmd.isEmpty()) {
                // Command display style
                String prompt = rootMode ? "root@pshacker:~# " : "user@pshacker:~$ ";
                outputView.append("\n" + prompt + cmd + "\n");
                
                // Clear input immediately for next command
                inputCommand.setText("");

                // Terminal Engine Execution
                TerminalEngine.runCommand(cmd, rootMode, result -> {
                    runOnUiThread(() -> {
                        // Append output and auto-scroll to bottom
                        outputView.append(result + "\n");
                        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                    });
                });
            }
        });
    }
}


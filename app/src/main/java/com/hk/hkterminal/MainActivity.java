package com.hk.hkterminal;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

public class MainActivity extends AppCompatActivity {

    private TextView outputView;
    private EditText inputCommand;
    private Button runButton;
    private ScrollView scrollView;
    private boolean rootMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Link to XML

        outputView = findViewById(R.id.outputView);
        inputCommand = findViewById(R.id.inputCommand);
        runButton = findViewById(R.id.runButton);
        scrollView = findViewById(R.id.scrollView);

        outputView.setText(">> SYSTEM READY, PRASHANT BHAI...\n");

        runButton.setOnClickListener(v -> {
            String cmd = inputCommand.getText().toString().trim();
            if (!cmd.isEmpty()) {
                outputView.append("\n# " + cmd + "\n");
                inputCommand.setText("");
                
                // Simulated Engine Call
                outputView.append("[SYSTEM]: Executing " + cmd + "...\n");
                
                scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
            }
        });
    }
}

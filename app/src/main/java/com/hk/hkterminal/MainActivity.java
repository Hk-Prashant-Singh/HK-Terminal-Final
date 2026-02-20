package com.hk.hkterminal;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.*;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    TextView outputView;
    EditText inputCommand;
    Button runButton;
    ScrollView scrollView;
    boolean rootMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        outputView = findViewById(R.id.outputView);
        inputCommand = findViewById(R.id.inputCommand);
        runButton = findViewById(R.id.runButton);
        scrollView = findViewById(R.id.scrollView);

        rootMode = RootUtils.isRootAvailable();
        outputView.setText(rootMode ? "ROOT MODE\n" : "USER MODE\n");

        runButton.setOnClickListener(v -> {
            String cmd = inputCommand.getText().toString();
            outputView.append("\n$ " + cmd + "\n");
            inputCommand.setText("");

            TerminalEngine.runCommand(cmd, rootMode, result -> {
                runOnUiThread(() -> {
                    outputView.append(result);
                    scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
                });
            });
        });
    }
}
package com.hk.hkterminal;

import android.content.Context;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ViewPager for terminal and package tabs
        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { return new TabFragment(p); }
        });
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        // Button to clear output
        findViewById(R.id.btnCLR).setOnClickListener(v -> outputView.setText(">> PS HACKER READY\nroot@pshacker:~# "));

        // Button for navigating history
        findViewById(R.id.btnUp).setOnClickListener(v -> navigateHistory(1));
        findViewById(R.id.btnDown).setOnClickListener(v -> navigateHistory(-1));

        // Handling touch for command execution
        outputView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    String command = outputView.getText().toString().split("\n")[0].replace("root@pshacker:~# ", "").trim();
                    if (!command.isEmpty()) {
                        executeCommand(command);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    // Execute command (either root or non-root)
    public void executeCommand(String command) {
        history.add(command);
        String finalCommand = isRooted() ? "su -c '" + command + "'" : command;

        try {
            Process process = Runtime.getRuntime().exec(finalCommand);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                outputView.append(line + "\n");
            }

            // Capture error stream
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = errorReader.readLine()) != null) {
                outputView.append("Error: " + line + "\n");
            }

        } catch (IOException e) {
            outputView.append("Error executing command: " + e.getMessage() + "\n");
        }
    }

    // Check if the device is rooted
    public boolean isRooted() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su", "/vendor/xbin/su"};
        for (String path : paths) {
            if (new java.io.File(path).exists()) {
                return true;  // Device is rooted
            }
        }
        return false;  // Device is not rooted
    }

    // Navigate through command history
    private void navigateHistory(int dir) {
        if (history.isEmpty()) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        if (hIndex != -1) outputView.setText(">> PS HACKER READY\nroot@pshacker:~# " + history.get(history.size() - 1 - hIndex));
    }

    // TabFragment for terminal output
    public static class TabFragment extends Fragment {
        int type;
        public TabFragment(int t) { type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type != 0) return new View(getContext()); // Package tab is empty

            ScrollView sv = new ScrollView(getContext());
            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
            outputView.setFocusableInTouchMode(true);

            // Enable command input
            outputView.setOnKeyListener((v, code, ev) -> {
                if (ev.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                    String[] lines = outputView.getText().toString().split("\n");
                    String last = lines[lines.length - 1].replace("root@pshacker:~# ", "");
                    executeCommand(last);
                    return true;
                }
                return false;
            });

            // Smooth zoom
            ScaleGestureDetector gd = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    outputView.setTextSize(0, outputView.getTextSize() * d.getScaleFactor());
                    return true;
                }
            });
            sv.setOnTouchListener((v, e) -> { gd.onTouchEvent(e); return false; });
            sv.addView(outputView);
            return sv;
        }
    }
}

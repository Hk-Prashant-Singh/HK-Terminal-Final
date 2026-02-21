package com.hk.hkterminal;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
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

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { return new TabFragment(p); }
        });
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        findViewById(R.id.btnCLR).setOnClickListener(v -> 
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# "));

        findViewById(R.id.btnUp).setOnClickListener(v -> navigateHistory(1));
        findViewById(R.id.btnDown).setOnClickListener(v -> navigateHistory(-1));
    }

    public void executeCommand(final String command) {
        history.add(command);
        outputView.append("\n"); // Move to next line before output
        
        // Fix: Execute in Background Thread to avoid app hang
        new Thread(() -> {
            try {
                // Check for Root and run shell
                Process process = Runtime.getRuntime().exec(isRooted() ? "su" : "sh");
                OutputStream os = process.getOutputStream();
                os.write((command + "\nexit\n").getBytes());
                os.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String output = line;
                    new Handler(Looper.getMainLooper()).post(() -> outputView.append(output + "\n"));
                }

                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                while ((line = errorReader.readLine()) != null) {
                    final String error = line;
                    new Handler(Looper.getMainLooper()).post(() -> outputView.append("Error: " + error + "\n"));
                }
                
                new Handler(Looper.getMainLooper()).post(() -> outputView.append("root@pshacker:~# "));
                process.waitFor();
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> outputView.append("Execution Error: " + e.getMessage() + "\nroot@pshacker:~# "));
            }
        }).start();
    }

    public boolean isRooted() {
        String[] paths = {"/system/xbin/su", "/system/bin/su", "/sbin/su", "/vendor/xbin/su"};
        for (String path : paths) { if (new File(path).exists()) return true; }
        return false;
    }

    private void navigateHistory(int dir) {
        if (history.isEmpty()) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        if (hIndex != -1) {
            String currentText = outputView.getText().toString();
            int lastPrompt = currentText.lastIndexOf("root@pshacker:~# ");
            if (lastPrompt != -1) {
                outputView.setText(currentText.substring(0, lastPrompt + 17) + history.get(history.size() - 1 - hIndex));
            }
        }
    }

    public static class TabFragment extends Fragment {
        int type;
        public TabFragment() {} // Required empty constructor
        public TabFragment(int t) { this.type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type != 0) return new View(getContext());

            ScrollView sv = new ScrollView(getContext());
            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
            outputView.setFocusableInTouchMode(true);
            outputView.setClickable(true);

            // Fix: Touch to open keyboard
            outputView.setOnClickListener(v -> {
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            });

            outputView.setOnKeyListener((v, code, ev) -> {
                if (ev.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                    String fullText = outputView.getText().toString();
                    String lastLine = fullText.substring(fullText.lastIndexOf("root@pshacker:~# ") + 17).trim();
                    ((MainActivity)getActivity()).executeCommand(lastLine);
                    return true;
                }
                return false;
            });

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

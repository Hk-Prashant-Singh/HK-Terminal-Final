package com.hk.hkterminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList; // FIXED: For setBackgroundTintList
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * PROJECT: HK TERMINAL FINAL
 * LEAD DEVELOPER: PRASHANT BHAI (TECH WIZARD)
 * ARCHITECTURE: ELITE ALPHA INDIAN HACKER SYSTEM [cite: 2026-01-20]
 */
public class MainActivity extends AppCompatActivity {

    // --- SYSTEM CORE COMPONENTS ---
    public static TextView outputView;
    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    private boolean isCtrlPressed = false;

    // --- INTERFACE FOR TERMINAL ENGINE ---
    // FIXED: Symbol available for TerminalEngine
    public interface Callback {
        void onOutput(String line);
    }

    // --- JNI BRIDGE INITIALIZATION ---
    static {
        // System.loadLibrary("hk_terminal_jni");
    }

    // --- SYSTEM CORE: TermuxService Background Management ---
    // FIXED: ServiceConnection & ComponentName symbols resolved via imports
    private ServiceConnection termuxServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            logInfo("SYSTEM_CORE", "TermuxService connected successfully.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            logInfo("SYSTEM_CORE", "TermuxService disconnected unexpectedly.");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- UI: FullScreenWorkAround (Keyboard Resize) ---
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // --- VIEW INITIALIZATION ---
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        // --- VIEW PAGER & TAB CONFIGURATION ---
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() { return 2; }

            @Override
            public Fragment createFragment(int position) {
                return new TerminalTabFragment(position);
            }
        });

        new TabLayoutMediator(findViewById(R.id.tabLayout), viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "TERMINAL" : "PACKAGES");
        }).attach();

        // --- UI: ExtraKeys (Toolbar Toggles) ---
        findViewById(R.id.btnCtrl).setOnClickListener(v -> {
            isCtrlPressed = !isCtrlPressed;
            // FIXED: Using direct hex colors to avoid 'R.color.red_active not found' error
            int activeColor = 0xFFFF0000; // Red
            int inactiveColor = 0xFF333333; // Gray
            v.setBackgroundTintList(ColorStateList.valueOf(isCtrlPressed ? activeColor : inactiveColor));
            showToast("Ctrl " + (isCtrlPressed ? "Enabled" : "Disabled"));
        });

        findViewById(R.id.btnAlt).setOnClickListener(v -> logInfo("INPUT_EVENT", "Alt Key Triggered"));
        findViewById(R.id.btnFn).setOnClickListener(v -> logInfo("INPUT_EVENT", "Function Key Triggered"));

        // --- GLOBAL ACTION LISTENERS ---
        findViewById(R.id.btnCLR).setOnClickListener(v -> {
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
            showToast("Terminal Buffer Cleared.");
        });

        findViewById(R.id.btnUp).setOnClickListener(v -> navigateHistory(1));
        findViewById(R.id.btnDown).setOnClickListener(v -> navigateHistory(-1));

        // --- INITIALIZE BACKGROUND SOCKET SERVER ---
        TerminalEngine.startAmSocketServer();
    }

    // --- ROBUSTNESS: Logger Utility ---
    public static void logError(String tag, String message, Throwable e) {
        Log.e("HK_LOG_" + tag, message, e);
    }

    public static void logInfo(String tag, String message) {
        Log.i("HK_INFO_" + tag, message);
    }

    // --- DATA & SECURITY: Professional Clipboard Manager ---
    public void copyToSystemClipboard(String data) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("APK_BLUEPRINT", data);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            showToast("Professional Blueprint Copied!");
        }
    }

    // --- CORE EXECUTION ENGINE ---
    public void executeCommand(final String command) {
        if (command == null || command.trim().isEmpty()) return;
        commandHistory.add(command);
        if (headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
        outputView.append("\n");

        TerminalEngine.run(command, line -> runOnUiThread(() -> {
            if (outputView != null) {
                outputView.append(line + "\n");
                // --- TERMINAL BUFFER: Auto-Scroll ---
                final ScrollView sv = (ScrollView) outputView.getParent();
                if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            }
            if (headerProgress != null) headerProgress.setVisibility(View.GONE);
        }));
    }

    // --- NAVIGATION: Command History Logic ---
    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;
        historyIndex = Math.max(-1, Math.min(historyIndex + direction, commandHistory.size() - 1));
        if (historyIndex != -1) {
            String currentText = outputView.getText().toString();
            int lastPromptIndex = currentText.lastIndexOf("root@pshacker:~# ");
            if (lastPromptIndex != -1) {
                String restoredLine = currentText.substring(0, lastPromptIndex + 17) + commandHistory.get(commandHistory.size() - 1 - historyIndex);
                outputView.setText(restoredLine);
                // FIXED: TextView does not have setSelection. Using focus instead
                outputView.requestFocus();
            }
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // --- INNER CLASS: Terminal Tab Manager ---
    public static class TerminalTabFragment extends Fragment {
        private int type;
        public TerminalTabFragment() {}
        public TerminalTabFragment(int t) { this.type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type != 0) return new View(getContext());

            final ScrollView sv = new ScrollView(getContext());
            sv.setFillViewport(true);

            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setTypeface(android.graphics.Typeface.MONOSPACE);
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
            outputView.setFocusableInTouchMode(true);
            outputView.setClickable(true);

            // --- UI: Touch-to-Keyboard Activation ---
            View.OnClickListener keyboardTrigger = v -> {
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            };
            sv.setOnClickListener(keyboardTrigger);
            outputView.setOnClickListener(keyboardTrigger);

            // --- INPUT: Enter Key Execution ---
            outputView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    String fullText = outputView.getText().toString();
                    int lastPromptIndex = fullText.lastIndexOf("root@pshacker:~# ");
                    if (lastPromptIndex != -1) {
                        String lastCommand = fullText.substring(lastPromptIndex + 17).trim();
                        ((MainActivity)getActivity()).executeCommand(lastCommand);
                    }
                    return true;
                }
                return false;
            });

            sv.addView(outputView);
            return sv;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TerminalEngine.stopAmSocketServer(); // Security Cleanup
        logInfo("LIFECYCLE", "Activity destroyed, services cleaned up.");
    }
}

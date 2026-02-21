package com.hk.hkterminal;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> commandHistory = new ArrayList<>(); // Renamed for clarity
    private int historyIndex = -1; // Renamed for clarity
    private ProgressBar headerProgress;
    private LinearLayout extraKeysLayout;
    private boolean isCtrlPressed = false; // For Ctrl key functionality

    // SYSTEM CORE: JNI Bridge Placeholder
    static {
        // System.loadLibrary("termux-jni"); // Example for JNI (Requires native code)
    }

    // SYSTEM CORE: TermuxService Placeholder (Background Session Management)
    private ServiceConnection termuxServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // Implement service binding logic here for background shell
            Log.d("PS_HACKER", "TermuxService Connected!");
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d("PS_HACKER", "TermuxService Disconnected!");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI & INTERFACE: FullScreenWorkAround - Initial setup
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { return new TerminalTabFragment(p); } // Renamed for clarity
        });
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) ->
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        // ExtraKeys: Listeners for custom toolbar buttons
        findViewById(R.id.btnCtrl).setOnClickListener(v -> {
            isCtrlPressed = !isCtrlPressed;
            v.setBackgroundTintList(isCtrlPressed ? getColorStateList(R.color.red_active) : getColorStateList(R.color.gray_inactive)); // Placeholder colors
            Toast.makeText(this, "Ctrl " + (isCtrlPressed ? "Active" : "Inactive"), Toast.LENGTH_SHORT).show();
            // Implement Ctrl key logic (e.g., sending Ctrl+C)
        });
        findViewById(R.id.btnAlt).setOnClickListener(v -> sendSpecialKey(KeyEvent.KEYCODE_ALT_LEFT));
        findViewById(R.id.btnFn).setOnClickListener(v -> sendSpecialKey(KeyEvent.KEYCODE_FUNCTION));
        findViewById(R.id.btnCLR).setOnClickListener(v -> outputView.setText(">> PS HACKER READY\nroot@pshacker:~# "));
        findViewById(R.id.btnUp).setOnClickListener(v -> navigateHistory(1));
        findViewById(R.id.btnDown).setOnClickListener(v -> navigateHistory(-1));
        findViewById(R.id.btnDrawer).setOnClickListener(v -> Toast.makeText(this, "Drawer Toggle (Not Implemented)", Toast.LENGTH_SHORT).show()); // Placeholder

        // Bind to TermuxService (Placeholder)
        // Intent serviceIntent = new Intent(this, TermuxService.class);
        // bindService(serviceIntent, termuxServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unbindService(termuxServiceConnection); // Unbind TermuxService
    }

    private void sendSpecialKey(int keyCode) {
        // Logic to send special keys to the terminal (requires JNI or input connection)
        Toast.makeText(this, "Key " + keyCode + " sent (Not fully implemented)", Toast.LENGTH_SHORT).show();
    }

    // ROBUSTNESS: Logger - Detailed stack trace logging
    public static void logError(String tag, String message, Throwable e) {
        Log.e(tag, message, e);
        // Add more robust logging to file/remote server for production
    }

    // DATA & SECURITY: SHA-256 Validation
    public String calculateSHA256(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            fis.close();
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            logError("SHA256", "Error calculating SHA-256", e);
            return null;
        }
    }

    // Professional Clipboard Manager Logic
    public void copyToSystemClipboard(String data) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("APK_BLUEPRINT", data);
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Professional Blueprint Copied!", Toast.LENGTH_SHORT).show();
        }
    }

    public void executeCommand(final String command) {
        if (command.isEmpty()) return;
        commandHistory.add(command);
        headerProgress.setVisibility(View.VISIBLE); // Show loading
        outputView.append("\n"); // New line for command output

        TerminalEngine.run(command, line -> runOnUiThread(() -> {
            if (outputView != null) {
                outputView.append(line + "\n");
                // UI & INTERFACE: TerminalBuffer - Auto-scroll to bottom
                final ScrollView sv = (ScrollView) outputView.getParent();
                if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            }
            headerProgress.setVisibility(View.GONE); // Hide loading
        }));
    }

    private void navigateHistory(int direction) {
        if (commandHistory.isEmpty()) return;
        historyIndex = Math.max(-1, Math.min(historyIndex + direction, commandHistory.size() - 1));
        if (historyIndex != -1) {
            String currentText = outputView.getText().toString();
            int lastPromptIndex = currentText.lastIndexOf("root@pshacker:~# ");
            if (lastPromptIndex != -1) {
                String newPromptLine = currentText.substring(0, lastPromptIndex + 17) + commandHistory.get(commandHistory.size() - 1 - historyIndex);
                outputView.setText(newPromptLine);
                outputView.setSelection(newPromptLine.length()); // Move cursor to end
            }
        }
    }

    // Terminal Tab Fragment
    public static class TerminalTabFragment extends Fragment {
        int type;
        public TerminalTabFragment() {} // Required empty constructor
        public TerminalTabFragment(int t) { this.type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type != 0) return new View(getContext()); // For "PACKAGES" tab

            final ScrollView sv = new ScrollView(getContext());
            sv.setFillViewport(true); // FullScreenWorkAround for scrollable area

            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setTypeface(android.graphics.Typeface.MONOSPACE);
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
            outputView.setFocusableInTouchMode(true);
            outputView.setClickable(true);

            // UI & INTERFACE: Touch-to-Keyboard (FullScreenWorkAround)
            View.OnClickListener keyboardTrigger = v -> {
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
                ((MainActivity)getActivity()).extraKeysLayout.setVisibility(View.VISIBLE); // Show ExtraKeys on keyboard
            };
            sv.setOnClickListener(keyboardTrigger); // Click anywhere on terminal to open keyboard
            outputView.setOnClickListener(keyboardTrigger); // Also works on actual textview

            // Command input on Enter key
            outputView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    String fullText = outputView.getText().toString();
                    String lastCommand = fullText.substring(fullText.lastIndexOf("root@pshacker:~# ") + 17).trim();
                    ((MainActivity)getActivity()).executeCommand(lastCommand);
                    return true;
                }
                return false;
            });

            // UI & INTERFACE: TerminalBuffer - Smooth Zoom
            ScaleGestureDetector scaleDetector = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    outputView.setTextSize(0, outputView.getTextSize() * detector.getScaleFactor());
                    return true;
                }
            });
            sv.setOnTouchListener((v, event) -> {
                scaleDetector.onTouchEvent(event);
                return false; // Allows click listener to also work
            });

            sv.addView(outputView);
            return sv;
        }
    }
                    }
        

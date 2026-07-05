package com.hk.hkterminal;

import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.text.*;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.File;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    private boolean isCtrl = false;

    // HK-OPERATION : ALPHA NATIVE BRIDGES
    private PtyBridge ptyBridge;
    private String currentPrompt = "pshacker@hk:~$ ";
    private boolean isRootMode = false;

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Elite resizing for professional terminal feel
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        // HK-Operation: Auto-grant executable powers to binaries
        initHKEnvironment();

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { return new TerminalTabFragment(p); }
        });
        
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        setupSystemButtons();
        
        // Start Socket & Ignite the Persistent Ghost Engine
        TerminalEngine.startAmSocketServer();
        
        // 15-Second System Access: Ignite Native C++ Kernel Bridge
        String[] env = {"PATH=" + TerminalEngine.BIN_PATH + ":/system/bin:/system/xbin", "TERM=xterm-256color", "HOME=" + TerminalEngine.HOME_PATH};
        ptyBridge = new PtyBridge("/system/bin/sh", env, TerminalEngine.HOME_PATH);

        // Background Thread for Native JNI Output
        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = ptyBridge.getInputStream().read(buffer)) != -1) {
                    String output = new String(buffer, 0, read, "UTF-8");
                    runOnUiThread(() -> {
                        if (outputView != null) {
                            outputView.append(output);
                            scrollToBottom();
                        }
                    });
                }
            } catch (Exception e) {
                Log.e("HK_NATIVE", "PTY Stream Disconnected", e);
            }
        }).start();

        // Legacy Terminal Engine Fallback
        TerminalEngine.igniteEngine(line -> runOnUiThread(() -> {
            if (outputView != null) {
                outputView.append(line + "\n");
                scrollToBottom();
            }
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
        }));
    }

    private void scrollToBottom() {
        final ScrollView sv = (ScrollView) outputView.getParent();
        if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    public String getCurrentPrompt() {
        return currentPrompt;
    }

    // Surgical function to grant execute powers to binaries silently
    private void initHKEnvironment() {
        new Thread(() -> {
            File usrDir = new File(TerminalEngine.PREFIX_PATH);
            if (usrDir.exists()) {
                try {
                    Runtime.getRuntime().exec("chmod -R 755 " + TerminalEngine.PREFIX_PATH).waitFor();
                } catch (Exception e) {
                    Log.e("HK_INIT", "Permission Grant Failed", e);
                }
            }
        }).start();
    }

    private void setupSystemButtons() {
        View btnCb = findViewById(R.id.esc); 
        if (btnCb != null) btnCb.setOnClickListener(v -> showCommandBox());

        View btnCtrl = findViewById(R.id.ctrl);
        if (btnCtrl != null) btnCtrl.setOnClickListener(v -> {
            isCtrl = !isCtrl;
            v.setBackgroundColor(isCtrl ? 0xFFFF0000 : 0xFF333333);
            if (isCtrl && ptyBridge != null) {
                ptyBridge.kill(2); // SIGINT for Job Control
                if (outputView != null) outputView.append("^C\n" + currentPrompt);
                isCtrl = false;
                v.setBackgroundColor(0xFF333333);
            }
        });

        // Prompt restored for Elite Alpha energy dynamically
        View btnCLR = findViewById(R.id.slash); 
        if (btnCLR != null) btnCLR.setOnClickListener(v -> {
            if (outputView != null) outputView.setText(">> HK Prashant Bhai\n" + currentPrompt);
        });

        View btnUp = findViewById(R.id.up);
        if (btnUp != null) btnUp.setOnClickListener(v -> navigateHistory(1));

        View btnDown = findViewById(R.id.down);
        if (btnDown != null) btnDown.setOnClickListener(v -> navigateHistory(-1));
    }

    private void showCommandBox() {
        final EditText input = new EditText(this);
        // Changed to Clean White
        input.setTextColor(0xFF00FF41); // Aggressive Hacker Green
        input.setBackgroundColor(0xFF050505);
        input.setTypeface(Typeface.MONOSPACE);
        
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setTitle("HK DIRECTIVE INPUT")
            .setView(input)
            .setPositiveButton("EXECUTE", (d, w) -> {
                String cmd = input.getText().toString().trim();
                if(!cmd.isEmpty()) {
                    if (outputView != null) outputView.append(cmd); 
                    executeCommand(cmd);
                }
            }).setNegativeButton("CANCEL", null).show();
        
        input.requestFocus();
        forceKeyboard(input);
    }

    public static void logError(String t, String m, Throwable e) { Log.e(t, m, e); }

    private void forceKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void executeCommand(final String command) {
        if (command.isEmpty()) return;
        history.add(command);
        hIndex = -1;
        if(headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
        
        outputView.append("\n");

        // 1. Intercept HKPackageManager Directives
        if (command.startsWith("hk-pkg install ")) {
            String pkg = command.replace("hk-pkg install ", "").trim();
            HKPackageManager.installPackage(pkg, msg -> runOnUiThread(() -> {
                outputView.append(msg + "\n" + currentPrompt);
                scrollToBottom();
            }));
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            return;
        }

        // 2. Intercept Dynamic Root Escalation
        String trimmedCmd = command.trim();
        if (trimmedCmd.equals("su")) {
            if (RootUtils.isRootAvailable()) {
                isRootMode = true;
                currentPrompt = "root@pshacker:~# ";
            } else {
                outputView.append("su: Permission denied (System Guardian blocked request)\n");
            }
            outputView.append(currentPrompt);
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            return;
        } else if (trimmedCmd.equals("exit") && isRootMode) {
            isRootMode = false;
            currentPrompt = "pshacker@hk:~$ ";
            outputView.append(currentPrompt);
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            return;
        }

        // 3. Fire command directly into the living Native PTY Tunnel
        if (ptyBridge != null) {
            ptyBridge.writeCommand(command + "\n");
        } else {
            TerminalEngine.run(command);
        }
        
        // Fallback progress hide
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
        }, 500);
    }

    private void navigateHistory(int dir) {
        if (history.isEmpty() || outputView == null) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        String txt = outputView.getText().toString();
        int last = txt.lastIndexOf(currentPrompt);
        if (last != -1) {
            String cmd = (hIndex == -1) ? "" : history.get(history.size() - 1 - hIndex);
            outputView.setText(txt.substring(0, last + currentPrompt.length()) + cmd);
        }
    }

    public static class TerminalTabFragment extends Fragment {
        int type;
        public TerminalTabFragment() {}
        public TerminalTabFragment(int t) { this.type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type != 0) return new View(getContext());
            final ScrollView sv = new ScrollView(getContext());
            sv.setFillViewport(true);
            sv.setBackgroundColor(Color.parseColor("#050505")); // Abyss Black UI

            outputView = new TextView(getContext());
            
            // HK-OPERATION: Radioactive UI Matrix Styling
            outputView.setTextColor(Color.parseColor("#00FF41")); // Hacker Green
            outputView.setBackgroundColor(Color.parseColor("#050505"));
            outputView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            outputView.setShadowLayer(8f, 0f, 0f, Color.parseColor("#00FF41")); // Neon Glow
            outputView.setPadding(10, 10, 10, 10);
            
            String prompt = ((MainActivity)getActivity()).getCurrentPrompt();
            outputView.setText(">> HK Prashant Bhai\n" + prompt);
            outputView.setFocusableInTouchMode(true);
            outputView.setCursorVisible(true);

            final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(getContext(), 
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        float size = outputView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                        outputView.setTextSize(size * d.getScaleFactor());
                        return true;
                    }
                });

            sv.setOnTouchListener((v, e) -> {
                scaleDetector.onTouchEvent(e);
                if (e.getAction() == MotionEvent.ACTION_UP && !scaleDetector.isInProgress()) {
                    outputView.requestFocus();
                    ((MainActivity)getActivity()).forceKeyboard(outputView);
                }
                return false; 
            });

            outputView.setOnKeyListener((v, code, ev) -> {
                String activePrompt = ((MainActivity)getActivity()).getCurrentPrompt();
                if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                    if (ev.getUnicodeChar() != 0 && code != KeyEvent.KEYCODE_ENTER && code != KeyEvent.KEYCODE_DEL) {
                        outputView.append(String.valueOf((char) ev.getUnicodeChar()));
                        return true;
                    }
                    if (code == KeyEvent.KEYCODE_DEL) {
                        String s = outputView.getText().toString();
                        if (!s.endsWith(activePrompt)) {
                            outputView.setText(s.substring(0, s.length()-1));
                        }
                        return true;
                    }
                    if (code == KeyEvent.KEYCODE_ENTER) {
                        String s = outputView.getText().toString();
                        int start = s.lastIndexOf(activePrompt) + activePrompt.length();
                        ((MainActivity)getActivity()).executeCommand(s.substring(start).trim());
                        return true;
                    }
                }
                return false;
            });
            sv.addView(outputView);
            return sv;
        }
    }

    @Override protected void onDestroy() { 
        super.onDestroy(); 
        TerminalEngine.stopAmSocketServer(); 
        if (ptyBridge != null) ptyBridge.destroy();
    }
}

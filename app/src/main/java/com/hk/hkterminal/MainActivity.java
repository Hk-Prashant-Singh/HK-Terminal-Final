package com.hk.hkterminal;

import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.text.*;
import android.text.style.ForegroundColorSpan;
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

/**
 * HK-OPERATION : ELITE COMMAND CENTER (ULTIMATE ARSENAL & SYNC MATRIX)
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Zero Echo, Live Package Manager UI & Alpha Sync Terminal
 */
public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    private boolean isCtrl = false;

    private PtyBridge ptyBridge;
    private String currentPrompt = "pshacker@hk:~$ ";
    private boolean isRootMode = false;

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        initHKEnvironment();

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { return new TerminalTabFragment(p); }
        });
        
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        setupSystemButtons();
        TerminalEngine.startAmSocketServer();
        
        // ==========================================
        // [!] FULL-STACK SHELL SANITIZATION MATRIX
        // ==========================================
        String[] env = {
            "PATH=" + TerminalEngine.BIN_PATH + ":/system/bin:/system/xbin", 
            "TERM=vt100", // VT100 prevents advanced ANSI cursor jumping errors
            "HOME=" + TerminalEngine.HOME_PATH,
            "PS1= " // Hard reset prompt to bypass Android shell garbage
        };
        ptyBridge = new PtyBridge("/system/bin/sh", env, TerminalEngine.HOME_PATH);

        // Turn off shell echo natively (Replaced stty to fix binary not found error)
        ptyBridge.writeCommand("set +o echo\n"); 

        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = ptyBridge.getInputStream().read(buffer)) != -1) {
                    String output = new String(buffer, 0, read, "UTF-8");
                    runOnUiThread(() -> appendMatrixText(output));
                }
            } catch (Exception e) {
                Log.e("HK_NATIVE", "PTY Stream Disconnected", e);
            }
        }).start();

        TerminalEngine.igniteEngine(line -> runOnUiThread(() -> {
            appendMatrixText(line + "\n");
        }));
    }

    private void appendMatrixText(String rawText) {
        if (outputView == null || rawText == null) return;

        String cleanText = rawText.replaceAll("\u001B\\[[;\\d]*[a-zA-Z]", ""); 
        cleanText = cleanText.replace("\r", "").replaceAll(".\\x08", "");

        // Filter out silent configuration commands from UI
        if (cleanText.contains("set +o echo")) return;

        if (cleanText.contains("HK-SYNC-DONE")) {
            cleanText = cleanText.replace("HK-SYNC-DONE", currentPrompt);
            if (headerProgress != null) headerProgress.setVisibility(View.GONE); 
        }

        if (cleanText.trim().isEmpty() && !cleanText.contains(" ")) return; 

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        String[] lines = cleanText.split("\n", -1);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            SpannableString ss = new SpannableString(line);
            String lower = line.toLowerCase();

            if (lower.contains("error") || lower.contains("failed") || lower.contains("denied") 
                || lower.contains("not found") || lower.contains("[-]")) {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FF6600")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else {
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFFFFF")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            
            ssb.append(ss);
            if (i < lines.length - 1) ssb.append("\n");
        }
        
        outputView.append(ssb);
        scrollToBottom();
    }

    private void scrollToBottom() {
        final ScrollView sv = (ScrollView) outputView.getParent();
        if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    public String getCurrentPrompt() {
        return currentPrompt;
    }

    private void initHKEnvironment() {
        new Thread(() -> {
            File usrDir = new File(TerminalEngine.PREFIX_PATH);
            if (usrDir.exists()) {
                try { Runtime.getRuntime().exec("chmod -R 755 " + TerminalEngine.PREFIX_PATH).waitFor(); } 
                catch (Exception e) { Log.e("HK_INIT", "Permission Grant Failed", e); }
            }
        }).start();
    }

    private void setupSystemButtons() {
        View btnCb = findViewById(R.id.esc); 
        if (btnCb != null) btnCb.setOnClickListener(v -> showCommandBox());

        View btnCtrl = findViewById(R.id.ctrl);
        if (btnCtrl != null) btnCtrl.setOnClickListener(v -> {
            isCtrl = !isCtrl;
            v.setBackgroundColor(isCtrl ? 0xFF8A2BE2 : 0xFF333333); 
            
            if (isCtrl && ptyBridge != null) {
                ptyBridge.kill(2); 
                appendMatrixText("^C\n" + currentPrompt); 
                isCtrl = false;
                v.setBackgroundColor(0xFF333333); 
                if (headerProgress != null) headerProgress.setVisibility(View.GONE);
            }
        });

        View btnCLR = findViewById(R.id.slash); 
        if (btnCLR != null) btnCLR.setOnClickListener(v -> {
            if (outputView != null) {
                outputView.setText("");
                appendMatrixText(">> HK Prashant Singh\n" + currentPrompt);
            }
        });

        View btnUp = findViewById(R.id.up);
        if (btnUp != null) btnUp.setOnClickListener(v -> navigateHistory(1));

        View btnDown = findViewById(R.id.down);
        if (btnDown != null) btnDown.setOnClickListener(v -> navigateHistory(-1));
    }

    private void showCommandBox() {
        final EditText input = new EditText(this);
        input.setTextColor(Color.parseColor("#FFFFFF")); 
        input.setBackgroundColor(Color.parseColor("#050505"));
        input.setTypeface(Typeface.MONOSPACE);
        
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setTitle("HK DIRECTIVE INPUT")
            .setView(input)
            .setPositiveButton("EXECUTE", (d, w) -> {
                String cmd = input.getText().toString().trim();
                if(!cmd.isEmpty()) executeCommand(cmd);
            }).setNegativeButton("CANCEL", null).show();
        
        input.requestFocus();
        forceKeyboard(input);
    }

    public static void logError(String t, String m, Throwable e) { 
        Log.e(t, m, e); 
    }

    public void forceKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    public void executeCommand(final String command) {
        if (command.isEmpty()) return;
        history.add(command);
        hIndex = -1;
        if(headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
        
        // Outputting manual command input exactly once
        appendMatrixText(command + "\n");
        String trimmedCmd = command.trim();

        if (trimmedCmd.startsWith("hk install ")) {
            String pkg = trimmedCmd.replace("hk install ", "").trim();
            if (!pkg.isEmpty()) {
                HKPackageManager.installPackage(pkg, msg -> runOnUiThread(() -> {
                    appendMatrixText(msg + "\n" + currentPrompt);
                    if(headerProgress != null) headerProgress.setVisibility(View.GONE);
                }));
            } else {
                appendMatrixText("[-] HK-PKG Error: Specify package name.\n" + currentPrompt);
                if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            }
            return;
        }

        if (trimmedCmd.equals("hk-guardian") || trimmedCmd.equals("hk-setup-storage")) {
            MainActivity.Callback cb = msg -> runOnUiThread(() -> {
                appendMatrixText(msg + "\n" + currentPrompt);
                if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            });
            if (trimmedCmd.equals("hk-guardian")) HKGuardian.activateShield(cb);
            else HKGuardian.setupStorage(cb);
            return;
        }

        if (trimmedCmd.equals("su")) {
            if (RootUtils.isRootAvailable()) {
                isRootMode = true;
                currentPrompt = "root@pshacker:~# ";
            } else {
                appendMatrixText("su: Permission denied (System Guardian blocked request)\n");
            }
            appendMatrixText(currentPrompt);
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            return;
        } else if (trimmedCmd.equals("exit") && isRootMode) {
            isRootMode = false;
            currentPrompt = "pshacker@hk:~$ ";
            appendMatrixText(currentPrompt);
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            return;
        }

        if (ptyBridge != null) {
            ptyBridge.writeCommand(trimmedCmd + "\n");
            ptyBridge.writeCommand("echo 'HK-SYNC-DONE'\n");
        } else {
            TerminalEngine.run(command);
        }
    }

    private void navigateHistory(int dir) {
        if (history.isEmpty() || outputView == null) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        String txt = outputView.getText().toString();
        int last = txt.lastIndexOf(currentPrompt);
        if (last != -1) {
            String cmd = (hIndex == -1) ? "" : history.get(history.size() - 1 - hIndex);
            outputView.setText(txt.substring(0, last + currentPrompt.length()));
            appendMatrixText(cmd);
        }
    }

    public static class TerminalTabFragment extends Fragment {
        int type;
        public TerminalTabFragment() {}
        public TerminalTabFragment(int t) { this.type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            // ==================================================
            // PACKAGES TAB MATRIX (TAB 2)
            // ==================================================
            if (type == 1) { 
                ScrollView sv = new ScrollView(getContext());
                sv.setFillViewport(true);
                sv.setBackgroundColor(Color.parseColor("#050505")); 

                LinearLayout rootLayout = new LinearLayout(getContext());
                rootLayout.setOrientation(LinearLayout.VERTICAL);
                rootLayout.setPadding(40, 40, 40, 40);
                sv.addView(rootLayout);

                renderPackagesMatrix(rootLayout, getContext());
                return sv;
            }

            // ==================================================
            // TERMINAL TAB MATRIX (TAB 1)
            // ==================================================
            final ScrollView sv = new ScrollView(getContext());
            sv.setFillViewport(true);
            sv.setBackgroundColor(Color.parseColor("#050505")); 

            outputView = new TextView(getContext());
            outputView.setTextColor(Color.parseColor("#FFFFFF")); 
            outputView.setBackgroundColor(Color.parseColor("#050505"));
            outputView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            outputView.setPadding(10, 10, 10, 10);
            
            String prompt = ((MainActivity)getActivity()).getCurrentPrompt();
            ((MainActivity)getActivity()).appendMatrixText(">> HK Prashant Singh\n" + prompt);
            
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
                        if (!s.endsWith(activePrompt)) outputView.setText(s.substring(0, s.length()-1));
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

        // ==================================================
        // HK-OPERATION: PACKAGE RENDER & DELETE PROTOCOL
        // ==================================================
        private void renderPackagesMatrix(LinearLayout rootLayout, Context context) {
            rootLayout.removeAllViews();
            
            TextView title = new TextView(context);
            title.setText(">> HK WEAPON ARSENAL");
            title.setTextColor(Color.parseColor("#00FF41")); 
            title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            title.setPadding(0, 0, 0, 50);
            title.setTextSize(18f);
            rootLayout.addView(title);

            File binDir = new File(TerminalEngine.BIN_PATH);
            if (binDir.exists() && binDir.isDirectory()) {
                File[] files = binDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File file : files) {
                        if (file.isDirectory()) continue; 

                        LinearLayout row = new LinearLayout(context);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(0, 20, 0, 20);
                        row.setGravity(Gravity.CENTER_VERTICAL);

                        TextView pkgName = new TextView(context);
                        pkgName.setText(file.getName());
                        pkgName.setTextColor(Color.parseColor("#FFFFFF"));
                        pkgName.setTypeface(Typeface.MONOSPACE);
                        pkgName.setTextSize(16f);
                        pkgName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

                        Button delBtn = new Button(context);
                        delBtn.setText("DEL");
                        delBtn.setTextColor(Color.parseColor("#FF003C")); 
                        delBtn.setBackgroundColor(Color.parseColor("#1A1A1A"));
                        delBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                        
                        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        btnLp.setMargins(20, 0, 0, 0);
                        delBtn.setLayoutParams(btnLp);
                        
                        delBtn.setOnClickListener(v -> {
                            if (file.delete()) {
                                Toast.makeText(context, "[+] Target Destroyed: " + file.getName(), Toast.LENGTH_SHORT).show();
                                renderPackagesMatrix(rootLayout, context); 
                            } else {
                                Toast.makeText(context, "[-] Core System Blocked Deletion", Toast.LENGTH_SHORT).show();
                            }
                        });

                        row.addView(pkgName);
                        row.addView(delBtn);
                        rootLayout.addView(row);
                    }
                } else {
                    TextView empty = new TextView(context);
                    empty.setText("[-] Arsenal is empty.\nUse 'hk install <pkg>' to load weapons.");
                    empty.setTextColor(Color.parseColor("#888888"));
                    empty.setTypeface(Typeface.MONOSPACE);
                    empty.setPadding(0, 20, 0, 0);
                    rootLayout.addView(empty);
                }
            }

            Button refreshBtn = new Button(context);
            refreshBtn.setText("SCAN MATRIX");
            refreshBtn.setTextColor(Color.parseColor("#00FF41"));
            refreshBtn.setBackgroundColor(Color.parseColor("#1A1A1A"));
            refreshBtn.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 80, 0, 0);
            refreshBtn.setLayoutParams(lp);
            refreshBtn.setOnClickListener(v -> renderPackagesMatrix(rootLayout, context));
            rootLayout.addView(refreshBtn);
        }
    }

    @Override protected void onDestroy() { 
        super.onDestroy(); 
        TerminalEngine.stopAmSocketServer(); 
        if (ptyBridge != null) ptyBridge.destroy();
    }
}

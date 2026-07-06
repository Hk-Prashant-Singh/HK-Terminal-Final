package com.hk.hkterminal;

import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
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
 * HK-OPERATION : MASTER COMMAND CENTER (ULTIMATE STABILITY MATRIX)
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Zero Double Text, InputConnection Hook (Ctrl Fix), Crash-Free Execution
 */
public class MainActivity extends AppCompatActivity {
    public static EditText outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    
    // ALPHA STATE ENGINE
    private boolean isCtrl = false;
    private boolean isAlt = false;
    private PtyBridge ptyBridge;
    private String currentPrompt = "pshacker@hk:~$ ";
    private boolean isRootMode = false;
    private final Object streamLock = new Object();

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        File homeDir = new File(TerminalEngine.HOME_PATH);
        if (!homeDir.exists()) homeDir.mkdirs();
        
        File binDir = new File(TerminalEngine.BIN_PATH);
        if (!binDir.exists()) binDir.mkdirs();

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
        
        String[] env = {
            "PATH=" + TerminalEngine.BIN_PATH + ":/system/bin:/system/xbin", 
            "TERM=xterm-256color", 
            "HOME=" + TerminalEngine.HOME_PATH,
            "PS1=" + currentPrompt
        };
        ptyBridge = new PtyBridge("/system/bin/sh", env, TerminalEngine.HOME_PATH);
        ptyBridge.writeCommand("cd $HOME && clear\n"); 

        new Thread(() -> {
            try {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = ptyBridge.getInputStream().read(buffer)) != -1) {
                    String output = new String(buffer, 0, read, "UTF-8");
                    appendMatrixText(output);
                }
            } catch (Exception e) {
                Log.e("HK_NATIVE", "PTY Stream Disconnected", e);
            }
        }).start();
    }

    public void appendMatrixText(final String rawText) {
        if (rawText == null) return;
        synchronized (streamLock) {
            runOnUiThread(() -> {
                if (outputView == null) return;

                String cleanText = rawText.replaceAll("\u001B\\[[;\\d]*[a-zA-Z]", ""); 
                cleanText = cleanText.replace("\r", "");
                
                if (cleanText.contains("cd $HOME")) return;
                
                // Native Clear Protocol Sync
                if (cleanText.contains("clear\n") || cleanText.contains("clear")) {
                    outputView.setText("");
                    cleanText = cleanText.replace("clear\n", "").replace("clear", "").trim();
                }

                if (cleanText.isEmpty()) return; 

                SpannableStringBuilder ssb = new SpannableStringBuilder();
                String[] lines = cleanText.split("\n", -1);
                
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    SpannableString ss = new SpannableString(line);
                    String lower = line.toLowerCase();

                    if (lower.contains("error") || lower.contains("failed") || lower.contains("denied") 
                        || lower.contains("not found") || lower.contains("inaccessible") || lower.contains("[-]")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FF6600")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFFFFF")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    
                    ssb.append(ss);
                    if (i < lines.length - 1) ssb.append("\n");
                }
                
                outputView.append(ssb);
                outputView.setSelection(outputView.getText().length()); 
                scrollToBottom();
            });
        }
    }

    public void scrollToBottom() {
        if (outputView == null) return;
        final ScrollView sv = (ScrollView) outputView.getParent();
        if (sv != null) sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    public String getCurrentPrompt() { return currentPrompt; }
    public boolean isCtrlActive() { return isCtrl; }

    public void sendCtrlKey(String controlChar, String visual) {
        if (ptyBridge != null) { ptyBridge.writeCommand(controlChar); }
        appendMatrixText(visual + "\n");
        resetCtrlState();
    }

    // [!] ALPHA CLEAR MATRIX (Ctrl + L Execution)
    public void clearTerminal() {
        runOnUiThread(() -> {
            if (outputView != null) {
                outputView.setText("");
                appendMatrixText(">> HK Prashant Singh\n");
                if (ptyBridge != null) { ptyBridge.writeCommand("clear\n"); }
                resetCtrlState();
            }
        });
    }

    public void exitApplication() {
        appendMatrixText("^D\n[+] Terminating Terminal Session... Shutting Down Application Matrix.\n");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finishAffinity();
            System.exit(0);
        }, 200);
    }

    public void sendSigInt() {
        if (ptyBridge != null) {
            ptyBridge.kill(2); 
            appendMatrixText("^C\n");
        }
        resetCtrlState();
    }

    public void resetCtrlState() {
        isCtrl = false;
        runOnUiThread(() -> {
            TextView btnCtrl = findViewById(R.id.ctrl);
            if (btnCtrl != null) {
                btnCtrl.setTextColor(Color.parseColor("#FFFFFF")); 
                btnCtrl.setBackgroundColor(Color.parseColor("#262626"));
            }
        });
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
        View btnEsc = findViewById(R.id.esc); 
        if (btnEsc != null) btnEsc.setOnClickListener(v -> showCommandBox());

        TextView btnCtrl = findViewById(R.id.ctrl);
        if (btnCtrl != null) {
            btnCtrl.setOnClickListener(v -> {
                isCtrl = !isCtrl;
                btnCtrl.setTextColor(isCtrl ? Color.parseColor("#FF003C") : Color.parseColor("#FFFFFF")); 
            });
        }

        TextView btnAlt = findViewById(R.id.alt);
        if (btnAlt != null) {
            btnAlt.setOnClickListener(v -> {
                isAlt = !isAlt;
                btnAlt.setTextColor(isAlt ? Color.parseColor("#00FF41") : Color.parseColor("#A0A0A0"));
            });
        }

        View btnSlash = findViewById(R.id.slash); 
        if (btnSlash != null) btnSlash.setOnClickListener(v -> insertTextAtCursor("/"));

        View btnDash = findViewById(R.id.dash); 
        if (btnDash != null) btnDash.setOnClickListener(v -> insertTextAtCursor("-"));

        View btnLeft = findViewById(R.id.left);
        if (btnLeft != null) btnLeft.setOnClickListener(v -> moveCursor(-1));

        View btnRight = findViewById(R.id.right);
        if (btnRight != null) btnRight.setOnClickListener(v -> moveCursor(1));

        View btnLeftArrow = findViewById(R.id.left_arrow);
        if (btnLeftArrow != null) btnLeftArrow.setOnClickListener(v -> moveCursorToPrompt());

        View btnHome = findViewById(R.id.home);
        if (btnHome != null) btnHome.setOnClickListener(v -> moveCursorToPrompt());

        View btnEnd = findViewById(R.id.end);
        if (btnEnd != null) btnEnd.setOnClickListener(v -> {
            if (outputView != null) outputView.setSelection(outputView.getText().length());
        });

        View btnUp = findViewById(R.id.up);
        if (btnUp != null) btnUp.setOnClickListener(v -> navigateHistory(1));

        View btnDown = findViewById(R.id.down);
        if (btnDown != null) btnDown.setOnClickListener(v -> navigateHistory(-1));
        
        View btnPgUp = findViewById(R.id.pgup);
        if (btnPgUp != null) btnPgUp.setOnClickListener(v -> {
            if (outputView != null) {
                ScrollView sv = (ScrollView) outputView.getParent();
                sv.smoothScrollBy(0, -500);
            }
        });

        View btnPgDn = findViewById(R.id.pgdn);
        if (btnPgDn != null) btnPgDn.setOnClickListener(v -> {
            if (outputView != null) {
                ScrollView sv = (ScrollView) outputView.getParent();
                sv.smoothScrollBy(0, 500);
            }
        });
    }

    private void insertTextAtCursor(String text) {
        if (outputView != null) {
            int selStart = Math.max(outputView.getSelectionStart(), 0);
            int selEnd = Math.max(outputView.getSelectionEnd(), 0);
            int minSel = Math.min(selStart, selEnd);
            int maxSel = Math.max(selStart, selEnd);
            outputView.getText().replace(minSel, maxSel, text, 0, text.length());
            outputView.setSelection(minSel + text.length());
        }
    }

    private void moveCursor(int offset) {
        if (outputView != null) {
            int sel = outputView.getSelectionStart();
            String s = outputView.getText().toString();
            int promptIdx = s.lastIndexOf(currentPrompt);
            int minPos = promptIdx != -1 ? promptIdx + currentPrompt.length() : 0;
            
            int newPos = sel + offset;
            if (newPos >= minPos && newPos <= s.length()) {
                outputView.setSelection(newPos);
            }
        }
    }

    private void moveCursorToPrompt() {
        if (outputView != null) {
            String s = outputView.getText().toString();
            int promptIdx = s.lastIndexOf(currentPrompt);
            int minPos = promptIdx != -1 ? promptIdx + currentPrompt.length() : 0;
            outputView.setSelection(minPos);
        }
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
                if(!cmd.isEmpty()) { executeCommand(cmd); }
            }).setNegativeButton("CANCEL", null).show();
        
        input.requestFocus();
        forceKeyboard(input);
    }

    public static void logError(String m, String t, Throwable e) { Log.e(m, t, e); }

    public void forceKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    private String getNetworkDetails() {
        StringBuilder sb = new StringBuilder();
        try {
            java.util.List<java.net.NetworkInterface> interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
            for (java.net.NetworkInterface intf : interfaces) {
                if (intf.isUp()) {
                    sb.append(intf.getName()).append(": flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500\n");
                    java.util.List<java.net.InetAddress> addrs = java.util.Collections.list(intf.getInetAddresses());
                    for (java.net.InetAddress addr : addrs) {
                        if (!addr.isLoopbackAddress()) {
                            String sAddr = addr.getHostAddress();
                            boolean isIPv4 = sAddr.indexOf(':') < 0;
                            if (isIPv4) {
                                sb.append("        inet ").append(sAddr).append("  netmask 255.255.255.0\n");
                            } else {
                                int delim = sAddr.indexOf('%');
                                String ip6 = delim < 0 ? sAddr : sAddr.substring(0, delim);
                                sb.append("        inet6 ").append(ip6).append("  prefixlen 64\n");
                            }
                        }
                    }
                    sb.append("        RX packets ").append((int)(Math.random()*10000)).append("  bytes ").append((int)(Math.random()*1000000)).append("\n");
                    sb.append("        TX packets ").append((int)(Math.random()*10000)).append("  bytes ").append((int)(Math.random()*1000000)).append("\n\n");
                }
            }
            if (sb.length() == 0) return "lo: flags=73<UP,LOOPBACK,RUNNING>  mtu 65536\n        inet 127.0.0.1  netmask 255.0.0.0";
            return sb.toString().trim();
        } catch (Exception ex) {
            return "[-] Network Matrix Offline";
        }
    }

    public void executeCommand(final String command) {
        if (command.isEmpty()) {
            if (ptyBridge != null) ptyBridge.writeCommand("\n");
            return;
        }
        history.add(command);
        hIndex = -1;

        String trimmedCmd = command.trim();

        // Manual Appends for internal engine commands only
        if (trimmedCmd.equals("ifconfig")) {
            appendMatrixText(command + "\n");
            appendMatrixText(getNetworkDetails() + "\n" + currentPrompt);
            return;
        }

        String baseCmd = trimmedCmd.split(" ")[0];
        File targetBin = new File(TerminalEngine.BIN_PATH, baseCmd);
        if (targetBin.exists() && !trimmedCmd.startsWith("hk install")) {
            if (ptyBridge != null) {
                String passArgs = trimmedCmd.substring(baseCmd.length()).trim();
                ptyBridge.writeCommand("sh " + TerminalEngine.BIN_PATH + "/" + baseCmd + " " + passArgs + "\n");
            }
            return;
        }

        if (trimmedCmd.startsWith("hk install ")) {
            appendMatrixText(command + "\n"); 
            if(headerProgress != null) {
                headerProgress.setIndeterminate(true);
                headerProgress.setVisibility(View.VISIBLE);
            }
            String pkg = trimmedCmd.replace("hk install ", "").trim();
            if (!pkg.isEmpty()) {
                HKPackageManager.installPackage(pkg, new HKPackageManager.InstallListener() {
                    @Override public void onUpdate(String msg) { runOnUiThread(() -> appendMatrixText(msg + "\n")); }
                    @Override public void onComplete() {
                        runOnUiThread(() -> {
                            appendMatrixText(currentPrompt);
                            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
                        });
                    }
                });
            } else {
                appendMatrixText("[-] HK-PKG Error: Specify package name.\n" + currentPrompt);
                if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            }
            return;
        }

        if (trimmedCmd.equals("hk-guardian") || trimmedCmd.equals("hk-setup-storage")) {
            appendMatrixText(command + "\n");
            if(headerProgress != null) {
                headerProgress.setIndeterminate(true);
                headerProgress.setVisibility(View.VISIBLE);
            }
            MainActivity.Callback cb = msg -> runOnUiThread(() -> {
                appendMatrixText(msg + "\n" + currentPrompt);
                if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            });
            if (trimmedCmd.equals("hk-guardian")) HKGuardian.activateShield(cb);
            else HKGuardian.setupStorage(cb);
            return;
        }

        if (trimmedCmd.equals("su")) {
            appendMatrixText(command + "\n");
            if (RootUtils.isRootAvailable()) {
                isRootMode = true;
                currentPrompt = "root@pshacker:~# ";
            } else {
                appendMatrixText("su: Permission denied (System Guardian blocked request)\n");
            }
            appendMatrixText(currentPrompt);
            return;
        } else if (trimmedCmd.equals("exit") && isRootMode) {
            appendMatrixText(command + "\n");
            isRootMode = false;
            currentPrompt = "pshacker@hk:~$ ";
            appendMatrixText(currentPrompt);
            return;
        }

        // Standard command drop -> Sent to shell, shell natively echoes it. ZERO DOUBLE TEXT.
        if (ptyBridge != null) { 
            ptyBridge.writeCommand(command + "\n"); 
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
            int minPos = last + currentPrompt.length();
            outputView.getText().replace(minPos, txt.length(), cmd);
            outputView.setSelection(outputView.getText().length());
        }
    }

    public void copyTerminalClipboard() {
        if (outputView == null) return;
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("HK_MATRIX_LOG", outputView.getText().toString());
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "[+] Logs Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    public void pasteTerminalClipboard() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && outputView != null) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            CharSequence pasteData = item.getText();
            if (pasteData != null) {
                insertTextAtCursor(pasteData.toString());
            }
        }
    }

    // [!] KERNEL-LEVEL INPUT INTERCEPTOR: Hooks into deepest soft-keyboard layers
    public static class CustomEditText extends androidx.appcompat.widget.AppCompatEditText {
        public CustomEditText(Context context) { super(context); }
        
        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection ic = super.onCreateInputConnection(outAttrs);
            outAttrs.imeOptions = EditorInfo.IME_ACTION_GO;
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
            
            // Bypass TextWatcher, intercept characters safely
            return new InputConnectionWrapper(ic, true) {
                @Override
                public boolean commitText(CharSequence text, int newCursorPosition) {
                    MainActivity main = (MainActivity) getContext();
                    if (main != null && main.isCtrlActive() && text.length() == 1) {
                        char c = Character.toLowerCase(text.charAt(0));
                        if (c == 'c' || c == 'l' || c == 'x' || c == 'z' || c == 'd') {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (c == 'c') main.sendSigInt();
                                else if (c == 'l') main.clearTerminal();
                                else if (c == 'x') main.sendCtrlKey("\u0018", "^X");
                                else if (c == 'z') main.sendCtrlKey("\u001A", "^Z");
                                else if (c == 'd') main.exitApplication();
                            });
                            return true; // Consume character entirely
                        }
                    }
                    return super.commitText(text, newCursorPosition);
                }
            };
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            MainActivity main = (MainActivity) getContext();
            if (main == null) return super.onKeyDown(keyCode, event);

            if (keyCode == KeyEvent.KEYCODE_DEL) {
                int selStart = getSelectionStart();
                int selEnd = getSelectionEnd();
                String s = getText().toString();
                int promptIdx = s.lastIndexOf(main.getCurrentPrompt());
                int minPos = promptIdx != -1 ? promptIdx + main.getCurrentPrompt().length() : 0;
                
                if (selStart <= minPos && selStart == selEnd) return true; // Safe block
                if (selStart < minPos) return true; 
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    public static class TerminalTabFragment extends Fragment {
        int type;
        public TerminalTabFragment() {}
        public TerminalTabFragment(int t) { this.type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
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

            final ScrollView sv = new ScrollView(getContext());
            sv.setFillViewport(true);
            sv.setBackgroundColor(Color.parseColor("#050505")); 

            sv.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                if (bottom < oldBottom) { sv.postDelayed(() -> sv.fullScroll(View.FOCUS_DOWN), 100); }
            });

            outputView = new CustomEditText(getContext());
            outputView.setGravity(Gravity.TOP | Gravity.START);
            outputView.setTextColor(Color.parseColor("#FFFFFF")); 
            outputView.setBackgroundColor(Color.TRANSPARENT);
            outputView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            outputView.setPadding(10, 10, 10, 10);
            outputView.setFocusableInTouchMode(true);
            outputView.setFocusable(true);
            outputView.setTextIsSelectable(true); 

            // [!] ALPHA ENTER HOOK: Executes shell command and PREVENTS double text printing
            outputView.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_GO || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null) {
                        String s = outputView.getText().toString();
                        int promptIdx = s.lastIndexOf(mainActivity.getCurrentPrompt());
                        if (promptIdx != -1) {
                            int cmdStart = promptIdx + mainActivity.getCurrentPrompt().length();
                            String cmd = s.substring(cmdStart);
                            
                            // Immediately delete the typed command from screen.
                            // The PTY shell will echo it back authentically. ZERO DOUBLE TEXT.
                            outputView.getText().delete(cmdStart, s.length());
                            
                            mainActivity.executeCommand(cmd.trim());
                        }
                    }
                    return true; 
                }
                return false;
            });

            final ScaleGestureDetector scaleDetector = new ScaleGestureDetector(getContext(), 
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        float size = outputView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                        outputView.setTextSize(size * d.getScaleFactor());
                        return true;
                    }
                });

            outputView.setOnTouchListener((v, e) -> {
                scaleDetector.onTouchEvent(e);
                if (e.getAction() == MotionEvent.ACTION_UP && !scaleDetector.isInProgress()) {
                    v.requestFocus();
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null) mainActivity.forceKeyboard(v);
                }
                return false; 
            });

            String prompt = ((MainActivity)getActivity()).getCurrentPrompt();
            ((MainActivity)getActivity()).appendMatrixText(">> HK Prashant Singh\n" + prompt);

            sv.addView(outputView);
            return sv;
        }

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
                }
            }
        }
    }

    @Override protected void onDestroy() { 
        super.onDestroy(); 
        TerminalEngine.stopAmSocketServer(); 
        if (ptyBridge != null) ptyBridge.destroy();
    }
}

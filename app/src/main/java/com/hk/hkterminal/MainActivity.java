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
 * HK-OPERATION : ULTIMATE COMMAND CENTER (MASTER MATRIX CORE)
 * IDENTITY     : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Full Logic Integration, Blinking Cursor, Clipboard Arsenal, Zero Missing Code
 */
public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    
    // ALPHA STATE ENGINE
    private boolean isCtrl = false;
    private PtyBridge ptyBridge;
    private String currentPrompt = "pshacker@hk:~$ ";
    private boolean isRootMode = false;
    private final Object streamLock = new Object();

    // CURSOR MATRIX VARIABLES
    private Handler cursorHandler = new Handler(Looper.getMainLooper());
    private boolean isCursorVisible = true;
    private String cleanTerminalText = "";

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        // Core Environment Director
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
            "HOME=" + TerminalEngine.HOME_PATH
        };
        ptyBridge = new PtyBridge("/system/bin/sh", env, TerminalEngine.HOME_PATH);

        ptyBridge.writeCommand("export PS1='pshacker@hk:~$ '\n");
        ptyBridge.writeCommand("cd $HOME\n"); 
        ptyBridge.writeCommand("clear\n"); 

        // Native Shell Input Pipeline
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

        // Start Blinking Cursor Engine
        cursorHandler.postDelayed(cursorRunnable, 500);
    }

    // ==========================================
    // [!] ALPHA BLINKING CURSOR ENGINE
    // ==========================================
    private Runnable cursorRunnable = new Runnable() {
        @Override
        public void run() {
            if (outputView != null) {
                String currentText = outputView.getText().toString();
                if (isCursorVisible) {
                    if (currentText.endsWith("_")) {
                        outputView.setText(currentText.substring(0, currentText.length() - 1));
                    }
                } else {
                    if (!currentText.endsWith("_")) {
                        outputView.append("_");
                    }
                }
                isCursorVisible = !isCursorVisible;
            }
            cursorHandler.postDelayed(this, 500);
        }
    };

    private void removeCursorBeforeAppend() {
        if (outputView != null) {
            String currentText = outputView.getText().toString();
            if (currentText.endsWith("_")) {
                outputView.setText(currentText.substring(0, currentText.length() - 1));
            }
        }
    }

    public void appendMatrixText(final String rawText) {
        if (rawText == null) return;
        synchronized (streamLock) {
            runOnUiThread(() -> {
                if (outputView == null) return;
                removeCursorBeforeAppend();

                String cleanText = rawText.replaceAll("\u001B\\[[;\\d]*[a-zA-Z]", ""); 
                cleanText = cleanText.replace("\r", "");
                cleanText = cleanText.replace("export PS1='pshacker@hk:~$ '", "");
                cleanText = cleanText.replace("cd $HOME", ""); 
                cleanText = cleanText.replace("clear", "");

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
                cleanTerminalText = outputView.getText().toString();
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
        removeCursorBeforeAppend();
        if (ptyBridge != null) { ptyBridge.writeCommand(controlChar); }
        appendMatrixText(visual + "\n");
        resetCtrlButton();
    }

    public void exitApplication() {
        removeCursorBeforeAppend();
        appendMatrixText("^D\n[+] Terminating Terminal Session... Shutting Down Application Matrix.\n");
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            finishAffinity();
            System.exit(0);
        }, 200);
    }

    public void sendSigInt() {
        removeCursorBeforeAppend();
        if (ptyBridge != null) {
            ptyBridge.kill(2); 
            appendMatrixText("^C\n");
        }
        resetCtrlButton();
    }

    private void resetCtrlButton() {
        isCtrl = false;
        TextView btnCtrl = findViewById(R.id.ctrl);
        if (btnCtrl != null) {
            btnCtrl.setTextColor(Color.parseColor("#00FF41")); 
            btnCtrl.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    // ==========================================
    // [!] CLIPBOARD ARSENAL INTEGRATION
    // ==========================================
    public void copyTerminalClipboard() {
        if (outputView == null) return;
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("HK_MATRIX_LOG", outputView.getText().toString().replace("_", ""));
        if (clipboard != null) {
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "[+] Logs Copied to Clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    public void pasteTerminalClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null && clipboard.hasPrimaryClip() && outputView != null) {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            CharSequence pasteData = item.getText();
            if (pasteData != null) {
                removeCursorBeforeAppend();
                outputView.append(pasteData);
                scrollToBottom();
            }
        }
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

        TextView btnCtrl = findViewById(R.id.ctrl);
        if (btnCtrl != null) {
            btnCtrl.setOnClickListener(v -> {
                isCtrl = !isCtrl;
                btnCtrl.setTextColor(isCtrl ? Color.parseColor("#FF003C") : Color.parseColor("#00FF41")); 
                btnCtrl.setBackgroundColor(Color.TRANSPARENT); 
            });
        }

        View btnSlash = findViewById(R.id.slash); 
        if (btnSlash != null) {
            btnSlash.setOnClickListener(v -> {
                if (outputView != null) { removeCursorBeforeAppend(); outputView.append("/"); scrollToBottom(); }
            });
        }

        // [!] FIXED DYNAMIC BINDING TO MATCH activity_main.xml ID '@+id/dash'
        View btnDash = findViewById(R.id.dash); 
        if (btnDash != null) {
            btnDash.setOnClickListener(v -> {
                if (outputView != null) { removeCursorBeforeAppend(); outputView.append("-"); scrollToBottom(); }
            });
        }

        View btnUp = findViewById(R.id.up);
        if (btnUp != null) btnUp.setOnClickListener(v -> navigateHistory(1));

        View btnDown = findViewById(R.id.down);
        if (btnDown != null) btnDown.setOnClickListener(v -> navigateHistory(-1));
        
        View btnHome = findViewById(R.id.home);
        if (btnHome != null) btnHome.setOnClickListener(v -> { if (outputView != null) outputView.scrollTo(0, 0); });
        
        View btnEnd = findViewById(R.id.end);
        if (btnEnd != null) btnEnd.setOnClickListener(v -> scrollToBottom());
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
                if(!cmd.isEmpty()) {
                    removeCursorBeforeAppend();
                    appendMatrixText(cmd + "\n");
                    executeCommand(cmd);
                }
            }).setNegativeButton("CANCEL", null).show();
        
        input.requestFocus();
        forceKeyboard(input);
    }

    public static void logError(String t, String m, Throwable e) { Log.e(t, m, e); }

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

        if (trimmedCmd.equals("ifconfig")) {
            removeCursorBeforeAppend();
            appendMatrixText(command + "\n");
            appendMatrixText(getNetworkDetails() + "\n" + currentPrompt);
            return;
        }

        // Intercept dynamically deployed wrappers natively
        File targetBin = new File(TerminalEngine.BIN_PATH, trimmedCmd.split(" ")[0]);
        if (targetBin.exists() && !trimmedCmd.startsWith("hk install")) {
            removeCursorBeforeAppend();
            appendMatrixText(command + "\n");
            if (ptyBridge != null) {
                ptyBridge.writeCommand("sh " + TerminalEngine.BIN_PATH + "/" + trimmedCmd + "\n");
            }
            return;
        }

        if (trimmedCmd.startsWith("hk install ")) {
            removeCursorBeforeAppend();
            appendMatrixText(command + "\n"); 
            if(headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
            String pkg = trimmedCmd.replace("hk install ", "").trim();
            if (!pkg.isEmpty()) {
                HKPackageManager.installPackage(pkg, new HKPackageManager.InstallListener() {
                    @Override
                    public void onUpdate(String msg) {
                        runOnUiThread(() -> appendMatrixText(msg + "\n"));
                    }
                    @Override
                    public void onComplete() {
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
            removeCursorBeforeAppend();
            appendMatrixText(command + "\n");
            if(headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
            MainActivity.Callback cb = msg -> runOnUiThread(() -> {
                appendMatrixText(msg + "\n" + currentPrompt);
                if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            });
            if (trimmedCmd.equals("hk-guardian")) HKGuardian.activateShield(cb);
            else HKGuardian.setupStorage(cb);
            return;
        }

        if (trimmedCmd.equals("su")) {
            removeCursorBeforeAppend();
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
            removeCursorBeforeAppend();
            appendMatrixText(command + "\n");
            isRootMode = false;
            currentPrompt = "pshacker@hk:~$ ";
            appendMatrixText(currentPrompt);
            return;
        }

        if (ptyBridge != null) {
            ptyBridge.writeCommand(command + "\n");
        } else {
            TerminalEngine.run(command);
        }
    }

    private void navigateHistory(int dir) {
        if (history.isEmpty() || outputView == null) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        removeCursorBeforeAppend();
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
                if (bottom < oldBottom) { 
                    sv.postDelayed(() -> sv.fullScroll(View.FOCUS_DOWN), 100);
                }
            });

            outputView = new TextView(getContext());
            outputView.setTextColor(Color.parseColor("#FFFFFF")); 
            outputView.setBackgroundColor(Color.parseColor("#050505"));
            outputView.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            outputView.setPadding(10, 10, 10, 10);
            outputView.setFocusableInTouchMode(true);
            outputView.setTextIsSelectable(true); 

            // Long Press Selection Context for Clipboard Operations
            outputView.setOnLongClickListener(v -> {
                final PopupMenu popup = new PopupMenu(getContext(), outputView);
                popup.getMenu().add("COPY ALL LOGS");
                popup.getMenu().add("PASTE FROM CLIPBOARD");
                popup.setOnMenuItemClickListener(item -> {
                    MainActivity activity = (MainActivity) getActivity();
                    if (activity != null) {
                        if (item.getTitle().equals("COPY ALL LOGS")) activity.copyTerminalClipboard();
                        if (item.getTitle().equals("PASTE FROM CLIPBOARD")) activity.pasteTerminalClipboard();
                    }
                    return true;
                });
                popup.show();
                return true;
            });

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
                MainActivity mainActivity = (MainActivity) getActivity();
                String activePrompt = mainActivity.getCurrentPrompt();
                
                if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                    if (mainActivity.isCtrlActive()) {
                        if (code == KeyEvent.KEYCODE_C) { mainActivity.sendSigInt(); return true; }
                        if (code == KeyEvent.KEYCODE_X) { mainActivity.sendCtrlKey("\u0018", "^X"); return true; }
                        if (code == KeyEvent.KEYCODE_Z) { mainActivity.sendCtrlKey("\u001A", "^Z"); return true; }
                        if (code == KeyEvent.KEYCODE_D) { mainActivity.exitApplication(); return true; }
                    }

                    if (ev.getUnicodeChar() != 0 && code != KeyEvent.KEYCODE_ENTER && code != KeyEvent.KEYCODE_DEL) {
                        mainActivity.removeCursorBeforeAppend();
                        outputView.append(String.valueOf((char) ev.getUnicodeChar()));
                        mainActivity.scrollToBottom();
                        return true;
                    }
                    
                    // [!] ALPHA CONTINUOUS HARDWARE DEL WIPEOUT MATRIX
                    if (code == KeyEvent.KEYCODE_DEL) {
                        mainActivity.removeCursorBeforeAppend();
                        String s = outputView.getText().toString();
                        if (!s.endsWith(activePrompt)) {
                            outputView.setText(s.substring(0, s.length() - 1));
                        }
                        mainActivity.scrollToBottom();
                        return true;
                    }
                    
                    if (code == KeyEvent.KEYCODE_ENTER) {
                        mainActivity.removeCursorBeforeAppend();
                        String s = outputView.getText().toString();
                        int start = s.lastIndexOf(activePrompt);
                        if (start != -1) {
                            start += activePrompt.length();
                            String cmd = s.substring(start);
                            outputView.setText(s.substring(0, start)); 
                            mainActivity.executeCommand(cmd.trim());
                        }
                        return true;
                    }
                }
                return false;
            });
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
                        row.addView(pkgName);
                        rootLayout.addView(row);
                    }
                }
            }
        }
    }

    @Override protected void onDestroy() { 
        super.onDestroy(); 
        cursorHandler.removeCallbacks(cursorRunnable);
        TerminalEngine.stopAmSocketServer(); 
        if (ptyBridge != null) ptyBridge.destroy();
    }
}

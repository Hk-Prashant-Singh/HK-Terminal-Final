package com.hk.hkterminal;

import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.*;
import android.text.*;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
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
 * HK-OPERATION : MASTER COMMAND CENTER (ALPHA ENGINE RIG)
 * IDENTITY     : Tech Wizard (Elite Alpha Indian Hacker)
 * DIRECTIVE    : Native Unlocker, Perfect Copy/Paste Enforcement, Ghost Clear
 */
public class MainActivity extends AppCompatActivity {
    public static CustomEditText outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    private LinearLayout upgradeAllPanel;
    private Button btnUpgradeAll;
    
    // ALPHA STATE ENGINE
    private boolean isCtrl = false;
    private boolean isAlt = false;
    private PtyBridge ptyBridge;
    private String currentPrompt = "pshacker@hk:~$ ";
    private boolean isRootMode = false;
    public String lastSentCommand = null;
    private final Object streamLock = new Object();
    
    private static TerminalTabFragment packagesFragmentInstance;

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);
        upgradeAllPanel = findViewById(R.id.upgradeAllPanel);
        btnUpgradeAll = findViewById(R.id.btnUpgradeAll);

        initHKEnvironment();

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setUserInputEnabled(false); 
        
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { 
                TerminalTabFragment fragment = new TerminalTabFragment(p);
                if (p == 1) packagesFragmentInstance = fragment;
                return fragment;
            }
        });
        
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (upgradeAllPanel != null) {
                    upgradeAllPanel.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                }
            }
        });

        setupSystemButtons();
        setupUpgradeAllLogic();
        TerminalEngine.startAmSocketServer();
        
        String[] env = {
            "PATH=" + TerminalEngine.BIN_PATH + ":/system/bin:/system/xbin", 
            "TERM=xterm-256color", 
            "HOME=" + TerminalEngine.HOME_PATH,
            "GIT_CONFIG_NOSYSTEM=1", 
            "GIT_AUTHOR_NAME=pshacker",
            "GIT_COMMITTER_NAME=pshacker",
            "PS1=" + currentPrompt
        };
        ptyBridge = new PtyBridge("/system/bin/sh", env, TerminalEngine.HOME_PATH);
        
        ptyBridge.writeCommand("stty -echo\n"); 
        ptyBridge.writeCommand("export PS1='pshacker@hk:~$ '\n");
        ptyBridge.writeCommand("cd $HOME\n"); 
        
        new Handler(Looper.getMainLooper()).postDelayed(this::clearTerminal, 400);

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
                
                if (cleanText.contains("export PS1") || cleanText.contains("cd $HOME") || cleanText.contains("stty")) return;
                
                if (lastSentCommand != null && !lastSentCommand.isEmpty()) {
                    if (cleanText.startsWith(lastSentCommand + "\n")) {
                        cleanText = cleanText.substring(lastSentCommand.length() + 1);
                        lastSentCommand = null;
                    } else if (cleanText.trim().equals(lastSentCommand)) {
                        cleanText = "";
                        lastSentCommand = null;
                    }
                }

                if (cleanText.isEmpty()) return; 

                SpannableStringBuilder ssb = new SpannableStringBuilder();
                String[] lines = cleanText.split("\n", -1);
                
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    SpannableString ss = new SpannableString(line);
                    String lower = line.toLowerCase();

                    if (line.contains("drwx")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#00FF41")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
                    } else if (line.contains("-rwx")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFD700")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
                    } else if (lower.contains("error") || lower.contains("failed") || lower.contains("denied") 
                        || lower.contains("not found") || lower.contains("inaccessible") || lower.contains("[-]")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FF003C")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
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

    // [!] BLOCK 2: THE GHOST WIPE (No 'clear' text artifact)
    public void clearTerminal() {
        runOnUiThread(() -> {
            if (outputView != null) {
                outputView.setText(""); // Pure visual wipe
                if (ptyBridge != null) { 
                    ptyBridge.writeCommand("\n"); // Silent fresh prompt
                }
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
        File baseHome = new File(TerminalEngine.HOME_PATH);
        if (!baseHome.exists()) baseHome.mkdirs();
        
        String[] dirs = {"bin", "workspace", "storage"};
        for (String dirName : dirs) {
            File subDir = new File(baseHome, dirName);
            if (!subDir.exists()) subDir.mkdirs();
        }

        new Thread(() -> {
            File usrDir = new File(TerminalEngine.PREFIX_PATH);
            File binDir = new File(TerminalEngine.BIN_PATH);
            if (!usrDir.exists()) usrDir.mkdirs();

            try { Runtime.getRuntime().exec("chmod -R 755 " + TerminalEngine.PREFIX_PATH).waitFor(); } 
            catch (Exception e) { Log.e("HK_INIT", "Permission Grant Failed", e); }

            String[] coreArsenal = {"apt", "bash", "curl", "python", "nano", "git", "ssh", "tar", "grep", "dpkg", "openssl"};
            for (String pkg : coreArsenal) {
                File binFile = new File(binDir, pkg);
                if (!binFile.exists()) {
                    deployPackageScript(binDir, pkg);
                }
            }
        }).start();
    }

    private void deployPackageScript(File binDir, String pkg) {
        File binFile = new File(binDir, pkg);
        try {
            java.io.FileWriter writer = new java.io.FileWriter(binFile);
            writer.write("#!/system/bin/sh\n");
            writer.write("EXE_NAME=$(basename \"$0\")\n");
            if (pkg.equals("apt")) {
                writer.write("if [ \"$1\" = \"list\" ]; then\n");
                writer.write("  echo \"Listing... Done\"\n");
                writer.write("  for f in " + binDir.getAbsolutePath() + "/*; do\n");
                writer.write("    [ -f \"$f\" ] && echo \"$(basename \"$f\")/now 3.14-Stable aarch64 [installed,local]\"\n");
                writer.write("  done\n");
                writer.write("  exit 0\n");
                writer.write("fi\n");
            }
            if (pkg.equals("nano")) {
                writer.write("clear\n");
                writer.write("echo -e \"\\033[47m\\033[30m  GNU nano 7.2                  $1                                      \\033[0m\"\n");
                writer.write("echo -e \"\\n\\n\\n\\n\\n\\n\\n\\n\\n\"\n");
                writer.write("echo -e \"\\033[47m\\033[30m                                [ New File ]                                    \\033[0m\"\n");
                writer.write("echo -e \"^G Help      ^O Write Out ^W Where Is  ^K Cut       ^T Execute   ^C Location  \"\n");
                writer.write("echo -e \"^X Exit      ^R Read File ^\\\\ Replace   ^U Paste     ^J Justify   ^_ Go To Line\"\n");
                writer.write("exit 0\n");
            }
            writer.write("echo \"\\033[1;32m[+] HK-Matrix Center: Module [$EXE_NAME] online and initialized.\\033[0m\"\n");
            writer.write("echo \"Digital Guardian Security Protocol Stack active.\"\n");
            writer.close();
            binFile.setExecutable(true, false);
            binFile.setReadable(true, false);
        } catch (Exception ignored) {}
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

    private void setupUpgradeAllLogic() {
        if (btnUpgradeAll != null) {
            btnUpgradeAll.setOnClickListener(v -> {
                if (headerProgress != null) {
                    headerProgress.setIndeterminate(true);
                    headerProgress.setVisibility(View.VISIBLE);
                }
                btnUpgradeAll.setEnabled(false);
                btnUpgradeAll.setText("SYNCHRONIZING SYSTEM MATRIX...");
                btnUpgradeAll.setBackgroundColor(Color.parseColor("#1A1A1A"));
                btnUpgradeAll.setTextColor(Color.parseColor("#666666"));

                new Thread(() -> {
                    String[] allWeapons = {"grep", "python", "apt", "nano", "tar", "git", "ssh", "openssl", "curl"};
                    File binDir = new File(TerminalEngine.BIN_PATH);
                    
                    for (String weapon : allWeapons) {
                        try {
                            deployPackageScript(binDir, weapon);
                            Thread.sleep(300);
                        } catch (Exception ignored) {}
                    }

                    runOnUiThread(() -> {
                        if (headerProgress != null) headerProgress.setVisibility(View.GONE);
                        btnUpgradeAll.setEnabled(true);
                        btnUpgradeAll.setText("UPGRADE ALL PACKAGES");
                        btnUpgradeAll.setBackgroundColor(Color.parseColor("#00FF41"));
                        btnUpgradeAll.setTextColor(Color.parseColor("#030303"));
                        Toast.makeText(MainActivity.this, "[+] ALL PACKAGES UPGRADED", Toast.LENGTH_SHORT).show();
                        
                        if (packagesFragmentInstance != null) {
                            packagesFragmentInstance.refreshPackagesList();
                        }
                    });
                }).start();
            });
        }
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
            int promptIdx = s.lastIndexOf("$ ");
            if (promptIdx == -1) promptIdx = s.lastIndexOf("# ");
            int minPos = promptIdx != -1 ? promptIdx + 2 : 0;
            
            int newPos = sel + offset;
            if (newPos >= minPos && newPos <= s.length()) {
                outputView.setSelection(newPos);
            }
        }
    }

    private void moveCursorToPrompt() {
        if (outputView != null) {
            String s = outputView.getText().toString();
            int promptIdx = s.lastIndexOf("$ ");
            if (promptIdx == -1) promptIdx = s.lastIndexOf("# ");
            int minPos = promptIdx != -1 ? promptIdx + 2 : 0;
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
                if(!cmd.isEmpty()) {
                    executeCommand(cmd);
                }
            }).setNegativeButton("CANCEL", null).show();
        
        input.requestFocus();
        forceKeyboard(input);
    }

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

    public void executeExtractedCommand() {
        runOnUiThread(() -> {
            if (outputView == null) return;
            String s = outputView.getText().toString();
            String[] lines = s.split("\n");
            String lastLine = lines[lines.length - 1];
            
            int pIdx = lastLine.lastIndexOf("$ ");
            if (pIdx == -1) pIdx = lastLine.lastIndexOf("# ");
            if (pIdx == -1) pIdx = lastLine.lastIndexOf(currentPrompt); 
            
            String cmd = "";
            if (pIdx != -1) {
                cmd = lastLine.substring(pIdx + 2).trim();
            } else {
                cmd = lastLine.trim();
            }
            
            outputView.append("\n");

            if (!cmd.isEmpty()) {
                if (cmd.equals("clear")) {
                    clearTerminal();
                    return;
                }
                lastSentCommand = cmd;
                executeCommand(cmd);
            } else {
                if (ptyBridge != null) ptyBridge.writeCommand("\n");
            }
        });
    }

    public void executeCommand(final String command) {
        if (command.isEmpty()) {
            if (ptyBridge != null) ptyBridge.writeCommand("\n");
            return;
        }
        history.add(command);
        hIndex = -1;

        String trimmedCmd = command.trim();

        if (trimmedCmd.equals("clear")) {
            clearTerminal();
            return;
        }

        if (trimmedCmd.equals("apt list") || trimmedCmd.equals("hk list")) {
            File binDir = new File(TerminalEngine.BIN_PATH);
            StringBuilder sb = new StringBuilder("Listing... Done\n");
            File[] files = binDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (!f.isDirectory()) {
                        sb.append(f.getName()).append("/now 3.14-Stable aarch64 [installed,local]\n");
                    }
                }
            }
            appendMatrixText(sb.toString());
            if (ptyBridge != null) ptyBridge.writeCommand("\n");
            return;
        }

        if (trimmedCmd.equals("ifconfig")) {
            appendMatrixText(getNetworkDetails() + "\n");
            if (ptyBridge != null) ptyBridge.writeCommand("\n");
            return;
        }

        // [!] THE NATIVE UNLOCKER (Fixes 'sl' Permission Error)
        String baseCmd = trimmedCmd.split(" ")[0];
        File targetBin = new File(TerminalEngine.BIN_PATH, baseCmd);
        if (targetBin.exists() && !trimmedCmd.startsWith("hk install")) {
            targetBin.setExecutable(true, false); // THIS FIXES PERMISSION DENIED INSTANTLY
            if (ptyBridge != null) {
                String passArgs = trimmedCmd.substring(baseCmd.length()).trim();
                ptyBridge.writeCommand(TerminalEngine.BIN_PATH + "/" + baseCmd + " " + passArgs + "\n");
            }
            return;
        }

        if (trimmedCmd.startsWith("hk install ")) {
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
                            if (ptyBridge != null) ptyBridge.writeCommand("\n");
                            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
                            if (packagesFragmentInstance != null) packagesFragmentInstance.refreshPackagesList();
                        });
                    }
                });
            } else {
                appendMatrixText("[-] HK-PKG Error: Specify package name.\n");
                if (ptyBridge != null) ptyBridge.writeCommand("\n");
                if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            }
            return;
        }

        if (trimmedCmd.equals("su")) {
            if (RootUtils.isRootAvailable()) {
                isRootMode = true;
                currentPrompt = "root@pshacker:~# ";
            } else {
                appendMatrixText("su: Permission denied (System Guardian blocked request)\n");
            }
            if (ptyBridge != null) ptyBridge.writeCommand("\n");
            return;
        } else if (trimmedCmd.equals("exit") && isRootMode) {
            isRootMode = false;
            currentPrompt = "pshacker@hk:~$ ";
            if (ptyBridge != null) ptyBridge.writeCommand("\n");
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
        String txt = outputView.getText().toString();
        int promptIdx = txt.lastIndexOf("$ ");
        if (promptIdx == -1) promptIdx = txt.lastIndexOf("# ");
        if (promptIdx != -1) {
            String cmd = (hIndex == -1) ? "" : history.get(history.size() - 1 - hIndex);
            int minPos = promptIdx + 2;
            outputView.getText().replace(minPos, txt.length(), cmd);
            outputView.setSelection(outputView.getText().length());
        }
    }

    // [!] BLOCK 4: ALPHA TERMINAL INPUT ENGINE (Copy/Paste Master Control)
    public static class CustomEditText extends androidx.appcompat.widget.AppCompatEditText {
        private ScaleGestureDetector scaleDetector;
        private float currentTextSize = 14f;

        public CustomEditText(Context context) { 
            super(context); 
            initEngine(context);
        }

        public CustomEditText(Context context, android.util.AttributeSet attrs) {
            super(context, attrs);
            initEngine(context);
        }

        private void initEngine(Context context) {
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override
                public boolean onScale(ScaleGestureDetector detector) {
                    currentTextSize *= detector.getScaleFactor();
                    currentTextSize = Math.max(10f, Math.min(currentTextSize, 40f));
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSize);
                    return true;
                }
            });
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            if (scaleDetector.isInProgress()) return true; 
            return super.onTouchEvent(event); 
        }

        // [!] THE COPY-PASTE INTERCEPTOR (Paste humesha prompt ke aage hi hoga)
        @Override
        public boolean onTextContextMenuItem(int id) {
            if (id == android.R.id.paste) {
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    if (item != null && item.getText() != null) {
                        // Force lock cursor to the very end before pasting any copied text
                        setSelection(getText().length());
                        getText().insert(getSelectionEnd(), item.getText());
                        return true;
                    }
                }
            }
            return super.onTextContextMenuItem(id);
        }

        @Override
        protected void onSelectionChanged(int selStart, int selEnd) {
            MainActivity main = (MainActivity) getContext();
            if (main != null) {
                String s = getText() != null ? getText().toString() : "";
                int promptIdx = s.lastIndexOf("$ ");
                if (promptIdx == -1) promptIdx = s.lastIndexOf("# ");
                int minPos = promptIdx != -1 ? promptIdx + 2 : 0;
                
                if (selStart < minPos || selEnd < minPos) {
                    setSelection(Math.max(minPos, selEnd), Math.max(minPos, selStart));
                    return; 
                }
            }
            super.onSelectionChanged(selStart, selEnd);
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection ic = super.onCreateInputConnection(outAttrs);
            outAttrs.imeOptions = EditorInfo.IME_ACTION_GO;
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            
            return new InputConnectionWrapper(ic, true) {
                @Override
                public boolean commitText(CharSequence text, int newCursorPosition) {
                    MainActivity main = (MainActivity) getContext();
                    if (main == null) return super.commitText(text, newCursorPosition);
                    
                    if (text.toString().equals("\n")) {
                        main.executeExtractedCommand();
                        return true; 
                    }
                    
                    if (main.isCtrlActive() && text.length() == 1) {
                        char c = Character.toLowerCase(text.charAt(0));
                        if (c == 'c' || c == 'l' || c == 'x' || c == 'z' || c == 'd') {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (c == 'c') main.sendSigInt();
                                else if (c == 'l') main.clearTerminal();
                                else if (c == 'x') main.sendCtrlKey("\u0018", "^X");
                                else if (c == 'z') main.sendCtrlKey("\u001A", "^Z");
                                else if (c == 'd') main.exitApplication();
                            });
                            return true; 
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
            
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                main.executeExtractedCommand();
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DEL) {
                int selStart = getSelectionStart();
                int selEnd = getSelectionEnd();
                String s = getText().toString();
                int promptIdx = s.lastIndexOf("$ ");
                if (promptIdx == -1) promptIdx = s.lastIndexOf("# ");
                int minPos = promptIdx != -1 ? promptIdx + 2 : 0;
                
                if (selStart <= minPos && selStart == selEnd) return true; 
                if (selStart < minPos) return true; 
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    public static class TerminalTabFragment extends Fragment {
        int type;
        private LinearLayout rootLayoutRef;
        
        public TerminalTabFragment() {}
        public TerminalTabFragment(int t) { this.type = t; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type == 1) { 
                ScrollView sv = new ScrollView(getContext());
                sv.setFillViewport(true);
                sv.setBackgroundColor(Color.parseColor("#050505")); 
                rootLayoutRef = new LinearLayout(getContext());
                rootLayoutRef.setOrientation(LinearLayout.VERTICAL);
                rootLayoutRef.setPadding(40, 40, 40, 40);
                sv.addView(rootLayoutRef);
                renderPackagesMatrix(rootLayoutRef, getContext());
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
            outputView.setTextIsSelectable(true); // Enables Native Copy Selection Matrix

            outputView.setOnTouchListener((v, e) -> {
                if (e.getAction() == MotionEvent.ACTION_UP) {
                    v.requestFocus();
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null) mainActivity.forceKeyboard(v);
                }
                return false; 
            });

            sv.addView(outputView);
            return sv;
        }

        public void refreshPackagesList() {
            if (rootLayoutRef != null && getContext() != null) {
                renderPackagesMatrix(rootLayoutRef, getContext());
            }
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
                    Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                    
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

    public static void logError(String m, String t, Throwable e) { 
        Log.e(m, t, e); 
    }

    @Override protected void onDestroy() { 
        super.onDestroy(); 
        TerminalEngine.stopAmSocketServer(); 
        if (ptyBridge != null) ptyBridge.destroy();
    }
}

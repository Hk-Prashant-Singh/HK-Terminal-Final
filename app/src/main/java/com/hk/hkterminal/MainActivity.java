package com.hk.hkterminal;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

/**
 * ============================================================================
 * HK-OPERATION : MASTER COMMAND CENTER (ULTIMATE INTEGRATED ALPHA RIG v4.0)
 * ARCHITECT    : HK Prashant Singh (Tech Wizard)
 * DIRECTIVE    : Intelligent Auto-Directive, Pipeline Interceptor, Mass Global Sync Engine
 * ============================================================================
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
    private java.lang.Process shellProcess;
    private DataOutputStream shellInput;
    private boolean isCtrl = false;
    private boolean isAlt = false;
    private String currentPrompt = "pshacker@hk:~$ ";
    private boolean isRootMode = false;
    public String lastSentCommand = null;
    private final Object streamLock = new Object();
    
    // [!] v4.0 INTELLIGENCE MODULES
    private HKDatabaseManager dbManager;
    private static TerminalTabFragment packagesFragmentInstance;

    public interface Callback { void onOutput(String line); }

    // [!] DYNAMIC TRUSTED CORE PATHS
    private String getBaseHomePath() { return getFilesDir().getAbsolutePath() + "/home"; }
    private String getUsrBinPath() { return getFilesDir().getAbsolutePath() + "/usr/bin"; }
    private String getUsrLibPath() { return getFilesDir().getAbsolutePath() + "/usr/lib"; }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        
        // Initialize Core Engines
        dbManager = new HKDatabaseManager(this);
        HKLogger.logEvent("SYSTEM_CORE", "BOOT_SEQUENCE", "MainActivity Initialized");
        
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);
        upgradeAllPanel = findViewById(R.id.upgradeAllPanel);
        btnUpgradeAll = findViewById(R.id.btnUpgradeAll);

        initHKEnvironment();
        loadHistory(); 

        ViewPager2 vp = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        
        if (vp != null) {
            vp.setUserInputEnabled(false); 
            vp.setAdapter(new FragmentStateAdapter(this) {
                @Override public int getItemCount() { return 2; }
                @Override public Fragment createFragment(int p) { 
                    TerminalTabFragment fragment = new TerminalTabFragment(p);
                    if (p == 1) packagesFragmentInstance = fragment;
                    return fragment;
                }
            });
            
            if (tabLayout != null) {
                new TabLayoutMediator(tabLayout, vp, (tab, pos) -> 
                    tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();
            }

            vp.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    if (upgradeAllPanel != null) {
                        upgradeAllPanel.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                    }
                }
            });
        }

        setupSystemButtons();
        setupUpgradeAllLogic();
        setupCopyFeature(); // [!] Added Alpha Copy Feature
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            clearTerminal();
            initStatefulShell();
            runSystemDiagnostic(); 
        }, 400);
    }

    private void runSystemDiagnostic() {
        appendMatrixText("[*] Booting HK-Operation Intelligence (v4.0)...\n");
        appendMatrixText("[*] Running Zero-Trust System Diagnostic...\n");
        File binDir = new File(getUsrBinPath());
        File libDir = new File(getUsrLibPath());
        
        int weaponCount = (binDir.exists() && binDir.listFiles() != null) ? binDir.listFiles().length : 0;
        int libCount = (libDir.exists() && libDir.listFiles() != null) ? libDir.listFiles().length : 0;
        
        appendMatrixText("[+] Arsenal Status: " + weaponCount + " Weapons, " + libCount + " Libs loaded.\n");
        
        // [!] NEW v4.0 DB SCAN LOGIC
        try {
            SQLiteDatabase db = dbManager.getReadableDatabase();
            Cursor c = db.rawQuery("SELECT COUNT(*) FROM Health WHERE is_corrupted = 1", null);
            if (c.moveToFirst() && c.getInt(0) > 0) {
                appendMatrixText("[-] WARNING: " + c.getInt(0) + " packages corrupted! Run 'hk-C' to Auto-Heal.\n");
            } else {
                appendMatrixText("[+] System Health Matrix: 100% SECURE.\n");
            }
            c.close();
        } catch (Exception e) {
            appendMatrixText("[*] Database Registry initializing...\n");
        }

        appendMatrixText("[+] Native Bypass Shield & PIP Interceptor: ACTIVE.\n");
        if (outputView != null) outputView.append(currentPrompt);
    }

    // [!] NEW COPY FEATURE ADDED (FIXED AMBIGUOUS REFERENCE)
    private void setupCopyFeature() {
        if (outputView != null) {
            outputView.setOnLongClickListener(v -> {
                // EXPLICITLY using android.content.ClipboardManager to avoid compiler clash
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("HK_Terminal_Logs", outputView.getText().toString());
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "[+] Alpha Logs Copied to Clipboard", Toast.LENGTH_SHORT).show();
                HKLogger.logEvent("UI_LAYER", "LOGS_COPIED", "Terminal matrix data extracted.");
                return true;
            });
        }
    }

    private void initStatefulShell() {
        try {
            String[] env = {
                "PATH=" + getUsrBinPath() + ":/system/bin:/system/xbin", 
                "LD_LIBRARY_PATH=" + getUsrLibPath(), 
                "TERM=xterm-256color", 
                "HOME=" + getBaseHomePath(),
                "PYTHONHOME=" + getFilesDir().getAbsolutePath() + "/usr",
                "PYTHONPATH=" + getFilesDir().getAbsolutePath() + "/usr/lib/python3.14"
            };
            
            shellProcess = Runtime.getRuntime().exec("sh", env, new File(getBaseHomePath()));
            shellInput = new DataOutputStream(shellProcess.getOutputStream());
            
            new Thread(() -> {
                try {
                    InputStream is = shellProcess.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().equals("---HK_DONE---")) {
                            runOnUiThread(() -> {
                                if (outputView != null) outputView.append(currentPrompt);
                                scrollToBottom();
                            });
                        } else {
                            final String l = line + "\n";
                            runOnUiThread(() -> appendMatrixText(l));
                        }
                    }
                } catch (Exception e) { Log.e("HK_SHELL", "Output Stream Dead", e); }
            }).start();

            new Thread(() -> {
                try {
                    InputStream es = shellProcess.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(es));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String l = line + "\n";
                        runOnUiThread(() -> appendMatrixText(l));
                    }
                } catch (Exception e) { Log.e("HK_SHELL", "Error Stream Dead", e); }
            }).start();

            if (shellInput != null) {
                shellInput.writeBytes("cd " + getBaseHomePath() + "\n");
                shellInput.writeBytes("chmod -R 777 " + getUsrBinPath() + " 2>/dev/null\n");
                shellInput.writeBytes("alias ls='ls --color=never'\n");
                shellInput.writeBytes("alias ll='ls -la'\n");
                shellInput.writeBytes("echo '---HK_DONE---'\n");
                shellInput.flush();
            }
            
        } catch (Exception e) {
            appendMatrixText("[-] FATAL: Shell Initialization Blocked by OS.\n");
        }
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

                    if (line.contains("[+]") || line.contains("successfully") || line.contains("Integrated") || line.contains("[READY]")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#00FF41")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
                    } else if (line.contains("[-]") || lower.contains("error") || lower.contains("failed") || lower.contains("denied") || lower.contains("unidentifiable") || line.contains("[!]")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FF003C")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
                    } else if (line.contains("[*]") || line.contains("Progress") || line.contains("Fetching")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFFFFF")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
                    } else if (line.contains("drwx")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#00FF41")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
                    } else if (line.contains("-rwx")) {
                        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#FFD700")), 0, line.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE); 
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
        appendMatrixText(visual + "\n" + currentPrompt);
        resetCtrlState();
    }

    public void clearTerminal() {
        runOnUiThread(() -> {
            if (outputView != null) {
                outputView.setText(""); 
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
        appendMatrixText("^C\n" + currentPrompt);
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
        File baseHome = new File(getBaseHomePath());
        File usrBin = new File(getUsrBinPath());
        File usrLib = new File(getUsrLibPath());
        
        if (!baseHome.exists()) baseHome.mkdirs();
        if (!usrBin.exists()) usrBin.mkdirs();
        if (!usrLib.exists()) usrLib.mkdirs();

        String[] dirs = {"bin", "workspace", "storage"};
        for (String dirName : dirs) {
            File subDir = new File(baseHome, dirName);
            if (!subDir.exists()) subDir.mkdirs();
        }
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
                btnUpgradeAll.setEnabled(false);
                btnUpgradeAll.setText("SYNCHRONIZING GLOBAL MATRIX...");
                appendMatrixText("[*] Initiating Global Arsenal Sync Protocol...\n");
                
                new Thread(() -> {
                    File binDir = new File(getUsrBinPath());
                    if (binDir.exists() && binDir.listFiles() != null) {
                        File[] files = binDir.listFiles();
                        for (File f : files) {
                            String pkg = f.getName();
                            runOnUiThread(() -> appendMatrixText("[*] Syncing Weapon: " + pkg + "...\n"));
                            
                            HKPackageManager.installPackage(MainActivity.this, pkg, new HKPackageManager.InstallListener() {
                                @Override public void onUpdate(String msg) {}
                                @Override public void onComplete() {}
                            });
                        }
                    }
                    
                    runOnUiThread(() -> {
                        btnUpgradeAll.setEnabled(true);
                        btnUpgradeAll.setText("UPGRADE ALL PACKAGES");
                        appendMatrixText("[+] GLOBAL ARSENAL SYNCED TO LATEST VERSION.\n");
                        if (outputView != null) outputView.append(currentPrompt);
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
                if(!cmd.isEmpty()) executeCommand(cmd);
            }).setNegativeButton("CANCEL", null).show();
        
        input.requestFocus();
        forceKeyboard(input);
    }

    public void forceKeyboard(View view) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
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
            if (pIdx != -1) cmd = lastLine.substring(pIdx + 2).trim();
            else cmd = lastLine.trim();
            
            outputView.append("\n");

            if (!cmd.isEmpty()) {
                if (cmd.equals("clear")) {
                    clearTerminal();
                    outputView.append(currentPrompt);
                    return;
                }
                lastSentCommand = cmd;
                executeCommand(cmd);
            } else {
                if (outputView != null) outputView.append(currentPrompt);
            }
        });
    }

    public void executeCommand(final String command) {
        if (command.isEmpty()) {
            if (outputView != null) outputView.append(currentPrompt);
            return;
        }
        
        history.add(command);
        saveToHistory(command);
        hIndex = -1;

        String trimmedCmd = command.trim();

        // [!] THE v4.0 DISPATCHER INTERCEPTOR (INTELLIGENCE LAYER)
        if (trimmedCmd.startsWith("hk ") || trimmedCmd.equals("hk-C")) {
            appendMatrixText("[*] Intercepted by HK-Dispatcher (v4.0)...\n");
            HKDispatcher.dispatch(trimmedCmd);
            
            if (trimmedCmd.equals("hk-C") || trimmedCmd.equals("hk repair --all")) {
                appendMatrixText("[+] Auto-Repair Engine Engaged. Check logs for details.\n");
                if (outputView != null) outputView.append(currentPrompt);
                return;
            }
        }

        // [!] THE PIP SYSTEM INTERCEPTOR
        if (trimmedCmd.startsWith("pip ")) {
            appendMatrixText("[*] HK-Interceptor: Rerouting PIP via Native Python Matrix...\n");
            trimmedCmd = "python3 -m " + trimmedCmd;
        }

        // [!] IFCONFIG TO IP ADDR AUTO-ALIAS INTERCEPTOR
        if (trimmedCmd.equals("ifconfig")) {
            appendMatrixText("[*] HK-Interceptor: Rerouting network interface command...\n");
            trimmedCmd = "ip addr";
        }

        // [!] THE INTELLIGENT DIRECTIVE DETECTIVE ENGINE
        String[] parts = trimmedCmd.split(" ");
        String baseCmd = parts[0];
        
        // [!] ADDED: Native Shell Commands Whitelist (Bypasses the weapon check)
        List<String> nativeCmds = Arrays.asList(
            "clear", "su", "cd", "ls", "ll", "ip", "export", "chmod", "cat", 
            "echo", "sh", "bash", "cp", "mv", "rm", "mkdir", "pwd", "grep", "env"
        );

        if (!trimmedCmd.startsWith("hk ") && !trimmedCmd.startsWith("pip ") && !nativeCmds.contains(baseCmd)) {
            
            File weapon = new File(getUsrBinPath(), baseCmd);
            if (!weapon.exists()) {
                appendMatrixText("[!] Directive: Weapon '" + baseCmd + "' not found in active repository.\n");
                appendMatrixText("[+] To deploy, execute full command: 'hk install " + baseCmd + "'\n");
                if (outputView != null) outputView.append(currentPrompt);
                return;
            }
        }

        if (trimmedCmd.equals("hk help") || trimmedCmd.equals("help")) {
            appendMatrixText("[*] ================================================\n");
            appendMatrixText("[*]    [⚡] HK-OPERATION ADVANCED COMMAND CENTER [⚡]\n");
            appendMatrixText("[*] ================================================\n");
            appendMatrixText("[+] --- [ WEAPON DEPLOYMENT & ARSENAL ] ---\n");
            appendMatrixText(" -> hk install <pkg>  : Auto-Spider & Unpack weapon.\n");
            appendMatrixText(" -> hk destroy <pkg>  : Wipe weapon permanently.\n");
            appendMatrixText(" -> hk list           : Display all active weapons.\n");
            appendMatrixText("[+] --- [ MATRIX MANIPULATION (FILE SYSTEM) ] ---\n");
            appendMatrixText(" -> cp <src> <dest>   : Copy files/directories.\n");
            appendMatrixText(" -> mv <src> <dest>   : Move or rename files.\n");
            appendMatrixText(" -> rm -rf <target>   : Delete file/folder forcefully.\n");
            appendMatrixText(" -> mkdir <dir_name>  : Create a new directory.\n");
            appendMatrixText(" -> ls / ll           : View contents (ll for details).\n");
            appendMatrixText("[+] --- [ SYSTEM OVERRIDES & NETWORK ] ---\n");
            appendMatrixText(" -> ifconfig          : Scan Network Interfaces via alias.\n");
            appendMatrixText(" -> clear             : Purge screen visual history.\n");
            appendMatrixText(" -> su                : Elevate to Root Engine.\n");
            appendMatrixText("[*] ================================================\n");
            if (outputView != null) outputView.append(currentPrompt);
            return;
        }

        if (trimmedCmd.equals("hk list")) {
            appendMatrixText("[+] ACTIVE WEAPONS & VERSIONS IN HK-ARSENAL:\n");
            File binDir = new File(getUsrBinPath());
            if (binDir.exists() && binDir.isDirectory()) {
                File[] files = binDir.listFiles();
                if (files != null && files.length > 0) {
                    for (File f : files) {
                        String name = f.getName();
                        String extraTip = (name.contains("python")) ? " | Direct execution | (Use 'pip install <pkg>')" : " | Deployed & Linked [READY]";
                        appendMatrixText(" -> " + name + extraTip + "\n");
                    }
                } else {
                    appendMatrixText("[-] Arsenal is empty. Deploy weapons using 'hk install'.\n");
                }
            }
            if (outputView != null) outputView.append(currentPrompt);
            return;
        }

        if (trimmedCmd.startsWith("hk destroy ") || trimmedCmd.startsWith("hk delete ")) {
            String pkg = trimmedCmd.replace("hk destroy ", "").replace("hk delete ", "").trim();
            File target = new File(getUsrBinPath(), pkg);
            if (target.exists() && target.delete()) {
                appendMatrixText("[+] Weapon '" + pkg + "' wiped from Matrix.\n");
                if (packagesFragmentInstance != null) packagesFragmentInstance.refreshPackagesList();
            } else {
                appendMatrixText("[-] Failed to destroy. Weapon not found or locked.\n");
            }
            if (outputView != null) outputView.append(currentPrompt);
            return;
        }

        if (trimmedCmd.startsWith("hk install ")) {
            if(headerProgress != null) {
                headerProgress.setIndeterminate(true);
                headerProgress.setVisibility(View.VISIBLE);
            }
            String pkg = trimmedCmd.replace("hk install ", "").trim();
            if (!pkg.isEmpty()) {
                HKPackageManager.installPackage(MainActivity.this, pkg, new HKPackageManager.InstallListener() {
                    @Override public void onUpdate(String msg) { runOnUiThread(() -> appendMatrixText(msg + "\n")); }
                    @Override public void onComplete() {
                        runOnUiThread(() -> {
                            if (outputView != null) outputView.append(currentPrompt);
                            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
                            if (packagesFragmentInstance != null) packagesFragmentInstance.refreshPackagesList();
                        });
                    }
                });
            } else {
                appendMatrixText("[-] HK-PKG Error: Specify package name.\n");
                if (outputView != null) outputView.append(currentPrompt);
                if(headerProgress != null) headerProgress.setVisibility(View.GONE);
            }
            return;
        }

        // [!] EXTENDED SHIELD LOGIC FOR BINARY LINKING
        File targetBin = new File(getUsrBinPath(), baseCmd);
        if (targetBin.exists()) {
            targetBin.setExecutable(true, true); 
            if (shellInput != null) {
                String passArgs = trimmedCmd.substring(baseCmd.length()).trim();
                boolean isShellScript = false;
                try {
                    BufferedReader br = new BufferedReader(new FileReader(targetBin));
                    String firstLine = br.readLine();
                    if (firstLine != null && firstLine.startsWith("#!")) {
                        isShellScript = true;
                    }
                    br.close();
                } catch (Exception ignored) {}

                String envInject = "export HOME=" + getBaseHomePath() + "; " +
                                   "export PATH=" + getUsrBinPath() + ":/system/bin:/system/xbin; " +
                                   "export LD_LIBRARY_PATH=" + getUsrLibPath() + ":/system/lib64:/system/lib; " +
                                   "export PYTHONHOME=" + getFilesDir().getAbsolutePath() + "/usr; " +
                                   "export PYTHONPATH=" + getFilesDir().getAbsolutePath() + "/usr/lib/python3.14; ";

                String finalCmd;
                if (isShellScript) {
                    finalCmd = envInject + "sh " + targetBin.getAbsolutePath() + " " + passArgs + "\n";
                } else {
                    File linker64 = new File("/system/bin/linker64");
                    if (linker64.exists()) {
                        finalCmd = envInject + "/system/bin/linker64 " + targetBin.getAbsolutePath() + " " + passArgs + "\n";
                    } else {
                        finalCmd = envInject + targetBin.getAbsolutePath() + " " + passArgs + "\n";
                    }
                }

                try {
                    shellInput.writeBytes(finalCmd);
                    shellInput.writeBytes("echo '---HK_DONE---'\n");
                    shellInput.flush();
                } catch (Exception e) {
                    appendMatrixText("[-] Shell Comm Error: " + e.getMessage() + "\n");
                }
            } else {
                appendMatrixText("[-] Execution Blocked: Native Engine is offline.\n");
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
            if (outputView != null) outputView.append(currentPrompt);
            return;
        } else if (trimmedCmd.equals("exit") && isRootMode) {
            isRootMode = false;
            currentPrompt = "pshacker@hk:~$ ";
            if (outputView != null) outputView.append(currentPrompt);
            return;
        }

        // STANDARD SHELL EXECUTION BINDING
        if (shellInput != null) { 
            try {
                shellInput.writeBytes(trimmedCmd + "\n");
                shellInput.writeBytes("echo '---HK_DONE---'\n");
                shellInput.flush();
            } catch (Exception e) {
                appendMatrixText("[-] Shell Comm Error: " + e.getMessage() + "\n");
            }
        } else { 
            appendMatrixText("[-] FATAL: Shell Engine is Offline.\n");
            if (outputView != null) outputView.append(currentPrompt);
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

    private void loadHistory() {
        try {
            File historyFile = new File(getBaseHomePath(), ".hk_history");
            if (historyFile.exists()) {
                BufferedReader reader = new BufferedReader(new FileReader(historyFile));
                String line;
                history.clear();
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty()) history.add(line);
                }
                reader.close();
            }
        } catch (Exception ignored) {}
    }

    private void saveToHistory(String command) {
        try {
            File historyFile = new File(getBaseHomePath(), ".hk_history");
            FileWriter writer = new FileWriter(historyFile, true); 
            writer.write(command + "\n");
            writer.close();
        } catch (Exception ignored) {}
    }

    // [!] INTERACTIVE INPUT MATRIX ENGINE
    public static class CustomEditText extends androidx.appcompat.widget.AppCompatEditText {
        private ScaleGestureDetector scaleDetector;
        private float currentTextSize = 14f;

        public CustomEditText(Context context) { super(context); initEngine(context); }
        public CustomEditText(Context context, android.util.AttributeSet attrs) { super(context, attrs); initEngine(context); }

        private void initEngine(Context context) {
            scaleDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector detector) {
                    currentTextSize *= detector.getScaleFactor();
                    currentTextSize = Math.max(10f, Math.min(currentTextSize, 40f));
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, currentTextSize);
                    return true;
                }
            });
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            scaleDetector.onTouchEvent(event);
            if (scaleDetector.isInProgress()) return true; 
            return super.onTouchEvent(event); 
        }

        @Override public boolean onTextContextMenuItem(int id) {
            if (id == android.R.id.paste) {
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null && clipboard.hasPrimaryClip()) {
                    ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                    if (item != null && item.getText() != null) {
                        setSelection(getText().length());
                        getText().insert(getSelectionEnd(), item.getText());
                        return true;
                    }
                }
            }
            return super.onTextContextMenuItem(id);
        }

        @Override public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            InputConnection ic = super.onCreateInputConnection(outAttrs);
            outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION; 
            outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
            
            return new InputConnectionWrapper(ic, true) {
                
                @Override public boolean deleteSurroundingText(int beforeLength, int afterLength) {
                    String s = CustomEditText.this.getText().toString();
                    int promptIdx = s.lastIndexOf("$ ");
                    if (promptIdx == -1) promptIdx = s.lastIndexOf("# ");
                    int minPos = promptIdx != -1 ? promptIdx + 2 : 0;
                    
                    if (CustomEditText.this.getSelectionStart() <= minPos) return false;
                    return super.deleteSurroundingText(beforeLength, afterLength);
                }

                @Override public boolean commitText(CharSequence text, int newCursorPosition) {
                    MainActivity main = (MainActivity) getContext();
                    if (main == null) return super.commitText(text, newCursorPosition);
                    
                    String s = CustomEditText.this.getText().toString();
                    int promptIdx = s.lastIndexOf("$ ");
                    if (promptIdx == -1) promptIdx = s.lastIndexOf("# ");
                    int minPos = promptIdx != -1 ? promptIdx + 2 : 0;
                    
                    if (CustomEditText.this.getSelectionStart() < minPos) {
                        CustomEditText.this.setSelection(CustomEditText.this.getText().length());
                    }

                    if (text.toString().equals("\n")) {
                        main.executeExtractedCommand();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> main.forceKeyboard(CustomEditText.this), 50);
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

        @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
            MainActivity main = (MainActivity) getContext();
            if (main == null) return super.onKeyDown(keyCode, event);
            
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN) {
                CustomEditText.this.setSelection(CustomEditText.this.getText().length());
                main.executeExtractedCommand();
                new Handler(Looper.getMainLooper()).postDelayed(() -> main.forceKeyboard(this), 50);
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_DEL) {
                int selStart = CustomEditText.this.getSelectionStart();
                int selEnd = CustomEditText.this.getSelectionEnd();
                String s = CustomEditText.this.getText().toString();
                int promptIdx = s.lastIndexOf("$ ");
                if (promptIdx == -1) promptIdx = s.lastIndexOf("# ");
                int minPos = promptIdx != -1 ? promptIdx + 2 : 0;
                
                if (selStart <= minPos && selStart == selEnd) return true; 
                if (selStart < minPos) return true; 
            }
            return super.onKeyDown(keyCode, event);
        }
    }

    // [!] TABS AND VISUAL FACTORY (UPGRADED WITH DB SYNC)
    public static class TerminalTabFragment extends Fragment {
        int type;
        private LinearLayout rootLayoutRef;
        
        public TerminalTabFragment() {}
        public TerminalTabFragment(int t) { this.type = t; }

        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
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
            outputView.setTextIsSelectable(true); 

            outputView.setOnEditorActionListener((v, actionId, event) -> {
                boolean isEnter = (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
                if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_UNSPECIFIED || isEnter) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    if (mainActivity != null) {
                        mainActivity.executeExtractedCommand();
                        new Handler(Looper.getMainLooper()).postDelayed(() -> mainActivity.forceKeyboard(v), 50);
                    }
                    return true; 
                }
                return false;
            });

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
            if (rootLayoutRef != null && getContext() != null) renderPackagesMatrix(rootLayoutRef, getContext());
        }

        private void renderPackagesMatrix(LinearLayout rootLayout, Context context) {
            rootLayout.removeAllViews();
            TextView title = new TextView(context);
            title.setText(">> HK WEAPON ARSENAL (v4.0 MATRIX)");
            title.setTextColor(Color.parseColor("#00FF41")); 
            title.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            title.setPadding(0, 0, 0, 50);
            title.setTextSize(18f);
            rootLayout.addView(title);

            MainActivity main = (MainActivity) getActivity();
            if (main == null) return;
            
            // [!] Connect to DB to fetch package status
            HKDatabaseManager localDbManager = new HKDatabaseManager(context);
            SQLiteDatabase db = null;
            try { db = localDbManager.getReadableDatabase(); } catch(Exception ignored){}

            File binDir = new File(main.getUsrBinPath());
            if (binDir.exists() && binDir.isDirectory()) {
                File[] files = binDir.listFiles();
                if (files != null && files.length > 0) {
                    Arrays.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                    for (File file : files) {
                        if (file.isDirectory()) continue; 
                        
                        String pkgNameStr = file.getName();
                        String statusStr = "UNKNOWN";
                        String colorCode = "#FFFFFF";

                        // [!] Query DB for Status
                        if (db != null) {
                            try {
                                Cursor c = db.rawQuery("SELECT status FROM Packages WHERE package_name=?", new String[]{pkgNameStr});
                                if (c.moveToFirst()) {
                                    statusStr = c.getString(0);
                                    if (statusStr.equals("READY")) colorCode = "#00FF41";
                                    else if (statusStr.equals("FAILED") || statusStr.equals("WARNING")) colorCode = "#FF003C";
                                }
                                c.close();
                            } catch (Exception ignored) {}
                        }

                        LinearLayout row = new LinearLayout(context);
                        row.setOrientation(LinearLayout.HORIZONTAL);
                        row.setPadding(0, 20, 0, 20);
                        row.setGravity(Gravity.CENTER_VERTICAL);

                        TextView pkgName = new TextView(context);
                        pkgName.setText(pkgNameStr + " [" + statusStr + "]");
                        pkgName.setTextColor(Color.parseColor(colorCode));
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
                                localDbManager.updatePackageState(pkgNameStr, "DELETED"); // [!] Sync with DB
                                Toast.makeText(context, "[+] Target Wiped: " + file.getName(), Toast.LENGTH_SHORT).show();
                                renderPackagesMatrix(rootLayout, context);
                            } else Toast.makeText(context, "[-] Matrix Deletion Blocked", Toast.LENGTH_SHORT).show();
                        });

                        row.addView(pkgName);
                        row.addView(delBtn);
                        rootLayout.addView(row);
                    }
                }
            }
        }
    }

    public static void logError(String m, String t, Throwable e) { Log.e(m, t, e); }
    
    @Override protected void onDestroy() { 
        super.onDestroy(); 
        try {
            if (shellProcess != null) shellProcess.destroy();
            if (shellInput != null) shellInput.close();
        } catch (Throwable ignored) {}
    }
}

package com.hk.hkterminal;

import android.content.*;
import android.graphics.Typeface;
import android.os.*;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.util.Log;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.*;

/**
 * HK TERMINAL - MAIN CONTROL UNIT
 * DEVELOPED BY: HK PRASHANT SINGH (TECH WIZARD)
 * PROJECT: HK-OPERATION (SECRET)
 */
public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    
    // Status flags for modifier keys
    private boolean isCtrl = false;
    private boolean isAlt = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Ensure the layout adjusts when the keyboard is shown
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        // Terminal Tabs Configuration
        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override 
            public int getItemCount() { 
                return 2; 
            }
            @Override 
            public Fragment createFragment(int position) { 
                return new TerminalTabFragment(position); 
            }
        });

        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> {
            if (pos == 0) {
                tab.setText("TERMINAL");
            } else {
                tab.setText("PACKAGES");
            }
        }).attach();

        // Initialize Specialized Key Listeners
        setupExtraKeys();

        // Start Internal Socket Server for Terminal Operations
        TerminalEngine.startAmSocketServer();
    }

    private void setupExtraKeys() {
        // --- ROW 1: System Commands ---
        findViewById(R.id.esc).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_ESCAPE));
        findViewById(R.id.slash).setOnClickListener(v -> injectTextAtCursor("/"));
        findViewById(R.id.dash).setOnClickListener(v -> injectTextAtCursor("-"));
        findViewById(R.id.home).setOnClickListener(v -> moveCursorToPromptBoundary());
        findViewById(R.id.up).setOnClickListener(v -> handleHistoryNavigation(1)); // Cycle History Up
        findViewById(R.id.end).setOnClickListener(v -> moveCursorToAbsoluteEnd());
        findViewById(R.id.pgup).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_PAGE_UP));

        // --- ROW 2: Navigation & Modifiers ---
        findViewById(R.id.left_arrow).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_TAB));
        
        findViewById(R.id.ctrl).setOnClickListener(v -> {
            isCtrl = !isCtrl;
            v.setBackgroundColor(isCtrl ? 0xFFFF0000 : 0xFF333333);
        });

        findViewById(R.id.alt).setOnClickListener(v -> {
            isAlt = !isAlt;
            v.setBackgroundColor(isAlt ? 0xFF00FF00 : 0xFF333333);
        });

        // Directional Logic: Left Arrow (←)
        findViewById(R.id.left).setOnClickListener(v -> performCursorMovement(-1));

        // Directional Logic: Down Arrow (↓)
        findViewById(R.id.down).setOnClickListener(v -> handleHistoryNavigation(-1)); // Cycle History Down

        // Directional Logic: Right Arrow (→)
        findViewById(R.id.right).setOnClickListener(v -> performCursorMovement(1));

        findViewById(R.id.pgdn).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_PAGE_DOWN));
    }

    private void performCursorMovement(int direction) {
        if (outputView == null) return;
        
        int currentSelection = outputView.getSelectionStart();
        int targetPosition = currentSelection + direction;
        String content = outputView.getText().toString();
        
        // Critical: Maintain prompt integrity
        int promptEndIndex = content.lastIndexOf("root@pshacker:~# ") + 17;

        if (targetPosition >= promptEndIndex && targetPosition <= content.length()) {
            Selection.setSelection((Spannable) outputView.getText(), targetPosition);
        }
    }

    private void handleHistoryNavigation(int direction) {
        if (history.isEmpty()) return;
        
        hIndex = Math.max(-1, Math.min(hIndex + direction, history.size() - 1));
        
        String currentText = outputView.getText().toString();
        int lastPromptPos = currentText.lastIndexOf("root@pshacker:~# ");
        
        if (lastPromptPos != -1) {
            String terminalBase = currentText.substring(0, lastPromptPos + 17);
            String commandFromHistory = (hIndex == -1) ? "" : history.get(history.size() - 1 - hIndex);
            
            outputView.setText(terminalBase + commandFromHistory);
            moveCursorToAbsoluteEnd();
        }
    }

    private void injectTextAtCursor(String sequence) {
        if (outputView == null) return;
        
        int start = Math.max(outputView.getSelectionStart(), 0);
        int end = Math.max(outputView.getSelectionEnd(), 0);
        
        Editable editable = (Editable) outputView.getText();
        editable.replace(Math.min(start, end), Math.max(start, end), sequence);
    }

    public void executeCommand(final String cmdInput) {
        if (cmdInput.trim().isEmpty()) return;
        
        history.add(cmdInput);
        hIndex = -1; // Reset history index on new execution
        
        if (headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
        outputView.append("\n");

        TerminalEngine.run(cmdInput, resultLine -> runOnUiThread(() -> {
            if (outputView != null) {
                outputView.append(resultLine + "\n");
                triggerAutoScroll();
            }
            if (headerProgress != null) headerProgress.setVisibility(View.GONE);
        }));
    }

    private void triggerAutoScroll() {
        final ScrollView scrollViewContainer = (ScrollView) outputView.getParent();
        scrollViewContainer.post(() -> scrollViewContainer.fullScroll(View.FOCUS_DOWN));
    }

    private void moveCursorToPromptBoundary() {
        String text = outputView.getText().toString();
        int boundary = text.lastIndexOf("root@pshacker:~# ") + 17;
        Selection.setSelection((Spannable) outputView.getText(), boundary);
    }

    private void moveCursorToAbsoluteEnd() {
        Selection.setSelection((Spannable) outputView.getText(), outputView.length());
    }

    private void sendSystemKey(int code) {
        outputView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
    }

    public static class TerminalTabFragment extends Fragment {
        private int tabType;

        public TerminalTabFragment() {}
        public TerminalTabFragment(int type) { this.tabType = type; }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (tabType != 0) return new View(getContext());

            final ScrollView scrollWrapper = new ScrollView(getContext());
            scrollWrapper.setFillViewport(true);

            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00); // Matrix Green
            outputView.setTypeface(Typeface.MONOSPACE);
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
            outputView.setTextIsSelectable(true);
            outputView.setFocusableInTouchMode(true);
            
            outputView.setOnKeyListener((v, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    String fullText = outputView.getText().toString();
                    String lastCmd = fullText.substring(fullText.lastIndexOf("root@pshacker:~# ") + 17).trim();
                    ((MainActivity)getActivity()).executeCommand(lastCmd);
                    return true;
                }
                return false;
            });

            scrollWrapper.addView(outputView);
            return scrollWrapper;
        }
    }

    @Override 
    protected void onDestroy() { 
        super.onDestroy(); 
        TerminalEngine.stopAmSocketServer(); 
    }
}
}

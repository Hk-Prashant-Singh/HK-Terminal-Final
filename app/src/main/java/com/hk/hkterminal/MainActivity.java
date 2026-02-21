package com.hk.hkterminal;

import android.content.*;
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
import java.util.*;

public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    private boolean isCtrl = false, isAlt = false;

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Soft input mode fixed for perfect focus and resizing
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int pos) { 
                return new TerminalTabFragment(pos); 
            }
        });

        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> {
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES");
        }).attach();

        setupExtraKeys();
        TerminalEngine.startAmSocketServer();
    }

    public static void logError(String tag, String msg, Exception e) { 
        Log.e(tag, msg, e); 
    }

    private void setupExtraKeys() {
        // Elite specialized buttons for HK-Operation
        findViewById(R.id.esc).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_ESCAPE));
        findViewById(R.id.slash).setOnClickListener(v -> injectChar("/"));
        findViewById(R.id.dash).setOnClickListener(v -> injectChar("-"));
        findViewById(R.id.home).setOnClickListener(v -> jumpToPrompt());
        findViewById(R.id.up).setOnClickListener(v -> cycleHistory(1));
        findViewById(R.id.end).setOnClickListener(v -> jumpToEnd());
        findViewById(R.id.pgup).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_PAGE_UP));

        findViewById(R.id.left_arrow).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_TAB));
        findViewById(R.id.ctrl).setOnClickListener(v -> toggleMod(v, "CTRL"));
        findViewById(R.id.alt).setOnClickListener(v -> toggleMod(v, "ALT"));
        findViewById(R.id.left).setOnClickListener(v -> moveCursor(-1));
        findViewById(R.id.down).setOnClickListener(v -> cycleHistory(-1));
        findViewById(R.id.right).setOnClickListener(v -> moveCursor(1));
        findViewById(R.id.pgdn).setOnClickListener(v -> sendSystemKey(KeyEvent.KEYCODE_PAGE_DOWN));
    }

    private void toggleMod(View v, String type) {
        if(type.equals("CTRL")) { 
            isCtrl = !isCtrl; 
            v.setBackgroundColor(isCtrl ? 0xFFFF0000 : 0x00000000); 
        } else { 
            isAlt = !isAlt; 
            v.setBackgroundColor(isAlt ? 0xFF00FF00 : 0x00000000); 
        }
    }

    private void moveCursor(int off) {
        if (outputView == null) return;
        int target = outputView.getSelectionStart() + off;
        int limit = outputView.getText().toString().lastIndexOf("root@pshacker:~# ") + 17;
        if (target >= limit && target <= outputView.length()) {
            Selection.setSelection((Spannable) outputView.getText(), target);
        }
    }

    private void cycleHistory(int dir) {
        if (history.isEmpty() || outputView == null) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        String current = outputView.getText().toString();
        int lastP = current.lastIndexOf("root@pshacker:~# ");
        if (lastP != -1) {
            outputView.setText(current.substring(0, lastP + 17) + (hIndex == -1 ? "" : history.get(history.size() - 1 - hIndex)));
            jumpToEnd();
        }
    }

    private void injectChar(String s) {
        if (outputView == null) return;
        outputView.requestFocus();
        int start = Math.max(outputView.getSelectionStart(), 0);
        int end = Math.max(outputView.getSelectionEnd(), 0);
        ((Editable) outputView.getText()).replace(Math.min(start, end), Math.max(start, end), s);
    }

    public void executeCommand(final String input) {
        if (input.trim().isEmpty()) return;
        history.add(input); hIndex = -1;
        if (headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
        outputView.append("\n");
        TerminalEngine.run(input, res -> runOnUiThread(() -> {
            outputView.append(res + "\nroot@pshacker:~# ");
            autoScroll();
            if (headerProgress != null) headerProgress.setVisibility(View.GONE);
        }));
    }

    private void autoScroll() {
        final ScrollView sv = (ScrollView) outputView.getParent();
        sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
    }

    private void jumpToPrompt() {
        if (outputView == null) return;
        Selection.setSelection((Spannable) outputView.getText(), outputView.getText().toString().lastIndexOf("root@pshacker:~# ") + 17);
    }

    private void jumpToEnd() { 
        if (outputView != null) Selection.setSelection((Spannable) outputView.getText(), outputView.length()); 
    }

    private void sendSystemKey(int code) { 
        if (outputView != null) outputView.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code)); 
    }

    public static class TerminalTabFragment extends Fragment {
        private int type;
        public TerminalTabFragment() {}
        public TerminalTabFragment(int t) { this.type = t; }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type != 0) return new View(getContext());
            final ScrollView scroll = new ScrollView(getContext()); scroll.setFillViewport(true);
            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00); outputView.setTypeface(Typeface.MONOSPACE);
            outputView.setText(">> HK Prashant Singh\nroot@pshacker:~# ");
            outputView.setFocusable(true); outputView.setFocusableInTouchMode(true);
            outputView.setClickable(true); outputView.setTextIsSelectable(true);
            outputView.setCursorVisible(true);
            
            outputView.setOnClickListener(v -> {
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            });
            
            outputView.setOnKeyListener((v, code, ev) -> {
                if (ev.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                    String full = outputView.getText().toString();
                    ((MainActivity)getActivity()).executeCommand(full.substring(full.lastIndexOf("root@pshacker:~# ") + 17).trim());
                    return true;
                }
                return false;
            });
            scroll.addView(outputView); return scroll;
        }
    }
    
    @Override protected void onDestroy() { 
        super.onDestroy(); 
        TerminalEngine.stopAmSocketServer(); 
    }
}

package com.hk.hkterminal;

import android.content.*;
import android.graphics.Typeface;
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
import java.util.*;

public class MainActivity extends AppCompatActivity {
    public static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private ProgressBar headerProgress;
    public LinearLayout extraKeysLayout;
    
    // Status flags for modifier keys
    private boolean isCtrl = false;
    private boolean isAlt = false;

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        headerProgress = findViewById(R.id.headerProgress);
        extraKeysLayout = findViewById(R.id.extraKeysLayout);

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { return new TerminalTabFragment(p); }
        });
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        // --- NEW 2-LINE KEYBOARD BUTTONS INTEGRATION ---
        setupExtraKeys();

        TerminalEngine.startAmSocketServer();
    }

    private void setupExtraKeys() {
        // Row 1
        findViewById(R.id.esc).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_ESCAPE));
        findViewById(R.id.slash).setOnClickListener(v -> appendChar("/"));
        findViewById(R.id.dash).setOnClickListener(v -> appendChar("-"));
        findViewById(R.id.home).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_MOVE_HOME));
        findViewById(R.id.up).setOnClickListener(v -> navigateHistory(1));
        findViewById(R.id.end).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_MOVE_END));
        findViewById(R.id.pgup).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_PAGE_UP));

        // Row 2
        findViewById(R.id.left_arrow).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_TAB)); // Tab for completion
        findViewById(R.id.ctrl).setOnClickListener(v -> {
            isCtrl = !isCtrl;
            v.setBackgroundColor(isCtrl ? 0xFFFF0000 : 0xFF333333);
        });
        findViewById(R.id.alt).setOnClickListener(v -> {
            isAlt = !isAlt;
            v.setBackgroundColor(isAlt ? 0xFFFF0000 : 0xFF333333);
        });
        findViewById(R.id.left).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_LEFT));
        findViewById(R.id.down).setOnClickListener(v -> navigateHistory(-1));
        findViewById(R.id.right).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_DPAD_RIGHT));
        findViewById(R.id.pgdn).setOnClickListener(v -> sendKey(KeyEvent.KEYCODE_PAGE_DOWN));
    }

    private void appendChar(String c) {
        if(outputView != null) outputView.append(c);
    }

    private void sendKey(int keyCode) {
        // Implementation for virtual key dispatch
        if(outputView != null) {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            outputView.dispatchKeyEvent(event);
        }
    }

    public static void logError(String t, String m, Throwable e) { Log.e(t, m, e); }

    public void executeCommand(final String command) {
        history.add(command);
        if(headerProgress != null) headerProgress.setVisibility(View.VISIBLE);
        outputView.append("\n");

        TerminalEngine.run(command, line -> runOnUiThread(() -> {
            if (outputView != null) {
                outputView.append(line + "\n");
                final ScrollView sv = (ScrollView) outputView.getParent();
                sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            }
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
        }));
    }

    private void navigateHistory(int dir) {
        if (history.isEmpty()) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        if (hIndex != -1) {
            String txt = outputView.getText().toString();
            int last = txt.lastIndexOf("root@pshacker:~# ");
            if (last != -1) outputView.setText(txt.substring(0, last + 17) + history.get(history.size() - 1 - hIndex));
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
            sv.setVerticalScrollBarEnabled(true);

            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setTypeface(Typeface.MONOSPACE);
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
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
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.showSoftInput(outputView, InputMethodManager.SHOW_IMPLICIT);
                }
                return false; 
            });

            outputView.setOnKeyListener((v, code, ev) -> {
                if (ev.getAction() == KeyEvent.ACTION_DOWN) {
                    if (ev.getUnicodeChar() != 0 && code != KeyEvent.KEYCODE_ENTER && code != KeyEvent.KEYCODE_DEL) {
                        outputView.append(String.valueOf((char) ev.getUnicodeChar()));
                        return true;
                    }
                    if (code == KeyEvent.KEYCODE_DEL) {
                        String s = outputView.getText().toString();
                        if (!s.endsWith("root@pshacker:~# ")) outputView.setText(s.substring(0, s.length()-1));
                        return true;
                    }
                    if (code == KeyEvent.KEYCODE_ENTER) {
                        String s = outputView.getText().toString();
                        executeCommand(s.substring(s.lastIndexOf("root@pshacker:~# ") + 17).trim());
                        return true;
                    }
                }
                return false;
            });

            sv.addView(outputView);
            return sv;
        }

        private void executeCommand(String cmd) { ((MainActivity)getActivity()).executeCommand(cmd); }
    }

    @Override protected void onDestroy() { super.onDestroy(); TerminalEngine.stopAmSocketServer(); }
}

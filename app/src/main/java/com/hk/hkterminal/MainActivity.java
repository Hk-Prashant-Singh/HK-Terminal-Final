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
    private boolean isCtrl = false;

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Elite resizing for professional terminal feel
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

        setupSystemButtons();
        TerminalEngine.startAmSocketServer();
    }

    private void setupSystemButtons() {
        // IDs fixed to match common layout naming conventions
        View btnCb = findViewById(R.id.esc); // Placeholder fix: use your actual XML ID here
        if (btnCb != null) btnCb.setOnClickListener(v -> showCommandBox());

        View btnCtrl = findViewById(R.id.ctrl);
        if (btnCtrl != null) btnCtrl.setOnClickListener(v -> {
            isCtrl = !isCtrl;
            v.setBackgroundColor(isCtrl ? 0xFFFF0000 : 0xFF333333);
        });

        // Prompt restored for Elite Alpha energy
        View btnCLR = findViewById(R.id.slash); 
        if (btnCLR != null) btnCLR.setOnClickListener(v -> 
            outputView.setText(">> HK Prashant Singh\nroot@pshacker:~# "));

        View btnUp = findViewById(R.id.up);
        if (btnUp != null) btnUp.setOnClickListener(v -> navigateHistory(1));

        View btnDown = findViewById(R.id.down);
        if (btnDown != null) btnDown.setOnClickListener(v -> navigateHistory(-1));
    }

    private void showCommandBox() {
        final EditText input = new EditText(this);
        input.setTextColor(0xFF00FF00);
        input.setBackgroundColor(0xFF111111);
        input.setTypeface(Typeface.MONOSPACE);
        
        new android.app.AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_NoActionBar_Fullscreen)
            .setTitle("COMMAND BOX")
            .setView(input)
            .setPositiveButton("EXECUTE", (d, w) -> {
                String cmd = input.getText().toString().trim();
                if(!cmd.isEmpty()) {
                    outputView.append(cmd); 
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

        TerminalEngine.run(command, line -> runOnUiThread(() -> {
            if (outputView != null) {
                outputView.append(line + "\nroot@pshacker:~# ");
                final ScrollView sv = (ScrollView) outputView.getParent();
                sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            }
            if(headerProgress != null) headerProgress.setVisibility(View.GONE);
        }));
    }

    private void navigateHistory(int dir) {
        if (history.isEmpty()) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        String txt = outputView.getText().toString();
        int last = txt.lastIndexOf("root@pshacker:~# ");
        if (last != -1) {
            String cmd = (hIndex == -1) ? "" : history.get(history.size() - 1 - hIndex);
            outputView.setText(txt.substring(0, last + 17) + cmd);
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
            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setTypeface(Typeface.MONOSPACE);
            outputView.setText(">> HK Prashant Singh\nroot@pshacker:~# ");
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
                        int start = s.lastIndexOf("root@pshacker:~# ") + 17;
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

    @Override protected void onDestroy() { super.onDestroy(); TerminalEngine.stopAmSocketServer(); }
}

package com.hk.hkterminal;

import android.content.Context;
import android.os.*;
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

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ViewPager2 vp = findViewById(R.id.viewPager);
        vp.setAdapter(new FragmentStateAdapter(this) {
            @Override public int getItemCount() { return 2; }
            @Override public Fragment createFragment(int p) { return new TabFragment(p); }
        });
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        findViewById(R.id.btnCLR).setOnClickListener(v -> outputView.setText(">> PS HACKER READY\nroot@pshacker:~# "));
        findViewById(R.id.btnUp).setOnClickListener(v -> navigateHistory(1));
        findViewById(R.id.btnDown).setOnClickListener(v -> navigateHistory(-1));
    }

    public void executeCommand(final String command) {
        if (command.isEmpty()) return;
        history.add(command);
        outputView.append("\n");
        TerminalEngine.run(command, line -> runOnUiThread(() -> {
            outputView.append(line + "\n");
            final ScrollView sv = (ScrollView) outputView.getParent();
            sv.post(() -> sv.fullScroll(View.FOCUS_DOWN));
            if (line.contains("exit")) outputView.append("root@pshacker:~# ");
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

    public static class TabFragment extends Fragment {
        int type;
        public TabFragment(int t) { this.type = t; }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (type != 0) return new View(getContext());
            ScrollView sv = new ScrollView(getContext());
            sv.setFillViewport(true);
            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setText(">> PS HACKER READY\nroot@pshacker:~# ");
            outputView.setFocusableInTouchMode(true);
            
            outputView.setOnClickListener(v -> {
                v.requestFocus();
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT);
            });

            outputView.setOnKeyListener((v, code, ev) -> {
                if (ev.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                    String full = outputView.getText().toString();
                    String last = full.substring(full.lastIndexOf("root@pshacker:~# ") + 17).trim();
                    ((MainActivity)getActivity()).executeCommand(last);
                    return true;
                }
                return false;
            });

            ScaleGestureDetector gd = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                @Override public boolean onScale(ScaleGestureDetector d) {
                    outputView.setTextSize(0, outputView.getTextSize() * d.getScaleFactor());
                    return true;
                }
            });
            sv.setOnTouchListener((v, e) -> { gd.onTouchEvent(e); return false; });
            sv.addView(outputView);
            return sv;
        }
    }
}

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
        new TabLayoutMediator(findViewById(R.id.tabLayout), vp, (tab, pos) -> tab.setText(pos==0?"TERMINAL":"PACKAGES")).attach();

        // Shortcut Listeners
        findViewById(R.id.btnCLR).setOnClickListener(v -> outputView.setText(">> HK READY\nroot@pshacker:~# "));
        findViewById(R.id.btnUp).setOnClickListener(v -> navigateHistory(1));
        findViewById(R.id.btnDown).setOnClickListener(v -> navigateHistory(-1));
    }

    public void execute(String cmd) {
        history.add(cmd);
        TerminalEngine.run(cmd, line -> runOnUiThread(() -> outputView.append(line + "\nroot@pshacker:~# ")));
    }

    private void navigateHistory(int dir) {
        if(history.isEmpty()) return;
        hIndex = Math.max(-1, Math.min(hIndex + dir, history.size() - 1));
        if(hIndex != -1) outputView.append(history.get(history.size() - 1 - hIndex));
    }

    public static class TabFragment extends Fragment {
        int type;
        public TabFragment(int t) { type = t; }
        @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if(type != 0) return new View(getContext());
            ScrollView sv = new ScrollView(getContext());
            outputView = new TextView(getContext());
            outputView.setTextColor(0xFF00FF00);
            outputView.setFocusableInTouchMode(true);
            outputView.setText(">> HK READY\nroot@pshacker:~# ");
            
            // Direct Key Input
            outputView.setOnKeyListener((v, code, ev) -> {
                if(ev.getAction() == KeyEvent.ACTION_DOWN && code == KeyEvent.KEYCODE_ENTER) {
                    String[] lines = outputView.getText().toString().split("\n");
                    String last = lines[lines.length-1].replace("root@pshacker:~# ", "");
                    ((MainActivity)getActivity()).execute(last);
                    return true;
                }
                return false;
            });

            // Smooth Zoom
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

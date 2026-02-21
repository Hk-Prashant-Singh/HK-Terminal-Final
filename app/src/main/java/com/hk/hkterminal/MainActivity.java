package com.hk.hkterminal;

import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private EditText inputCommand;
    private static TextView outputView;
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        inputCommand = findViewById(R.id.inputCommand);

        viewPager.setAdapter(new TerminalPagerAdapter(this));
        new TabLayoutMediator(findViewById(R.id.tabLayout), viewPager, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        // Keyboard Enter Power
        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String cmd = inputCommand.getText().toString();
                outputView.append("\nroot@pshacker:~# " + cmd + "\n");
                executeCommand(cmd);
                history.add(cmd);
                inputCommand.setText("");
                return true;
            }
            return false;
        });

        // History Navigation
        findViewById(R.id.btnUp).setOnClickListener(v -> {
            if(!history.isEmpty() && hIndex < history.size()-1) {
                hIndex++;
                inputCommand.setText(history.get(history.size()-1-hIndex));
            }
        });

        findViewById(R.id.btnCLR).setOnClickListener(v -> outputView.setText(">> HK READY\n"));
    }

    private void executeCommand(String cmd) {
        // Pkg install logic with Red Loading bar
        findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
        TerminalEngine.run(cmd, line -> runOnUiThread(() -> {
            outputView.append(line + "\n");
            if(line.contains("%")) { /* Update txtProgress logic */ }
        }));
    }

    public static class TabFragment extends androidx.fragment.app.Fragment {
        private float mScale = 14f;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (getArguments().getInt("type") == 0) {
                ScrollView sv = new ScrollView(getContext());
                outputView = new TextView(getContext());
                outputView.setTextColor(0xFF00FF00);
                outputView.setTypeface(android.graphics.Typeface.MONOSPACE);
                outputView.setTextIsSelectable(true); // Touch-to-Copy
                
                // Pinch-to-Zoom
                ScaleGestureDetector gd = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        mScale *= d.getScaleFactor();
                        outputView.setTextSize(mScale);
                        return true;
                    }
                });
                sv.setOnTouchListener((v, e) -> { gd.onTouchEvent(e); return false; });
                
                sv.addView(outputView);
                return sv;
            }
            return new ListView(getContext());
        }
    }
}

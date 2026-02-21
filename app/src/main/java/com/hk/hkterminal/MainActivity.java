package com.hk.hkterminal;

import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private EditText inputCommand;
    public static TextView outputView; // Static for fragment access
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;
    private LinearLayout loadingPanel;

    // Fix 1: Callback Interface Defined
    public interface Callback {
        void onOutput(String line);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        inputCommand = findViewById(R.id.inputCommand);
        loadingPanel = findViewById(R.id.loadingPanel);

        // Fix 2: Adapter properly linked
        viewPager.setAdapter(new TerminalPagerAdapter(this));
        new TabLayoutMediator(findViewById(R.id.tabLayout), viewPager, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String cmd = inputCommand.getText().toString();
                if (outputView != null) outputView.append("\nroot@pshacker:~# " + cmd + "\n");
                executeCommand(cmd);
                history.add(cmd);
                inputCommand.setText("");
                hIndex = -1;
                return true;
            }
            return false;
        });

        findViewById(R.id.btnUp).setOnClickListener(v -> {
            if(!history.isEmpty() && hIndex < history.size()-1) {
                hIndex++;
                inputCommand.setText(history.get(history.size()-1-hIndex));
            }
        });

        findViewById(R.id.btnCLR).setOnClickListener(v -> {
            if (outputView != null) outputView.setText(">> HK TERMINAL READY\n");
        });
    }

    private void executeCommand(String cmd) {
        loadingPanel.setVisibility(View.VISIBLE);
        TerminalEngine.run(cmd, line -> runOnUiThread(() -> {
            if (outputView != null) {
                outputView.append(line + "\n");
                // Auto-scroll logic
                final int scrollAmount = outputView.getLayout().getLineTop(outputView.getLineCount()) - outputView.getHeight();
                if (scrollAmount > 0) outputView.scrollTo(0, scrollAmount);
            }
            if (line.contains("complete") || line.contains("Error")) {
                loadingPanel.setVisibility(View.GONE);
            }
        }));
    }

    // Fix 3: Internal Adapter Class
    private class TerminalPagerAdapter extends FragmentStateAdapter {
        public TerminalPagerAdapter(@NonNull AppCompatActivity activity) { super(activity); }
        @NonNull @Override public Fragment createFragment(int position) {
            TabFragment frag = new TabFragment();
            Bundle b = new Bundle(); b.putInt("type", position);
            frag.setArguments(b);
            return frag;
        }
        @Override public int getItemCount() { return 2; }
    }

    // Fix 4: Fragment with Zoom & Copy
    public static class TabFragment extends Fragment {
        private float mScale = 14f;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (getArguments().getInt("type") == 0) {
                ScrollView sv = new ScrollView(getContext());
                outputView = new TextView(getContext());
                outputView.setTextColor(0xFF00FF00);
                outputView.setTypeface(android.graphics.Typeface.MONOSPACE);
                outputView.setTextIsSelectable(true); 
                outputView.setPadding(20, 20, 20, 20);
                
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

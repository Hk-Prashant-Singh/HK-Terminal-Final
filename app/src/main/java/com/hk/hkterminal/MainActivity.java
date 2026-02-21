package com.hk.hkterminal;

import android.os.*;
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
    private ProgressBar headerProgress;
    public static TextView outputView; // Global reference for fragments
    private List<String> history = new ArrayList<>();
    private int hIndex = -1;

    public interface Callback { void onOutput(String line); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // UI Initialization
        viewPager = findViewById(R.id.viewPager);
        inputCommand = findViewById(R.id.inputCommand);
        headerProgress = findViewById(R.id.headerProgress);

        // Storage Access for Prashant Bhai
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        viewPager.setAdapter(new TerminalPagerAdapter(this));
        new TabLayoutMediator(findViewById(R.id.tabLayout), viewPager, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        // Keyboard Enter Power
        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                String cmd = inputCommand.getText().toString();
                execute(cmd);
                inputCommand.setText("");
                return true;
            }
            return false;
        });

        // Shortcut Buttons Logic
        findViewById(R.id.btnDash).setOnClickListener(v -> inputCommand.append("-"));
        findViewById(R.id.btnSlash).setOnClickListener(v -> inputCommand.append("/"));
        findViewById(R.id.btnCLR).setOnClickListener(v -> { if(outputView!=null) outputView.setText(">> HK READY\n"); });
        findViewById(R.id.btnUp).setOnClickListener(v -> {
            if(!history.isEmpty() && hIndex < history.size()-1) {
                hIndex++;
                inputCommand.setText(history.get(history.size()-1-hIndex));
            }
        });
    }

    public void execute(String cmd) {
        if (outputView != null) outputView.append("\nroot@pshacker:~# " + cmd + "\n");
        history.add(cmd);
        headerProgress.setVisibility(View.VISIBLE);
        TerminalEngine.run(cmd, line -> runOnUiThread(() -> {
            if (outputView != null) outputView.append(line + "\n");
            if (line.contains("complete") || line.contains("Error")) headerProgress.setVisibility(View.GONE);
        }));
    }

    private class TerminalPagerAdapter extends FragmentStateAdapter {
        public TerminalPagerAdapter(AppCompatActivity activity) { super(activity); }
        @NonNull @Override public Fragment createFragment(int position) {
            TabFragment fragment = new TabFragment();
            Bundle args = new Bundle(); args.putInt("type", position);
            fragment.setArguments(args);
            return fragment;
        }
        @Override public int getItemCount() { return 2; }
    }

    public static class TabFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int type = getArguments().getInt("type");
            if (type == 0) { // Terminal View
                ScrollView sv = new ScrollView(getContext());
                outputView = new TextView(getContext());
                outputView.setTextColor(0xFF00FF00);
                outputView.setTextIsSelectable(true);
                outputView.setPadding(10,10,10,10);
                sv.addView(outputView);
                return sv;
            } else { // Packages View
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setGravity(Gravity.BOTTOM);
                Button btnInstall = new Button(getContext());
                btnInstall.setText("INSTALL ALL ALPHA PACKAGES");
                btnInstall.setBackgroundColor(0xFFFF0000);
                btnInstall.setOnClickListener(v -> ((MainActivity)getActivity()).autoSetup());
                layout.addView(btnInstall);
                return layout;
            }
        }
    }

    private void autoSetup() {
        viewPager.setCurrentItem(0);
        String[] pkgs = {"pkg update -y", "pkg install python git -y", "pip install requests flask"};
        for(String p : pkgs) execute(p);
    }
                    }


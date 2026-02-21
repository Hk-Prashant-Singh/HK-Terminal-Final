package com.hk.hkterminal;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private EditText inputCommand;
    private ImageButton btnUp, btnDown;
    private static TextView globalOutputView;
    
    // History Logic
    private List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Accuracy and Professional Level Permissions [cite: 2026-02-01]
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
        }

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        inputCommand = findViewById(R.id.inputCommand);
        btnUp = findViewById(R.id.btnUp);
        btnDown = findViewById(R.id.btnDown);

        viewPager.setAdapter(new TerminalPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "TERMINAL" : "PACKAGES");
        }).attach();

        // Keyboard Enter Button Logic
        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || 
               (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                
                String cmd = inputCommand.getText().toString().trim();
                if (!cmd.isEmpty() && globalOutputView != null) {
                    commandHistory.add(cmd);
                    historyIndex = -1;
                    globalOutputView.append("\nroot@pshacker:~# " + cmd + "\n");
                    executeCommand(cmd);
                    inputCommand.setText("");
                }
                return true;
            }
            return false;
        });

        // History Navigation
        btnUp.setOnClickListener(v -> {
            if (!commandHistory.isEmpty() && historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
                inputCommand.setSelection(inputCommand.getText().length());
            }
        });

        btnDown.setOnClickListener(v -> {
            if (historyIndex > 0) {
                historyIndex--;
                inputCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
                inputCommand.setSelection(inputCommand.getText().length());
            } else {
                historyIndex = -1;
                inputCommand.setText("");
            }
        });
    }

    private void executeCommand(String command) {
        new Thread(() -> {
            try {
                // Professional Shell Execution
                ProcessBuilder pb = new ProcessBuilder("/system/bin/sh", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    runOnUiThread(() -> {
                        globalOutputView.append(finalLine + "\n");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> globalOutputView.append("[ERROR]: " + e.getMessage() + "\n"));
            }
        }).start();
    }

    private class TerminalPagerAdapter extends FragmentStateAdapter {
        public TerminalPagerAdapter(AppCompatActivity fa) { super(fa); }
        @Override public int getItemCount() { return 2; }
        @NonNull @Override public androidx.fragment.app.Fragment createFragment(int position) {
            return new TabFragment(position);
        }
    }

    public static class TabFragment extends androidx.fragment.app.Fragment {
        int type;
        public TabFragment() {}
        public TabFragment(int type) { this.type = type; }
        
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (type == 0) {
                ScrollView sv = new ScrollView(getContext());
                TextView tv = new TextView(getContext());
                tv.setText(">> HK TERMINAL READY\n>> PRASHANT BHAI, ENTER COMMAND...\n");
                tv.setTextColor(0xFF00FF00);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setTextIsSelectable(true); 
                globalOutputView = tv;
                sv.addView(tv);
                sv.setBackgroundColor(0xFF000000);
                return sv;
            } else {
                ListView lv = new ListView(getContext());
                lv.setBackgroundColor(0xFF000000);
                loadPackages(lv);
                return lv;
            }
        }

        private void loadPackages(ListView lv) {
            try {
                PackageManager pm = getActivity().getPackageManager();
                List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                String[] names = new String[apps.size()];
                for (int i = 0; i < apps.size(); i++) {
                    names[i] = " > " + apps.get(i).packageName;
                }
                lv.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, names));
            } catch (Exception e) {}
        }
    }
}

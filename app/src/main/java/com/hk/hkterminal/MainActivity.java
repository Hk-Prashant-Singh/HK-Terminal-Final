package com.hk.hkterminal;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private EditText inputCommand;
    private Button runButton;
    private static TextView globalOutputView; // To update from MainActivity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        inputCommand = findViewById(R.id.inputCommand);
        runButton = findViewById(R.id.runButton);

        viewPager.setAdapter(new TerminalPagerAdapter(this));
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "TERMINAL" : "PACKAGES");
        }).attach();

        runButton.setOnClickListener(v -> {
            String cmd = inputCommand.getText().toString().trim();
            if (!cmd.isEmpty() && globalOutputView != null) {
                globalOutputView.append("\n# " + cmd + "\n");
                executeCommand(cmd);
                inputCommand.setText("");
            }
        });
    }

    private void executeCommand(String command) {
        try {
            // Real Shell Execution
            Process process = new ProcessBuilder(command.split(" ")).redirectErrorStream(true).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            
            String line;
            while ((line = reader.readLine()) != null) {
                String finalLine = line;
                runOnUiThread(() -> globalOutputView.append(finalLine + "\n"));
            }
        } catch (Exception e) {
            runOnUiThread(() -> globalOutputView.append("[ERROR]: " + e.getMessage() + "\n"));
        }
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
        public TabFragment(int type) { this.type = type; }
        
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (type == 0) {
                ScrollView sv = new ScrollView(getContext());
                TextView tv = new TextView(getContext());
                tv.setText(">> SYSTEM READY, PRASHANT BHAI...\n");
                tv.setTextColor(0xFF00FF00);
                tv.setPadding(20, 20, 20, 20);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setTextIsSelectable(true); // FIX: Copy/Paste enable
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
                    names[i] = "[INSTALLED]: " + apps.get(i).packageName;
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, names) {
                    @Override public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text = view.findViewById(android.R.id.text1);
                        text.setTextColor(0xFF00FF00);
                        text.setTextSize(12);
                        return view;
                    }
                };
                lv.setAdapter(adapter);
            } catch (Exception e) {}
        }
    }
}

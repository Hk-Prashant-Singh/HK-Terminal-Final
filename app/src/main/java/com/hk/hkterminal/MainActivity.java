package com.hk.hkterminal;

import android.Manifest;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
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
import java.util.Locale;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private EditText inputCommand;
    private Button runButton;
    private static TextView globalOutputView;
    private TextToSpeech alexVoice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Professional Permission Check [cite: 2026-02-01]
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }

        // Initialize Alex (Speaker Option)
        alexVoice = new TextToSpeech(this, status -> {
            if (status != TextToSpeech.ERROR) alexVoice.setLanguage(Locale.UK);
        });

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
                globalOutputView.append("\nroot@pshacker:~# " + cmd + "\n");
                executeCommand(cmd);
                inputCommand.setText("");
            }
        });
    }

    private void executeCommand(String command) {
        new Thread(() -> {
            try {
                // Real Engine Execution
                Process process = Runtime.getRuntime().exec(command);
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String finalLine = line;
                    runOnUiThread(() -> {
                        globalOutputView.append(finalLine + "\n");
                        // Alex speaks the output
                        alexVoice.speak(finalLine, TextToSpeech.QUEUE_ADD, null, null);
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> globalOutputView.append("[DENIED]: Check Root/Permissions\n"));
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
        public TabFragment(int type) { this.type = type; }
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            if (type == 0) {
                ScrollView sv = new ScrollView(getContext());
                TextView tv = new TextView(getContext());
                tv.setText(">> SYSTEM READY, PRASHANT BHAI...\n>> SPEAKER ACTIVE\n");
                tv.setTextColor(0xFF00FF00);
                tv.setTypeface(android.graphics.Typeface.MONOSPACE);
                tv.setTextIsSelectable(true); // Fix for Copy/Paste
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

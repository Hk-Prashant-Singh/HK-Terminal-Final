package com.hk.hkterminal;

import android.Manifest;
import android.os.*;
import android.view.*;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private EditText inputCommand;
    public static TextView outputView; 
    private ProgressBar headerProgress;
    private List<String> history = new ArrayList<>();

    public interface Callback { 
        void onOutput(String line); 
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewPager);
        inputCommand = findViewById(R.id.inputCommand);
        headerProgress = findViewById(R.id.headerProgress);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.MODIFY_AUDIO_SETTINGS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET
            }, 101);
        }

        viewPager.setAdapter(new TerminalPagerAdapter(this));
        new TabLayoutMediator(findViewById(R.id.tabLayout), viewPager, (tab, pos) -> 
            tab.setText(pos == 0 ? "TERMINAL" : "PACKAGES")).attach();

        inputCommand.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                execute(inputCommand.getText().toString());
                inputCommand.setText("");
                return true;
            }
            return false;
        });

        findViewById(R.id.btnUp).setOnClickListener(v -> sendNanoKey(KeyEvent.KEYCODE_DPAD_UP));
        findViewById(R.id.btnDown).setOnClickListener(v -> sendNanoKey(KeyEvent.KEYCODE_DPAD_DOWN));
        findViewById(R.id.btnLeft).setOnClickListener(v -> sendNanoKey(KeyEvent.KEYCODE_DPAD_LEFT));
        findViewById(R.id.btnRight).setOnClickListener(v -> sendNanoKey(KeyEvent.KEYCODE_DPAD_RIGHT));
        findViewById(R.id.btnCLR).setOnClickListener(v -> { if(outputView!=null) outputView.setText(">> HK READY\n"); });
    }

    private void sendNanoKey(int keyCode) {
        BaseInputConnection ic = new BaseInputConnection(inputCommand, true);
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
    }

    public void execute(String cmd) {
        if (outputView != null) outputView.append("\nroot@pshacker:~# " + cmd + "\n");
        history.add(cmd);
        headerProgress.setVisibility(View.VISIBLE);
        TerminalEngine.run(cmd, line -> runOnUiThread(() -> {
            if (outputView != null) outputView.append(line + "\n");
            headerProgress.setVisibility(View.GONE);
        }));
    }

    public static class TabFragment extends Fragment {
        private float mScale = 14f;
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle b) {
            if (getArguments().getInt("type") == 0) {
                ScrollView sv = new ScrollView(getContext());
                outputView = new TextView(getContext());
                outputView.setTextColor(0xFF00FF00);
                outputView.setTypeface(android.graphics.Typeface.MONOSPACE);
                outputView.setTextIsSelectable(true); 
                
                ScaleGestureDetector gd = new ScaleGestureDetector(getContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override public boolean onScale(ScaleGestureDetector d) {
                        mScale *= d.getScaleFactor();
                        outputView.setTextSize(mScale);
                        return true;
                    }
                });

                outputView.setOnTouchListener((v, event) -> {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        int offset = outputView.getOffsetForPosition(event.getX(), event.getY());
                        ((MainActivity)getActivity()).execute("jump-to-offset " + offset);
                    }
                    return gd.onTouchEvent(event);
                });

                sv.addView(outputView);
                return sv;
            } else {
                return new View(getContext()); 
            }
        }
    }

    private class TerminalPagerAdapter extends FragmentStateAdapter {
        public TerminalPagerAdapter(AppCompatActivity a) { super(a); }
        @NonNull @Override public Fragment createFragment(int p) {
            TabFragment f = new TabFragment();
            Bundle b = new Bundle(); b.putInt("type", p);
            f.setArguments(b); return f;
        }
        @Override public int getItemCount() { return 2; }
    }
            }

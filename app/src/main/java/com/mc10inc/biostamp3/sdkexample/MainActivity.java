package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdkexample.streaming.StreamingFragment;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnItemSelected;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.pager)
    ViewPager pager;

    @BindView(R.id.selectedSensorSpinner)
    Spinner selectedSensorSpinner;

    @BindView(R.id.throughputText)
    TextView throughputText;

    private Unbinder unbinder;
    private ExampleViewModel viewModel;

    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        viewModel = new ViewModelProvider(this).get(ExampleViewModel.class);

        pager.setAdapter(new PagesAdapter(getSupportFragmentManager()));

        BioStampManager.getInstance().getBioStampsLiveData().observe(this, sensors -> {
            List<String> connectedSensors = sensors.values().stream()
                    .filter(s -> s.getState() == BioStamp.State.CONNECTED)
                    .map(BioStamp::getSerial)
                    .sorted()
                    .collect(Collectors.toList());
            String previousSelection = (String)selectedSensorSpinner.getSelectedItem();
            selectedSensorSpinner.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_dropdown_item, connectedSensors));
            if (previousSelection != null && connectedSensors.contains(previousSelection)) {
                selectedSensorSpinner.setSelection(connectedSensors.indexOf(previousSelection));
            }
            if (connectedSensors.isEmpty()) {
                viewModel.setSelectedSensor(null);
            }
        });

        BioStampManager.getInstance().getThroughput().observe(this, bps -> {
            if (bps == 0) {
                throughputText.setText("");
            } else {
                throughputText.setText(String.format("%dbps", bps));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.about_menu_item:
                showAboutPopup();
                return true;
            case R.id.test_crash_report_menu_item:
                showTestCrashReportPopup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAboutPopup() {
        new AlertDialog.Builder(this)
                .setMessage(String.format("Version %s", BuildConfig.GIT_HASH))
                .create()
                .show();
    }

    private void showTestCrashReportPopup() {
        new AlertDialog.Builder(this)
                .setMessage("Are you sure?")
                .setCancelable(true)
                .setPositiveButton("Yes", (dialogInterface, i) -> {
                    throw new RuntimeException("Test Crash");
                })
                .create()
                .show();
    }

    @OnItemSelected(R.id.selectedSensorSpinner) void onSensorSelected(int position) {
        viewModel.setSelectedSensor((String)selectedSensorSpinner.getItemAtPosition(position));
    }

    @OnItemSelected(value = R.id.selectedSensorSpinner, callback = OnItemSelected.Callback.NOTHING_SELECTED)
    void onSensorNothingSelected() {
        viewModel.setSelectedSensor(null);
    }

    private static class PagesAdapter extends FragmentPagerAdapter {
        private static class Page {
            private interface FragmentConstructor {
                Fragment construct();
            }

            FragmentConstructor constructor;
            String title;

            Page(FragmentConstructor constructor, String title) {
                this.constructor = constructor;
                this.title = title;
            }
        }

        private final List<Page> pages;

        PagesAdapter(FragmentManager fm) {
            super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);

            pages = new ArrayList<>();
            pages.add(new Page(ScanFragment::new, "Scan"));
            pages.add(new Page(SensorsFragment::new, "Sensors"));
            pages.add(new Page(ControlsFragment::new, "Controls"));
            pages.add(new Page(SensingFragment::new, "Sensing"));
            pages.add(new Page(StreamingFragment::new, "Streaming"));
            pages.add(new Page(DownloadFragment::new, "Download"));
            pages.add(new Page(RecordingsFragment::new, "Recordings"));
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            if (position < pages.size()) {
                return pages.get(position).constructor.construct();
            } else {
                return pages.get(0).constructor.construct();
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            if (position < pages.size()) {
                return pages.get(position).title;
            } else {
                return null;
            }
        }

        @Override
        public int getCount() {
            return pages.size();
        }
    }
}

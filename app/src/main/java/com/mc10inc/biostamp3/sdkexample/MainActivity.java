package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
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

    @OnItemSelected(R.id.selectedSensorSpinner) void onSensorSelected(int position) {
        viewModel.setSelectedSensor((String)selectedSensorSpinner.getItemAtPosition(position));
    }

    @OnItemSelected(value = R.id.selectedSensorSpinner, callback = OnItemSelected.Callback.NOTHING_SELECTED)
    void onSensorNothingSelected() {
        viewModel.setSelectedSensor(null);
    }

    private static class PagesAdapter extends FragmentPagerAdapter {
        PagesAdapter(FragmentManager fm) {
            super(fm);
        }

        @NotNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new ScanFragment();
                case 1:
                    return new SensorsFragment();
                case 2:
                    return new ControlsFragment();
                case 3:
                    return new StreamingFragment();
                case 4:
                    return new DownloadFragment();
                case 5:
                default:
                    return new RecordingsFragment();
            }
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Scan";
                case 1:
                    return "Sensors";
                case 2:
                    return "Controls";
                case 3:
                    return "Streaming";
                case 4:
                    return "Download";
                case 5:
                    return "Recordings";
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    }
}

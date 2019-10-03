package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.sensing.PredefinedConfigs;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ControlsFragment extends BaseFragment {
    @BindView(R.id.enableRecordingCheckBox)
    CheckBox enableRecordingCheckBox;

    @BindView(R.id.sensorConfigSpinner)
    Spinner sensorConfigSpinner;

    @BindView(R.id.statusText)
    TextView statusText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);
        unbinder = ButterKnife.bind(this, view);

        SpinnerAdapter sensorConfigAdapter = new ArrayAdapter<>(getContext(),
                R.layout.list_item_sensor_config,
                PredefinedConfigs.getConfigs());
        sensorConfigSpinner.setAdapter(sensorConfigAdapter);

        return view;
    }

    @OnClick(R.id.blinkLedButton) void blinkLedButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.blinkLed((error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
    }

    @OnClick(R.id.startSensingButton) void startSensingButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        SensorConfig sc = (SensorConfig)sensorConfigSpinner.getSelectedItem();
        sc.setRecordingEnabled(enableRecordingCheckBox.isChecked());
        s.startSensing(sc, (error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });

    }

    @OnClick(R.id.stopSensingButton) void stopSensingButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.stopSensing((error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
    }

    @SuppressLint("DefaultLocale")
    @OnClick(R.id.getStatusButton) void getStatusButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.getSensorStatus((error, result) -> {
            if (error == null) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Batt %d%% %s Uptime %ds\n",
                        result.getBatteryPercent(),
                        result.isCharging() ? "charging" : "",
                        result.getUptime()));
                if (result.getSensingInfo().isEnabled()) {
                    sb.append(result.getSensingInfo().toString());
                } else {
                    sb.append("Sensing is idle\n");
                }
                statusText.setText(sb.toString());
            } else {
                Timber.e(error);
            }
        });
    }
}

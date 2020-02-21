package com.mc10inc.biostamp3.sdkexample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.sensing.PredefinedConfigs;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class SensingFragment extends BaseFragment {
    @BindView(R.id.enableRecordingCheckBox)
    CheckBox enableRecordingCheckBox;

    @BindView(R.id.sensorConfigSpinner)
    Spinner sensorConfigSpinner;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sensing, container, false);
        unbinder = ButterKnife.bind(this, view);

        SpinnerAdapter sensorConfigAdapter = new ArrayAdapter<>(getContext(),
                R.layout.list_item_sensor_config,
                PredefinedConfigs.getConfigs());
        sensorConfigSpinner.setAdapter(sensorConfigAdapter);

        return view;
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
}

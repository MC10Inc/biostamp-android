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

import java.nio.charset.StandardCharsets;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import timber.log.Timber;

public class SensingFragment extends BaseFragment {
    @BindView(R.id.enableRecordingCheckBox)
    CheckBox enableRecordingCheckBox;

    @BindView(R.id.maxDurationText)
    TextView maxDurationText;

    @BindView(R.id.metadataText)
    TextView metadataText;

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

        maxDurationText.setEnabled(false);
        metadataText.setEnabled(false);

        return view;
    }

    @SuppressLint("DefaultLocale")
    @OnClick(R.id.startSensingButton) void startSensingButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }

        SensorConfig sc = (SensorConfig)sensorConfigSpinner.getSelectedItem();
        sc.setRecordingEnabled(enableRecordingCheckBox.isChecked());

        int maxDuration;
        try {
            maxDuration = Integer.parseInt(maxDurationText.getText().toString());
        } catch (NumberFormatException e) {
            maxDuration = 0;
        }

        byte[] metadata = null;
        String metadataStr = metadataText.getText().toString();
        if (metadataStr.length() > 0) {
            metadata = metadataStr.getBytes(StandardCharsets.UTF_8);
            if (metadata.length > s.getRecordingMetadataMaxSize()) {
                errorPopup(String.format(
                        "Recording metadata is too large (%d bytes). Maximum size is %d bytes.",
                        metadata.length,
                        s.getRecordingMetadataMaxSize()
                ));
                return;
            }
        }

        s.startSensing(sc, maxDuration, metadata, (error, result) -> {
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

    @OnCheckedChanged(R.id.enableRecordingCheckBox) void enableRecordingChanged(boolean enabled) {
        maxDurationText.setEnabled(enabled);
        metadataText.setEnabled(enabled);
    }
}

package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.SpinnerAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.sensing.PredefinedConfigs;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdkexample.databinding.FragmentSensingBinding;

import java.nio.charset.StandardCharsets;

import timber.log.Timber;

public class SensingFragment extends BaseFragment {
    private FragmentSensingBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSensingBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        SpinnerAdapter sensorConfigAdapter = new ArrayAdapter<>(getContext(),
                R.layout.list_item_sensor_config,
                PredefinedConfigs.getConfigs());
        binding.sensorConfigSpinner.setAdapter(sensorConfigAdapter);
        binding.startSensingButton.setOnClickListener(this::startSensingButton);
        binding.stopSensingButton.setOnClickListener(this::stopSensingButton);
        binding.annotateButton.setOnClickListener(this::annotateButton);
        binding.enableRecordingCheckBox.setOnCheckedChangeListener(this::enableRecordingChanged);

        binding.maxDurationText.setEnabled(false);
        binding.metadataText.setEnabled(false);

        return view;
    }

    @SuppressLint("DefaultLocale")
    private void startSensingButton(View v) {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }

        SensorConfig sc = (SensorConfig)binding.sensorConfigSpinner.getSelectedItem();
        sc.setRecordingEnabled(binding.enableRecordingCheckBox.isChecked());

        int maxDuration;
        try {
            maxDuration = Integer.parseInt(binding.maxDurationText.getText().toString());
        } catch (NumberFormatException e) {
            maxDuration = 0;
        }

        byte[] metadata = null;
        String metadataStr = binding.metadataText.getText().toString();
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

    private void stopSensingButton(View v) {
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

    private void enableRecordingChanged(CompoundButton cb, boolean enabled) {
        binding.maxDurationText.setEnabled(enabled);
        binding.metadataText.setEnabled(enabled);
    }

    @SuppressLint("DefaultLocale")
    private void annotateButton(View v) {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }

        String annoText = binding.annotationText.getText().toString();
        if (annoText.length() < 1) {
            errorPopup("Annotation text may not be empty");
            return;
        }

        byte[] annoData = annoText.getBytes(StandardCharsets.UTF_8);
        if (annoData.length > s.getRecordingMetadataMaxSize()) {
            errorPopup(String.format(
                    "Annotation is too large (%d bytes). Maximum size is %d bytes.",
                    annoData.length,
                    s.getAnnotationDataMaxSize()
            ));
            return;
        }

        s.annotate(annoData, (error, result) -> {
            if (error == null) {
                Timber.i("Created annotation at time %.3f", result);
            } else {
                Timber.e(error);
            }
        });
    }
}

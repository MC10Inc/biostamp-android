package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.sensing.PredefinedConfigs;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ControlsFragment extends BaseFragment {
    private static final int REQUEST_CODE_OPEN_FW = 0;

    @BindView(R.id.enableRecordingCheckBox)
    CheckBox enableRecordingCheckBox;

    @BindView(R.id.sensorConfigSpinner)
    Spinner sensorConfigSpinner;

    @BindView(R.id.statusText)
    TextView statusText;

    @BindView(R.id.selectFirmwareButton)
    Button selectFirmwareButton;

    @BindView(R.id.uploadFirmwareButton)
    Button uploadFirmwareButton;

    @BindView(R.id.firmwareProgressBar)
    ProgressBar firmwareProgressBar;

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

    @OnClick(R.id.resetButton) void resetButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.reset((error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
    }

    @OnClick(R.id.loadImageButton) void loadImageButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.loadFirmwareImage((error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
    }

    @OnClick(R.id.selectFirmwareButton) void selectFirmware() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, REQUEST_CODE_OPEN_FW);
    }

    @OnClick(R.id.uploadFirmwareButton) void uploadFirmware() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }

        byte[] image = viewModel.getFirmwareImage().getValue();
        if (image == null) {
            Timber.e("No firmware image loaded");
        }

        s.uploadFirmware(image, (error, result) -> {
            if (error == null) {
                Timber.i("Firmware upload complete");
            } else {
                Timber.e(error);
            }
        }, progress -> {
            firmwareProgressBar.setProgress((int)(progress * firmwareProgressBar.getMax()));
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_OPEN_FW && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                try {
                    loadFirmwareImage(uri);
                } catch (IOException e) {
                    Timber.e(e);
                }
            }
        }
    }

    private void loadFirmwareImage(Uri uri) throws IOException {
        if (getActivity() == null) {
            return;
        }
        InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            Timber.e("Input stream is null");
            return;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] b = new byte[1024];
        while (true) {
            int len = inputStream.read(b);
            if (len == -1) {
                break;
            }
            baos.write(b, 0, len);
        }

        viewModel.setFirmwareImage(baos.toByteArray());
    }
}

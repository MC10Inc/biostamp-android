package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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
        statusText.setMovementMethod(new ScrollingMovementMethod());
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

    @SuppressLint("DefaultLocale")
    @OnClick(R.id.getStatusButton) void getStatusButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.getSensorStatus((error, result) -> {
            if (error == null) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Sensor %s\nBatt %d%% %s Uptime %ds Reset Reason %d\nFW %s BL: %s\n",
                        s.getSerial(),
                        result.getBatteryPercent(),
                        result.isCharging() ? "charging" : "",
                        result.getUptime(),
                        result.getResetReason(),
                        result.getFirmwareVersion(),
                        result.getBootloaderVersion()));
                if (result.getSensingInfo().isEnabled()) {
                    sb.append(result.getSensingInfo().toString());
                } else {
                    sb.append("Sensing is idle\n");
                }
                String fault = result.getFault();
                if (fault != null) {
                    sb.append("\n--------- FAULT ---------\n");
                    sb.append(fault);
                    sb.append("\n");
                }
                setStatusText(sb.toString());
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

    @OnClick(R.id.powerOffButton) void powerOffButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.powerOff((error, result) -> {
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

        firmwareProgressBar.setVisibility(View.VISIBLE);
        s.uploadFirmware(image, (error, result) -> {
            firmwareProgressBar.setVisibility(View.INVISIBLE);
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

    @OnClick(R.id.shareStatusButton) void shareStatusButton() {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, statusText.getText());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    @OnClick(R.id.getFaultLogButton) void getFaultLogButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.getFaultLogs((error, result) -> {
            if (error == null) {
                StringBuilder sb = new StringBuilder();
                for (String fault : result) {
                    sb.append(fault);
                    sb.append("\n--------------------------------------\n");
                }
                if (result.isEmpty()) {
                    sb.append("No faults logged");
                }
                setStatusText(sb.toString());
            } else {
                Timber.e(error);
            }
        });
    }

    @OnClick(R.id.clearFaultLogButton) void clearFaultLogButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.clearFaultLogs((error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
    }

    private void setStatusText(String text) {
        statusText.setText(text);
        statusText.scrollTo(0, 0);
    }
}

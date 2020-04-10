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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdkexample.databinding.FragmentControlsBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import timber.log.Timber;

public class ControlsFragment extends BaseFragment {
    private static final int REQUEST_CODE_OPEN_FW = 0;

    private FragmentControlsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentControlsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        binding.statusText.setMovementMethod(new ScrollingMovementMethod());
        binding.blinkLedButton.setOnClickListener(this::blinkLedButton);
        binding.getStatusButton.setOnClickListener(this::getStatusButton);
        binding.resetButton.setOnClickListener(this::resetButton);
        binding.powerOffButton.setOnClickListener(this::powerOffButton);
        binding.loadImageButton.setOnClickListener(this::loadImageButton);
        binding.selectFirmwareButton.setOnClickListener(this::selectFirmwareButton);
        binding.uploadFirmwareButton.setOnClickListener(this::uploadFirmwareButton);
        binding.shareStatusButton.setOnClickListener(this::shareStatusButton);
        binding.getFaultLogButton.setOnClickListener(this::getFaultLogButton);
        binding.clearFaultLogButton.setOnClickListener(this::clearFaultLogButton);
        return view;
    }

    private void blinkLedButton(View v) {
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
    private void getStatusButton(View v) {
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

    private void resetButton(View v) {
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

    private void powerOffButton(View v) {
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

    private void loadImageButton(View v) {
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

    private void selectFirmwareButton(View v) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, REQUEST_CODE_OPEN_FW);
    }

    private void uploadFirmwareButton(View v) {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }

        byte[] image = viewModel.getFirmwareImage().getValue();
        if (image == null) {
            Timber.e("No firmware image loaded");
        }

        binding.firmwareProgressBar.setVisibility(View.VISIBLE);
        s.uploadFirmware(image, (error, result) -> {
            binding.firmwareProgressBar.setVisibility(View.INVISIBLE);
            if (error == null) {
                Timber.i("Firmware upload complete");
            } else {
                Timber.e(error);
            }
        }, progress -> {
            binding.firmwareProgressBar.setProgress(
                    (int)(progress * binding.firmwareProgressBar.getMax()));
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

    private void shareStatusButton(View v) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, binding.statusText.getText());
        sendIntent.setType("text/plain");

        Intent shareIntent = Intent.createChooser(sendIntent, null);
        startActivity(shareIntent);
    }

    private void getFaultLogButton(View v) {
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

    private void clearFaultLogButton(View v) {
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
        binding.statusText.setText(text);
        binding.statusText.scrollTo(0, 0);
    }
}

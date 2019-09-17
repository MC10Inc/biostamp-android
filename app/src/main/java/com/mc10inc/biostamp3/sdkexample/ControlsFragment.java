package com.mc10inc.biostamp3.sdkexample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.sensing.PredefinedConfigs;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ControlsFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);
        unbinder = ButterKnife.bind(this, view);
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
        SensorConfig sc = PredefinedConfigs.getAccel();
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

    @OnClick(R.id.getSensingConfigButton) void getSensingConfigButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.getSensingInfo((error, result) -> {
            if (error == null) {
                Timber.i(result.toString());
            } else {
                Timber.e(error);
            }
        });
    }
}

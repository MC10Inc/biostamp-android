package com.mc10inc.biostamp3.sdkexample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.sensing.RawSamples;
import com.mc10inc.biostamp3.sdk.sensing.StreamingListener;
import com.mc10inc.biostamp3.sdk.sensing.StreamingType;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class StreamingFragment extends BaseFragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    private final StreamingListener motionListener = samples -> {
        Timber.i("Received %d samples %f", samples.getSize(), samples.getValue(RawSamples.ColumnType.ACCEL_X, 0));
    };

    private final StreamingListener environmentListener = samples -> {
        Timber.i("Env lux=%f pressure=%f temp=%f",
                samples.getValue(RawSamples.ColumnType.LUX, 0),
                samples.getValue(RawSamples.ColumnType.PASCALS, 0),
                samples.getValue(RawSamples.ColumnType.TEMPERATURE, 0));
    };

    @OnClick(R.id.startStreamingButton) void startStreamingButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.startStreaming(StreamingType.MOTION, (error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
        s.addStreamingListener(StreamingType.MOTION, motionListener);
        s.startStreaming(StreamingType.ENVIRONMENT, (error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
        s.addStreamingListener(StreamingType.ENVIRONMENT, environmentListener);
    }

    @OnClick(R.id.stopStreamingButton) void stopStreamingButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.stopStreaming(StreamingType.MOTION, (error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
        s.removeStreamingListener(StreamingType.MOTION, motionListener);
        s.stopStreaming(StreamingType.ENVIRONMENT, (error, result) -> {
            if (error != null) {
                Timber.e(error);
            }
        });
        s.removeStreamingListener(StreamingType.ENVIRONMENT, environmentListener);
    }
}

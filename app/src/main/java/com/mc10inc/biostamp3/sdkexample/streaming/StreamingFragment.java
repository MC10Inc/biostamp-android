package com.mc10inc.biostamp3.sdkexample.streaming;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.exception.SensorCannotStartException;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.sensing.StreamingType;
import com.mc10inc.biostamp3.sdkexample.BaseFragment;
import com.mc10inc.biostamp3.sdkexample.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class StreamingFragment extends BaseFragment implements PlotContainer.Listener {
    @BindView(R.id.plotGroup)
    LinearLayout plotGroup;

    private Map<PlotKey, PlotContainer> plotContainers = new HashMap<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_streaming, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.addPlotButton) void addPlotButton() {
        BioStamp s = viewModel.getSensor();
        if (s == null) {
            return;
        }
        s.getSensingInfo((error, result) -> {
            if (error == null) {
                if (result.isEnabled()) {
                    selectPlotType(s, result.getSensorConfig());
                } else {
                    errorPopup("Sensing is not enabled");
                }
            } else {
                Timber.e(error);
            }
        });
    }

    private void selectPlotType(BioStamp s, SensorConfig sensorConfig) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }

        List<PlotType> plotTypes = new ArrayList<>();
        if (sensorConfig.hasMotionAccel()) {
            plotTypes.add(PlotType.ACCEL);
        }
        if (sensorConfig.hasMotionGyro()) {
            plotTypes.add(PlotType.GYRO);
        }
        if (sensorConfig.hasEnvironment()) {
            plotTypes.add(PlotType.ENVIRONMENT);
        }
        if (sensorConfig.hasAfe4900Ecg()) {
            plotTypes.add(PlotType.BIOPOTENTIAL);
        }

        CharSequence[] items = plotTypes.stream()
                .map(Enum::toString)
                .toArray(CharSequence[]::new);
        new AlertDialog.Builder(getActivity())
                .setTitle("Select a plot type:")
                .setItems(items, (dialog, which) -> {
                    addPlot(s, sensorConfig, plotTypes.get(which));
                })
                .setCancelable(true)
                .show();
    }

    private void addPlot(BioStamp s, SensorConfig sensorConfig, PlotType plotType) {
        if (getActivity() == null || getActivity().isFinishing()) {
            return;
        }
        switch (plotType) {
            case ACCEL:
                addPlotAccel(s, sensorConfig);
                break;
            case GYRO:
                addPlotGyro(s, sensorConfig);
                break;
            case ENVIRONMENT:
                addPlotEnvironment(s, sensorConfig);
                break;
            case BIOPOTENTIAL:
                addPlotAfe4900Ecg(s, sensorConfig);
                break;
        }
    }

    private void addPlotContainer(PlotKey key, StreamingPlot plot) {
        PlotContainer plotContainer = new PlotContainer(getContext());
        plotContainer.init(key, this, plot);
        plotContainers.put(key, plotContainer);
        plotGroup.addView(plotContainer);
    }

    private void addPlotAccel(BioStamp s, SensorConfig sensorConfig) {
        PlotKey key = new PlotKey(s.getSerial(), PlotType.ACCEL);
        if (!plotContainers.containsKey(key)) {
            SignalPlotView plot = new SignalPlotView(getContext());
            plot.init(key, sensorConfig);
            addPlotContainer(key, plot);
            s.addStreamingListener(StreamingType.MOTION, plot);
        }
        enableStreaming(s, StreamingType.MOTION);
    }

    private void addPlotGyro(BioStamp s, SensorConfig sensorConfig) {
        PlotKey key = new PlotKey(s.getSerial(), PlotType.GYRO);
        if (!plotContainers.containsKey(key)) {
            SignalPlotView plot = new SignalPlotView(getContext());
            plot.init(key, sensorConfig);
            addPlotContainer(key, plot);
            s.addStreamingListener(StreamingType.MOTION, plot);
        }
        enableStreaming(s, StreamingType.MOTION);
    }

    private void addPlotEnvironment(BioStamp s, SensorConfig sensorConfig) {
        PlotKey key = new PlotKey(s.getSerial(), PlotType.ENVIRONMENT);
        if (!plotContainers.containsKey(key)) {
            EnvironmentPlotView plot = new EnvironmentPlotView(getContext());
            plot.init(key, sensorConfig);
            addPlotContainer(key, plot);
            s.addStreamingListener(StreamingType.ENVIRONMENT, plot);
        }
        enableStreaming(s, StreamingType.ENVIRONMENT);
    }

    private void addPlotAfe4900Ecg(BioStamp s, SensorConfig sensorConfig) {
        PlotKey key = new PlotKey(s.getSerial(), PlotType.BIOPOTENTIAL);
        if (!plotContainers.containsKey(key)) {
            SignalPlotView plot = new SignalPlotView(getContext());
            plot.init(key, sensorConfig);
            addPlotContainer(key, plot);
            s.addStreamingListener(StreamingType.AFE4900, plot);
        }
        enableStreaming(s, StreamingType.AFE4900);
    }

    private void enableStreaming(BioStamp s, StreamingType streamingType) {
        s.startStreaming(streamingType, (error, result) -> {
            if (error != null && !(error instanceof SensorCannotStartException)) {
                Timber.e(error);
            }
        });
    }

    @Override
    public void closePlot(PlotKey key) {
        PlotContainer plotContainer = plotContainers.get(key);
        if (plotContainer == null) {
            Timber.e("Plot container %s not found", key);
            return;
        }
        BioStamp s = BioStampManager.getInstance().getBioStamp(key.getSerial());
        if (s != null) {
            s.removeStreamingListener(plotContainer.getPlot());
        }
        plotGroup.removeView(plotContainer);
        plotContainers.remove(key);
    }
}

package com.mc10inc.biostamp3.sdkexample.streaming;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import com.mc10inc.biostamp3.sdk.sensing.RawSamples;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdkexample.databinding.LayoutEnvironmentPlotBinding;

public class EnvironmentPlotView extends LinearLayout implements StreamingPlot {
    private LayoutEnvironmentPlotBinding binding;

    public EnvironmentPlotView(Context context) {
        super(context);
        initView(context);
    }

    public EnvironmentPlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public EnvironmentPlotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        binding = LayoutEnvironmentPlotBinding.inflate(LayoutInflater.from(context), this, true);
    }

    @Override
    public void init(PlotKey key, SensorConfig sensorConfig) {

    }

    @Override
    public View getView() {
        return this;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean handleRawSamples(RawSamples samples) {
        binding.temperatureText.setText(String.format("Temperature: %.1f°C",
                samples.getValue(RawSamples.ColumnType.TEMPERATURE, samples.getSize() - 1)));
        binding.externalTemperatureText.setText(String.format("External Temperature: %.1f°C",
                samples.getValue(RawSamples.ColumnType.EXTERNAL_TEMPERATURE,
                        samples.getSize() - 1)));
        binding.pressureText.setText(String.format("Pressure: %.0f pascals",
                samples.getValue(RawSamples.ColumnType.PASCALS, samples.getSize() - 1)));

        return true;
    }
}

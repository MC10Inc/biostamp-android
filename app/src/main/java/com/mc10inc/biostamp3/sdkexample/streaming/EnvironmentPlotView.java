package com.mc10inc.biostamp3.sdkexample.streaming;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.mc10inc.biostamp3.sdk.sensing.RawSamples;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdkexample.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EnvironmentPlotView extends LinearLayout implements StreamingPlot {
    @BindView(R.id.temperatureText)
    TextView temperatureText;

    @BindView(R.id.pressureText)
    TextView pressureText;

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
        View view = LayoutInflater.from(context).inflate(R.layout.layout_environment_plot, this, true);
        ButterKnife.bind(this, view);
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
    public void handleRawSamples(RawSamples samples) {
        temperatureText.setText(String.format("Temperature: %.1f°C",
                samples.getValue(RawSamples.ColumnType.TEMPERATURE, samples.getSize() - 1)));
        pressureText.setText(String.format("Pressure: %.0f pascals",
                samples.getValue(RawSamples.ColumnType.PASCALS, samples.getSize() - 1)));
    }
}

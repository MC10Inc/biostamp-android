package com.mc10inc.biostamp3.sdkexample.streaming;

import android.content.Context;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.StepMode;
import com.androidplot.xy.XYGraphWidget;
import com.androidplot.xy.XYPlot;
import com.mc10inc.biostamp3.sdk.sensing.RawSamples;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.sensing.StreamingListener;
import com.mc10inc.biostamp3.sdkexample.R;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SignalPlotView extends FrameLayout implements StreamingListener, StreamingPlot {
    private static final int DURATION_SEC = 6;
    private static final double VOLTS_TO_MV = 1000;

    @BindView(R.id.plot)
    XYPlot plot;

    private List<RawSamplesDataSeries> dataSeriesList = new ArrayList<>();

    public SignalPlotView(Context context) {
        super(context);
        initView(context);
    }

    public SignalPlotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public SignalPlotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.layout_signal_plot, this, true);
        ButterKnife.bind(this, view);
    }

    @Override
    public void init(PlotKey key, SensorConfig sensorConfig) {
        genericPlotSetup(plot);
        switch (key.getPlotType()) {
            case ACCEL:
                initAccel(sensorConfig);
                break;
            case GYRO:
                initGyro(sensorConfig);
                break;
            case BIOPOTENTIAL:
                initBiopotential(sensorConfig);
                break;
        }
    }

    private void initAccel(SensorConfig sensorConfig) {
        plot.setTitle("Accelerometer");

        int gRange = sensorConfig.getAccelGRange();
        plot.setRangeLabel("Acceleration (g)");
        plot.setRangeBoundaries(-gRange, gRange, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.SUBDIVIDE, 5);

        int samplingPeriodUs = sensorConfig.getMotionSamplingPeriodUs();
        RawSamplesDataSeries seriesX = new RawSamplesDataSeries("X", samplingPeriodUs,
                getDurationSec() * 1000000, RawSamples.ColumnType.ACCEL_X, 1);
        RawSamplesDataSeries seriesY = new RawSamplesDataSeries("Y", samplingPeriodUs,
                getDurationSec() * 1000000, RawSamples.ColumnType.ACCEL_Y, 1);
        RawSamplesDataSeries seriesZ = new RawSamplesDataSeries("Z", samplingPeriodUs,
                getDurationSec() * 1000000, RawSamples.ColumnType.ACCEL_Z, 1);
        dataSeriesList.add(seriesX);
        dataSeriesList.add(seriesY);
        dataSeriesList.add(seriesZ);

        plot.addSeries(seriesX, getLineAndPointFormatter(Color.rgb(0, 0, 200)));
        plot.addSeries(seriesY, getLineAndPointFormatter(Color.rgb(200, 0, 0)));
        plot.addSeries(seriesZ, getLineAndPointFormatter(Color.rgb(0, 200, 0)));
        plot.redraw();
    }

    private void initGyro(SensorConfig sensorConfig) {
        plot.setTitle("Gyroscope");

        int dpsRange = sensorConfig.getGyroDpsRange();
        plot.setRangeLabel("Angular rate (dps)");
        plot.setRangeBoundaries(-dpsRange, dpsRange, BoundaryMode.FIXED);
        plot.setRangeStep(StepMode.SUBDIVIDE, 5);

        int samplingPeriodUs = sensorConfig.getMotionSamplingPeriodUs();
        RawSamplesDataSeries seriesX = new RawSamplesDataSeries("X", samplingPeriodUs,
                getDurationSec() * 1000000, RawSamples.ColumnType.GYRO_X, 1);
        RawSamplesDataSeries seriesY = new RawSamplesDataSeries("Y", samplingPeriodUs,
                getDurationSec() * 1000000, RawSamples.ColumnType.GYRO_Y, 1);
        RawSamplesDataSeries seriesZ = new RawSamplesDataSeries("Z", samplingPeriodUs,
                getDurationSec() * 1000000, RawSamples.ColumnType.GYRO_Z, 1);
        dataSeriesList.add(seriesX);
        dataSeriesList.add(seriesY);
        dataSeriesList.add(seriesZ);

        plot.addSeries(seriesX, getLineAndPointFormatter(Color.rgb(0, 0, 200)));
        plot.addSeries(seriesY, getLineAndPointFormatter(Color.rgb(200, 0, 0)));
        plot.addSeries(seriesZ, getLineAndPointFormatter(Color.rgb(0, 200, 0)));
        plot.redraw();
    }

    private void initBiopotential(SensorConfig sensorConfig) {
        genericPlotSetup(plot);
        plot.setTitle("AFE4900 Biopotential");

        plot.setRangeLabel("Voltage (mV)");
        plot.setRangeBoundaries(0, 0, BoundaryMode.AUTO);
        plot.setRangeStep(StepMode.SUBDIVIDE, 5);

        int samplingPeriodUs = 1000000 / 250; // TODO Add to sensor config
        RawSamplesDataSeries series = new RawSamplesDataSeries("Voltage", samplingPeriodUs,
                getDurationSec() * 1000000, RawSamples.ColumnType.ECG, VOLTS_TO_MV);
        dataSeriesList.add(series);

        plot.addSeries(series, getLineAndPointFormatter(Color.rgb(0, 0, 200)));
        plot.redraw();
    }

    private void genericPlotSetup(XYPlot plot) {
        plot.setDomainLabel("Seconds");
        plot.setDomainBoundaries(0, getDurationSec(), BoundaryMode.FIXED);
        DashPathEffect dashFx = new DashPathEffect(
                new float[] {PixelUtils.dpToPix(3), PixelUtils.dpToPix(3)}, 0);
        plot.getGraph().getDomainGridLinePaint().setPathEffect(dashFx);
        plot.getGraph().getRangeGridLinePaint().setPathEffect(dashFx);
        plot.setDomainStep(StepMode.SUBDIVIDE, getDurationSec() + 1);
        NumberFormat numberFormat = NumberFormat.getIntegerInstance();
        numberFormat.setGroupingUsed(false); // Do not add commas for thousands
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).setFormat(numberFormat);
        plot.getGraph().getLineLabelStyle(XYGraphWidget.Edge.LEFT).setFormat(numberFormat);
        plot.getLegend().setDrawIconBackgroundEnabled(false);
    }

    private LineAndPointFormatter getLineAndPointFormatter(int color) {
        LineAndPointFormatter f = new LineAndPointFormatter(color, null, null, null);
        f.getLinePaint().setStrokeJoin(Paint.Join.ROUND);
        f.getLinePaint().setStrokeWidth(3);
        return f;
    }

    public int getDurationSec() {
        return DURATION_SEC;
    }

    @Override
    public void handleRawSamples(RawSamples samples) {
        for (RawSamplesDataSeries dataSeries : dataSeriesList) {
            dataSeries.update(samples);
        }

        plot.redraw();
    }

    @Override
    public View getView() {
        return this;
    }
}

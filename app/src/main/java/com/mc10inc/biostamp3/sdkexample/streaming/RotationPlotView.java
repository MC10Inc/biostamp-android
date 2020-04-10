package com.mc10inc.biostamp3.sdkexample.streaming;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.sensing.RawSamples;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdkexample.databinding.LayoutRotationPlotBinding;

import org.rajawali3d.math.Quaternion;
import org.rajawali3d.view.ISurface;

public class RotationPlotView extends FrameLayout implements StreamingPlot {
    private LayoutRotationPlotBinding binding;
    private RotationRenderer renderer;

    public RotationPlotView(@NonNull Context context) {
        super(context);
        initView(context);
    }

    public RotationPlotView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public RotationPlotView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        binding = LayoutRotationPlotBinding.inflate(LayoutInflater.from(context), this, true);

        binding.surfaceView.setRenderMode(ISurface.RENDERMODE_WHEN_DIRTY);
        binding.surfaceView.setTransparent(true);
        renderer = new RotationRenderer(context);
        binding.surfaceView.setSurfaceRenderer(renderer);
    }

    @Override
    public void init(PlotKey key, SensorConfig sensorConfig) {

    }

    @Override
    public View getView() {
        return this;
    }

    @Override
    public boolean handleRawSamples(RawSamples samples) {
        int i = samples.getSize() - 1;
        Quaternion quat = new Quaternion(
                samples.getValue(RawSamples.ColumnType.QUAT_A, i),
                samples.getValue(RawSamples.ColumnType.QUAT_B, i),
                samples.getValue(RawSamples.ColumnType.QUAT_C, i),
                samples.getValue(RawSamples.ColumnType.QUAT_D, i));
        renderer.update(quat);

        return true;
    }
}

package com.mc10inc.biostamp3.sdkexample.streaming;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.mc10inc.biostamp3.sdkexample.databinding.LayoutPlotContainerBinding;

public class PlotContainer extends ConstraintLayout {
    public interface Listener {
        void closePlot(PlotKey key);
    }

    private LayoutPlotContainerBinding binding;
    private Listener listener;
    private PlotKey key;
    private StreamingPlot plot;

    public PlotContainer(Context context) {
        super(context);
        initView(context);
    }

    public PlotContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public PlotContainer(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        binding = LayoutPlotContainerBinding.inflate(LayoutInflater.from(context), this, true);
        binding.closeButton.setOnClickListener(this::closeButton);
    }

    public void init(PlotKey key, Listener listener, StreamingPlot plot) {
        this.key = key;
        this.listener = listener;
        this.plot = plot;

        binding.titleText.setText(key.getSerial());

        binding.container.addView(plot.getView());
    }

    private void closeButton(View v) {
        listener.closePlot(key);
    }

    public StreamingPlot getPlot() {
        return plot;
    }
}

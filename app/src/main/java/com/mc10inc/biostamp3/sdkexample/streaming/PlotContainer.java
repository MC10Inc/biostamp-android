package com.mc10inc.biostamp3.sdkexample.streaming;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.mc10inc.biostamp3.sdkexample.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class PlotContainer extends ConstraintLayout {
    public interface Listener {
        void closePlot(PlotKey key);
    }

    @BindView(R.id.container)
    FrameLayout container;

    @BindView(R.id.titleText)
    TextView titleText;

    private Listener listener;
    private PlotKey key;
    private SignalPlotView plot;

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
        View view = LayoutInflater.from(context).inflate(R.layout.layout_plot_container, this, true);
        ButterKnife.bind(this, view);
    }

    public void init(PlotKey key, Listener listener, SignalPlotView plot) {
        this.key = key;
        this.listener = listener;
        this.plot = plot;

        titleText.setText(key.getSerial());

        container.addView(plot);
    }

    @OnClick(R.id.closeButton) void closeButton() {
        listener.closePlot(key);
    }

    public SignalPlotView getPlot() {
        return plot;
    }
}

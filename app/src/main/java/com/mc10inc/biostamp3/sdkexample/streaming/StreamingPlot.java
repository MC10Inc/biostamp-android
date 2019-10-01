package com.mc10inc.biostamp3.sdkexample.streaming;

import android.view.View;

import com.mc10inc.biostamp3.sdk.sensing.RawSamples;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.sensing.StreamingListener;

interface StreamingPlot extends StreamingListener {
    void init(PlotKey key, SensorConfig sensorConfig);

    View getView();
}

package com.mc10inc.biostamp3.sdkexample.streaming;

import com.androidplot.xy.XYSeries;
import com.mc10inc.biostamp3.sdk.sensing.RawSamples;

import org.apache.commons.collections4.queue.CircularFifoQueue;

public class RawSamplesDataSeries implements XYSeries {
    private static final double US_PER_SEC = 1000000.0;

    private String title;
    private int samplingPeriodUs;
    private RawSamples.ColumnType column;
    private double scale;
    private CircularFifoQueue<Double> samples;

    public RawSamplesDataSeries(String title, int samplingPeriodUs, int durationUs,
                                RawSamples.ColumnType column, double scale) {
        this.title = title;
        this.samplingPeriodUs = samplingPeriodUs;
        this.column = column;
        this.scale = scale;
        int numSamples = durationUs / samplingPeriodUs;
        samples = new CircularFifoQueue<>(numSamples);
    }

    @Override
    public int size() {
        return samples.size();
    }

    @Override
    public synchronized Number getX(int index) {
        return index * samplingPeriodUs / US_PER_SEC;
    }

    @Override
    public synchronized Number getY(int index) {
        if (index >= samples.size()) {
            return null;
        } else {
            return samples.get(index) * scale;
        }
    }

    @Override
    public String getTitle() {
        return title;
    }

    public synchronized void update(RawSamples rawSamples) {
        for (int i = 0; i < rawSamples.getSize(); i++) {
            samples.add(rawSamples.getValue(column, i));
        }
    }
}

package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public class AD5940Samples extends RawSamples {
    private Brc3.AD5940Samples samples;

    AD5940Samples(long timestamp, int samplingPeriod, RawSampleInfo rawSampleInfo,
                  Brc3.AD5940Samples samples) {
        super(timestamp, samplingPeriod, rawSampleInfo);
        this.samples = samples;
    }

    @Override
    public int getSize() {
        return samples.getZMagCount();
    }

    @Override
    public double getValue(ColumnType columnType, int index) {
        switch (columnType) {
            case Z_MAG:
                return samples.getZMag(index);
            case Z_PHASE:
                return samples.getZPhase(index);
            default:
                throw new IllegalArgumentException();
        }
    }
}

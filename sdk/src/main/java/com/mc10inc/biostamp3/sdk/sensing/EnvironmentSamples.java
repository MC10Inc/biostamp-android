package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public class EnvironmentSamples extends RawSamples {
    private Brc3.EnvironmentSamples samples;

    EnvironmentSamples(double firstTimestamp, RawSampleInfo rawSampleInfo,
                       Brc3.EnvironmentSamples samples) {
        super(firstTimestamp, rawSampleInfo);
        this.samples = samples;
    }

    @Override
    public int getSize() {
        return samples.getLuxCount();
    }

    @Override
    public double getValue(ColumnType columnType, int index) {
        switch (columnType) {
            case LUX:
                return samples.getLux(index);
            case PASCALS:
                return samples.getPascals(index);
            case TEMPERATURE:
                return samples.getTemperatureC(index);
            default:
                throw new IllegalArgumentException();
        }
    }
}

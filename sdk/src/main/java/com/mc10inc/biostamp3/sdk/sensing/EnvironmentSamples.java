package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public class EnvironmentSamples extends RawSamples {
    private Brc3.EnvironmentSamples samples;

    EnvironmentSamples(long timestamp, int samplingPeriod, RawSampleInfo rawSampleInfo,
                       Brc3.EnvironmentSamples samples) {
        super(timestamp, samplingPeriod, rawSampleInfo);
        this.samples = samples;
    }

    @Override
    public int getSize() {
        return samples.getPascalsCount();
    }

    @Override
    public double getValue(ColumnType columnType, int index) {
        switch (columnType) {
            case PASCALS:
                return samples.getPascals(index);
            case EXTERNAL_TEMPERATURE:
                return samples.getExternalTemperatureC(index);
            case TEMPERATURE:
                return samples.getTemperatureC(index);
            default:
                throw new IllegalArgumentException();
        }
    }
}

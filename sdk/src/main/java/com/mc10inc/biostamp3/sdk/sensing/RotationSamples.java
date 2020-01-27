package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public class RotationSamples extends RawSamples {
    private Brc3.RotationSamples samples;

    RotationSamples(long timestamp, int samplingPeriod, RawSampleInfo rawSampleInfo,
                    Brc3.RotationSamples samples) {
        super(timestamp, samplingPeriod, rawSampleInfo);
        this.samples = samples;
    }

    @Override
    public int getSize() {
        return samples.getQuatACount();
    }

    @Override
    public double getValue(ColumnType columnType, int index) {
        switch (columnType) {
            case QUAT_A:
                return samples.getQuatA(index);
            case QUAT_B:
                return samples.getQuatB(index);
            case QUAT_C:
                return samples.getQuatC(index);
            case QUAT_D:
                return samples.getQuatD(index);
            default:
                throw new IllegalArgumentException();
        }
    }
}

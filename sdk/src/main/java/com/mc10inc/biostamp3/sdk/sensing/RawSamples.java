package com.mc10inc.biostamp3.sdk.sensing;

import java.util.List;

public abstract class RawSamples {
    public enum ColumnType {
        ACCEL_X,
        ACCEL_Y,
        ACCEL_Z,
        GYRO_X,
        GYRO_Y,
        GYRO_Z,
        MAG_X,
        MAG_Y,
        MAG_Z,
        QUAT_A,
        QUAT_B,
        QUAT_C,
        QUAT_D,
        ECG,
        PPG,
        AMBIENT,
        PASCALS,
        TEMPERATURE,
        Z_MAG,
        Z_PHASE
    }

    private final double firstTimestamp;
    protected final RawSampleInfo rawSampleInfo;

    RawSamples(double firstTimestamp, RawSampleInfo rawSampleInfo) {
        this.firstTimestamp = firstTimestamp;
        this.rawSampleInfo = rawSampleInfo;
    }

    public abstract int getSize();

    public double getTimestamp(int index) {
        if (index >= getSize()) {
            throw new IndexOutOfBoundsException();
        }
        return firstTimestamp + index * rawSampleInfo.getSamplingPeriod();
    }

    public abstract double getValue(ColumnType columnType, int index);

    protected int[] differentialToAbsolute(List<Integer> d) {
        int[] a = new int[d.size()];
        if (a.length > 0) {
            a[0] = d.get(0);
            for (int i = 1; i < a.length; i++) {
                a[i] = a[i-1] + d.get(i);
            }
        }
        return a;
    }
}

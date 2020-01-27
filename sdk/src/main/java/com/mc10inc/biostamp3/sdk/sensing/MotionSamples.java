package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

import java.util.EnumMap;
import java.util.Objects;

public class MotionSamples extends RawSamples {
    private final int size;
    private final EnumMap<ColumnType, int[]> columns = new EnumMap<>(ColumnType.class);

    MotionSamples(long timestamp, int samplingPeriod, RawSampleInfo rawSampleInfo,
                  Brc3.MotionSamples samples) {
        super(timestamp, samplingPeriod, rawSampleInfo);
        size = getSize(samples);
        if (samples.getAccelXCount() > 0) {
            columns.put(ColumnType.ACCEL_X, differentialToAbsolute(samples.getAccelXList()));
        }
        if (samples.getAccelYCount() > 0) {
            columns.put(ColumnType.ACCEL_Y, differentialToAbsolute(samples.getAccelYList()));
        }
        if (samples.getAccelZCount() > 0) {
            columns.put(ColumnType.ACCEL_Z, differentialToAbsolute(samples.getAccelZList()));
        }
        if (samples.getGyroXCount() > 0) {
            columns.put(ColumnType.GYRO_X, differentialToAbsolute(samples.getGyroXList()));
        }
        if (samples.getGyroYCount() > 0) {
            columns.put(ColumnType.GYRO_Y, differentialToAbsolute(samples.getGyroYList()));
        }
        if (samples.getGyroZCount() > 0) {
            columns.put(ColumnType.GYRO_Z, differentialToAbsolute(samples.getGyroZList()));
        }
        if (samples.getMagXCount() > 0) {
            columns.put(ColumnType.MAG_X, differentialToAbsolute(samples.getMagXList()));
        }
        if (samples.getMagYCount() > 0) {
            columns.put(ColumnType.MAG_Y, differentialToAbsolute(samples.getMagYList()));
        }
        if (samples.getMagZCount() > 0) {
            columns.put(ColumnType.MAG_Z, differentialToAbsolute(samples.getMagZList()));
        }
    }

    public static int getSize(Brc3.MotionSamples s) {
        if (s.getAccelXCount() > 0) {
            return s.getAccelXCount();
        } else if (s.getMagXCount() > 0) {
            return s.getMagXCount();
        } else {
            return 0;
        }
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public double getValue(ColumnType columnType, int index) {
        int raw = Objects.requireNonNull(columns.get(columnType))[index];
        switch (columnType) {
            case ACCEL_X:
            case ACCEL_Y:
            case ACCEL_Z:
                return raw * rawSampleInfo.getAccelGScale();
            case GYRO_X:
            case GYRO_Y:
            case GYRO_Z:
                return raw * rawSampleInfo.getGyroDpsScale();
            case MAG_X:
            case MAG_Y:
            case MAG_Z:
                return raw * rawSampleInfo.getMagUtScale();
            default:
                throw new IllegalArgumentException();
        }
    }
}

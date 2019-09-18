package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

import java.util.EnumMap;
import java.util.Objects;

public class AFE4900Samples extends RawSamples {
    private final int size;
    private final EnumMap<ColumnType, int[]> columns = new EnumMap<>(ColumnType.class);

    AFE4900Samples(double firstTimestamp, RawSampleInfo rawSampleInfo, Brc3.AFE4900Samples samples) {
        super(firstTimestamp, rawSampleInfo);
        size = getSize(samples);
        if (samples.getEcgCount() > 0) {
            columns.put(ColumnType.ECG, differentialToAbsolute(samples.getEcgList()));
        }
        if (samples.getPpgCount() > 0) {
            columns.put(ColumnType.PPG, differentialToAbsolute(samples.getPpgList()));
        }
        if (samples.getAmbientCount() > 0) {
            columns.put(ColumnType.AMBIENT, differentialToAbsolute(samples.getAmbientList()));
        }
    }

    public static int getSize(Brc3.AFE4900Samples s) {
        if (s.getEcgCount() > 0) {
            return s.getEcgCount();
        } else if (s.getPpgCount() > 0) {
            return s.getPpgCount();
        } else if (s.getAmbientCount() > 0) {
            return s.getAmbientCount();
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
            case ECG:
                return raw * rawSampleInfo.getEcgVScale();
            case PPG:
            case AMBIENT:
                return raw;
            default:
                throw new IllegalArgumentException();
        }
    }
}

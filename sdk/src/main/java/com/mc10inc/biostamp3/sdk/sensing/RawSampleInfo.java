package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public class RawSampleInfo {
    private double timestampScale;
    private double samplingPeriodScale;
    private double accelGScale;
    private double gyroDpsScale;
    private double magUtScale;
    private double ecgVScale;

    public RawSampleInfo(Brc3.StreamingInfo streamingInfo) {
        timestampScale = streamingInfo.getTimestampScale();
        samplingPeriodScale = streamingInfo.getSamplingPeriodScale();
        accelGScale = streamingInfo.getAccelGScale();
        gyroDpsScale = streamingInfo.getGyroDpsScale();
        magUtScale = streamingInfo.getMagUtScale();
        ecgVScale = streamingInfo.getAfe4900EcgVScale();
    }

    public RawSampleInfo(Brc3.RawDataInfo rawDataInfo) {
        timestampScale = rawDataInfo.getTimestampScale();
        samplingPeriodScale = rawDataInfo.getSamplingPeriodScale();
        accelGScale = rawDataInfo.getAccelGScale();
        gyroDpsScale = rawDataInfo.getGyroDpsScale();
        ecgVScale = rawDataInfo.getAfe4900EcgVScale();
    }

    public double getTimestampScale() {
        return timestampScale;
    }

    public double getSamplingPeriodScale() {
        return samplingPeriodScale;
    }

    public double getAccelGScale() {
        return accelGScale;
    }

    public double getGyroDpsScale() {
        return gyroDpsScale;
    }

    public double getMagUtScale() {
        return magUtScale;
    }

    public double getEcgVScale() {
        return ecgVScale;
    }
}

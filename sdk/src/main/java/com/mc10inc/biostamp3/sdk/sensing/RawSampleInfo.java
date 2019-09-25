package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public class RawSampleInfo {
    private double samplingPeriod;
    private double accelGScale;
    private double gyroDpsScale;
    private double magUtScale;
    private double ecgVScale;

    public RawSampleInfo(Brc3.StreamingInfo streamingInfo, double previousTimestamp,
                         double timestamp, int numSamples) {
        samplingPeriod = (timestamp - previousTimestamp) / numSamples;
        accelGScale = streamingInfo.getAccelGScale();
        gyroDpsScale = streamingInfo.getGyroDpsScale();
        magUtScale = streamingInfo.getMagUtScale();
        ecgVScale = streamingInfo.getAfe4900EcgVScale();
    }

    public RawSampleInfo(Brc3.RawDataInfo rawDataInfo) {
        samplingPeriod = rawDataInfo.getSamplingPeriodScale();
        accelGScale = rawDataInfo.getAccelGScale();
        gyroDpsScale = rawDataInfo.getGyroDpsScale();
        ecgVScale = rawDataInfo.getAfe4900EcgVScale();
    }

    public double getSamplingPeriod() {
        return samplingPeriod;
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

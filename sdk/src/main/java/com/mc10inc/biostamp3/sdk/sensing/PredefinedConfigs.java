package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public class PredefinedConfigs {
    public static SensorConfig getAccel() {
        return new SensorConfig(Brc3.SensorConfig.newBuilder()
                .setMotion(Brc3.MotionConfig.newBuilder()
                        .setMode(Brc3.MotionMode.ACCEL)
                        .setSamplingPeriodUs(10000)
                        .setAccelGRange(16))
                .build());
    }
}

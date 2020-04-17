package com.mc10inc.biostamp3.sdk.sensing;

import java.util.ArrayList;
import java.util.List;

public class PredefinedConfigs {
    private static List<SensorConfig> configs;

    public static List<SensorConfig> getConfigs() {
        return configs;
    }

    static {
        configs = new ArrayList<>();

        configs.add(new SensorConfig.Builder()
                .enableMotionAccel(10000, 16)
                .build());

        configs.add(new SensorConfig.Builder()
                .enableMotionRotationFromAccelGyroCompass(50000, 16, 500)
                .build());

        configs.add(new SensorConfig.Builder()
                .enableMotionAccelGyro(10000, 16, 500)
                .build());

        configs.add(new SensorConfig.Builder()
                .enableMotionAccel(10000, 16)
                .enableEnvironment(1000000)
                .build());

        configs.add(new SensorConfig.Builder()
                .enableAd5940ElectrodermalActivity()
                .build());

        configs.add(new SensorConfig.Builder()
                .enableAfe4900Ecg(4000, 12)
                .build());

        configs.add(new SensorConfig.Builder()
                .enableAfe4900Ecg(4000, 12)
                .enableMotionAccelGyro(10000, 16, 500)
                .enableEnvironment(1000000)
                .build());
    }
}

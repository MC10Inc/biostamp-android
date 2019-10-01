package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

import java.util.ArrayList;
import java.util.List;

public class PredefinedConfigs {
    private static List<SensorConfig> configs;

    public static List<SensorConfig> getConfigs() {
        return configs;
    }

    static {
        configs = new ArrayList<>();

        configs.add(new SensorConfig(Brc3.SensorConfig.newBuilder()
                .setMotion(Brc3.MotionConfig.newBuilder()
                        .setMode(Brc3.MotionMode.ACCEL)
                        .setSamplingPeriodUs(10000)
                        .setAccelGRange(16))
                .build()));

        configs.add(new SensorConfig(Brc3.SensorConfig.newBuilder()
                .setMotion(Brc3.MotionConfig.newBuilder()
                        .setMode(Brc3.MotionMode.ACCEL_GYRO)
                        .setSamplingPeriodUs(10000)
                        .setAccelGRange(16)
                        .setGyroDpsRange(500))
                .build()));

        configs.add(new SensorConfig(Brc3.SensorConfig.newBuilder()
                .setMotion(Brc3.MotionConfig.newBuilder()
                        .setMode(Brc3.MotionMode.ACCEL)
                        .setSamplingPeriodUs(10000)
                        .setAccelGRange(16))
                .setEnvironment(Brc3.EnvironmentConfig.newBuilder()
                        .setMode(Brc3.EnvironmentMode.ALL)
                        .setSamplingPeriodUs(1000000))
                .build()));

        configs.add(new SensorConfig(Brc3.SensorConfig.newBuilder()
                .setAd5940(Brc3.AD5940Config.newBuilder()
                        .setMode(Brc3.AD5940Mode.EDA))
                .build()));

        configs.add(new SensorConfig(Brc3.SensorConfig.newBuilder()
                .setAfe4900(Brc3.AFE4900Config.newBuilder()
                        .setMode(Brc3.AFE4900Mode.ECG)
                        .setEcgGain(Brc3.AFE4900ECGGain.GAIN_12))
                .build()));
    }
}

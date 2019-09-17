package com.mc10inc.biostamp3.sdk.sensing;

import android.annotation.SuppressLint;

import com.mc10inc.biostamp3.sdk.Brc3;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

public class SensorConfig {
    private Brc3.SensorConfig.Builder msg;

    public SensorConfig(Brc3.SensorConfig msg) {
        this.msg = msg.toBuilder();
    }

    public Brc3.SensorConfig getMsg() {
        return msg.build();
    }

    public boolean isRecordingEnabled() {
        return msg.getRecordingEnabled();
    }

    public void setRecordingEnabled(boolean enabled) {
        msg.setRecordingEnabled(enabled);

    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (msg.getRecordingEnabled()) {
            s.append("Recording enabled\n");
        } else {
            s.append("Streaming\n");
        }
        if (msg.hasMotion()) {
            describeMotion(s, msg.getMotion());
        }
        if (msg.hasEnvironment()) {
            describeEnvironment(s, msg.getEnvironment());
        }
        if (msg.hasAfe4900()) {
            describeAfe4900(s, msg.getAfe4900());
        }
        if (msg.hasAd5940()) {
            describeAd5940(s, msg.getAd5940());
        }
        return s.toString();
    }

    @SuppressLint("DefaultLocale")
    private void describeMotion(StringBuilder s, Brc3.MotionConfig c) {
        s.append("Motion ");
        if (c.getMode() == Brc3.MotionMode.ACCEL) {
            s.append("Accel");
        } else if (c.getMode() == Brc3.MotionMode.ACCEL_GYRO) {
            s.append("Accel+Gyro");
        } else if (c.getMode() == Brc3.MotionMode.ROTATION) {
            if (c.getRotationType() == Brc3.MotionRotationType.ROT_ACCEL_GYRO) {
                s.append("Rotation from Accel+Gyro");
            } else if (c.getRotationType() == Brc3.MotionRotationType.ROT_ACCEL_GYRO_MAG) {
                s.append("Rotation from Accel+Gyro+Mag");
            } else if (c.getRotationType() == Brc3.MotionRotationType.ROT_ACCEL_MAG) {
                s.append("Rotation from Accel+Mag");
            }
        } else if (c.getMode() == Brc3.MotionMode.COMPASS) {
            s.append("Mag");
        }
        s.append(" ");
        if (c.getMode() == Brc3.MotionMode.ACCEL || c.getMode() == Brc3.MotionMode.ACCEL_GYRO) {
            s.append(String.format("+/-%dG", c.getAccelGRange()));
        }
        if (c.getMode() == Brc3.MotionMode.ACCEL_GYRO) {
            s.append(" ");
            s.append(String.format("+/-%ddps", c.getGyroDpsRange()));
        }
        s.append(" ").append(rate(c.getSamplingPeriodUs())).append("\n");
    }

    private void describeEnvironment(StringBuilder s, Brc3.EnvironmentConfig c) {
        s.append("Environment ");
        s.append(rate(c.getSamplingPeriodUs()));
        s.append("\n");
    }

    private void describeAfe4900(StringBuilder s, Brc3.AFE4900Config c) {
        s.append("AFE ");
        if (c.getMode() == Brc3.AFE4900Mode.ECG) {
            s.append("ECG");
        } else if (c.getMode() == Brc3.AFE4900Mode.PPG) {
            s.append("PPG");
        } else if (c.getMode() == Brc3.AFE4900Mode.PTT) {
            s.append("ECG+PPG");
        }
        s.append("\n");
    }

    private void describeAd5940(StringBuilder s, Brc3.AD5940Config c) {
        s.append("AD5940 EDA");
        s.append("\n");
    }

    private String rate(int periodUs) {
        return new DecimalFormat("0.###").format(1000000.0 / periodUs) + "Hz";
    }
}

package com.mc10inc.biostamp3.sdk.sensing;

import android.annotation.SuppressLint;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.Brc3;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

/**
 * Sensor configuration.
 * <p/>
 * The sensor configuration defines which of the BioStamp's sensors are enabled, contains the
 * configuration for each enabled sensor, and contains a flag that indicates whether or not
 * recording to flash memory is enabled.
 * <p/>
 * The sensor configuration is supplied when {@link com.mc10inc.biostamp3.sdk.BioStamp#startSensing(SensorConfig,
 * int, byte[], BioStamp.Listener)} is called to enable sensing. This exact configuration remains in
 * effect for as long as sensing is enabled and for the entire recording; the only way to change any
 * setting defined here is to stop sensing and then restart sensing with a new configuration,
 * creating a new recording.
 */
public class SensorConfig {
    private Brc3.SensorConfig.Builder msg;

    public SensorConfig(Brc3.SensorConfig msg) {
        this.msg = msg.toBuilder();
    }

    public Brc3.SensorConfig getMsg() {
        return msg.build();
    }

    /**
     * Returns true if recording is enabled.
     *
     * @return true if recording is enabled
     */
    public boolean isRecordingEnabled() {
        return msg.getRecordingEnabled();
    }

    /**
     * Enable recording.
     * <p/>
     * If true, calling {@link BioStamp#startSensing(SensorConfig, int, byte[], BioStamp.Listener)}
     * will create a new recording in the sensor's flash memory which will contain samples from all
     * enabled sensors.
     *
     * @param enabled true to enable recording
     */
    public void setRecordingEnabled(boolean enabled) {
        msg.setRecordingEnabled(enabled);
    }

    /**
     * Returns true if the AD5940 bio-impedance sensor is enabled.
     *
     * @return true if the AD5940 is enabled
     */
    public boolean hasAd5940() {
        return msg.hasAd5940();
    }

    /**
     * Returns true if AD5940 impedance output is enabled.
     *
     * @return true if AD5940 impedance output is enabled
     */
    public boolean hasAd5940Z() {
        if (msg.hasAd5940()) {
            return msg.getAd5940().getMode() == Brc3.AD5940Mode.EDA;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the AFE4900 PPG / biopotential sensor is enabled.
     *
     * @return true if the AFE4900 is enabled
     */
    public boolean hasAfe4900() {
        return msg.hasAfe4900();
    }

    /**
     * Returns true if AFE4900 biopotential output is enabled.
     *
     * @return true if AFE4900 biopotential output is enabled
     */
    public boolean hasAfe4900Ecg() {
        if (hasAfe4900()) {
            return msg.getAfe4900().getMode() == Brc3.AFE4900Mode.ECG
                    || msg.getAfe4900().getMode() == Brc3.AFE4900Mode.PTT;
        } else {
            return false;
        }
    }

    /**
     * Returns true if AFE4900 PPG output is enabled.
     *
     * @return true if AFE4900 PPG output is enabled
     */
    public boolean hasAfe4900Ppg() {
        if (hasAfe4900()) {
            return msg.getAfe4900().getMode() == Brc3.AFE4900Mode.PPG
                    || msg.getAfe4900().getMode() == Brc3.AFE4900Mode.PTT;
        } else {
            return false;
        }
    }

    /**
     * Returns true if the temperature and pressure sensors are enabled.
     *
     * @return true if temperature and pressure are enabled
     */
    public boolean hasEnvironment() {
        return msg.hasEnvironment();
    }

    /**
     * Returns true if the ICM-20948 motion sensor is enabled.
     *
     * @return true if the ICM-20948 is enabled
     */
    public boolean hasMotion() {
        return msg.hasMotion();
    }

    /**
     * Returns true if ICM-20948 accelerometer output is enabled.
     *
     * @return true if ICM-20948 accelerometer output is enabled
     */
    public boolean hasMotionAccel() {
        if (hasMotion()) {
            switch (msg.getMotion().getMode()) {
                case ACCEL:
                case ACCEL_GYRO:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns true if ICM-20948 gyroscope output is enabled.
     *
     * @return true if ICM-20948 gyroscope output is enabled
     */
    public boolean hasMotionGyro() {
        if (hasMotion()) {
            switch (msg.getMotion().getMode()) {
                case ACCEL_GYRO:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Returns true if ICM-20948 rotation output is enabled.
     *
     * @return true if ICM-20948 rotation output is enabled
     */
    public boolean hasMotionRotation() {
        if (hasMotion()) {
            switch (msg.getMotion().getMode()) {
                case ROTATION:
                    return true;
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Get the accelerometer G range.
     * <p/>
     * For return value X, the samples will range from -X to X G's.
     *
     * @return accelerometer G range
     */
    public int getAccelGRange() {
        return msg.getMotion().getAccelGRange();
    }

    /**
     * Get the gyroscope DPS (degrees per second) range.
     * <p/>
     * For return value X, the samples will range from -X to X DPS.
     *
     * @return gyroscope DPS (degrees per second) range
     */
    public int getGyroDpsRange() {
        return msg.getMotion().getGyroDpsRange();
    }

    /**
     * Get the ICM-20948 motion sensor sampling period in microseconds.
     * <p/>
     * This applies to all samples produced by the ICM-20948 (accelerometer, gyroscope, or
     * rotation).
     *
     * @return motion sampling period in microseconds
     */
    public int getMotionSamplingPeriodUs() {
        return msg.getMotion().getSamplingPeriodUs();
    }

    /**
     * Describe the sensing configuration.
     * <p/>
     * Returns a multiple-line human readable string containing all details of the sensor
     * configuration.
     *
     * @return Description of the sensing configuration
     */
    @NotNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (msg.getRecordingEnabled()) {
            s.append("Rec\n");
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
        s.append("Motion(");
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
        s.append(" ").append(rate(c.getSamplingPeriodUs())).append(") ");
    }

    private void describeEnvironment(StringBuilder s, Brc3.EnvironmentConfig c) {
        s.append("Environment(");
        s.append(rate(c.getSamplingPeriodUs()));
        s.append(") ");
    }

    private void describeAfe4900(StringBuilder s, Brc3.AFE4900Config c) {
        s.append("AFE(");
        if (c.getMode() == Brc3.AFE4900Mode.ECG) {
            s.append("ECG");
        } else if (c.getMode() == Brc3.AFE4900Mode.PPG) {
            s.append("PPG");
        } else if (c.getMode() == Brc3.AFE4900Mode.PTT) {
            s.append("ECG+PPG");
        }
        s.append(")");
    }

    private void describeAd5940(StringBuilder s, Brc3.AD5940Config c) {
        s.append("AD5940(EDA) ");
    }

    private String rate(int periodUs) {
        return new DecimalFormat("0.###").format(1000000.0 / periodUs) + "Hz";
    }
}

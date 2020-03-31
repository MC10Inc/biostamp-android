package com.mc10inc.biostamp3.sdk.sensing;

import android.annotation.SuppressLint;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.Brc3;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
 * <p/>
 * Use the {@link SensorConfig.Builder} to create a sensor configuration programmatically.
 */
public class SensorConfig {
    private static final List<Integer> accelGRanges =
            Collections.unmodifiableList(Arrays.asList(2, 4, 8, 16));
    private static final List<Integer> gyroDpsRanges =
            Collections.unmodifiableList(Arrays.asList(250, 500, 1000, 2000));

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

    /**
     * Create a sensor configuration programmatically.
     * <p/>
     * To create a sensor configuration, first construct a SensorConfig.Builder. Call the enable
     * method for each sensor that you want to enable. Finally, call {@link #build()} to obtain the
     * sensor configuration object. There can be only one configuration per sensor type; for example
     * it is an error to call {@link #enableMotionAccel(int, int)} followed by {@link
     * #enableMotionAccelGyro(int, int, int)} because they both enable the same sensor but in
     * different modes. An IllegalArgumentException is thrown if any sensor setting is invalid or
     * unsupported.
     * <p/>
     * All sampling periods are specified in units of microseconds. For example a sampling period
     * of 50000µs corresponds to (1000000 / 50000) = 20Hz.
     */
    public static class Builder {
        private Brc3.SensorConfig.Builder msg;

        /**
         * Create a Builder to build a sensor configuration.
         */
        public Builder() {
            msg = Brc3.SensorConfig.newBuilder();
        }

        /**
         * Build the sensor configuration.
         *
         * @return sensor configuration
         */
        public SensorConfig build() {
            if (!msg.hasMotion() && !msg.hasAd5940() && !msg.hasEnvironment() && !msg.hasAfe4900()) {
                throw new IllegalArgumentException("No sensors are enabled!");
            }
            return new SensorConfig(msg.build());
        }

        /**
         * Enable recording to flash memory.
         *
         * @return builder
         */
        public Builder enableRecording() {
            msg.setRecordingEnabled(true);
            return this;
        }

        /**
         * Enable the environment (temperature and atmospheric pressure) sensor.
         *
         * @param samplingPeriodUs sampling period
         * @return builder
         */
        public Builder enableEnvironment(int samplingPeriodUs) {
            if (msg.hasEnvironment()) {
                throw new IllegalArgumentException("Environment sensor is already enabled!");
            }
            msg.setEnvironment(Brc3.EnvironmentConfig.newBuilder()
                    .setMode(Brc3.EnvironmentMode.ALL)
                    .setSamplingPeriodUs(samplingPeriodUs));
            return this;
        }

        /**
         * Enable the ICM-20948 motion sensor in accelerometer mode.
         *
         * @param samplingPeriodUs sampling period
         * @param accelGRange      Accelerometer G range. Must be 2, 4, 8, or 16. For example if 16
         *                         is specified, the full scale range of the sensor is -16G to
         *                         +16G.
         * @return builder
         */
        public Builder enableMotionAccel(int samplingPeriodUs, int accelGRange) {
            if (msg.hasMotion()) {
                throw new IllegalArgumentException("Motion sensor is already enabled!");
            }
            validateAccelGRange(accelGRange);
            msg.setMotion(Brc3.MotionConfig.newBuilder()
                    .setSamplingPeriodUs(samplingPeriodUs)
                    .setAccelGRange(accelGRange)
                    .setMode(Brc3.MotionMode.ACCEL));
            return this;
        }

        /**
         * Enable the ICM-20948 motion sensor in accelerometer and gyroscope mode.
         *
         * @param samplingPeriodUs sampling period
         * @param accelGRange      Accelerometer G range. Must be 2, 4, 8, or 16. For example if 16
         *                         is specified, the full scale range of the sensor is -16G to
         *                         +16G.
         * @param gyroDpsRange     Gyroscope DPS (degrees per second) range. Must be 250, 500, 1000,
         *                         or 2000. For example if 2000 is specified, the full scale range
         *                         of the sensor is -2000DPS to +2000DPS.
         * @return builder
         */
        public Builder enableMotionAccelGyro(int samplingPeriodUs, int accelGRange,
                                             int gyroDpsRange) {
            if (msg.hasMotion()) {
                throw new IllegalArgumentException("Motion sensor is already enabled!");
            }
            validateAccelGRange(accelGRange);
            validateGyroDpsRange(gyroDpsRange);
            msg.setMotion(Brc3.MotionConfig.newBuilder()
                    .setSamplingPeriodUs(samplingPeriodUs)
                    .setAccelGRange(accelGRange)
                    .setGyroDpsRange(gyroDpsRange)
                    .setMode(Brc3.MotionMode.ACCEL_GYRO));
            return this;
        }

        private Builder enableMotionRotation(int samplingPeriodUs, int accelGRange,
                                             int gyroDpsRange,
                                             Brc3.MotionRotationType rotationType) {
            if (msg.hasMotion()) {
                throw new IllegalArgumentException("Motion sensor is already enabled!");
            }
            validateAccelGRange(accelGRange);
            validateGyroDpsRange(gyroDpsRange);
            msg.setMotion(Brc3.MotionConfig.newBuilder()
                    .setSamplingPeriodUs(samplingPeriodUs)
                    .setAccelGRange(accelGRange)
                    .setGyroDpsRange(gyroDpsRange)
                    .setRotationType(rotationType)
                    .setMode(Brc3.MotionMode.ROTATION));
            return this;
        }

        /**
         * Enable the ICM-20948 motion sensor in accel+gyro rotation mode.
         * <p/>
         * The accelerometer and gyroscope are used to determine the sensor's orientation.
         *
         * @param samplingPeriodUs sampling period
         * @param accelGRange      Accelerometer G range. Must be 2, 4, 8, or 16. For example if 16
         *                         is specified, the full scale range of the sensor is -16G to
         *                         +16G.
         * @param gyroDpsRange     Gyroscope DPS (degrees per second) range. Must be 250, 500, 1000,
         *                         or 2000. For example if 2000 is specified, the full scale range
         *                         of the sensor is -2000DPS to +2000DPS.
         * @return builder
         */
        public Builder enableMotionRotationFromAccelGyro(int samplingPeriodUs, int accelGRange,
                                                         int gyroDpsRange) {
            return enableMotionRotation(samplingPeriodUs, accelGRange, gyroDpsRange,
                    Brc3.MotionRotationType.ROT_ACCEL_GYRO);
        }

        /**
         * Enable the ICM-20948 motion sensor in accel+gyro+compass rotation mode.
         * <p/>
         * The accelerometer, gyroscope, and compass are used to determine the sensor's
         * orientation.
         *
         * @param samplingPeriodUs sampling period
         * @param accelGRange      Accelerometer G range. Must be 2, 4, 8, or 16. For example if 16
         *                         is specified, the full scale range of the sensor is -16G to
         *                         +16G.
         * @param gyroDpsRange     Gyroscope DPS (degrees per second) range. Must be 250, 500, 1000,
         *                         or 2000. For example if 2000 is specified, the full scale range
         *                         of the sensor is -2000DPS to +2000DPS.
         * @return builder
         */
        public Builder enableMotionRotationFromAccelGyroCompass(int samplingPeriodUs, int accelGRange,
                                                                int gyroDpsRange) {
            return enableMotionRotation(samplingPeriodUs, accelGRange, gyroDpsRange,
                    Brc3.MotionRotationType.ROT_ACCEL_GYRO_MAG);
        }

        /**
         * Enable the ICM-20948 motion sensor in accel+compass rotation mode.
         * <p/>
         * The accelerometer and compass are used to determine the sensor's orientation.
         *
         * @param samplingPeriodUs sampling period
         * @param accelGRange      Accelerometer G range. Must be 2, 4, 8, or 16. For example if 16
         *                         is specified, the full scale range of the sensor is -16G to
         *                         +16G.
         * @return builder
         */
        public Builder enableMotionRotationFromAccelCompass(int samplingPeriodUs, int accelGRange) {
            // Gyro is not used in this mode, specify any valid DPS range for it
            return enableMotionRotation(samplingPeriodUs, accelGRange, 2000,
                    Brc3.MotionRotationType.ROT_ACCEL_MAG);
        }

        /**
         * Enable the ICM-20948 motion sensor in compass mode.
         *
         * @param samplingPeriodUs sampling period
         * @return builder
         */
        public Builder enableMotionCompass(int samplingPeriodUs) {
            if (msg.hasMotion()) {
                throw new IllegalArgumentException("Motion sensor is already enabled!");
            }
            msg.setMotion(Brc3.MotionConfig.newBuilder()
                    .setSamplingPeriodUs(samplingPeriodUs)
                    .setMode(Brc3.MotionMode.COMPASS));
            return this;
        }

        /**
         * Enable the AD5940 bio-impedance sensor in electrodermal activity mode.
         *
         * @return builder
         */
        public Builder enableAd5940ElectrodermalActivity() {
            if (msg.hasAd5940()) {
                throw new IllegalArgumentException("AD5940 sensor is already enabled!");
            }
            msg.setAd5940(Brc3.AD5940Config.newBuilder()
                    .setMode(Brc3.AD5940Mode.EDA));
            return this;
        }

        /**
         * Enable the AFE4900 sensor in ECG (biopotential) mode.
         *
         * @param ecgGain ECG gain. Must be 2, 3, 4, 5, 6, 9, or 12.
         * @return builder
         */
        public Builder enableAfe4900Ecg(int ecgGain) {
            if (msg.hasAfe4900()) {
                throw new IllegalArgumentException("AFE4900 sensor is already enabled!");
            }
            msg.setAfe4900(Brc3.AFE4900Config.newBuilder()
                    .setMode(Brc3.AFE4900Mode.ECG)
                    .setEcgGain(convertAfe4900EcgGain(ecgGain)));
            return this;
        }

        private Brc3.AFE4900ECGGain convertAfe4900EcgGain(int ecgGain) {
            switch (ecgGain) {
                case 2:
                    return Brc3.AFE4900ECGGain.GAIN_2;
                case 3:
                    return Brc3.AFE4900ECGGain.GAIN_3;
                case 4:
                    return Brc3.AFE4900ECGGain.GAIN_4;
                case 5:
                    return Brc3.AFE4900ECGGain.GAIN_5;
                case 6:
                    return Brc3.AFE4900ECGGain.GAIN_6;
                case 9:
                    return Brc3.AFE4900ECGGain.GAIN_9;
                case 12:
                    return Brc3.AFE4900ECGGain.GAIN_12;
                default:
                    throw new IllegalArgumentException("Invalid AFE4900 ECG gain");
            }
        }

        private void validateAccelGRange(int accelGRange) {
            if (!accelGRanges.contains(accelGRange)) {
                throw new IllegalArgumentException("Invalid accelerometer G range");
            }
        }

        private void validateGyroDpsRange(int gyroDpsRange) {
            if (!gyroDpsRanges.contains(gyroDpsRange)) {
                throw new IllegalArgumentException("Invalid gyroscope DPS range");
            }
        }
    }
}

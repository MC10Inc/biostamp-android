package com.mc10inc.biostamp3.sdk.sensing;

import java.util.List;

/**
 * Represents a group of samples produced by one of the sensors within the BioStamp.
 * <p/>
 * The group of samples is either obtained by streaming samples from the sensor over the BLE
 * connection or by decoding a downloaded recording.
 * <p/>
 * Each RawSamples object contains an arbitrary number of samples, where each sample contains of a
 * set of values. This object can be thought of as a table, where each sample is a row and each type
 * of value is a column. For example, for accelerometer samples each sample consists of {@link
 * ColumnType#ACCEL_X}, {@link ColumnType#ACCEL_Y}, and {@link ColumnType#ACCEL_Z} values.
 */
public abstract class RawSamples {
    /**
     * Define the types of sample values
     */
    public enum ColumnType {
        /** ICM-20948 Accelerometer X axis in G's */
        ACCEL_X,
        /** ICM-20948 Accelerometer Y axis in G's */
        ACCEL_Y,
        /** ICM-20948 Accelerometer Z axis in G's */
        ACCEL_Z,
        /** ICM-20948 Gyroscope X axis in DPS */
        GYRO_X,
        /** ICM-20948 Gyroscope Y axis in DPS */
        GYRO_Y,
        /** ICM-20948 Gyroscope Z axis in DPS */
        GYRO_Z,
        /** ICM-20948 Magnetometer X axis in microtesla */
        MAG_X,
        /** ICM-20948 Magnetometer Y axis in microtesla */
        MAG_Y,
        /** ICM-20948 Magnetometer Z axis in microtesla */
        MAG_Z,
        /** ICM-20948 Rotation - a component of quaternion a+b*i+c*j+d*k */
        QUAT_A,
        /** ICM-20948 Rotation - b component of quaternion a+b*i+c*j+d*k */
        QUAT_B,
        /** ICM-20948 Rotation - c component of quaternion a+b*i+c*j+d*k */
        QUAT_C,
        /** ICM-20948 Rotation - d component of quaternion a+b*i+c*j+d*k */
        QUAT_D,
        /** AFE4900 biopotential in volts */
        ECG,
        /** AFE4900 PPG in arbitrary units */
        PPG,
        /** AFE4900 PPG ambient in arbitrary units */
        AMBIENT,
        /** Atmospheric pressure in pascals */
        PASCALS,
        EXTERNAL_TEMPERATURE,
        /** Temperature in degrees Celsius */
        TEMPERATURE,
        /** AD5940 impedance magnitude in ohms */
        Z_MAG,
        /** AD5940 impedance phase in radians */
        Z_PHASE
    }

    private final double timestamp;
    private final double samplingPeriod;
    protected final RawSampleInfo rawSampleInfo;

    RawSamples(long timestamp, int samplingPeriod, RawSampleInfo rawSampleInfo) {
        this.timestamp = timestamp * rawSampleInfo.getTimestampScale();
        this.samplingPeriod = samplingPeriod * rawSampleInfo.getSamplingPeriodScale();
        this.rawSampleInfo = rawSampleInfo;
    }

    /**
     * Get the number of samples.
     *
     * @return number of samples
     */
    public abstract int getSize();

    /**
     * Get the timestamp of a sample.
     *
     * @param index Index of the sample from 0 to {@link #getSize()}-1
     * @return timestamp of the sample in seconds
     */
    public double getTimestamp(int index) {
        if (index >= getSize()) {
            throw new IndexOutOfBoundsException();
        }
        return timestamp + index * samplingPeriod;
    }

    /**
     * Get one of the values from a sample.
     * <p/>
     * Of all of the different types of values, any given RawSamples object will only contain a
     * subset. For example, if the RawSamples object is passed to a listener that was registered to
     * receive streaming motion samples, and the sensor configuration sets up the motion sensor in
     * accelerometer mode, then only {@link ColumnType#ACCEL_X}, {@link ColumnType#ACCEL_Y}, and
     * {@link ColumnType#ACCEL_Z} would be defined.
     * <p/>
     * See {@link com.mc10inc.biostamp3.sdk.sensing.RecordingDecoder.RawSamplesType} for the units
     * of the requested type of value.
     *
     * @param columnType Type of value to get from the sample
     * @param index      Index of the sample from 0 to {@link #getSize()}-1
     * @return Value of the sample
     */
    public abstract double getValue(ColumnType columnType, int index);

    protected int[] differentialToAbsolute(List<Integer> d) {
        int[] a = new int[d.size()];
        if (a.length > 0) {
            a[0] = d.get(0);
            for (int i = 1; i < a.length; i++) {
                a[i] = a[i-1] + d.get(i);
            }
        }
        return a;
    }
}

package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

/**
 * Define the types of samples that can be streamed over the BLE connection from the sensor.
 */
public enum StreamingType {
    /** AD5940 bio-impedance samples */
    AD5940(Brc3.StreamingType.AD5940),
    /** AFE4900 PPG or biopotential samples */
    AFE4900(Brc3.StreamingType.AFE4900),
    /** Temperature and pressure samples */
    ENVIRONMENT(Brc3.StreamingType.ENVIRONMENT),
    /** ICM-20948 accelerometer / gyroscope samples */
    MOTION(Brc3.StreamingType.MOTION),
    /** ICM-20948 rotation samples */
    ROTATION(Brc3.StreamingType.MOTION);

    Brc3.StreamingType msgType;

    StreamingType(Brc3.StreamingType msgType) {
        this.msgType = msgType;
    }

    public Brc3.StreamingType getMsgType() {
        return msgType;
    }
}

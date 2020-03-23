package com.mc10inc.biostamp3.sdk;

import com.fitbit.bluetooth.fbgatt.GattConnection;

/**
 * Info about a sensor in range.
 * <p/>
 * Describes a sensor which was detected through BLE scanning.
 */
public class ScannedSensorStatus {
    private String serial;

    ScannedSensorStatus(GattConnection conn) {
        serial = conn.getDevice().getName();
    }

    /**
     * Get the sensor serial number.
     *
     *  @return Sensor serial number
     */
    public String getSerial() {
        return serial;
    }
}

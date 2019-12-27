package com.mc10inc.biostamp3.sdk;

import com.fitbit.bluetooth.fbgatt.GattConnection;

public class ScannedSensorStatus {
    private String serial;

    ScannedSensorStatus(GattConnection conn) {
        serial = conn.getDevice().getName();
    }

    public String getSerial() {
        return serial;
    }
}
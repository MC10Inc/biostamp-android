package com.mc10inc.biostamp3.sdk;

import com.fitbit.bluetooth.fbgatt.GattConnection;

public class SensorStatus {
    private String name;

    SensorStatus(GattConnection conn) {
        name = conn.getDevice().getName();
    }

    public String getName() {
        return name;
    }
}

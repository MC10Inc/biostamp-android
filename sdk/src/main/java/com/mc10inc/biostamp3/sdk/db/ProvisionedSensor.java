package com.mc10inc.biostamp3.sdk.db;

import androidx.annotation.NonNull;

public class ProvisionedSensor {
    @NonNull
    private final String serial;

    public ProvisionedSensor(@NonNull String serial) {
        this.serial = serial;
    }

    @NonNull
    public String getSerial() {
        return serial;
    }
}

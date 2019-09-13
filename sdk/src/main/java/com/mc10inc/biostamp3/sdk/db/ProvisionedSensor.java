package com.mc10inc.biostamp3.sdk.db;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ProvisionedSensor {
    @PrimaryKey
    @NonNull
    private String serial;

    public ProvisionedSensor(@NonNull String serial) {
        this.serial = serial;
    }

    @NonNull
    public String getSerial() {
        return serial;
    }
}

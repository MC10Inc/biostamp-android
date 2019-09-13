package com.mc10inc.biostamp3.sdk.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ProvisionedSensor.class}, version = 1)
public abstract class BioStampDatabase extends RoomDatabase {
    public abstract ProvisionedSensorDao provisionedSensorDao();

    private static volatile BioStampDatabase INSTANCE;

    public static BioStampDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (BioStampDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            BioStampDatabase.class, "BioStampDB").build();
                }
            }
        }
        return INSTANCE;
    }
}

package com.mc10inc.biostamp3.sdk.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class BioStampDb {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "BioStamp.db";

    private static final String CREATE_TABLE_PROVISIONED_SENSORS =
            "CREATE TABLE IF NOT EXISTS provisioned_sensors ("
                    + "serial TEXT NOT NULL, "
                    + "PRIMARY KEY (serial))";

    private class DbHelper extends SQLiteOpenHelper {
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_PROVISIONED_SENSORS);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }
    }

    private DbHelper dbHelper;

    public BioStampDb(Context context) {
        dbHelper = new DbHelper((context));
    }

    public List<ProvisionedSensor> getProvisionedSensors() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<ProvisionedSensor> sensors = new ArrayList<>();
        try (Cursor cursor = db.query("provisioned_sensors",
                new String[]{
                        "serial"},
                null,
                null,
                null,
                null,
                "serial ASC")) {
            while (cursor.moveToNext()) {
                String serial = cursor.getString(0);
                ProvisionedSensor sensor = new ProvisionedSensor(serial);
                sensors.add(sensor);
            }
        }
        return sensors;
    }

    public void insertProvisionedSensor(ProvisionedSensor sensor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("serial", sensor.getSerial());
        db.insertWithOnConflict("provisioned_sensors", null, cv,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void deleteProvisionedSensor(ProvisionedSensor sensor) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("provisioned_sensors",
                "serial = ?",
                new String[]{sensor.getSerial()});
    }
}

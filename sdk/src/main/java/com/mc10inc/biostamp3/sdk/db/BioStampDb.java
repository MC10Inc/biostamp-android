package com.mc10inc.biostamp3.sdk.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.recording.DownloadStatus;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class BioStampDb {
    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "BioStamp.db";

    private static final String CREATE_TABLE_PROVISIONED_SENSORS =
            "CREATE TABLE IF NOT EXISTS provisioned_sensors ("
                    + "serial TEXT PRIMARY KEY)";

    private static final String CREATE_TABLE_RECORDINGS =
            "CREATE TABLE IF NOT EXISTS recordings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "serial TEXT NOT NULL, "
                    + "recording_id INTEGER NOT NULL, "
                    + "num_pages INTEGER NOT NULL, "
                    + "info_msg BLOB NOT NULL, "
                    + "UNIQUE(serial, recording_id))";

    private static final String CREATE_TABLE_PAGES =
            "CREATE TABLE IF NOT EXISTS pages ("
                    + "recording INTEGER NOT NULL REFERENCES recordings(id) ON DELETE CASCADE, "
                    + "page_number INTEGER NOT NULL, "
                    + "page_msg BLOB NOT NULL, "
                    + "PRIMARY KEY(recording, page_number)) "
                    + "WITHOUT ROWID";

    private class DbHelper extends SQLiteOpenHelper {
        DbHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_PROVISIONED_SENSORS);
            db.execSQL(CREATE_TABLE_RECORDINGS);
            db.execSQL(CREATE_TABLE_PAGES);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        }

        @Override
        public void onConfigure(SQLiteDatabase db) {
            super.onConfigure(db);
            db.setForeignKeyConstraintsEnabled(true);
        }
    }

    private class DbRecInfo {
        final long id;
        final int numPages;

        DbRecInfo(long id, int numPages) {
            this.id = id;
            this.numPages = numPages;
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

    public void insertRecording(RecordingInfo recording) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("serial", recording.getSerial());
        cv.put("recording_id", recording.getRecordingId());
        cv.put("num_pages", recording.getNumPages());
        cv.put("info_msg", recording.getMsg().toByteArray());
        long id = db.insertWithOnConflict("recordings", null, cv,
                SQLiteDatabase.CONFLICT_FAIL);
    }

    public void deleteRecording(RecordingKey recordingKey) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("recordings",
                "serial = ? AND recording_id = ?",
                new String[]{
                        recordingKey.getSerial(),
                        String.valueOf(recordingKey.getRecordingId())});
    }

    private DbRecInfo getDbRecInfo(RecordingKey recordingKey) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query("recordings",
                new String[]{"id", "num_pages"},
                "serial = ? AND recording_id = ?",
                new String[]{
                        recordingKey.getSerial(),
                        String.valueOf(recordingKey.getRecordingId())},
                null,
                null,
                null,
                null)) {
            if (cursor.moveToNext()) {
                 return new DbRecInfo(cursor.getLong(0), cursor.getInt(1));
            } else {
                return null;
            }
        }
    }

    public void insertRecordingPages(RecordingKey key, List<Brc3.RecordingPage> pages) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        DbRecInfo dbRecInfo = getDbRecInfo(key);
        if (dbRecInfo == null) {
            return;
        }
        SQLiteStatement stmt = db.compileStatement(
                "INSERT INTO pages (recording, page_number, page_msg) VALUES (?, ?, ?)");
        db.beginTransaction();
        try {
            for (Brc3.RecordingPage page : pages) {
                stmt.bindLong(1, dbRecInfo.id);
                stmt.bindLong(2, page.getPageNumber());
                stmt.bindBlob(3, page.toByteArray());
                stmt.executeInsert();
                stmt.clearBindings();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public DownloadStatus getRecordingDownloadStatus(RecordingKey key) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        DbRecInfo dbRecInfo = getDbRecInfo(key);
        if (dbRecInfo == null) {
            return null;
        }
        try (Cursor cursor = db.rawQuery(
                "SELECT COUNT(*), MAX(page_number) FROM pages WHERE recording = ?",
                new String[]{String.valueOf(dbRecInfo.id)})) {
            cursor.moveToNext();
            int downloadedPages = cursor.getInt(0);
            int lastDownloadedPageNum = cursor.getInt(1);
            if (lastDownloadedPageNum != downloadedPages - 1) {
                Timber.e("Downloaded recording is missing pages");
            }
            return new DownloadStatus(dbRecInfo.numPages, false, downloadedPages);
        }
    }
}

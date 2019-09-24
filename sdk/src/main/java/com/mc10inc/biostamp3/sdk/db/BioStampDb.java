package com.mc10inc.biostamp3.sdk.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Handler;
import android.os.Looper;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.recording.DownloadStatus;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import timber.log.Timber;

public class BioStampDb {
    public interface RecordingUpdateListener {
        void recordingsDbUpdated();
    }

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
    private Handler handler = new Handler(Looper.getMainLooper());
    private Set<RecordingUpdateListener> recordingUpdateListeners = new CopyOnWriteArraySet<>();

    public BioStampDb(Context context) {
        dbHelper = new DbHelper(context);
    }

    public void addRecordingUpdateListener(RecordingUpdateListener listener) {
        recordingUpdateListeners.add(listener);
    }

    public void removeRecordingUpdateListener(RecordingUpdateListener listener) {
        recordingUpdateListeners.remove(listener);
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
        notifyRecordingUpdate();
    }

    public void deleteAllRecordings() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("recordings", null, null);
        notifyRecordingUpdate();
    }

    public void deleteRecording(RecordingKey recordingKey) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("recordings",
                "serial = ? AND recording_id = ?",
                new String[]{
                        recordingKey.getSerial(),
                        String.valueOf(recordingKey.getRecordingId())});
        notifyRecordingUpdate();
    }

    public List<RecordingInfo> getRecordings() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<RecordingInfo> recs = new ArrayList<>();
        try (Cursor cursor = db.query("recordings",
                new String[]{"serial", "info_msg"},
                null,
                null,
                null,
                null,
                "recording_id ASC",
                null)) {
            while (cursor.moveToNext()) {
                Brc3.RecordingInfo msg;
                String serial = cursor.getString(0);
                try {
                    msg = Brc3.RecordingInfo.parseFrom(cursor.getBlob(1));
                } catch (InvalidProtocolBufferException e) {
                    Timber.e(e);
                    continue;
                }
                RecordingInfo recInfo = new RecordingInfo(msg, serial);
                recs.add(recInfo);
            }
        }
        for (RecordingInfo recInfo : recs) {
            DownloadStatus ds = getRecordingDownloadStatus(recInfo);
            recInfo.setDownloadStatus(ds);
        }
        return recs;
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
        notifyRecordingUpdate();
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

    private void notifyRecordingUpdate() {
        for (RecordingUpdateListener listener : recordingUpdateListeners) {
            handler.post(listener::recordingsDbUpdated);
        }
    }
}

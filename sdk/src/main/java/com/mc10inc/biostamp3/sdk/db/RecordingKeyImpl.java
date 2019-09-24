package com.mc10inc.biostamp3.sdk.db;

public class RecordingKeyImpl implements RecordingKey {
    private final String serial;
    private final int recordingId;

    public RecordingKeyImpl(String serial, int recordingId) {
        this.serial = serial;
        this.recordingId = recordingId;
    }

    @Override
    public String getSerial() {
        return serial;
    }

    @Override
    public int getRecordingId() {
        return recordingId;
    }
}

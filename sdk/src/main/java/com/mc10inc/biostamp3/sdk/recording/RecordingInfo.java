package com.mc10inc.biostamp3.sdk.recording;

import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RecordingInfo {
    private Brc3.RecordingInfo msg;
    private String serial;

    public RecordingInfo(Brc3.RecordingInfo msg, String serial) {
        this.msg = msg;
        this.serial = serial;
    }

    public int getDurationSec() {
        return msg.getDurationSec();
    }

    public boolean isInProgress() {
        return msg.getInProgress();
    }

    public int getNumPages() {
        return msg.getNumPages();
    }

    public int getRecordingId() {
        return msg.getRecordingId();
    }

    public SensorConfig getSensorConfig() {
        return new SensorConfig(msg.getSensorConfig());
    }

    public String getSerial() {
        return serial;
    }

    public long getStartTimestamp() {
        return msg.getTimestampStart();
    }

    public String getStartTimestampString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date(getStartTimestamp() * 1000));
    }
}

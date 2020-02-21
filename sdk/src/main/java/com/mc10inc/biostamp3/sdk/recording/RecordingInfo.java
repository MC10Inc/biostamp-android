package com.mc10inc.biostamp3.sdk.recording;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.db.RecordingKey;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RecordingInfo implements RecordingKey, Parcelable {
    private Brc3.RecordingInfo msg;
    private String serial;
    private DownloadStatus downloadStatus;

    public RecordingInfo(Brc3.RecordingInfo msg, String serial) {
        this.msg = msg;
        this.serial = serial;
    }

    protected RecordingInfo(Parcel in) {
        byte[] msgBytes = new byte[in.readInt()];
        in.readByteArray(msgBytes);
        try {
            msg = Brc3.RecordingInfo.parseFrom(msgBytes);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
        serial = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        byte[] msgBytes = msg.toByteArray();
        dest.writeInt(msgBytes.length);
        dest.writeByteArray(msgBytes);
        dest.writeString(serial);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RecordingInfo> CREATOR = new Creator<RecordingInfo>() {
        @Override
        public RecordingInfo createFromParcel(Parcel in) {
            return new RecordingInfo(in);
        }

        @Override
        public RecordingInfo[] newArray(int size) {
            return new RecordingInfo[size];
        }
    };

    public int getDurationSec() {
        return msg.getDurationSec();
    }

    public boolean isInProgress() {
        return msg.getInProgress();
    }

    public byte[] getMetadata() {
        return msg.getMetadata().toByteArray();
    }

    public Brc3.RecordingInfo getMsg() {
        return msg;
    }

    public int getNumPages() {
        return msg.getNumPages();
    }

    @Override
    public int getRecordingId() {
        return msg.getRecordingId();
    }

    public SensorConfig getSensorConfig() {
        return new SensorConfig(msg.getSensorConfig());
    }

    @Override
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

    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }
}

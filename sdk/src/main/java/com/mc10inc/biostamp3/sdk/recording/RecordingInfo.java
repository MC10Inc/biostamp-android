package com.mc10inc.biostamp3.sdk.recording;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampDb;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Info about a recording in the sensor's flash memory or in the database
 * <p/>
 * This may be used as the parameter to any method which accepts a {@link
 * com.mc10inc.biostamp3.sdk.BioStampDb.RecordingKey} to uniquely identify a recording.
 */
public class RecordingInfo implements BioStampDb.RecordingKey, Parcelable {
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

    /**
     * Get the duration of the recording in seconds.
     *
     * @return Duration of the recording in seconds
     */
    public int getDurationSec() {
        return msg.getDurationSec();
    }

    /**
     * Check if the recording is in progress
     * <p/>
     * This only applies to recordings in the sensor's flash memory, not in the database. A
     * recording cannot be downloaded from the sensor until it has been stopped by calling {@link
     * com.mc10inc.biostamp3.sdk.BioStamp#stopSensing(BioStamp.Listener)} and is no longer in
     * progress.
     *
     * @return True if the recording is in progress
     */
    public boolean isInProgress() {
        return msg.getInProgress();
    }

    /**
     * Get the recording's metadata.
     * <p/>
     * This is the value that was passed to {@link BioStamp#startSensing(SensorConfig, int, byte[],
     * BioStamp.Listener)} when the recording was started.
     *
     * @return Recording metadata
     */
    public byte[] getMetadata() {
        return msg.getMetadata().toByteArray();
    }

    public Brc3.RecordingInfo getMsg() {
        return msg;
    }

    /**
     * Get the size of the recording in pages.
     * <p/>
     * The size of the recording is given in units of 'pages' which correspond to a fixed amount of
     * space in the sensor's flash memory.
     *
     * @return Size of the recording in pages
     */
    public int getNumPages() {
        return msg.getNumPages();
    }

    @Override
    public int getRecordingId() {
        return msg.getRecordingId();
    }

    /**
     * Get the sensor configuration for the recording.
     *
     * @return Sensor configuration
     */
    public SensorConfig getSensorConfig() {
        return new SensorConfig(msg.getSensorConfig());
    }

    @Override
    public String getSerial() {
        return serial;
    }

    /**
     * Get the recording's start time in seconds.
     * <p/>
     * Multiply by 1000 to obtain a timestamp in milliseconds compatible with java.util.Date.
     *
     * @return Recording's start time in seconds.
     */
    public long getStartTimestamp() {
        return msg.getTimestampStart();
    }

    /**
     * Get the recording's start time formatted as a string.
     *
     * @return Recording's start time as a string
     */
    public String getStartTimestampString() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date(getStartTimestamp() * 1000));
    }

    /**
     * Get the download status of this recording within the database
     * <p/>
     * Returns null if the recording is not found in the database because the download had never
     * been started or because it was downloaded and has since been deleted.
     *
     * @return Download status within the database or null if recording is not in the database
     */
    public DownloadStatus getDownloadStatus() {
        return downloadStatus;
    }

    public void setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
    }
}

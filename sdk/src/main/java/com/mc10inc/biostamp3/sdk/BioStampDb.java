package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.recording.DownloadStatus;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.List;

public interface BioStampDb {
    void addRecordingUpdateListener(RecordingUpdateListener listener);

    void removeRecordingUpdateListener(RecordingUpdateListener listener);

    void deleteAllRecordings();

    void deleteRecording(RecordingKey recordingKey);

    List<RecordingInfo> getRecordings();

    DownloadStatus getRecordingDownloadStatus(RecordingKey key);

    interface RecordingUpdateListener {
        void recordingsDbUpdated();
    }

    interface RecordingKey {
        String getSerial();

        int getRecordingId();
    }
}

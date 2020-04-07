package com.mc10inc.biostamp3.sdk;

import androidx.lifecycle.LiveData;

import com.mc10inc.biostamp3.sdk.recording.DownloadStatus;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.List;

/**
 * BioStamp recording database.
 * <p/>
 * Provides access to the database in the application's local storage which contains recordings
 * downloaded from sensors.
 */
public interface BioStampDb {
    /**
     * Delete all recordings in the database.
     */
    void deleteAllRecordings();

    /**
     * Delete one recording from the database.
     *
     * @param recordingKey Recording to delete
     */
    void deleteRecording(RecordingKey recordingKey);

    /**
     * Get a list of all recordings in the database.
     * <p/>
     * The list includes recordings which are fully downloaded, partially downloaded, and which are
     * currently being downloaded. Use {@link #getRecordingDownloadStatus(RecordingKey)} to
     * determine the status of each individual recording.
     *
     * @return List of all recordings in the database
     */
    List<RecordingInfo> getRecordings();

    /**
     * Get a list of all recordings in the database as LiveData.
     * <p/>
     * The value of the LiveData is the same as the value returned by {@link #getRecordings()}.
     * Observers are notified any time the contents of the database change. This may be used to
     * update a UI interface that shows the contents of the database.
     *
     * @return LiveData of list of all recordings in the database
     */
    LiveData<List<RecordingInfo>> getRecordingsLiveData();

    /**
     * Get the download status of a recording.
     * <p/>
     * The status indicates whether the download is complete or incomplete. If the download is
     * incomplete the status indicates the progress of the download.
     *
     * @param key Recording to get status for
     * @return Download status of the recording, or null if the recording is not found
     */
    DownloadStatus getRecordingDownloadStatus(RecordingKey key);

    /**
     * Listener to receive notification when the contents of the database change.
     */
    interface RecordingUpdateListener {
        /**
         * Method to be called when the contents of the database change.
         * <p/>
         * This method is called on the main thread any time a new recording is added, a recording
         * is deleted, or the progress of a download advances.
         */
        void recordingsDbUpdated();
    }

    /**
     * Key which uniquely identifies a recording in the database.
     */
    interface RecordingKey {
        /**
         * Get the serial number of the sensor the recording was downloaded from.
         *
         * @return Sensor serial number
         */
        String getSerial();

        /**
         * Get the recording id.
         * <p/>
         * The recording id is an integer which identifies a recording stored within a specific
         * sensor. These are not unique across multiple sensors which is why they must be combined
         * with the sensor serial number to uniquely identify a recording.
         *
         * @return Recording id
         */
        int getRecordingId();
    }
}

package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.sensing.StreamingListener;
import com.mc10inc.biostamp3.sdk.sensing.StreamingType;

import java.util.List;

/**
 * BioStamp object representing a single BioStamp sensor.
 * <p/>
 * The BioStamp object for a specific sensor, as identified by the sensor serial number, is obtained
 * through the {@link BioStampManager#getBioStamp(String)} method.
 * <p/>
 * This object can perform various sensor tasks by communicating with the sensor over BLE. Each task
 * is represented by a method which accepts a {@link Listener}. The task method returns immediately
 * and starts performing the task in the background. The listener is called on the main thread when
 * the task is completed or fails. It is guaranteed that the listener passed to a task method will
 * be called exactly once regardless of whether the task succeeds, fails, or the connection is
 * lost.
 * <p/>
 * Some long-running tasks also accept a {@link ProgressListener} which is called periodically on
 * the main thread to indicate the task's progress.
 */
public interface BioStamp {
    /**
     * Request a connection to the sensor.
     * <p/>
     * There must not be a connection already established or in progress. It is a fatal error to
     * call this method if {@link #getState()} returns any value other than {@link
     * State#DISCONNECTED}.
     * <p/>
     * When this method is called, the SDK will attempt to connect to the sensor. If the connection
     * is successful, then {@link ConnectListener#connected()} is called and when the connection is
     * eventually disconnected for any reason, then {@link ConnectListener#disconnected()} will be
     * called. If the connection attempt fails or times out, then {@link
     * ConnectListener#connectFailed()} is called and {@link ConnectListener#disconnected()} is
     * never called.
     *
     * @param connectListener listener to receive connect and disconnect callbacks
     */
    void connect(ConnectListener connectListener);

    /**
     * Request disconnection from the sensor.
     * <p/>
     * If the sensor is not connected, then nothing happens and there is no error. When
     * disconnection is complete, {@link ConnectListener#disconnected()} is called for the {@link
     * ConnectListener} that was passed to {@link #connect(ConnectListener)}.
     */
    void disconnect();

    /**
     * Get the sensor serial number.
     *
     * @return serial number
     */
    String getSerial();

    /**
     * Get the state of the connection to this sensor
     *
     * @return connection state
     */
    State getState();

    /**
     * Cancel the sensor task that is currently being executed.
     *
     * If there is no task in progress, nothing is done.
     */
    void cancelTask();

    /**
     * Blink the sensor's LEDs.
     * <p/>
     * This is useful for visually identifying which sensor is connected.
     *
     * @param listener
     */
    void blinkLed(Listener<Void> listener);

    /**
     * Start sensing.
     * <p/>
     * The sensors within the {@link SensorConfig} are initialized and begin sampling. If the {@link
     * SensorConfig#setRecordingEnabled(boolean)} flag is set, a new recording is started in the
     * sensor's flash memory and all samples are written to that recording.
     * <p/>
     * If recording is enabled and there is no space left in the sensor's flash, {@link
     * com.mc10inc.biostamp3.sdk.exception.SensorMemoryFullException} is returned to the listener.
     * If the specified sensor configuration is invalid or unsupported, {@link
     * com.mc10inc.biostamp3.sdk.exception.SensorInvalidParameterException} is returned. If sensing
     * is already enabled, {@link com.mc10inc.biostamp3.sdk.exception.SensorCannotStartException} is
     * returned.
     *
     * @param sensorConfig Sensor configuration including recording enabled flag
     * @param maxDuration  Maximum duration of the recording in seconds (if recording is enabled).
     *                     Specify 0 to record indefinitely until {@link #stopSensing(Listener)} is
     *                     called.
     * @param metadata     Optional metadata that describes the recording (if recording is enabled).
     *                     The maximum size of the metadata is given by {@link
     *                     #getRecordingMetadataMaxSize()}. Set to null if metadata is not needed.
     *                     The metadata is available through {@link #getRecordingList(Listener)}.
     * @param listener
     */
    void startSensing(SensorConfig sensorConfig, int maxDuration, byte[] metadata,
                      Listener<Void> listener);

    /**
     * Stop sensing.
     * <p/>
     * If recording had been enabled, the recording ends.
     * <p/>
     * If sensing is not enabled, {@link com.mc10inc.biostamp3.sdk.exception.SensorCannotStopException}
     * is returned to the listener.
     *
     * @param listener
     */
    void stopSensing(Listener<Void> listener);

    /**
     * Get info about the current sensing operation.
     * <p/>
     * The info passed to the listener includes the {@link SensorConfig} if sensing is enabled. See
     * {@link SensingInfo} for details of what is returned.
     *
     * @param listener
     */
    void getSensingInfo(Listener<SensingInfo> listener);

    /**
     * Get sensor status.
     * <p/>
     * The response include info about sensing, power, firmware, and faults. See {@link
     * SensorStatus} for details of what is returned.
     *
     * @param listener
     */
    void getSensorStatus(Listener<SensorStatus> listener);

    /**
     * Start streaming sensor samples.
     * <p/>
     * Once sensing is enabled, this command may be used to start streaming a specific type of
     * sensor samples over the BLE connection to the SDK. Once this command completes successfully,
     * those samples may be obtained by the application by calling {@link
     * #addStreamingListener(StreamingType, StreamingListener)}.
     * <p/>
     * If sensing is not enabled, {@link com.mc10inc.biostamp3.sdk.exception.SensorCannotStartException}
     * is returned to the listener. If sensing is enabled but the specific type of sample requested
     * by type is not enabled in the sensor configuration, then {@link
     * com.mc10inc.biostamp3.sdk.exception.SensorFailException} is returned.
     *
     * @param type     type of sensor samples to stream
     * @param listener
     */
    void startStreaming(StreamingType type, Listener<Void> listener);

    /**
     * Stop streaming sensor samples.
     * <p/>
     * The specified type of sensor samples stop streaming over the BLE connection. Any streaming
     * listeners that had been added by calling {@link #addStreamingListener(StreamingType,
     * StreamingListener)} will stop receiving samples.
     * <p/>
     * If sensing is not enabled, {@link com.mc10inc.biostamp3.sdk.exception.SensorCannotStartException}
     * is returned to the listener. If sensing is enabled but the specific type of sample requested
     * by type is not enabled in the sensor configuration, then {@link
     * com.mc10inc.biostamp3.sdk.exception.SensorFailException} is returned.
     *
     * @param type     type of sensor samples to stop streaming
     * @param listener
     */
    void stopStreaming(StreamingType type, Listener<Void> listener);

    /**
     * Add a listener to receive streaming sensor samples
     * <p/>
     * Sensing must be enabled and {@link #startStreaming(StreamingType, Listener)} must be called
     * in order to receive streaming sensor samples. Every time a set of samples arrive from the
     * sensor over the BLE connection, the specified listener will be called on the main thread.
     *
     * @param type              type of sensor samples to receive
     * @param streamingListener listener to receive sensor samples
     */
    void addStreamingListener(StreamingType type, StreamingListener streamingListener);

    /**
     * Remove a listener to stop receiving streaming sensor samples
     * <p/>
     * Provide a listener object that had been previously passed to {@link
     * #addStreamingListener(StreamingType, StreamingListener)}. That listener will no longer be
     * called. Any other listeners that had been added for the specified type of samples will
     * continue to receive samples.
     *
     * @param streamingListener listener to remove
     */
    void removeStreamingListener(StreamingListener streamingListener);

    /**
     * Get a list of recordings in the sensor's flash memory.
     * <p/>
     * Every time {@link #startSensing(SensorConfig, int, byte[], Listener)} is called with
     * recording enabled in the {@link SensorConfig}, a new recording is created which will appear
     * in this list. If a recording is currently in progress, that recording will appear at the end
     * of the list.
     * <p/>
     * The {@link RecordingInfo} may be passed to {@link #downloadRecording(RecordingInfo, Listener,
     * ProgressListener)} to download a specific recording.
     *
     * @param listener listener to receive list of recordings
     */
    void getRecordingList(Listener<List<RecordingInfo>> listener);

    /**
     * Clear all recordings in the sensor's flash memory.
     * <p/>
     * A recording must not be in progress in order to clear all recordings. If a recording is in
     * progress, call {@link #stopSensing(Listener)} to stop it before clearing recordings.
     *
     * @param listener
     */
    void clearAllRecordings(Listener<Void> listener);

    /**
     * Download a recording from the sensor's flash memory.
     * <p/>
     * Specify one of the recordings that was returned from {@link #getRecordingList(Listener)}. The
     * recording will be downloaded into the SDK's recording database {@link
     * com.mc10inc.biostamp3.sdk.db.BioStampDb} within the application's local storage and the
     * contents can be accessed through the database once the download is complete.
     * <p/>
     * To stop the download before it completes, call {@link #cancelTask()}. If a download is
     * interrupted for any reason (loss of connection, etc) the partial download will remain in the
     * database, and will automatically be resumed when this method is called again.
     * <p/>
     * If the specified recording is not found in the sensor's flash memory, {@link
     * com.mc10inc.biostamp3.sdk.exception.SensorRecordingNotFoundException} is returned to the
     * listener.
     *
     * @param recording        Recording info as received from {@link #getRecordingList(Listener)}
     * @param listener         Listener to be called on completion or failure of the download
     * @param progressListener Listener to be called periodically as the download proceeds
     */
    void downloadRecording(RecordingInfo recording, Listener<Void> listener,
                           ProgressListener progressListener);

    /**
     * Upload a firmware image to the sensor.
     * <p/>
     * This is the first step in performing an over the air firmware update. It has no effect on the
     * running firmware, but only stores the firmware image in the sensor's flash memory where it
     * can then be loaded by calling {@link #loadFirmwareImage(Listener)}.
     * <p/>
     * There is no risk of leaving the sensor in a non-functional state if anything goes wrong while
     * this command is executing.
     *
     * @param file             Firmware image file to upload
     * @param listener         Listener to be called on completion or failure of the upload
     * @param progressListener Listener to be called periodically as the upload proceeds
     */
    void uploadFirmware(byte[] file, Listener<Void> listener,
                           ProgressListener progressListener);

    /**
     * Load a firmware image.
     * <p/>
     * This is the second step in performing an over the air firmware update, to be called after the
     * firmware image has been uploaded by calling {@link #uploadFirmware(byte[], Listener,
     * ProgressListener)}. The sensor checks that the uploaded image is valid, not corrupt, and
     * compatible, and then sets a flag indicating that the sensor's bootloader should load the
     * firmware on the next reset. The next step after this completes successfully is to {@link
     * #reset(Listener)} the sensor.
     *
     * @param listener
     */
    void loadFirmwareImage(Listener<Void> listener);

    /**
     * Reset the sensor.
     * <p/>
     * After the sensor acknowledges this command (causing the specified listener to be called), it
     * immediately disconnects the BLE connection and resets.
     * <p/>
     * If a firmware image had been loaded by calling {@link #loadFirmwareImage(Listener)}, then the
     * firmware update will be performed and when the sensor reappears in range after a few seconds
     * it will be running the new firmware. After reconnecting to the sensor, call {@link
     * #getSensorStatus(Listener)} to verify that the firmware version has been updated as
     * expected.
     *
     * @param listener
     */
    void reset(Listener<Void> listener);

    /**
     * Power off the sensor.
     * <p/>
     * After the sensor acknowledges this command (causing the specified listener to be called), it
     * immediately disconnects the BLE connection and powers off. It will remain powered off until
     * it is placed on a charger.
     *
     * @param listener
     */
    void powerOff(Listener<Void> listener);

    void annotate(byte[] annotationData, Listener<Double> listener);

    int getAnnotationDataMaxSize();

    int getRecordingMetadataMaxSize();

    void clearFaultLogs(Listener<Void> listener);

    void getFaultLogs(Listener<List<String>> listener);

    /** State of the connection to this sensor. */
    enum State {
        /**
         * Disconnected.
         * <p/>
         * {@link #connect(ConnectListener)} may be called to request a connection.
         */
        DISCONNECTED,
        /**
         * Connection attempt is in progress.
         * <p/>
         * Either {@link ConnectListener#connected()} or {@link ConnectListener#disconnected()} will
         * be called depending on whether the attempt succeeds or fails.
         */
        CONNECTING,
        /**
         * Connected.
         * <p/>
         * When this connection disconnects, either because disconnection is requested or the
         * connection is lost, {@link ConnectListener#disconnected()} will be called.
         */
        CONNECTED
    }

    /**
     * Listener to handle connection events.
     *
     * See {@link #connect(ConnectListener)} for details.
     */
    interface ConnectListener {
        void connected();
        void connectFailed();
        void disconnected();
    }

    /** Listener to handle completion of a sensor task. */
    interface Listener<T> {
        /**
         * Method to be called when a sensor task completes asynchronously.
         * <p/>
         * This is always called from the main thread, so it is safe to access the Android UI from
         * within this method.
         * <p/>
         * If any error occurs executing the task (for example lost connection or an error returned
         * by the sensor firmware) then a Throwable describing the error is provided in error. If
         * error is null, this indicates that the task completed successfully.
         *
         * @param error  null if task was successful, or a Throwable describing the error if it
         *               failed
         * @param result result of task if task was successful and this task provides a result
         */
        void done(Throwable error, T result);
    }

    interface ProgressListener {
        void updateProgress(double progress);
    }
}

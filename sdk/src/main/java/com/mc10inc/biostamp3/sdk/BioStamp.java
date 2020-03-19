package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.sensing.StreamingListener;
import com.mc10inc.biostamp3.sdk.sensing.StreamingType;

import java.util.List;

/**
 * BioStamp object representing a single BioStamp sensor
 *
 * The BioStamp object for a specific sensor, as identified by the sensor serial number, is obtained
 * through the BioStampManger getBioStamp method.
 */
public interface BioStamp {
    /**
     * Request a connection to the sensor
     *
     * There must not be a connection already established or in progress. It is a fatal error to
     * call this method if getState does not return DISCONNECTED.
     *
     * @param connectListener listener to receive connect and disconnect callbacks
     */
    void connect(ConnectListener connectListener);

    void disconnect();

    String getSerial();

    State getState();

    void cancelTask();

    void blinkLed(Listener<Void> listener);

    <TC, TR> void execute(Request<TC, TR> request, TC param, Listener<TR> listener);

    void startSensing(SensorConfig sensorConfig, int maxDuration, byte[] metadata,
                      Listener<Void> listener);

    void stopSensing(Listener<Void> listener);

    void getSensingInfo(Listener<SensingInfo> listener);

    void getSensorStatus(Listener<SensorStatus> listener);

    void startStreaming(StreamingType type, Listener<Void> listener);

    void stopStreaming(StreamingType type, Listener<Void> listener);

    void addStreamingListener(StreamingType type, StreamingListener streamingListener);

    void removeStreamingListener(StreamingListener streamingListener);

    void getRecordingList(Listener<List<RecordingInfo>> listener);

    void clearAllRecordings(Listener<Void> listener);

    void downloadRecording(RecordingInfo recording, Listener<Void> listener,
                           ProgressListener progressListener);

    void uploadFirmware(byte[] file, Listener<Void> listener,
                           ProgressListener progressListener);

    void loadFirmwareImage(Listener<Void> listener);

    void reset(Listener<Void> listener);

    void powerOff(Listener<Void> listener);

    void annotate(byte[] annotationData, Listener<Double> listener);

    int getAnnotationDataMaxSize();

    int getRecordingMetadataMaxSize();

    void clearFaultLogs(Listener<Void> listener);

    void getFaultLogs(Listener<List<String>> listener);

    enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    /**
     * Listener to handle connection events
     */
    interface ConnectListener {
        void connected();
        void connectFailed();
        void disconnected();
    }

    interface Listener<T> {
        void done(Throwable error, T result);
    }

    interface ProgressListener {
        void updateProgress(double progress);
    }
}

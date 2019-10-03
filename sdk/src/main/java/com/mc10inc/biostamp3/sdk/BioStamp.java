package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.sensing.StreamingListener;
import com.mc10inc.biostamp3.sdk.sensing.StreamingType;

import java.util.List;

public interface BioStamp {
    void connect(ConnectListener connectListener);

    void disconnect();

    String getSerial();

    State getState();

    void cancelTask();

    void blinkLed(Listener<Void> listener);

    <TC, TR> void execute(Request<TC, TR> request, TC param, Listener<TR> listener);

    void startSensing(SensorConfig sensorConfig, Listener<Void> listener);

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

    enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

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

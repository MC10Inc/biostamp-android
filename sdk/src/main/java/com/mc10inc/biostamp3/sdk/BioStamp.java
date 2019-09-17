package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;

public interface BioStamp {
    void connect(ConnectListener connectListener);

    void disconnect();

    void blinkLed(Listener<Void> listener);

    <TC, TR> void execute(Request<TC, TR> request, TC param, Listener<TR> listener);

    void startSensing(SensorConfig sensorConfig, Listener<Void> listener);

    void stopSensing(Listener<Void> listener);

    void getSensingInfo(Listener<SensingInfo> listener);

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

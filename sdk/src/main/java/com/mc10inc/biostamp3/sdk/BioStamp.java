package com.mc10inc.biostamp3.sdk;

public interface BioStamp {
    void connect(ConnectListener connectListener);

    void disconnect();

    void blinkLed(Listener<Void> listener);

    <TC, TR> void execute(Request<TC, TR> request, TC param, Listener<TR> listener);

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

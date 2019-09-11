package com.mc10inc.biostamp3.sdk;

public interface BioStamp {
    void connect(ConnectListener connectListener);

    void disconnect();

    void test();

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

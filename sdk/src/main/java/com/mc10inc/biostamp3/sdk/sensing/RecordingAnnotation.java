package com.mc10inc.biostamp3.sdk.sensing;

public class RecordingAnnotation {
    private double timestamp;
    private byte[] data;

    public RecordingAnnotation(double timestamp, byte[] data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    public double getTimestamp() {
        return timestamp;
    }

    public byte[] getData() {
        return data;
    }
}

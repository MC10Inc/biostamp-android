package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.BioStamp;

/**
 * Represents an annotation within a recording.
 */
public class RecordingAnnotation {
    private double timestamp;
    private byte[] data;

    RecordingAnnotation(double timestamp, byte[] data) {
        this.timestamp = timestamp;
        this.data = data;
    }

    /**
     * Get the annotation's timestamp in seconds.
     * <p/>
     * This is the exact same value that would have been returned by {@link
     * com.mc10inc.biostamp3.sdk.BioStamp#annotate(byte[], BioStamp.Listener)} when the annotation
     * was created.
     *
     * @return Annotation's timestamp in seconds
     */
    public double getTimestamp() {
        return timestamp;
    }

    /**
     * Get the annotation's data.
     * <p/>
     * This is the value that was passed to {@link BioStamp#annotate(byte[], BioStamp.Listener)}
     * when the annotation was created.
     *
     * @return Annotation data
     */
    public byte[] getData() {
        return data;
    }
}

package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.BioStampDb;
import com.mc10inc.biostamp3.sdk.BioStampDbImpl;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.EnumMap;
import java.util.Map;

/**
 * Recording decoder for accessing downloaded recordings stored in the database.
 * <p/>
 * Once a recording has been downloaded from a sensor to the recording database within the
 * application's local storage, this class may be used to access the contents of that recording.
 * <p/>
 * To use, first construct the class supplying the {@link RecordingInfo} identifying the recording.
 * Next, call {@link #setListener(RawSamplesType, Listener)} and {@link
 * #setAnnotationListener(AnnotationListener)} for the types of data that are needed from the
 * recording. Finally, call {@link #decode()}. The listeners will be called repeatedly with the
 * decoded contents of the recording on the same thread that {@link #decode()} was called from. When
 * {@link #decode()} returns, decoding is complete.
 */
public class RecordingDecoder {
    /**
     * Type of raw samples that can be decoded.
     */
    public enum RawSamplesType {
        /** AD5940 bio-impedance samples */
        AD5940,
        /** AFE4900 PPG or biopotential samples */
        AFE4900,
        /** Temperature and pressure samples */
        ENVIRONMENT,
        /** ICM-20948 accelerometer / gyroscope samples */
        MOTION
    }

    /**
     * Listener to receive annotations.
     */
    public interface AnnotationListener {
        /**
         * Method to be called each time the decoder decodes an annotation.
         *
         * @param annotation Decoded annotation
         */
        void handleAnnotation(RecordingAnnotation annotation);
    }

    /**
     * Listener to receive a specific type of sensor samples.
     */
    public interface Listener {
        /**
         * Method to be called each time the decoder decodes a group of samples.
         *
         * @param samples Decoded samples
         */
        void handleRawSamples(RawSamples samples);
    }

    private RecordingInfo recordingInfo;
    private AnnotationListener annotationListener;
    private Map<RawSamplesType, Listener> listeners = new EnumMap<>(RawSamplesType.class);

    /**
     * Initialize a recording decoder to decode a specific recording in the database.
     *
     * @param recordingInfo Recording to decode, as obtained from {@link BioStampDb#getRecordings()}
     */
    public RecordingDecoder(RecordingInfo recordingInfo) {
        this.recordingInfo = recordingInfo;
    }

    /**
     * Register the listener to receive annotations
     * <p/>
     * Only one listener may be registered.
     *
     * @param annotationListener Listener to receive decoded annotations
     */
    public void setAnnotationListener(AnnotationListener annotationListener) {
        this.annotationListener = annotationListener;
    }

    /**
     * Register a listener to receive a specific type of sensor samples.
     * <p/>
     * Only one listener may be registered for each type of sensor samples.
     *
     * @param type Type of sensor samples to receive
     * @param listener Listener to receive sensor samples
     */
    public void setListener(RawSamplesType type, Listener listener) {
        listeners.put(type, listener);
    }

    /**
     * Decode the recording.
     * <p>
     * The decoder will iterate through the entire recording, calling all listeners that were
     * registered with {@link #setListener(RawSamplesType, Listener)} and {@link
     * #setAnnotationListener(AnnotationListener)} repeatedly with the decoded data. The listeners
     * are called on the thread that this method is called from. Once this method returns, decoding
     * is complete.
     */
    public void decode() {
        RawSampleInfo rawSampleInfo = new RawSampleInfo(recordingInfo.getMsg().getRawDataInfo());
        BioStampDbImpl db = BioStampManager.getInstance().getDbImpl();
        try (BioStampDbImpl.RecordingPagesLoader pages = db.getRecordingPages(recordingInfo)) {
            Brc3.RecordingPage page;
            while ((page = pages.getNext()) != null) {
                if (page.hasAd5940()) {
                    RawSamples samples = new AD5940Samples(
                            page.getTimestamp(), page.getSamplingPeriod(),
                            rawSampleInfo, page.getAd5940());
                    Listener listener = listeners.get(RawSamplesType.AD5940);
                    if (listener != null) {
                        listener.handleRawSamples(samples);
                    }
                } else if (page.hasAfe4900()) {
                    RawSamples samples = new AFE4900Samples(
                            page.getTimestamp(), page.getSamplingPeriod(),
                            rawSampleInfo, page.getAfe4900());
                    Listener listener = listeners.get(RawSamplesType.AFE4900);
                    if (listener != null) {
                        listener.handleRawSamples(samples);
                    }
                } else if (page.hasEnvironment()) {
                    RawSamples samples = new EnvironmentSamples(
                            page.getTimestamp(), page.getSamplingPeriod(),
                            rawSampleInfo, page.getEnvironment());
                    Listener listener = listeners.get(RawSamplesType.ENVIRONMENT);
                    if (listener != null) {
                        listener.handleRawSamples(samples);
                    }
                } else if (page.hasMotion()) {
                    RawSamples samples = new MotionSamples(
                            page.getTimestamp(), page.getSamplingPeriod(),
                            rawSampleInfo, page.getMotion());
                    Listener listener = listeners.get(RawSamplesType.MOTION);
                    if (listener != null) {
                        listener.handleRawSamples(samples);
                    }
                } else if (page.hasAnnotation()) {
                    if (annotationListener != null) {
                        double ts = page.getTimestamp() * rawSampleInfo.getTimestampScale();
                        byte[] data = page.getAnnotation().getAnnotationData().toByteArray();
                        annotationListener.handleAnnotation(new RecordingAnnotation(ts, data));
                    }
                }
            }
        }
    }
}

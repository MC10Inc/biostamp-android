package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.db.BioStampDb;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.EnumMap;
import java.util.Map;

public class RecordingDecoder {
    public enum RawSamplesType {
        AD5940,
        AFE4900,
        ENVIRONMENT,
        MOTION
    }

    public interface AnnotationListener {
        void handleAnnotation(RecordingAnnotation annotation);
    }

    public interface Listener {
        void handleRawSamples(RawSamples samples);
    }

    private RecordingInfo recordingInfo;
    private AnnotationListener annotationListener;
    private Map<RawSamplesType, Listener> listeners = new EnumMap<>(RawSamplesType.class);

    public RecordingDecoder(RecordingInfo recordingInfo) {
        this.recordingInfo = recordingInfo;
    }

    public void setAnnotationListener(AnnotationListener annotationListener) {
        this.annotationListener = annotationListener;
    }

    public void setListener(RawSamplesType type, Listener listener) {
        listeners.put(type, listener);
    }

    public void decode() {
        RawSampleInfo rawSampleInfo = new RawSampleInfo(recordingInfo.getMsg().getRawDataInfo());
        BioStampDb db = BioStampManager.getInstance().getDb();
        try (BioStampDb.RecordingPagesLoader pages = db.getRecordingPages(recordingInfo)) {
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

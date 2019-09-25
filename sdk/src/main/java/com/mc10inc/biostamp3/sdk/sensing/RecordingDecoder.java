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
        MOTION
    }

    public interface Listener {
        void handleRawSamples(RawSamples samples);
    }

    private RecordingInfo recordingInfo;
    private Map<RawSamplesType, Listener> listeners = new EnumMap<>(RawSamplesType.class);

    public RecordingDecoder(RecordingInfo recordingInfo) {
        this.recordingInfo = recordingInfo;
    }

    public void setListener(RawSamplesType type, Listener listener) {
        listeners.put(type, listener);
    }

    public void decode() {
        RawSampleInfo rawSampleInfo = new RawSampleInfo(recordingInfo.getMsg().getRawDataInfo());
        double tsScale = recordingInfo.getMsg().getRawDataInfo().getTimestampScale();
        BioStampDb db = BioStampManager.getInstance().getDb();
        try (BioStampDb.RecordingPagesLoader pages = db.getRecordingPages(recordingInfo)) {
            Brc3.RecordingPage page;
            while ((page = pages.getNext()) != null) {
                double ts = page.getTimestamp() * tsScale;

                if (page.hasAd5940()) {
                    RawSamples samples = new AD5940Samples(ts, rawSampleInfo, page.getAd5940());
                    Listener listener = listeners.get(RawSamplesType.AD5940);
                    if (listener != null) {
                        listener.handleRawSamples(samples);
                    }
                } else if (page.hasAfe4900()) {
                    RawSamples samples = new AFE4900Samples(ts, rawSampleInfo, page.getAfe4900());
                    Listener listener = listeners.get(RawSamplesType.AFE4900);
                    if (listener != null) {
                        listener.handleRawSamples(samples);
                    }
                } else if (page.hasMotion()) {
                    RawSamples samples = new MotionSamples(ts, rawSampleInfo, page.getMotion());
                    Listener listener = listeners.get(RawSamplesType.MOTION);
                    if (listener != null) {
                        listener.handleRawSamples(samples);
                    }
                }
            }
        }
    }
}

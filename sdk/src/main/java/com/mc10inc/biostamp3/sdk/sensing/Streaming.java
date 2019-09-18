package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import timber.log.Timber;

public class Streaming {
    private final Map<StreamingType, Long> previousTimestamps = new EnumMap<>(StreamingType.class);
    private final Map<StreamingType, Brc3.StreamingInfo> streamingInfos = new EnumMap<>(StreamingType.class);
    private final Map<StreamingType, Set<StreamingListener>> listeners = new EnumMap<>(StreamingType.class);

    public Streaming() {
        for (StreamingType type : StreamingType.values()) {
            listeners.put(type, new CopyOnWriteArraySet<>());
        }
    }

    public void handleStreamingSamples(Brc3.StreamingSamples samples) {
        StreamingType type;
        int numSamples = 0;
        if (samples.hasAd5940()) {
            type = StreamingType.AD5940;
        } else if (samples.hasAfe4900()) {
            type = StreamingType.AFE4900;
        } else if (samples.hasEnvironment()) {
            type = StreamingType.ENVIRONMENT;
        } else if (samples.hasMotion()) {
            type = StreamingType.MOTION;
            numSamples = MotionSamples.getSize(samples.getMotion());
        } else if (samples.hasRotation()) {
            type = StreamingType.ROTATION;
        } else {
            Timber.e("Invalid streaming samples %s", samples.toString());
            return;
        }

        Brc3.StreamingInfo streamingInfo = streamingInfos.get(type);
        if (streamingInfo == null) {
            Timber.e("Missing streaming info for type %s", type);
            return;
        }

        Long previousTimestamp = previousTimestamps.get(type);
        if (previousTimestamp == null) {
            previousTimestamps.put(type, samples.getTimestampUs());
            return;
        }
        previousTimestamps.put(type, samples.getTimestampUs());

        RawSampleInfo rsi = new RawSampleInfo(streamingInfo, previousTimestamp,
                samples.getTimestampUs(), numSamples);
        RawSamples rawSamples;
        switch (type) {
            case AD5940:
                rawSamples = new AD5940Samples(samples.getTimestampUs(), rsi, samples.getAd5940());
                break;
            case AFE4900:
                rawSamples = new AFE4900Samples(samples.getTimestampUs(), rsi, samples.getAfe4900());
                break;
            case ENVIRONMENT:
                rawSamples = new EnvironmentSamples(samples.getTimestampUs(), rsi,
                        samples.getEnvironment());
                break;
            case MOTION:
                rawSamples = new MotionSamples(samples.getTimestampUs(), rsi, samples.getMotion());
                break;
            case ROTATION:
                rawSamples = new RotationSamples(samples.getTimestampUs(), rsi,
                        samples.getRotation());
                break;
            default:
                return;
        }

        Set<StreamingListener> listenersToCall = Objects.requireNonNull(listeners.get(type));
        for (StreamingListener l : listenersToCall) {
            l.handleRawSamples(rawSamples);
        }
    }

    public void setStreamingInfo(StreamingType type, Brc3.StreamingInfo info) {
        streamingInfos.put(type, info);
    }

    public void addStreamingListener(StreamingType type, StreamingListener streamingListener) {
        Objects.requireNonNull(listeners.get(type)).add(streamingListener);
    }

    public void removeStreamingListener(StreamingType type, StreamingListener streamingListener) {
        Objects.requireNonNull(listeners.get(type)).remove(streamingListener);
    }
}

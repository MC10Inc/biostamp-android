package com.mc10inc.biostamp3.sdk.sensing;

public interface StreamingListener {
    boolean handleRawSamples(RawSamples samples);
}

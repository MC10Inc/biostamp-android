package com.mc10inc.biostamp3.sdk.sensing;

/**
 * Listener to receive a specific type of sensor samples.
 */
public interface StreamingListener {
    /**
     * Method to be called each time samples are received through streaming over BLE.
     * <p/>
     * This is always called from the main thread, so it is safe to access the Android UI from
     * within this method.
     *
     * @param samples Received samples
     */
    boolean handleRawSamples(RawSamples samples);
}

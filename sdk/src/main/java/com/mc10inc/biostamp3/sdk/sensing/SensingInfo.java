package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * Info about the current sensing operation.
 */
public class SensingInfo {
    private Brc3.SensingGetInfoResponseParam msg;
    private SensorConfig sensorConfig;

    public SensingInfo(Brc3.SensingGetInfoResponseParam msg) {
        this.msg = msg;
        if (msg.getEnabled()) {
            sensorConfig = new SensorConfig(msg.getSensorConfig());
        }
    }

    /**
     * Is sensing enabled?
     *
     * @return true if sensing is enabled
     */
    public boolean isEnabled() {
        return msg.getEnabled();
    }

    /**
     * Get current sensor configuration
     *
     * @return sensor configuration or null if sensing is not enabled
     */
    public SensorConfig getSensorConfig() {
        return sensorConfig;
    }

    /**
     * Get start time
     *
     * @return the time when sensing was enabled as a Unix timestamp in seconds
     */
    public int getStartTimestamp() {
        return msg.getTimestampStart();
    }

    private static String formatStartTimestamp(long tsSec) {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(new Date(tsSec * 1000));
    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        if (isEnabled()) {
            s.append("Started ").append(formatStartTimestamp(getStartTimestamp())).append("\n");
            s.append(getSensorConfig().toString());
        } else {
            s.append("Idle");
        }
        return s.toString();
    }
}

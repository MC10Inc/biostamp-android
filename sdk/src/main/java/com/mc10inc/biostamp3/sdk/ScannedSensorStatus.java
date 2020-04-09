package com.mc10inc.biostamp3.sdk;

import android.bluetooth.le.ScanRecord;

import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.mc10inc.biostamp3.sdk.ble.StatusBroadcast;

/**
 * Info about a sensor in range.
 * <p/>
 * Describes a sensor which was detected through BLE scanning.
 */
public class ScannedSensorStatus {
    private String serial;
    private StatusBroadcast statusBroadcast;

    ScannedSensorStatus(GattConnection conn) {
        serial = conn.getDevice().getName();
        ScanRecord scanRecord = conn.getDevice().getScanRecord();
        if (scanRecord != null) {
            statusBroadcast = new StatusBroadcast(scanRecord);
        }
    }

    /**
     * Get the sensor serial number.
     *
     *  @return Sensor serial number
     */
    public String getSerial() {
        return serial;
    }

    /**
     * Get the status broadcast.
     * <p/>
     * The status broadcast is a small message that the sensor sends within the BLE advertisement
     * which describes the state of the sensor. The return value may be null in the case that the
     * transmission from the sensor is corrupted or the sensor is running a newer firmware version
     * that transmits a message whose format is not recognized by this SDK.
     *
     * @return Status broadcast or null
     */
    public StatusBroadcast getStatusBroadcast() {
        return statusBroadcast;
    }
}

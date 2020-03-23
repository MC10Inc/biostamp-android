package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;

/**
 * Sensor status as returned from {@link BioStamp#getSensorStatus(BioStamp.Listener)}.
 */
public class SensorStatus {
    private final Brc3.SystemStatusResponseParam systemStatus;
    private final SensingInfo sensingInfo;
    private final Brc3.VersionGetResponseParam version;
    private final Brc3.FaultGetInfoResponseParam fault;

    public SensorStatus(Brc3.SystemStatusResponseParam systemStatus, SensingInfo sensingInfo,
                        Brc3.VersionGetResponseParam version, Brc3.FaultGetInfoResponseParam fault) {
        this.systemStatus = systemStatus;
        this.sensingInfo = sensingInfo;
        this.version = version;
        this.fault = fault;
    }

    /**
     * Get info about the current sensing operation.
     *
     * @return Info about the current sensing operation, or null if sensing is not enabled
     */
    public SensingInfo getSensingInfo() {
        return sensingInfo;
    }

    /**
     * Check if the sensor is currently charging.
     * <p/>
     * This is of limited usefulness as in the current hardware design, wireless charging prevents
     * the Bluetooth radio from communicating.
     *
     * @return True if sensor is charging.
     */
    public boolean isCharging() {
        return systemStatus.getChargePower();
    }

    /**
     * Get the battery voltage as a percentage.
     * <p/>
     * This returns the sensor's battery voltage as a percentage of the maximum voltage. The maximum
     * value is 98%. The sensor can be considered fully charged when this reaches 98%, but as the
     * battery drains this value does not scale proportionally with the remaining capacity and
     * should not be used as an indication of how long the sensor can run for.
     *
     * @return Battery percentage up to 98%
     */
    public int getBatteryPercent() {
        return systemStatus.getBatteryPercent();
    }

    /**
     * Get the number of seconds elapsed since the sensor powered on or reset.
     *
     * @return Uptime in seconds
     */
    public int getUptime() {
        return systemStatus.getUptimeSec();
    }

    public int getResetReason() {
        return systemStatus.getResetReason();
    }

    /**
     * Get the firmware version.
     *
     * @return Firmware version string
     */
    public String getFirmwareVersion() {
        return version.getFirmwareVersion();
    }

    /**
     * Get the bootloader version.
     * <p/>
     * Most firmware updates update only the application firmware and leave the bootloader version
     * unchanged. However it is possible for a firmware update to also include an update to the
     * bootloader.
     *
     * @return Bootloader version string
     */
    public String getBootloaderVersion() {
        if (version.getBootloaderVersion() == null) {
            return "";
        } else {
            return version.getBootloaderVersion();
        }
    }

    public String getFault() {
        if (fault.getFaultInfo().getType() == Brc3.FaultType.NO_FAULT) {
            return null;
        } else {
            return fault.getFaultInfo().toString();
        }
    }
}

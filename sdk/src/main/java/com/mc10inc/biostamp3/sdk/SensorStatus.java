package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;

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

    public SensingInfo getSensingInfo() {
        return sensingInfo;
    }

    public boolean isCharging() {
        return systemStatus.getChargePower();
    }

    public int getBatteryPercent() {
        return systemStatus.getBatteryPercent();
    }

    public int getUptime() {
        return systemStatus.getUptimeSec();
    }

    public int getResetReason() {
        return systemStatus.getResetReason();
    }

    public String getFirmwareVersion() {
        return version.getFirmwareVersion();
    }

    public String getBootloaderVersion() {
        if (version.getBootloaderVersion() == null) {
            return "";
        } else {
            return version.getBootloaderVersion();
        }
    }

    public String getFault() {
        if (fault.getFaultInfo() == null) {
            return null;
        } else {
            return fault.getFaultInfo().toString();
        }
    }
}

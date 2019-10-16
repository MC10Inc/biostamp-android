package com.mc10inc.biostamp3.sdk;

import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;

public class SensorStatus {
    private final Brc3.SystemStatusResponseParam systemStatus;
    private final SensingInfo sensingInfo;
    private final Brc3.VersionGetResponseParam version;

    public SensorStatus(Brc3.SystemStatusResponseParam systemStatus, SensingInfo sensingInfo,
                        Brc3.VersionGetResponseParam version) {
        this.systemStatus = systemStatus;
        this.sensingInfo = sensingInfo;
        this.version = version;
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

    public String getFirmwareVersion() {
        return version.getFirmwareVersion();
    }
}

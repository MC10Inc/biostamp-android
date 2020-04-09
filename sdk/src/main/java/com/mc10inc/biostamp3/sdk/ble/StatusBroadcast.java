package com.mc10inc.biostamp3.sdk.ble;

import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;

public class StatusBroadcast {
    private static final int MANUFACTURER_ID_MC10 = 0x00ca;
    private static final int MSD_ID_BRC3 = 0x30;
    private static final int FILTER_SIZE = 1;

    public static List<ScanFilter> getScanFilters() {
        ScanFilter.Builder sf = new ScanFilter.Builder();
        byte[] msd = new byte[FILTER_SIZE];
        byte[] mask = new byte[FILTER_SIZE];
        msd[0] = (byte)MSD_ID_BRC3;
        mask[0] = (byte)0xff;
        sf.setManufacturerData(MANUFACTURER_ID_MC10, msd, mask);
        return Collections.singletonList(sf.build());
    }

    private boolean valid;
    private BitSet bits;
    private int index;

    private int batteryPercent;
    private boolean fullyCharged;
    private boolean charging;
    private boolean sensingEnabled;
    private boolean recordingEnabled;
    private int freeSpace;
    private boolean recordingsEmpty;
    private boolean hardwareFault;
    private boolean faultLogged;

    public StatusBroadcast(ScanRecord scanRecord) {
        byte[] msd = scanRecord.getManufacturerSpecificData(MANUFACTURER_ID_MC10);
        if (msd == null) {
            valid = false;
        } else if (msd[0] != MSD_ID_BRC3) {
            valid = false;
        } else {
            bits = BitSet.valueOf(Arrays.copyOfRange(msd, 1, msd.length));
            index = 0;
            if (bits.size() < 4) {
                valid = false;
            } else {
                int version = getInt(4);
                if (version == 0) {
                    try {
                        decode();
                    } catch (IndexOutOfBoundsException e) {
                        valid = false;
                    }
                } else {
                    valid = false;
                }
            }
        }
    }

    private int getInt(int nbits) {
        if (index + nbits > bits.size()) {
            throw new IndexOutOfBoundsException();
        }
        int value = 0;
        for (int i = 0; i < nbits; i++) {
            if (bits.get(index + i)) {
                value += (1 << i);
            }
        }
        index += nbits;
        return value;
    }

    private boolean getBoolean() {
        if (index >= bits.size()) {
            throw new IndexOutOfBoundsException();
        }
        boolean value = bits.get(index);
        index++;
        return value;
    }

    private void decode() {
        batteryPercent = getInt(7);
        fullyCharged = getBoolean();
        charging = getBoolean();
        sensingEnabled = getBoolean();
        recordingEnabled = getBoolean();
        freeSpace = getInt(12);
        recordingsEmpty = getBoolean();
        hardwareFault = getBoolean();
        faultLogged = getBoolean();
        valid = true;
    }

    public boolean isValid() {
        return valid;
    }

    public int getBatteryPercent() {
        return batteryPercent;
    }

    public boolean isFullyCharged() {
        return fullyCharged;
    }

    public boolean isCharging() {
        return charging;
    }

    public boolean isSensingEnabled() {
        return sensingEnabled;
    }

    public boolean isRecordingEnabled() {
        return recordingEnabled;
    }

    public int getFreeSpace() {
        return freeSpace;
    }

    public boolean isRecordingsEmpty() {
        return recordingsEmpty;
    }

    public boolean isHardwareFault() {
        return hardwareFault;
    }

    public boolean isFaultLogged() {
        return faultLogged;
    }
}

package com.mc10inc.biostamp3.sdk.ble;

import android.bluetooth.le.ScanFilter;

import java.util.Collections;
import java.util.List;

public class StatusBroadcast {
    private static final int MANUFACTURER_ID_MC10 = 0x00ca;
    private static final int MSD_ID_BRC3 = 0x30;
    private static final int SIZE = 1;

    public static List<ScanFilter> getScanFilters() {
        ScanFilter.Builder sf = new ScanFilter.Builder();
        byte[] msd = new byte[SIZE];
        byte[] mask = new byte[SIZE];
        msd[0] = (byte)MSD_ID_BRC3;
        mask[0] = (byte)0xff;
        sf.setManufacturerData(MANUFACTURER_ID_MC10, msd, mask);
        return Collections.singletonList(sf.build());
    }
}

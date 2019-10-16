package com.mc10inc.biostamp3.sdk.task;

import android.os.SystemClock;

import com.google.protobuf.ByteString;
import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampImpl;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.Request;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.exception.BleException;
import com.mc10inc.biostamp3.sdk.util.CRC16;

import timber.log.Timber;

public class UploadFirmware extends Task<Void> {
    private static final int PAGE_SIZE = 256;

    private byte[] file;

    public UploadFirmware(BioStampImpl bs, BioStamp.Listener<Void> taskListener,
                          BioStamp.ProgressListener progressListener, byte[] file) {
        super(bs, taskListener, progressListener);
        this.file = file;
    }

    @Override
    public void doTask() {
        try {
            bs.getBle().requestConnectionSpeed(SensorBle.Speed.HIGH);
            progress(0);
            long t0 = SystemClock.elapsedRealtime();
            Request.uploadStart.execute(bs.getBle(), Brc3.UploadStartCommandParam.newBuilder()
                    .setType(Brc3.UploadType.FIRMWARE_IMAGE)
                    .setSize(file.length)
                    .setCrc(CRC16.calculateCrc(file)));

            for (int addr = 0; addr < file.length; addr += PAGE_SIZE) {
                int n;
                if (addr + PAGE_SIZE <= file.length) {
                    n = PAGE_SIZE;
                } else {
                    n = file.length - addr;
                }
                Request.uploadWritePage.execute(bs.getBle(), Brc3.UploadWritePageCommandParam.newBuilder()
                        .setData(ByteString.copyFrom(file, addr, n))
                        .setOffset(addr));
                progress((double)addr / file.length);
            }

            Request.uploadFinish.execute(bs.getBle());

            progress(1);
            long t1 = SystemClock.elapsedRealtime();
            Timber.i("Uploaded %d bytes in %ds, %d bytes/sec",
                    file.length,
                    (t1 - t0) / 1000,
                    file.length * 1000 / (t1 - t0));
            success(null);
        } catch (BleException e) {
            error(e);
        }
    }
}

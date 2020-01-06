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
        // Pad file with zeros so that length is an integral number of flash pages
        int paddedLen = ((file.length + PAGE_SIZE - 1) / PAGE_SIZE) * PAGE_SIZE;
        this.file = new byte[paddedLen];
        System.arraycopy(file, 0, this.file, 0, file.length);
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

            bs.getBle().setWriteFastProgressListener(this::progress);
            Request.uploadWritePagesFast.execute(bs.getBle(), null, file);

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

package com.mc10inc.biostamp3.sdk.task;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampDbImpl;
import com.mc10inc.biostamp3.sdk.BioStampImpl;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.Request;
import com.mc10inc.biostamp3.sdk.exception.BleException;
import com.mc10inc.biostamp3.sdk.recording.DownloadStatus;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class DownloadRecording extends Task<Void> {
    private RecordingInfo recordingInfo;
    private List<Brc3.RecordingPage> recordingPages;

    public DownloadRecording(BioStampImpl bs, BioStamp.Listener<Void> taskListener,
                             BioStamp.ProgressListener progressListener,
                             RecordingInfo recordingInfo) {
        super(bs, taskListener, progressListener);
        this.recordingInfo = recordingInfo;
    }

    @Override
    public void doTask() {
        try {
            progress(0);
            BioStampDbImpl db = BioStampManager.getInstance().getDbImpl();
            DownloadStatus downloadStatus = db.getRecordingDownloadStatus(recordingInfo);
            if (downloadStatus != null && downloadStatus.isComplete()) {
                Timber.i("Download is already complete");
                progress(1);
                success(null);
                return;
            }
            int pageNum;
            if (downloadStatus == null) {
                db.insertRecording(recordingInfo);
                pageNum = 0;
            } else {
                pageNum = downloadStatus.getDownloadedPages();
            }
            while (!canceled && pageNum < recordingInfo.getNumPages()) {
                CountDownLatch latch = new CountDownLatch(1);
                bs.setRecordingPagesListener(p -> {
                    recordingPages = p;
                    latch.countDown();
                });
                Request.readRecording.execute(bs.getBle(), Brc3.RecordingReadCommandParam.newBuilder()
                        .setFirstPage(pageNum)
                        .setRecordingId(recordingInfo.getRecordingId()));
                try {
                    boolean gotPages = latch.await(5, TimeUnit.SECONDS);
                    if (!gotPages) {
                        throw new BleException("Timeout waiting for recording page data");
                    }
                } catch (InterruptedException e) {
                    throw new BleException("Timeout waiting for recording page data");
                }
                Timber.i("Received %d pages", recordingPages.size());
                BioStampManager.getInstance().dbExecute(() ->
                        db.insertRecordingPages(recordingInfo, recordingPages));
                pageNum = recordingPages.get(recordingPages.size() - 1).getPageNumber() + 1;
                progress((double)pageNum / recordingInfo.getNumPages());
            }
            progress(1);
            success(null);
        } catch (BleException e) {
            error(e);
        }
    }
}

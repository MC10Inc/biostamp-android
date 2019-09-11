package com.mc10inc.biostamp3.sdk.task;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampImpl;
import com.mc10inc.biostamp3.sdk.BleException;

public abstract class Task<T> {
    protected BioStampImpl bs;
    private BioStamp.Listener<T> taskListener;
    private BioStamp.ProgressListener progressListener;

    protected Task(BioStampImpl bs, BioStamp.Listener<T> taskListener) {
        this(bs, taskListener, null);
    }

    protected Task(BioStampImpl bs, BioStamp.Listener<T> taskListener,
                   BioStamp.ProgressListener progressListener) {
        this.bs = bs;
        this.taskListener = taskListener;
        this.progressListener = progressListener;
    }

    public abstract void doTask();

    public void disconnected() {
        error(new BleException());
    }

    protected void error(Throwable error) {
        bs.getHandler().post(() -> taskListener.done(error, null));
    }

    protected void success(T result) {
        bs.getHandler().post(() -> taskListener.done(null, result));
    }

    protected void progress(double progress) {
        if (progressListener != null) {
            bs.getHandler().post(() -> progressListener.updateProgress(progress));
        }
    }
}

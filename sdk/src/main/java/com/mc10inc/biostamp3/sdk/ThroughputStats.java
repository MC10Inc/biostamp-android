package com.mc10inc.biostamp3.sdk;

import android.os.SystemClock;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

class ThroughputStats {
    private static final int MS_PER_SEC = 1000;
    private static final int DEFAULT_WINDOW_MS = 2000;

    private final int windowMs;
    private long currentWindow;
    private int totalBytes;
    private MutableLiveData<Integer> throughput = new MutableLiveData<>();

    ThroughputStats() {
        windowMs = DEFAULT_WINDOW_MS;
        currentWindow = SystemClock.elapsedRealtime() / windowMs;
    }

    LiveData<Integer> getThroughput() {
        return throughput;
    }

    void update(int bytes) {
        long now = SystemClock.elapsedRealtime() / windowMs;
        if (now == currentWindow) {
            totalBytes += bytes;
        } else {
            int bytesPerSec = totalBytes * MS_PER_SEC / windowMs;
            Integer lastBytesPerSec = throughput.getValue();
            if (lastBytesPerSec == null || lastBytesPerSec != bytesPerSec) {
                throughput.postValue(bytesPerSec);
            }
            currentWindow = now;
            totalBytes = 0;
        }
    }
}

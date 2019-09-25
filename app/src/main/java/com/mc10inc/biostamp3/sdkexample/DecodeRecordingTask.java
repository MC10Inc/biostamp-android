package com.mc10inc.biostamp3.sdkexample;

import android.os.AsyncTask;

import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdk.sensing.RecordingDecoder;

import timber.log.Timber;

public class DecodeRecordingTask extends AsyncTask<Void, Void, Void> {
    private RecordingInfo recordingInfo;

    public DecodeRecordingTask(RecordingInfo recordingInfo) {
        this.recordingInfo = recordingInfo;
    }

    @Override
    protected Void doInBackground(Void... voids) {
        RecordingDecoder decoder = new RecordingDecoder(recordingInfo);
        decoder.setListener(RecordingDecoder.RawSamplesType.MOTION, samples -> {
            Timber.i("samples %f", samples.getTimestamp(0));
        });
        decoder.decode();
        return null;
    }
}

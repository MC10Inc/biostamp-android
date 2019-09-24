package com.mc10inc.biostamp3.sdkexample;

import android.os.AsyncTask;

import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.db.BioStampDb;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.List;

class GetRecordingsTask extends AsyncTask<Void, Void, List<RecordingInfo>> {
    private ExampleViewModel viewModel;

    public GetRecordingsTask(ExampleViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    protected List<RecordingInfo> doInBackground(Void... voids) {
        BioStampDb db = BioStampManager.getInstance().getDb();
        return db.getRecordings();
    }

    @Override
    protected void onPostExecute(List<RecordingInfo> recordingInfos) {
        viewModel.setLocalRecordingList(recordingInfos);
    }
}

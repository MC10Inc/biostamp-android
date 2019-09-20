package com.mc10inc.biostamp3.sdkexample;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.List;

public class ExampleViewModel extends ViewModel {
    private MutableLiveData<List<RecordingInfo>> recordingList = new MutableLiveData<>();
    private BioStamp sensor;

    LiveData<List<RecordingInfo>> getRecordingList() {
        return recordingList;
    }

    void setRecordingList(List<RecordingInfo> recordingList) {
        this.recordingList.setValue(recordingList);
    }

    public BioStamp getSensor() {
        return sensor;
    }

    public void setSensor(BioStamp sensor) {
        this.sensor = sensor;
    }
}

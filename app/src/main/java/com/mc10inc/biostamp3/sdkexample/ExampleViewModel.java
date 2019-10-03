package com.mc10inc.biostamp3.sdkexample;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.List;

public class ExampleViewModel extends ViewModel {
    private MutableLiveData<Boolean> downloadInProgress = new MutableLiveData<>();
    private MutableLiveData<Double> downloadProgress = new MutableLiveData<>();
    private MutableLiveData<List<RecordingInfo>> recordingList = new MutableLiveData<>();
    private MutableLiveData<List<RecordingInfo>> localRecordingList = new MutableLiveData<>();
    private String selectedSensor;

    LiveData<Boolean> getDownloadInProgress() {
        return downloadInProgress;
    }

    public void setDownloadInProgress(boolean inProgress) {
        downloadInProgress.postValue(inProgress);
    }

    public MutableLiveData<Double> getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(double progress) {
        downloadProgress.postValue(progress);
    }

    LiveData<List<RecordingInfo>> getRecordingList() {
        return recordingList;
    }

    void setRecordingList(List<RecordingInfo> recordingList) {
        this.recordingList.setValue(recordingList);
    }

    LiveData<List<RecordingInfo>> getLocalRecordingList() {
        return localRecordingList;
    }

    void setLocalRecordingList(List<RecordingInfo> localRecordingList) {
        this.localRecordingList.setValue(localRecordingList);
    }

    public BioStamp getSensor() {
        if (selectedSensor == null) {
            return null;
        } else {
            return BioStampManager.getInstance().getBioStamp(selectedSensor);
        }
    }

    public void setSelectedSensor(String serial) {
        this.selectedSensor = serial;
    }
}

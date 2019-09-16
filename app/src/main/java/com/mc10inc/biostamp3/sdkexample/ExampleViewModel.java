package com.mc10inc.biostamp3.sdkexample;

import androidx.lifecycle.ViewModel;

import com.mc10inc.biostamp3.sdk.BioStamp;

public class ExampleViewModel extends ViewModel {
    private BioStamp sensor;

    public BioStamp getSensor() {
        return sensor;
    }

    public void setSensor(BioStamp sensor) {
        this.sensor = sensor;
    }
}

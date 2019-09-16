package com.mc10inc.biostamp3.sdkexample;

import android.app.Application;

import com.mc10inc.biostamp3.sdk.BioStampManager;

import timber.log.Timber;

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        BioStampManager.initialize(this);
    }
}

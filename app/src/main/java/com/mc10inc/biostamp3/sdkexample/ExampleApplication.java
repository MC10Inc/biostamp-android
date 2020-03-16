package com.mc10inc.biostamp3.sdkexample;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.mc10inc.biostamp3.sdk.BioStampManager;

import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

public class ExampleApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());

        // Do not report crashes to Crashlytics if there are any uncommitted changes. This avoids
        // unnecessary reports during development.
        if (BuildConfig.GIT_IS_CLEAN) {
            Fabric.with(this, new Crashlytics());
            Crashlytics.setString("GIT_HASH", BuildConfig.GIT_HASH);
        }

        BioStampManager.initialize(this);
    }
}

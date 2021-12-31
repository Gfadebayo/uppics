package com.exzell.uppics;

import android.app.Application;

import timber.log.Timber;

public class UppicsApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());
    }
}

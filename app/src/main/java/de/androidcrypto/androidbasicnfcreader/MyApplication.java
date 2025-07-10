package de.androidcrypto.androidbasicnfcreader;

import android.app.Application;


public class MyApplication extends Application {
    private static final String TAG = "MyApplication";
    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        CrashHandler.getInstance(this).init();
    }

    public static MyApplication getInstance() {
        return instance;
    }
}
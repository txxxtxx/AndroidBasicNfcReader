package de.androidcrypto.androidbasicnfcreader;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private static CrashHandler instance;
    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());

    private CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static synchronized CrashHandler getInstance(Context context) {
        if (instance == null) {
            instance = new CrashHandler(context);
        }
        return instance;
    }

    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
        if (!handleException(e) && defaultHandler != null) {
            defaultHandler.uncaughtException(t, e);
        } else {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException ex) {
                Log.e(TAG, "Error : ", ex);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    private boolean handleException(Throwable e) {
        if (e == null) {
            return false;
        }

        new Thread(() -> {
            Looper.prepare();
            showCrashDialog(e);
//            saveCrashInfoToFile(e);
            Looper.loop();
        }).start();

        return true;
    }

    private void showCrashDialog(Throwable e) {
        Intent intent = new Intent(context, CrashActivity.class);
        String errorMsg = getStackTrace(e);
        Log.e(TAG, errorMsg);
        intent.putExtra("error_message", errorMsg);
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    private String getStackTrace(Throwable e) {
        StringBuilder result = new StringBuilder();
        result.append(e.toString()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            result.append("\t").append(element.toString()).append("\n");
        }

        Throwable cause = e.getCause();
        if (cause != null) {
            result.append("\nCaused by: ").append(cause.toString()).append("\n");
            for (StackTraceElement element : cause.getStackTrace()) {
                result.append("\t").append(element.toString()).append("\n");
            }
        }

        return result.toString();
    }

    private void saveCrashInfoToFile(Throwable e) {
        String fileName = "crash_" + dateFormat.format(new Date()) + ".txt";
        File dir = new File(Environment.getExternalStorageDirectory(), "CrashReports");

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return;
            }
        }

        try (FileWriter writer = new FileWriter(new File(dir, fileName))) {
            writer.write("Device Info:\n");
            writer.write("Model: " + Build.MODEL + "\n");
            writer.write("Brand: " + Build.BRAND + "\n");
            writer.write("Version: " + Build.VERSION.RELEASE + "\n\n");
            writer.write("Crash Info:\n");
            writer.write(getStackTrace(e));
        } catch (IOException ex) {
            Log.e(TAG, "Error writing crash report", ex);
        }
    }

    private String getCrashInfo(Throwable ex) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);

        StringBuilder crashInfo = new StringBuilder();
//        crashInfo.append("版本: ").append(BuildConfig.VERSION_NAME).append("\n");
        crashInfo.append("设备: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
        crashInfo.append("系统: Android ").append(Build.VERSION.RELEASE).append("\n");
        crashInfo.append("异常: ").append(ex.getClass().getName()).append("\n");
        crashInfo.append("信息: ").append(ex.getMessage()).append("\n\n");
        crashInfo.append("堆栈: ").append(sw.toString());

        return crashInfo.toString();
    }
}
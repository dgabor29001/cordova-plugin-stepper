package org.apache.cordova.stepper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.content.SharedPreferences;

import org.apache.cordova.BuildConfig;
import org.apache.cordova.stepper.util.API26Wrapper;

public class AppUpdatedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.i("STEPPER", "AppUpdatedReceiver.onReceive");
        SharedPreferences prefs = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        if (!prefs.contains("enabled") && prefs.getAll().size() > 0) {
          // Handle upgrades from previous version
          prefs.edit().putBoolean("enabled", true).commit();
        } else if (!prefs.getBoolean("enabled", false)) {
          return;
        }
        if (Build.VERSION.SDK_INT >= 26) {
          API26Wrapper.startForegroundService(context, new Intent(context, SensorListener.class));
        } else {
          context.startService(new Intent(context, SensorListener.class));
        }
    }

}

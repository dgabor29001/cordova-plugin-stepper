package org.apache.cordova.stepper;

import android.annotation.SuppressLint;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.time.OffsetDateTime;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.stepper.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;
import android.Manifest;
import android.content.pm.PackageManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import org.apache.cordova.stepper.util.API26Wrapper;
import org.apache.cordova.stepper.util.Config;
import org.apache.cordova.stepper.util.Status;

import android.os.Build;
import android.util.Log;
import android.util.Pair;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static android.content.Context.POWER_SERVICE;

/**
 * This class listens to the pedometer sensor
 */
public class StepperPlugin extends CordovaPlugin implements SensorEventListener {

  public static int REQUEST_DYN_PERMS = 101;
  public static int REQUEST_MAN_PERMS = 102;
  public static int REQUEST_BATTERY_PERMS = 103;

  private int status;

  private int todayOffset, total_start, goal, since_boot, total_days;
  public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());

  private SensorManager sensorManager;      // Sensor manager
  private Sensor sensor;                    // Pedometer sensor returned by sensor manager

  private CallbackContext callbackContext;  // Keeps track of the JS callback context.
  private CallbackContext updateCallback;  // Keeps track of the persistent callback.

  /**
   * Executes the request.
   *
   * @param action the action to execute.
   * @param args the exec() arguments.
   * @param callbackContext the callback context used when calling back into JavaScript.
   * @return whether the action was valid.
   */
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    this.callbackContext = callbackContext;

    if (action.equals("isStepCountingAvailable")) {
      isStepCountingAvailable();
    } else if (action.equals("requestPermission")) {
      requestPermission();
    } else if (action.equals("disableBatteryOptimizations")) {
      disableBatteryOptimizations();
    } else if (action.equals("startStepperUpdates")) {
      this.updateCallback = callbackContext;
      start(args);
    }
    else if (action.equals("stopStepperUpdates")) {
      stop(args);
    }
    else if (action.equals("setNotificationLocalizedStrings")) {
      setNotificationLocalizedStrings(args);
      win();
    }
    else if (action.equals("setGoal")) {
      setGoal(args);
      win();
    }
    else if (action.equals("getStepsByPeriod")) {
      getStepsByPeriod(args);
    }
    else if (action.equals("getLastEntries")) {
      getLastEntries(args);
    }
    else {
      return false;
    }
    return true;
  }
  
  /**
   * Disables battery optimizations for the app.
   * Requires permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS to function.
   */
  @SuppressLint("BatteryLife")
  private void disableBatteryOptimizations() {
    try {
        Intent intent     = new Intent();
        String pkgName    = getActivity().getPackageName();
        PowerManager pm   = (PowerManager)getActivity().getSystemService(POWER_SERVICE);
  
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            this.fail(Status.ERROR_BATTERY_OPTIMIZATION, "Permission not relevant on this device");
          return;
        }
  
        if (pm.isIgnoringBatteryOptimizations(pkgName)) {
          win(true);
          return;
        }
  
        intent.setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + pkgName));
  
        cordova.startActivityForResult(this, intent, REQUEST_BATTERY_PERMS);
        answerLater();
    } catch(Exception e) {
          this.fail(Status.ERROR_BATTERY_OPTIMIZATION, e.getMessage());
    }
  }
  
  @Override
  public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
    if (requestCode == REQUEST_BATTERY_PERMS) {
      win(resultCode == cordova.getActivity().RESULT_OK);
          return;
    }
      // Handle other results if exists.
      super.onActivityResult(requestCode, resultCode, data);
  }

  private void setNotificationLocalizedStrings(JSONArray args) {
    String pedometerIsCounting;
    String stepsToGo;
    String yourProgress;
    String goalReached;

    try {
      JSONObject joStrings = args.getJSONObject(0);
      pedometerIsCounting = joStrings.getString("pedometerIsCounting");
      stepsToGo = joStrings.getString("stepsToGo");
      yourProgress = joStrings.getString("yourProgress");
      goalReached = joStrings.getString("goalReached");
    }
    catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    SharedPreferences prefs = cordova.getContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

    if (pedometerIsCounting != null) {
      prefs.edit().putString(Config.PEDOMETER_IS_COUNTING_TEXT, pedometerIsCounting).apply();
    }
    if (stepsToGo != null) {
      prefs.edit().putString(Config.PEDOMETER_STEPS_TO_GO_FORMAT_TEXT, stepsToGo).apply();
    }
    if (yourProgress != null) {
      prefs.edit().putString(Config.PEDOMETER_YOUR_PROGRESS_FORMAT_TEXT, yourProgress).apply();
    }
    if (goalReached != null) {
      prefs.edit().putString(Config.PEDOMETER_GOAL_REACHED_FORMAT_TEXT, goalReached).apply();
    }
  }

  private void setGoal(JSONArray args) {
    try {
      goal = args.getInt(0);
    }
    catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    SharedPreferences prefs = cordova.getContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
    if (goal >= 0) {
      prefs.edit().putInt(Config.GOAL_PREF_INT, goal).apply();
    }
  }

  private void getStepsByPeriod(JSONArray args) {
    long startdate = 0;
    long endate = 0;
    long today = Util.getToday();
    try {
      startdate = OffsetDateTime.parse(args.getString(0)).toEpochSecond() * 1000;
      endate = OffsetDateTime.parse(args.getString(1)).toEpochSecond() * 1000;
    }
    catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    Database db = Database.getInstance(getActivity());
    int steps = db.getSteps(startdate, endate);
    if (startdate <= today && endate >= today) {
        steps += db.getCurrentSteps();
    }
    db.close();
    
    JSONObject joresult = new JSONObject();
    try {
        joresult.put("steps", steps);
    }
    catch (JSONException e) {
      e.printStackTrace();
      return;
    }
    win(joresult);
  }

  private void getLastEntries(JSONArray args) {
    int num = 0;
    try {
      num = args.getInt(0);
    }
    catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    Database db = Database.getInstance(getActivity());
    List<Pair<Long, Integer>> entries = db.getLastEntries(num);
    db.close();

    JSONObject joresult = new JSONObject();
    try {
      JSONArray jaEntries = new JSONArray();
      for (int i = 0; i < entries.size(); i++) {
        JSONObject joEntry = new JSONObject();
        joEntry.put("data", entries.get(i).first);
        joEntry.put("steps", entries.get(i).second);
        jaEntries.put(joEntry);
      }
      joresult.put("entries", jaEntries);
    }
    catch (JSONException e) {
      e.printStackTrace();
      return;
    }
    win(joresult);
  }

  public void onStart() {
    initSensor();
  }

  public void onPause(boolean multitasking) {
    status = Status.PAUSED;
    uninitSensor();
  }

  /**
   * Called by the Broker when listener is to be shut down.
   * Stop listener.
   */
  public void onDestroy() {
    Log.i("STEPPER", "onDestroy");
  }

  /**
   * Called when the view navigates.
   */
  @Override
  public void onReset() {
    Log.i("STEPPER", "onReset");
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !cordova.hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
      cordova.requestPermission(this, REQUEST_MAN_PERMS, Manifest.permission.ACTIVITY_RECOGNITION);
      answerLater();
    } else {
      win(true);
    }
  }

  private void isStepCountingAvailable() {
    sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    if (sensor != null) {
      this.win(true);
    } else {
      this.status = Status.ERROR_NO_SENSOR_FOUND;
      this.win(false);
    }
  }
  
  // called when the dynamic permissions are asked
  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    if (requestCode == REQUEST_DYN_PERMS || requestCode == REQUEST_MAN_PERMS) {
      for (int i = 0; i < grantResults.length; i++) {
        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
          String errmsg = "Permission denied ";
          for (String perm : permissions) {
            errmsg += " " + perm;
          }
          this.status = Status.ERROR_NO_PERMISSION;
          this.fail(Status.ERROR_NO_PERMISSION, "Permission denied: " + permissions[i]);
          return;
        }
      }
      // all dynamic permissions accepted!
      Log.i("STEPPER", "Dynamic permissions accepted");
      if (requestCode == REQUEST_MAN_PERMS) {
        win(true);
      } else {
        start();
      }
    }
  }
  
  private void start(JSONArray args) throws JSONException {
    final JSONObject options = args.getJSONObject(0);
    
    SharedPreferences prefs = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

    // If already starting or running, then return
    if ((status == Status.RUNNING) || (status == Status.STARTING)) {
      prefs.edit().putBoolean("enabled", true).commit();
      updateUI();
      return;
    }

    // Set options
    if (options.has(PEDOMETER_GOAL_REACHED_FORMAT_TEXT)) {
      prefs.edit().putString(PEDOMETER_GOAL_REACHED_FORMAT_TEXT, options.getString(PEDOMETER_GOAL_REACHED_FORMAT_TEXT)).commit();
    }

    if (options.has(PEDOMETER_IS_COUNTING_TEXT)) {
      prefs.edit().putString(PEDOMETER_IS_COUNTING_TEXT, options.getString(PEDOMETER_IS_COUNTING_TEXT)).commit();
    }

    if (options.has(PEDOMETER_STEPS_TO_GO_FORMAT_TEXT)) {
      prefs.edit().putString(PEDOMETER_STEPS_TO_GO_FORMAT_TEXT, options.getString(PEDOMETER_STEPS_TO_GO_FORMAT_TEXT)).commit();
    }

    if (options.has(PEDOMETER_YOUR_PROGRESS_FORMAT_TEXT)) {
      prefs.edit().putString(PEDOMETER_YOUR_PROGRESS_FORMAT_TEXT, options.getString(PEDOMETER_YOUR_PROGRESS_FORMAT_TEXT)).commit();
    }

    try {
      goal = args.getInt(0);
      if (goal >= 0) {
        prefs.edit().putInt(Config.GOAL_PREF_INT, goal).apply();
      }
    } catch (JSONException e) {}
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !cordova.hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
      cordova.requestPermission(this, REQUEST_DYN_PERMS, Manifest.permission.ACTIVITY_RECOGNITION);
      answerLater();
      return;
    }
    
    start();
  }
  
  private void start() {
	SharedPreferences prefs = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
    prefs.edit().putBoolean("enabled", true).commit();
    if (Build.VERSION.SDK_INT >= 26) {
      API26Wrapper.startForegroundService(getActivity(),
        new Intent(getActivity(), SensorListener.class));
    } else {
      getActivity().startService(new Intent(getActivity(), SensorListener.class));
    }

    initSensor();
  }

  private void stop(JSONArray args) {
	boolean clearDatabase = false;
	try {
	  clearDatabase = args.getBoolean(0);
	} catch (JSONException e) {
	  return;
	}

	SharedPreferences prefs = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
    prefs.edit().putBoolean("enabled", false).commit();
    
    if (status != Status.STOPPED) {
      uninitSensor();
    }
    
    if (clearDatabase) {
      Database db = Database.getInstance(getActivity());
      db.clear();
      db.close();
    }

    getActivity().stopService(new Intent(getActivity(), SensorListener.class));
    status = Status.STOPPED;

    win();
  }

  private void initSensor() {
	SharedPreferences prefs = getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);
    // If already starting or running, then return
    if (status == Status.RUNNING || status == Status.STARTING) {
      prefs.edit().putBoolean("enabled", true).commit();
      updateUI();
      return;
    }

    Database db = Database.getInstance(getActivity());
    
    todayOffset = db.getSteps(Util.getToday());

    goal = prefs.getInt(Config.GOAL_PREF_INT, Config.DEFAULT_GOAL);
    since_boot = db.getCurrentSteps();

    // register a sensor listener to live update the UI if a step is taken
    sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    if (sensor == null) {
      prefs.edit().putBoolean("enabled", false).commit();
      this.fail(Status.ERROR_NO_SENSOR_FOUND, "Not Step counter sensor found");
      return;
    } else {
      sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME, 0);
    }

    total_start = db.getTotalWithoutToday();
    total_days = db.getDays();

    db.close();

    status = Status.STARTING;

    updateUI();
  }

  private void uninitSensor() {
    try {
      sensorManager.unregisterListener(this);
    } catch (Exception e) {
      e.printStackTrace();
    }
    Database db = Database.getInstance(getActivity());
    db.saveCurrentSteps(since_boot);
    db.close();
  }

  @Override
  public void onSensorChanged(final SensorEvent event) {
    if (status == Status.STOPPED) {
      return;
    }
    status = Status.RUNNING;

    if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0 || event.values[0] < 0) {
      return;
    }
    since_boot = (int) event.values[0];
    if (todayOffset == Integer.MIN_VALUE) {
      // no values for today
      // we don`t know when the reboot was, so set today`s steps to 0 by
      // initializing them with -STEPS_SINCE_BOOT
      todayOffset = -since_boot;
      Database db = Database.getInstance(getActivity());
      db.insertNewDay(Util.getToday(), since_boot);
      db.close();
    } else if (todayOffset + since_boot < 0) {
      // offset is wrong. Recalculate it
//       todayOffset = -since_boot;
//       Database db = Database.getInstance(getActivity());
//       db.updateSteps(Util.getToday(), -since_boot);
//       db.close();
    }

    updateUI();
  }

  @Override
  public void onAccuracyChanged(final Sensor sensor, int accuracy) {
    // won't happen
  }

  private void updateUI() {

    // Today offset might still be Integer.MIN_VALUE on first start
    int steps_today = Math.max(todayOffset + since_boot, 0);
    int total = total_start + steps_today;
    int average = (total_start + steps_today) / Math.max(1, total_days);

    JSONObject result = new JSONObject();

    try {
      result.put("steps_today", steps_today);
      result.put("total", total);
      result.put("average", average);
    } catch (JSONException e) {
      e.printStackTrace();
    }

    PluginResult r = new PluginResult(PluginResult.Status.OK, result);
    r.setKeepCallback(true);
    if (this.updateCallback != null) {
      this.updateCallback.sendPluginResult(r);
    } else if (this.callbackContext != null) {
      this.callbackContext.sendPluginResult(r);      
    }
  }
  
  private void answerLater() {
    PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
    r.setKeepCallback(true);
    callbackContext.sendPluginResult(r);
  }
  
  private void win(JSONObject message) {
    PluginResult result;
    if(message != null) {
      result = new PluginResult(PluginResult.Status.OK, message);
    }
    else {
      result = new PluginResult(PluginResult.Status.OK);
    }
    callbackContext.sendPluginResult(result);
  }

  private void win(boolean success) {
    PluginResult result = new PluginResult(PluginResult.Status.OK, success);
    callbackContext.sendPluginResult(result);
  }

  private void win() {
    PluginResult result = new PluginResult(PluginResult.Status.OK);
    callbackContext.sendPluginResult(result);
  }

  private void fail(int code, String message) {
    // Error object
    JSONObject errorObj = new JSONObject();
    try {
      errorObj.put("code", code);
      errorObj.put("message", message);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    PluginResult err = new PluginResult(PluginResult.Status.ERROR, errorObj);
    callbackContext.sendPluginResult(err);
  }

  private Activity getActivity() {
    return cordova.getActivity();
  }
}

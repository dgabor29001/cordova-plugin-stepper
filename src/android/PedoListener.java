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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import org.apache.cordova.stepper.util.API26Wrapper;

import android.os.Build;
import android.util.Log;
import android.util.Pair;

import static android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS;
import static android.content.Context.POWER_SERVICE;

/**
 * This class listens to the pedometer sensor
 */
public class PedoListener extends CordovaPlugin implements SensorEventListener {

  public static int REQUEST_DYN_PERMS = 101;
  public static int REQUEST_MAN_PERMS = 102;
  public static int REQUEST_BATTERY_PERMS = 103;

  public static int STOPPED = 0;
  public static int STARTING = 1;
  public static int RUNNING = 2;
  public static int ERROR_FAILED_TO_START = 3;
  public static int ERROR_NO_SENSOR_FOUND = 4;
  public static int ERROR_NO_PERMISSION = 3;
  public static int ERROR_BATTERY_OPTIMIZATION = 6;
  public static int PAUSED = 5;

  public static int DEFAULT_GOAL = 0;

  public static String GOAL_PREF_INT = "GoalPrefInt";

  public static String PEDOMETER_IS_COUNTING_TEXT = "pedometerIsCountingText";
  public static String PEDOMETER_STEPS_TO_GO_FORMAT_TEXT = "pedometerStepsToGoFormatText";
  public static String PEDOMETER_YOUR_PROGRESS_FORMAT_TEXT = "pedometerYourProgressFormatText";
  public static String PEDOMETER_GOAL_REACHED_FORMAT_TEXT = "pedometerGoalReachedFormatText";

  private int status;

  private Integer startOffset;
  private int todayOffset, total_start, goal, since_boot, total_days;
  public final static NumberFormat formatter = NumberFormat.getInstance(Locale.getDefault());

  private SensorManager sensorManager;      // Sensor manager
  private Sensor sensor;                    // Pedometer sensor returned by sensor manager

  private CallbackContext callbackContext;  // Keeps track of the JS callback context.
  private CallbackContext updateCallback;  // Keeps track of the persistent callback.

  /**
   * Constructor
   */
  public PedoListener() {

  }

  /**
   * Sets the context of the Command. This can then be used to do things like
   * get file paths associated with the Activity.
   *
   * @param cordova the context of the main Activity.
   * @param webView the associated CordovaWebView.
   */
  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

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
      stop();
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
	          this.fail(PedoListener.ERROR_BATTERY_OPTIMIZATION, "Permission not relevant on this device");
	    	  return;
	      }
	
	      if (pm.isIgnoringBatteryOptimizations(pkgName)) {
	    	  win(true);
	    	  return;
	      }
	
	      intent.setAction(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
	      intent.setData(Uri.parse("package:" + pkgName));
	      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	
	      cordova.startActivityForResult(this, intent, REQUEST_BATTERY_PERMS);
	  } catch(Exception e) {
          this.fail(PedoListener.ERROR_BATTERY_OPTIMIZATION, e.getMessage());
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
      prefs.edit().putString(PedoListener.PEDOMETER_IS_COUNTING_TEXT, pedometerIsCounting).apply();
    }
    if (stepsToGo != null) {
      prefs.edit().putString(PedoListener.PEDOMETER_STEPS_TO_GO_FORMAT_TEXT, stepsToGo).apply();
    }
    if (yourProgress != null) {
      prefs.edit().putString(PedoListener.PEDOMETER_YOUR_PROGRESS_FORMAT_TEXT, yourProgress).apply();
    }
    if (goalReached != null) {
      prefs.edit().putString(PedoListener.PEDOMETER_GOAL_REACHED_FORMAT_TEXT, goalReached).apply();
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
      prefs.edit().putInt(PedoListener.GOAL_PREF_INT, goal).apply();
    }
  }

  private void getStepsByPeriod(JSONArray args) {
    long startdate = 0;
    long endate = 0;
    long today = Util.getToday();
    int steps = 0;
    try {
      startdate = OffsetDateTime.parse(args.getString(0)).toEpochSecond() * 1000;
      endate = OffsetDateTime.parse(args.getString(1)).toEpochSecond() * 1000;
    }
    catch (JSONException e) {
      e.printStackTrace();
      return;
    }

    if (startdate < today) {
        Database db = Database.getInstance(getActivity());
        steps = db.getSteps(startdate, endate);
        db.close();
    }

    if (startdate <= today && endate >= today) {
        steps += Math.max(todayOffset + since_boot, 0);
    }
    
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
    status = PedoListener.PAUSED;
    uninitSensor();
  }

  /**
   * Called by the Broker when listener is to be shut down.
   * Stop listener.
   */
  public void onDestroy() {
    Log.i("TAG", "onDestroy");
  }

  /**
   * Called when the view navigates.
   */
  @Override
  public void onReset() {
    Log.i("TAG", "onReset");
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !cordova.hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
      cordova.requestPermission(this, REQUEST_MAN_PERMS, Manifest.permission.ACTIVITY_RECOGNITION);
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
      this.status = PedoListener.ERROR_NO_SENSOR_FOUND;
      this.fail(PedoListener.ERROR_NO_SENSOR_FOUND, "Not Step counter sensor found");
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
          this.status = PedoListener.ERROR_NO_PERMISSION;
          this.fail(PedoListener.ERROR_NO_PERMISSION, "Permission denied: " + permissions[i]);
          return;
        }
      }
      // all dynamic permissions accepted!
      Log.i("TAG", "Dynamic permissions accepted");
      if (requestCode == REQUEST_MAN_PERMS) {
        win(true);
      } else {
        start();
      }
    }
  }
  
  private void start(JSONArray args) throws JSONException {
    final JSONObject options = args.getJSONObject(0);

    // If already starting or running, then return
    if ((status == PedoListener.RUNNING) || (status == PedoListener.STARTING)) {
      updateUI();
      return;
    }

    // Set options
    SharedPreferences prefs =
      getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

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
        prefs.edit().putInt(PedoListener.GOAL_PREF_INT, goal).apply();
      }
    } catch (JSONException e) {}
    
    try {
    	startOffset = options.getInt("offset");
    } catch(JSONException e) {}
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !cordova.hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
      cordova.requestPermission(this, REQUEST_DYN_PERMS, Manifest.permission.ACTIVITY_RECOGNITION);
      return;
    }
    
    start();
  }
  
  private void start() {
    if (Build.VERSION.SDK_INT >= 26) {
        API26Wrapper.startForegroundService(getActivity(),
          new Intent(getActivity(), SensorListener.class));
      } else {
        getActivity().startService(new Intent(getActivity(), SensorListener.class));
      }

      initSensor();
  }

  private void stop() {
    if (status != PedoListener.STOPPED) {
      uninitSensor();
    }

    Database db = Database.getInstance(getActivity());
    db.clear();
    db.close();

    getActivity().stopService(new Intent(getActivity(), SensorListener.class));
    status = PedoListener.STOPPED;

    win();
  }

  private void initSensor() {
    // If already starting or running, then return
    if ((status == PedoListener.RUNNING) || (status == PedoListener.STARTING)
      && status != PedoListener.PAUSED) {
      updateUI();
      return;
    }

    Database db = Database.getInstance(getActivity());

    todayOffset = db.getSteps(Util.getToday());
    if (startOffset != null) {
    	todayOffset -= startOffset;
    } else if (todayOffset > -200000) {
    	todayOffset = 0;
    }
    SharedPreferences prefs =
      getActivity().getSharedPreferences("pedometer", Context.MODE_PRIVATE);

    goal = prefs.getInt(PedoListener.GOAL_PREF_INT, PedoListener.DEFAULT_GOAL);
    since_boot = db.getCurrentSteps();
    int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);

    // register a sensor listener to live update the UI if a step is taken
    sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
    sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
    if (sensor == null) {
      new AlertDialog.Builder(getActivity()).setTitle("R.string.no_sensor")
        .setMessage("R.string.no_sensor_explain")
        .setOnDismissListener(new DialogInterface.OnDismissListener() {
          @Override
          public void onDismiss(final DialogInterface dialogInterface) {
            getActivity().finish();
          }
        }).setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(final DialogInterface dialogInterface, int i) {
          dialogInterface.dismiss();
        }
      }).create().show();
    } else {
      sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0);
    }

    since_boot -= pauseDifference;

    total_start = db.getTotalWithoutToday();
    total_days = db.getDays();

    db.close();

    status = PedoListener.STARTING;

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
    if (status == PedoListener.STOPPED) {
      return;
    }
    status = PedoListener.RUNNING;

    if (event.values[0] > Integer.MAX_VALUE || event.values[0] == 0) {
      return;
    }
    if (todayOffset == Integer.MIN_VALUE) {
      // no values for today
      // we don`t know when the reboot was, so set today`s steps to 0 by
      // initializing them with -STEPS_SINCE_BOOT
      todayOffset = -(int) event.values[0];
      Database db = Database.getInstance(getActivity());
      db.insertNewDay(Util.getToday(), (int) event.values[0]);
      db.close();
    }
    since_boot = (int) event.values[0];

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
    int average = (total_start + steps_today) / total_days;

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
    updateCallback.sendPluginResult(r);
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

package sk.henrichg.phoneprofilesplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

public class BatteryBroadcastReceiver extends BroadcastReceiver {

    static boolean isCharging = false;
    static int batteryPct = -100;
    //static boolean batteryLow = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        PPApplication.logE("##### BatteryBroadcastReceiver.onReceive", "xxx");

        CallsCounter.logCounter(context, "BatteryBroadcastReceiver.onReceive", "BatteryBroadcastReceiver_onReceive");
        CallsCounter.logCounterNoInc(context, "BatteryBroadcastReceiver.onReceive->action="+intent.getAction(), "BatteryBroadcastReceiver_onReceive");

        final Context appContext = context.getApplicationContext();

        if (!PPApplication.getApplicationStarted(appContext, true))
            // application is not started
            return;

        boolean statusReceived = false;
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "status=" + status);
        boolean _isCharging = false;
        if (status != -1) {
            statusReceived = true;
            _isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
        }
        else {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                if (status != -1) {
                    statusReceived = true;
                    _isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                            status == BatteryManager.BATTERY_STATUS_FULL;
                }
            }
        }

        boolean levelReceived = false;
        int pct = -100;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale;
        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "level=" + level);
        if (level != -1) {
            levelReceived = true;
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            pct = Math.round(level / (float) scale * 100);
        }
        else {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);
            if (batteryStatus != null) {
                level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                if (level != -1) {
                    levelReceived = true;
                    scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    pct = Math.round(level / (float) scale * 100);
                }
            }
        }

        //int _batteryLow = -1;
        String action = intent.getAction();
        if (action != null) {
            if (action.equals(Intent.ACTION_POWER_CONNECTED)) {
                statusReceived = true;
                _isCharging = true;
            } else if (action.equals(Intent.ACTION_POWER_DISCONNECTED)) {
                statusReceived = true;
                _isCharging = false;
            /*} else if (action.equals(Intent.ACTION_BATTERY_LOW)) {
                statusReceived = true;
                _batteryLow = 1;
            } else if (action.equals(Intent.ACTION_BATTERY_OKAY)) {
                statusReceived = true;
                _batteryLow = 0;*/
            }
        }

        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "action=" + action);
        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "isCharging=" + isCharging);
        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "_isCharging=" + _isCharging);
        //PPApplication.logE("BatteryBroadcastReceiver.onReceive", "batteryLow=" + batteryLow);
        //PPApplication.logE("BatteryBroadcastReceiver.onReceive", "_batteryLow=" + _batteryLow);
        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "batteryPct=" + batteryPct);
        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "pct=" + pct);

        /* In Samsung S8 lowLevel is configured to 105 :-(
        int _level = appContext.getResources().getInteger(com.android.internal.R.integer.config_lowBatteryWarningLevel);
        PPApplication.logE("BatteryBroadcastReceiver.onReceive", "lowLevel=" + Math.round(_level / (float) scale * 100));
        */

        if ((statusReceived && (isCharging != _isCharging)) ||
                //(statusReceived && (_batteryLow != -1) && (batteryLow != (_batteryLow == 1))) ||
                (levelReceived && (batteryPct != pct))) {
            PPApplication.logE("BatteryBroadcastReceiver.onReceive", "state changed");

            if (statusReceived) {
                isCharging = _isCharging;
                /*if (_batteryLow != -1)
                    batteryLow = (_batteryLow == 1);*/
            }
            if (levelReceived)
                batteryPct = pct;

            //BatteryJob.start(appContext, isCharging, batteryPct, statusReceived, levelReceived);

            // required for reschedule jobs for power save mode
            PPApplication.restartAllScanners(appContext, true);
            /*PPApplication.restartWifiScanner(appContext, true);
            PPApplication.restartBluetoothScanner(appContext, true);
            PPApplication.restartGeofenceScanner(appContext, true);
            PPApplication.restartPhoneStateScanner(appContext, true);
            PPApplication.restartOrientationScanner(appContext);*/

            if (Event.getGlobalEventsRunning(appContext)) {
                PPApplication.startHandlerThread();
                final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                        PowerManager.WakeLock wakeLock = null;
                        if (powerManager != null) {
                            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BatteryBroadcastReceiver.onReceive");
                            wakeLock.acquire(10 * 60 * 1000);
                        }

                        // start events handler
                        EventsHandler eventsHandler = new EventsHandler(appContext);
                        eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_BATTERY/*, false*/);

                        if ((wakeLock != null) && wakeLock.isHeld())
                            wakeLock.release();
                    }
                });
            }
        }
    }
}

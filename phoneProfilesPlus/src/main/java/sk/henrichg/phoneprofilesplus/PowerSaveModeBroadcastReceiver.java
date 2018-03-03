package sk.henrichg.phoneprofilesplus;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

public class PowerSaveModeBroadcastReceiver extends BroadcastReceiver {

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onReceive(Context context, Intent intent) {
        PPApplication.logE("##### PowerSaveModeBroadcastReceiver.onReceive", "xxx");

        CallsCounter.logCounter(context, "PowerSaveModeBroadcastReceiver.onReceive", "PowerSaveModeBroadcastReceiver_onReceive");

        final Context appContext = context.getApplicationContext();

        if (!PPApplication.getApplicationStarted(appContext, true))
            // application is not started
            return;

        //PowerSaveModeJob.start(appContext);

        // required for reschedule jobs for power save mode
        PPApplication.restartAllScanners(appContext, true);
        /*PPApplication.restartWifiScanner(appContext, true);
        PPApplication.restartBluetoothScanner(appContext, true);
        PPApplication.restartGeofenceScanner(appContext, true);
        PPApplication.restartPhoneStateScanner(appContext, true);
        PPApplication.restartOrientationScanner(appContext);*/

        PPApplication.startHandlerThread();
        final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                if (powerManager != null) {
                    wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PowerSaveModeBroadcastReceiver.onReceive");
                    wakeLock.acquire(10 * 60 * 1000);
                }

                // start events handler
                EventsHandler eventsHandler = new EventsHandler(appContext);
                eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_POWER_SAVE_MODE/*, false*/);

                if ((wakeLock != null) && wakeLock.isHeld())
                    wakeLock.release();
            }
        });

    }
}

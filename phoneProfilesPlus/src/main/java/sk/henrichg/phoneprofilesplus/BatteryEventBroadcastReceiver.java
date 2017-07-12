package sk.henrichg.phoneprofilesplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import com.commonsware.cwac.wakeful.WakefulIntentService;

public class BatteryEventBroadcastReceiver extends BroadcastReceiver {

    public static final String BROADCAST_RECEIVER_TYPE = "batteryEvent";

    private static boolean isCharging = false;
    private static int batteryPct = -100;

    @Override
    public void onReceive(Context context, Intent intent) {

        //Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        PPApplication.logE("##### BatteryEventBroadcastReceiver.onReceive","xxx");

        Context appContext = context.getApplicationContext();

        if (!PPApplication.getApplicationStarted(appContext, true))
            // application is not started
            return;

        //PPApplication.loadPreferences(context);

        //boolean batteryEventsExists = false;

        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        PPApplication.logE("BatteryEventBroadcastReceiver.onReceive", "status=" + status);

        if (status != -1) {
            boolean _isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL;
            PPApplication.logE("BatteryEventBroadcastReceiver.onReceive", "isCharging=" + isCharging);

            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            int pct = Math.round(level / (float) scale * 100);


            if ((isCharging != _isCharging) || (batteryPct != pct)) {
                PPApplication.logE("@@@ BatteryEventBroadcastReceiver.onReceive", "xxx");

                PPApplication.logE("BatteryEventBroadcastReceiver.onReceive", "state changed");
                PPApplication.logE("BatteryEventBroadcastReceiver.onReceive", "batteryPct=" + pct);
                PPApplication.logE("BatteryEventBroadcastReceiver.onReceive", "level=" + level);
                PPApplication.logE("BatteryEventBroadcastReceiver.onReceive", "isCharging=" + isCharging);
                PPApplication.logE("BatteryEventBroadcastReceiver.onReceive", "_isCharging=" + _isCharging);

                isCharging = _isCharging;
                batteryPct = pct;

                boolean oldPowerSaveMode = PPApplication.isPowerSaveMode;
                PPApplication.isPowerSaveMode = false;
                if ((!isCharging) &&
                    ((ApplicationPreferences.applicationPowerSaveModeInternal(appContext).equals("1") && (batteryPct <= 5)) ||
                     (ApplicationPreferences.applicationPowerSaveModeInternal(appContext).equals("2") && (batteryPct <= 15))))
                    PPApplication.isPowerSaveMode = true;
                else {
                    if (isCharging)
                        PPApplication.isPowerSaveMode = false;
                    else
                        PPApplication.isPowerSaveMode = oldPowerSaveMode;
                }

                if (Event.getGlobalEventsRuning(appContext)) {

                    if (PhoneProfilesService.instance != null) {
                        if (PhoneProfilesService.isGeofenceScannerStarted())
                            PhoneProfilesService.geofencesScanner.resetLocationUpdates(oldPowerSaveMode, false);
                        PhoneProfilesService.instance.resetListeningOrientationSensors(oldPowerSaveMode, false);
                        if (PhoneProfilesService.isPhoneStateStarted())
                            PhoneProfilesService.phoneStateScanner.resetListening(oldPowerSaveMode, false);
                    }

                    /*DataWrapper dataWrapper = new DataWrapper(appContext, false, false, 0);
                    batteryEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_BATTERY) > 0;
                    dataWrapper.invalidateDataWrapper();

                    if (batteryEventsExists)
                    {*/
                    // start service
                    Intent eventsServiceIntent = new Intent(appContext, EventsService.class);
                    eventsServiceIntent.putExtra(EventsService.EXTRA_BROADCAST_RECEIVER_TYPE, BROADCAST_RECEIVER_TYPE);
                    WakefulIntentService.sendWakefulWork(appContext, eventsServiceIntent);
                    //}
                }
            }
        }
    }
}

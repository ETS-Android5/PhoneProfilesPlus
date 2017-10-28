package sk.henrichg.phoneprofilesplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

public class EventTimeBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PPApplication.logE("##### EventTimeBroadcastReceiver.onReceive", "xxx");

        CallsCounter.logCounter(context, "EventTimeBroadcastReceiver.onReceive", "EventTimeBroadcastReceiver_onReceive");

        final Context appContext = context.getApplicationContext();

        if (!PPApplication.getApplicationStarted(appContext, true))
            // application is not started
            return;

        if (Event.getGlobalEventsRunning(appContext))
        {
            PPApplication.logE("@@@ EventTimeBroadcastReceiver.onReceive","xxx");

            /*boolean timeEventsExists = false;

            DataWrapper dataWrapper = new DataWrapper(appContext, false, false, 0);
            timeEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_TIME) > 0;
            PPApplication.logE("EventTimeBroadcastReceiver.onReceive","timeEventsExists="+timeEventsExists);
            dataWrapper.invalidateDataWrapper();

            if (timeEventsExists)
            {*/
                // start job
                //EventsHandlerJob.startForSensor(appContext, EventsHandler.SENSOR_TYPE_TIME);
                final Handler handler = new Handler(appContext.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EventTimeBroadcastReceiver.onReceive");
                        wakeLock.acquire(10 * 60 * 1000);

                        EventsHandler eventsHandler = new EventsHandler(appContext);
                        eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_TIME, false);

                        wakeLock.release();
                    }
                });
            //}

        }

    }

}

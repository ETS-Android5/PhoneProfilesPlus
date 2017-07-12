package sk.henrichg.phoneprofilesplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.commonsware.cwac.wakeful.WakefulIntentService;

import java.util.Calendar;
import java.util.TimeZone;

public class NFCBroadcastReceiver extends BroadcastReceiver {

    public static final String BROADCAST_RECEIVER_TYPE = "NFC";

    //private static ContentObserver smsObserver;
    //private static ContentObserver mmsObserver;
    //private static int mmsCount;

    @Override
    public void onReceive(Context context, Intent intent) {
        PPApplication.logE("##### NFCBroadcastReceiver.onReceive", "xxx");

        Context appContext = context.getApplicationContext();

        LocalBroadcastManager.getInstance(appContext).unregisterReceiver(PPApplication.nfcBroadcastReceiver);

        String tagName = intent.getStringExtra(EventsService.EXTRA_EVENT_NFC_TAG_NAME);

        Calendar now = Calendar.getInstance();
        int gmtOffset = TimeZone.getDefault().getRawOffset();
        long time = now.getTimeInMillis() + gmtOffset;

        startService(appContext, tagName, time);
    }

    private static void startService(Context context, String tagName, long time)
    {
        if (!PPApplication.getApplicationStarted(context, true))
            // application is not started
            return;

        //PPApplication.loadPreferences(context);

        if (Event.getGlobalEventsRuning(context))
        {
            PPApplication.logE("@@@ NFCBroadcastReceiver.startService","xxx");

            /*boolean smsEventsExists = false;

            DataWrapper dataWrapper = new DataWrapper(context, false, false, 0);
            smsEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_SMS) > 0;
            PPApplication.logE("SMSBroadcastReceiver.onReceive","smsEventsExists="+smsEventsExists);
            dataWrapper.invalidateDataWrapper();

            if (smsEventsExists)
            {*/
                // start service
                Intent eventsServiceIntent = new Intent(context, EventsService.class);
                eventsServiceIntent.putExtra(EventsService.EXTRA_BROADCAST_RECEIVER_TYPE, BROADCAST_RECEIVER_TYPE);
                eventsServiceIntent.putExtra(EventsService.EXTRA_EVENT_NFC_TAG_NAME, tagName);
                eventsServiceIntent.putExtra(EventsService.EXTRA_EVENT_NFC_DATE, time);
                WakefulIntentService.sendWakefulWork(context, eventsServiceIntent);
            //}
        }
    }

}

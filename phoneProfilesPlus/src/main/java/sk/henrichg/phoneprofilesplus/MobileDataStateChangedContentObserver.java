package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;

import com.commonsware.cwac.wakeful.WakefulIntentService;

class MobileDataStateChangedContentObserver extends ContentObserver {

    //public static boolean internalChange = false;

    private static boolean previousState = false;

    private Context context;

    MobileDataStateChangedContentObserver(Context c, Handler handler) {
        super(handler);

        context=c;

        //Log.e("### MobileDataStateChangedContentObserver", "xxx");

        previousState = ActivateProfileHelper.isMobileData(context);
    }

    /*
    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }
    */

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        PPApplication.logE("##### MobileDataStateChangedContentObserver", "onChange");

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            boolean actualState = ActivateProfileHelper.isMobileData(context);
            if (previousState != actualState) {

                if (Event.getGlobalEventsRunning(context)) {
                    Intent eventsServiceIntent = new Intent(context, EventsService.class);
                    eventsServiceIntent.putExtra(EventsService.EXTRA_BROADCAST_RECEIVER_TYPE, EventsService.SENSOR_TYPE_RADIO_SWITCH);
                    eventsServiceIntent.putExtra(EventsService.EXTRA_EVENT_RADIO_SWITCH_TYPE, EventPreferencesRadioSwitch.RADIO_TYPE_MOBILE_DATA);
                    eventsServiceIntent.putExtra(EventsService.EXTRA_EVENT_RADIO_SWITCH_STATE, actualState);
                    WakefulIntentService.sendWakefulWork(context, eventsServiceIntent);
                }

                previousState = actualState;
            }
        }
    }

}

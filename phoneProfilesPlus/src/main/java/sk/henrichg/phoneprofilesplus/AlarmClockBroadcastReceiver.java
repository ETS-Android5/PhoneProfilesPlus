package sk.henrichg.phoneprofilesplus;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;

import java.util.Calendar;

import static android.content.Context.POWER_SERVICE;

public class AlarmClockBroadcastReceiver extends BroadcastReceiver {
    public AlarmClockBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        PPApplication.logE("##### AlarmClockBroadcastReceiver.onReceive", "xxx");

        CallsCounter.logCounter(context, "AlarmClockBroadcastReceiver.onReceive", "AlarmClockBroadcastReceiver_onReceive");

        final Context appContext = context.getApplicationContext();

        if (!PPApplication.getApplicationStarted(context.getApplicationContext(), true))
            return;

        Calendar now = Calendar.getInstance();
        int gmtOffset = 0; //TimeZone.getDefault().getRawOffset();
        final long _time = now.getTimeInMillis() + gmtOffset;

        if (Event.getGlobalEventsRunning(context))
        {
            PPApplication.logE("@@@ AlarmClockBroadcastReceiver.onReceive","start service");

            // start job
            //EventsHandlerJob.startForSMSSensor(context.getApplicationContext(), origin, time);
            PPApplication.startHandlerThread("AlarmClockBroadcastReceiver.onReceive");
            final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = null;
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AlarmClockBroadcastReceiver.onReceive");
                        wakeLock.acquire(10 * 60 * 1000);
                    }

                    EventsHandler eventsHandler = new EventsHandler(appContext);
                    eventsHandler.setEventAlarmClockParameters(_time);
                    eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_ALARM_CLOCK/*, false*/);

                    if ((wakeLock != null) && wakeLock.isHeld()) {
                        try {
                            wakeLock.release();
                        } catch (Exception ignored) {}
                    }
                }
            });
        }

    }
}

/*

<receiver
            android:name=".AlarmClockBroadcastReceiver"
            android:enabled="false"
            android:exported="true"
            tools:ignore="ExportedReceiver">
            <intent-filter>

                <!--
                // Stock alarms
                // Nexus (?)
                -->
                <action android:name="com.android.deskclock.ALARM_ALERT"/>
                <!--
                <action android:name="com.android.deskclock.ALARM_DISMISS" />
                <action android:name="com.android.deskclock.ALARM_DONE" />
                <action android:name="com.android.deskclock.ALARM_SNOOZE" />
                -->
                <!-- // stock Android (?) -->
                <action android:name="com.android.alarmclock.ALARM_ALERT"/>
                <!--
                // Stock alarm Manufactures
                // Samsung
                -->
                <action android:name="com.samsung.sec.android.clockpackage.alarm.ALARM_ALERT"/>
                <!-- // HTC -->
                <action android:name="com.htc.android.worldclock.ALARM_ALERT"/>
                <action android:name="com.htc.android.ALARM_ALERT"/>
                <!-- // Sony -->
                <action android:name="com.sonyericsson.alarm.ALARM_ALERT"/>
                <!-- // ZTE -->
                <action android:name="zte.com.cn.alarmclock.ALARM_ALERT"/>
                <!-- // Motorola -->
                <action android:name="com.motorola.blur.alarmclock.ALARM_ALERT"/>
                <!-- // LG -->
                <action android:name="com.lge.clock.ALARM_ALERT"/>
                <!--
                // Thirdparty Alarms
                // Gentle Alarm
                -->
                <action android:name="com.mobitobi.android.gentlealarm.ALARM_INFO"/>
                <!-- // Sleep As Android -->
                <action android:name="com.urbandroid.sleep.alarmclock.ALARM_ALERT"/>
                <!-- // Alarmdroid (1.13.2) -->
                <action android:name="com.splunchy.android.alarmclock.ALARM_ALERT"/>
            </intent-filter>
        </receiver>

*/

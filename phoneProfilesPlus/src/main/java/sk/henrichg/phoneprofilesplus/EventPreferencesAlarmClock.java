package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

class EventPreferencesAlarmClock extends EventPreferences {

    long _startTime;
    boolean _permanentRun;
    int _duration;

    static final String PREF_EVENT_ALARM_CLOCK_ENABLED = "eventAlarmClockEnabled";
    private static final String PREF_EVENT_ALARM_CLOCK_PERMANENT_RUN = "eventAlarmClockPermanentRun";
    private static final String PREF_EVENT_ALARM_CLOCK_DURATION = "eventAlarmClockDuration";

    private static final String PREF_EVENT_ALARM_CLOCK_CATEGORY = "eventAlarmClockCategory";

    EventPreferencesAlarmClock(Event event,
                                    boolean enabled,
                                    boolean permanentRun,
                                    int duration)
    {
        super(event, enabled);

        this._permanentRun = permanentRun;
        this._duration = duration;

        this._startTime = 0;
    }

    @Override
    public void copyPreferences(Event fromEvent)
    {
        this._enabled = fromEvent._eventPreferencesAlarmClock._enabled;
        this._permanentRun = fromEvent._eventPreferencesAlarmClock._permanentRun;
        this._duration = fromEvent._eventPreferencesAlarmClock._duration;
        this.setSensorPassed(fromEvent._eventPreferencesAlarmClock.getSensorPassed());

        this._startTime = 0;
    }

    @Override
    public void loadSharedPreferences(SharedPreferences preferences)
    {
        Editor editor = preferences.edit();
        editor.putBoolean(PREF_EVENT_ALARM_CLOCK_ENABLED, _enabled);
        editor.putBoolean(PREF_EVENT_ALARM_CLOCK_PERMANENT_RUN, this._permanentRun);
        editor.putString(PREF_EVENT_ALARM_CLOCK_DURATION, String.valueOf(this._duration));
        editor.apply();
    }

    @Override
    public void saveSharedPreferences(SharedPreferences preferences)
    {
        this._enabled = preferences.getBoolean(PREF_EVENT_ALARM_CLOCK_ENABLED, false);
        this._permanentRun = preferences.getBoolean(PREF_EVENT_ALARM_CLOCK_PERMANENT_RUN, false);
        this._duration = Integer.parseInt(preferences.getString(PREF_EVENT_ALARM_CLOCK_DURATION, "5"));
    }

    @Override
    public String getPreferencesDescription(boolean addBullet, boolean addPassStatus, Context context)
    {
        String descr = "";

        if (!this._enabled) {
            if (!addBullet)
                descr = context.getString(R.string.event_preference_sensor_alarm_clock_summary);
        }
        else
        {
            if (addBullet) {
                descr = descr + "<b>\u2022 ";
                descr = descr + getPassStatusString(context.getString(R.string.event_type_alarm_clock), addPassStatus, DatabaseHandler.ETYPE_ALARM_CLOCK, context);
                descr = descr + ": </b>";
            }

            if (this._permanentRun)
                descr = descr + context.getString(R.string.pref_event_permanentRun);
            else
                descr = descr + context.getString(R.string.pref_event_duration) + ": " + GlobalGUIRoutines.getDurationString(this._duration);
        }

        return descr;
    }

    @Override
    void setSummary(PreferenceManager prefMng, String key, String value, Context context)
    {
        if (key.equals(PREF_EVENT_ALARM_CLOCK_ENABLED)) {
            CheckBoxPreference preference = (CheckBoxPreference) prefMng.findPreference(key);
            if (preference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, true, preference.isChecked(), false, false, false);
            }
        }

        if (key.equals(PREF_EVENT_ALARM_CLOCK_PERMANENT_RUN)) {
            CheckBoxPreference permanentRunPreference = (CheckBoxPreference) prefMng.findPreference(key);
            if (permanentRunPreference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(permanentRunPreference, true, permanentRunPreference.isChecked(), false, false, false);
            }
            Preference preference = prefMng.findPreference(PREF_EVENT_ALARM_CLOCK_DURATION);
            if (preference != null) {
                preference.setEnabled(value.equals("false"));
            }
        }
        if (key.equals(PREF_EVENT_ALARM_CLOCK_DURATION)) {
            Preference preference = prefMng.findPreference(key);
            int delay;
            try {
                delay = Integer.parseInt(value);
            } catch (Exception e) {
                delay = 0;
            }
            GlobalGUIRoutines.setPreferenceTitleStyle(preference, true, delay > 5, false, false, false);
        }
    }

    @Override
    public void setSummary(PreferenceManager prefMng, String key, SharedPreferences preferences, Context context)
    {
        if (key.equals(PREF_EVENT_ALARM_CLOCK_ENABLED) ||
            key.equals(PREF_EVENT_ALARM_CLOCK_PERMANENT_RUN)) {
            boolean value = preferences.getBoolean(key, false);
            setSummary(prefMng, key, value ? "true": "false", context);
        }
        if (key.equals(PREF_EVENT_ALARM_CLOCK_DURATION))
        {
            setSummary(prefMng, key, preferences.getString(key, ""), context);
        }
    }

    @Override
    public void setAllSummary(PreferenceManager prefMng, SharedPreferences preferences, Context context)
    {
        setSummary(prefMng, PREF_EVENT_ALARM_CLOCK_ENABLED, preferences, context);
        setSummary(prefMng, PREF_EVENT_ALARM_CLOCK_PERMANENT_RUN, preferences, context);
        setSummary(prefMng, PREF_EVENT_ALARM_CLOCK_DURATION, preferences, context);
    }

    @Override
    public void setCategorySummary(PreferenceManager prefMng, /*String key,*/ SharedPreferences preferences, Context context) {
        PreferenceAllowed preferenceAllowed = Event.isEventPreferenceAllowed(PREF_EVENT_ALARM_CLOCK_ENABLED, context);
        if (preferenceAllowed.allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
            EventPreferencesAlarmClock tmp = new EventPreferencesAlarmClock(this._event, this._enabled, this._permanentRun, this._duration);
            if (preferences != null)
                tmp.saveSharedPreferences(preferences);

            Preference preference = prefMng.findPreference(PREF_EVENT_ALARM_CLOCK_CATEGORY);
            if (preference != null) {
                CheckBoxPreference enabledPreference = (CheckBoxPreference)prefMng.findPreference(PREF_EVENT_ALARM_CLOCK_ENABLED);
                boolean enabled = (enabledPreference != null) && enabledPreference.isChecked();
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, enabled, tmp._enabled, false, !tmp.isRunnable(context), false);
                preference.setSummary(GlobalGUIRoutines.fromHtml(tmp.getPreferencesDescription(false, false, context)));
            }
        }
        else {
            Preference preference = prefMng.findPreference(PREF_EVENT_ALARM_CLOCK_CATEGORY);
            if (preference != null) {
                preference.setSummary(context.getResources().getString(R.string.profile_preferences_device_not_allowed)+
                        ": "+ preferenceAllowed.getNotAllowedPreferenceReasonString(context));
                preference.setEnabled(false);
            }
        }
    }

    @Override
    public boolean isRunnable(Context context)
    {
        return super.isRunnable(context);
    }

    long computeAlarm()
    {
        PPApplication.logE("EventPreferencesAlarmClock.computeAlarm","xxx");

        Calendar calEndTime = Calendar.getInstance();

        int gmtOffset = 0; //TimeZone.getDefault().getRawOffset();

        calEndTime.setTimeInMillis((_startTime - gmtOffset) + (_duration * 1000));
        //calEndTime.set(Calendar.SECOND, 0);
        //calEndTime.set(Calendar.MILLISECOND, 0);

        long alarmTime;
        alarmTime = calEndTime.getTimeInMillis();

        return alarmTime;
    }

    @Override
    public void setSystemEventForStart(Context context)
    {
        // set alarm for state PAUSE

        // this alarm generates broadcast, that change state into RUNNING;
        // from broadcast will by called EventsHandler

        PPApplication.logE("EventPreferencesAlarmClock.setSystemRunningEvent","xxx");

        Context _context = context;
        if (PhoneProfilesService.getInstance() != null)
            _context = PhoneProfilesService.getInstance();

        removeAlarm(_context);
    }

    @Override
    public void setSystemEventForPause(Context context)
    {
        // set alarm for state RUNNING

        // this alarm generates broadcast, that change state into PAUSE;
        // from broadcast will by called EventsHandler

        PPApplication.logE("EventPreferencesAlarmClock.setSystemPauseEvent","xxx");

        Context _context = context;
        if (PhoneProfilesService.getInstance() != null)
            _context = PhoneProfilesService.getInstance();

        removeAlarm(_context);

        if (!(isRunnable(context) && _enabled))
            return;

        setAlarm(computeAlarm(), _context);
    }

    @Override
    public void removeSystemEvent(Context context)
    {
        Context _context = context;
        if (PhoneProfilesService.getInstance() != null)
            _context = PhoneProfilesService.getInstance();

        removeAlarm(_context);

        PPApplication.logE("EventPreferencesAlarmClock.removeSystemEvent", "xxx");
    }

    private void removeAlarm(Context context)
    {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent();
            intent.setAction(PhoneProfilesService.ACTION_ALARM_CLOCK_EVENT_END_BROADCAST_RECEIVER);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) _event._id, intent, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                PPApplication.logE("EventPreferencesAlarmClock.removeAlarm", "alarm found");

                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    @SuppressLint({"SimpleDateFormat", "NewApi"})
    private void setAlarm(long alarmTime, Context context)
    {
        if (!_permanentRun) {
            if (_startTime > 0) {
                if (PPApplication.logEnabled()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("EE d.MM.yyyy HH:mm:ss:S");
                    String result = sdf.format(alarmTime);
                    PPApplication.logE("EventPreferencesAlarmClock.setAlarm", "endTime=" + result);
                }

                Intent intent = new Intent();
                intent.setAction(PhoneProfilesService.ACTION_ALARM_CLOCK_EVENT_END_BROADCAST_RECEIVER);

                //intent.putExtra(PPApplication.EXTRA_EVENT_ID, _event._id);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) _event._id, intent, PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
                if (alarmManager != null) {
                    if ((android.os.Build.VERSION.SDK_INT >= 21) &&
                            ApplicationPreferences.applicationUseAlarmClock(context)) {
                        Intent editorIntent = new Intent(context, EditorProfilesActivity.class);
                        PendingIntent infoPendingIntent = PendingIntent.getActivity(context, 1000, editorIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(alarmTime + Event.EVENT_ALARM_TIME_SOFT_OFFSET, infoPendingIntent);
                        alarmManager.setAlarmClock(clockInfo, pendingIntent);
                    }
                    else {
                        if (android.os.Build.VERSION.SDK_INT >= 23)
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime + Event.EVENT_ALARM_TIME_OFFSET, pendingIntent);
                        else //if (android.os.Build.VERSION.SDK_INT >= 19)
                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime + Event.EVENT_ALARM_TIME_OFFSET, pendingIntent);
                        //else
                        //    alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime + Event.EVENT_ALARM_TIME_OFFSET, pendingIntent);
                    }
                }
            }
        }
    }

    void saveStartTime(DataWrapper dataWrapper, long startTime) {
        if (this._startTime == 0) {
            // alarm for end is not set

            this._startTime = startTime + (10 * 1000);

            DatabaseHandler.getInstance(dataWrapper.context).updateAlarmClockStartTime(_event);

            setSystemEventForPause(dataWrapper.context);
        }
    }

}

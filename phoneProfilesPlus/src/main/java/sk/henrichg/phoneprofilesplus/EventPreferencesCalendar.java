package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.provider.CalendarContract.Instances;
import android.text.format.DateFormat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

class EventPreferencesCalendar extends EventPreferences {

    String _calendars;
    int _searchField;
    String _searchString;
    int _availability;
    boolean _ignoreAllDayEvents;
    int _startBeforeEvent;

    long _startTime;
    long _endTime;
    boolean _eventFound;

    static final String PREF_EVENT_CALENDAR_ENABLED = "eventCalendarEnabled";
    private static final String PREF_EVENT_CALENDAR_CALENDARS = "eventCalendarCalendars";
    private static final String PREF_EVENT_CALENDAR_SEARCH_FIELD = "eventCalendarSearchField";
    private static final String PREF_EVENT_CALENDAR_SEARCH_STRING = "eventCalendarSearchString";
    private static final String PREF_EVENT_CALENDAR_AVAILABILITY = "eventCalendarAvailability";
    private static final String PREF_EVENT_CALENDAR_IGNORE_ALL_DAY_EVENTS = "eventCalendarIgnoreAllDayEvents";
    private static final String PREF_EVENT_CALENDAR_START_BEFORE_EVENT = "eventCalendarStartBeforeEvent";

    private static final String PREF_EVENT_CALENDAR_CATEGORY = "eventCalendarCategory";

    private static final int SEARCH_FIELD_TITLE = 0;
    private static final int SEARCH_FIELD_DESCRIPTION = 1;
    private static final int SEARCH_FIELD_LOCATION = 2;

    //private static final int AVAILABILITY_NO_CHECK = 0;
    private static final int AVAILABILITY_BUSY = 1;
    private static final int AVAILABILITY_FREE = 2;
    private static final int AVAILABILITY_TENTATIVE = 3;

    EventPreferencesCalendar(Event event,
                                boolean enabled,
                                String calendars,
                                int searchField,
                                String searchString,
                                int availability,
                                boolean ignoreAllDayEvents,
                                int startBeforeEvent)
    {
        super(event, enabled);

        this._calendars = calendars;
        this._searchField = searchField;
        this._searchString = searchString;
        this._availability = availability;
        this._ignoreAllDayEvents = ignoreAllDayEvents;
        this._startBeforeEvent = startBeforeEvent;

        this._startTime = 0;
        this._endTime = 0;
        this._eventFound = false;
    }

    @Override
    public void copyPreferences(Event fromEvent)
    {
        this._enabled = fromEvent._eventPreferencesCalendar._enabled;
        this._calendars = fromEvent._eventPreferencesCalendar._calendars;
        this._searchField = fromEvent._eventPreferencesCalendar._searchField;
        this._searchString = fromEvent._eventPreferencesCalendar._searchString;
        this._availability = fromEvent._eventPreferencesCalendar._availability;
        this._ignoreAllDayEvents = fromEvent._eventPreferencesCalendar._ignoreAllDayEvents;
        this._startBeforeEvent = fromEvent._eventPreferencesCalendar._startBeforeEvent;

        this._startTime = 0;
        this._endTime = 0;
        this._eventFound = false;
    }

    @Override
    public void loadSharedPreferences(SharedPreferences preferences)
    {
        Editor editor = preferences.edit();
        editor.putBoolean(PREF_EVENT_CALENDAR_ENABLED, _enabled);
        editor.putString(PREF_EVENT_CALENDAR_CALENDARS, _calendars);
        editor.putString(PREF_EVENT_CALENDAR_SEARCH_FIELD, String.valueOf(_searchField));
        editor.putString(PREF_EVENT_CALENDAR_SEARCH_STRING, _searchString);
        editor.putString(PREF_EVENT_CALENDAR_AVAILABILITY, String.valueOf(_availability));
        editor.putBoolean(PREF_EVENT_CALENDAR_IGNORE_ALL_DAY_EVENTS, _ignoreAllDayEvents);
        editor.putString(PREF_EVENT_CALENDAR_START_BEFORE_EVENT, Integer.toString(_startBeforeEvent));
        editor.apply();
    }

    @Override
    public void saveSharedPreferences(SharedPreferences preferences)
    {
        this._enabled = preferences.getBoolean(PREF_EVENT_CALENDAR_ENABLED, false);
        this._calendars = preferences.getString(PREF_EVENT_CALENDAR_CALENDARS, "");
        this._searchField = Integer.parseInt(preferences.getString(PREF_EVENT_CALENDAR_SEARCH_FIELD, "0"));
        this._searchString = preferences.getString(PREF_EVENT_CALENDAR_SEARCH_STRING, "");
        this._availability = Integer.parseInt(preferences.getString(PREF_EVENT_CALENDAR_AVAILABILITY, "0"));
        this._ignoreAllDayEvents = preferences.getBoolean(PREF_EVENT_CALENDAR_IGNORE_ALL_DAY_EVENTS, false);
        this._startBeforeEvent = Integer.parseInt(preferences.getString(PREF_EVENT_CALENDAR_START_BEFORE_EVENT, "0"));

        this._startTime = 0;
        this._endTime = 0;
        this._eventFound = false;
    }

    @Override
    public String getPreferencesDescription(boolean addBullet, Context context)
    {
        String descr = "";

        if (!this._enabled) {
            if (!addBullet)
                descr = context.getString(R.string.event_preference_sensor_calendar_summary);
        }
        else
        {
            if (addBullet) {
                descr = descr + "<b>\u2022 </b>";
                descr = descr + "<b>" + context.getString(R.string.event_type_calendar) + ": " + "</b>";
            }

            String[] searchFields = context.getResources().getStringArray(R.array.eventCalendarSearchFieldArray);
            descr = descr + searchFields[this._searchField] + "; ";

            descr = descr + "\"" + this._searchString + "\""  + "; ";

            if (this._ignoreAllDayEvents)
                descr = descr + context.getString(R.string.event_preferences_calendar_ignore_all_day_events) + "; ";

            String[] availabilities = context.getResources().getStringArray(R.array.eventCalendarAvailabilityArray);
            descr = descr + availabilities[this._availability];

            if (this._startBeforeEvent > 0)
                descr = descr + "; " + context.getString(R.string.event_preferences_calendar_start_before_event) + ": " + GlobalGUIRoutines.getDurationString(this._startBeforeEvent);

            if (addBullet) {
                if (Event.getGlobalEventsRunning(context)) {
                    if (_eventFound) {
                        long alarmTime;
                        //SimpleDateFormat sdf = new SimpleDateFormat("EEd/MM/yy HH:mm");
                        String alarmTimeS;
                        if (_event.getStatus() == Event.ESTATUS_PAUSE) {
                            alarmTime = computeAlarm(true);
                            // date and time format by user system settings configuration
                            alarmTimeS = "(st) " + DateFormat.getDateFormat(context).format(alarmTime) +
                                    " " + DateFormat.getTimeFormat(context).format(alarmTime);
                            descr = descr + "<br>"; //'\n';
                            descr = descr + "&nbsp;&nbsp;&nbsp;-> " + alarmTimeS;
                        } else if (_event.getStatus() == Event.ESTATUS_RUNNING) {
                            alarmTime = computeAlarm(false);
                            // date and time format by user system settings configuration
                            alarmTimeS = "(et) " + DateFormat.getDateFormat(context).format(alarmTime) +
                                    " " + DateFormat.getTimeFormat(context).format(alarmTime);
                            descr = descr + "<br>"; //'\n';
                            descr = descr + "&nbsp;&nbsp;&nbsp;-> " + alarmTimeS;
                        }
                    } else {
                        descr = descr + "<br>"; //'\n';
                        descr = descr + "&nbsp;&nbsp;&nbsp;-> " + context.getResources().getString(R.string.event_preferences_calendar_no_event);
                    }
                }
            }
        }

        return descr;
    }

    @Override
    void setSummary(PreferenceManager prefMng, String key, String value, Context context)
    {
        if (key.equals(PREF_EVENT_CALENDAR_CALENDARS))
        {
            Preference preference = prefMng.findPreference(key);
            if (preference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, false, true, false, false);
            }
        }
        if (key.equals(PREF_EVENT_CALENDAR_SEARCH_FIELD) ||
            key.equals(PREF_EVENT_CALENDAR_AVAILABILITY))
        {
            ListPreference listPreference = (ListPreference)prefMng.findPreference(key);
            if (listPreference != null) {
                int index = listPreference.findIndexOfValue(value);
                CharSequence summary = (index >= 0) ? listPreference.getEntries()[index] : null;
                listPreference.setSummary(summary);
            }
        }
        if (key.equals(PREF_EVENT_CALENDAR_SEARCH_STRING))
        {
            Preference preference = prefMng.findPreference(key);
            if (preference != null) {
                preference.setSummary(value);
                String helpString = context.getString(R.string.pref_dlg_info_about_wildcards_1) + "\n" +
                        context.getString(R.string.pref_dlg_info_about_wildcards_2) + " " +
                        context.getString(R.string.calendar_pref_dlg_info_about_wildcards) + " " +
                        context.getString(R.string.pref_dlg_info_about_wildcards_3) + "\n" +
                        context.getString(R.string.pref_dlg_info_about_wildcards_4)
                        ;
                ((EditTextPreference) preference).setDialogMessage(helpString);
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, false, true, false, false);
            }
        }
    }

    @Override
    public void setSummary(PreferenceManager prefMng, String key, SharedPreferences preferences, Context context)
    {
        if (key.equals(PREF_EVENT_CALENDAR_CALENDARS) ||
            key.equals(PREF_EVENT_CALENDAR_SEARCH_FIELD) ||
            key.equals(PREF_EVENT_CALENDAR_SEARCH_STRING) ||
            key.equals(PREF_EVENT_CALENDAR_AVAILABILITY) ||
            key.equals(PREF_EVENT_CALENDAR_START_BEFORE_EVENT))
        {
            setSummary(prefMng, key, preferences.getString(key, ""), context);
        }
    }

    @Override
    public void setAllSummary(PreferenceManager prefMng, SharedPreferences preferences, Context context)
    {
        setSummary(prefMng, PREF_EVENT_CALENDAR_CALENDARS, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALENDAR_SEARCH_FIELD, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALENDAR_SEARCH_STRING, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALENDAR_AVAILABILITY, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALENDAR_START_BEFORE_EVENT, preferences, context);
    }

    @Override
    public void setCategorySummary(PreferenceManager prefMng, /*String key,*/ SharedPreferences preferences, Context context) {
        if (Event.isEventPreferenceAllowed(PREF_EVENT_CALENDAR_ENABLED, context) == PPApplication.PREFERENCE_ALLOWED) {
            EventPreferencesCalendar tmp = new EventPreferencesCalendar(this._event, this._enabled, this._calendars,
                    this._searchField, this._searchString, this._availability, this._ignoreAllDayEvents, this._startBeforeEvent);
            if (preferences != null)
                tmp.saveSharedPreferences(preferences);

            Preference preference = prefMng.findPreference(PREF_EVENT_CALENDAR_CATEGORY);
            if (preference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, tmp._enabled, false, !tmp.isRunnable(context), false);
                preference.setSummary(GlobalGUIRoutines.fromHtml(tmp.getPreferencesDescription(false, context)));
            }
        }
        else {
            Preference preference = prefMng.findPreference(PREF_EVENT_CALENDAR_CATEGORY);
            if (preference != null) {
                preference.setSummary(context.getResources().getString(R.string.profile_preferences_device_not_allowed)+
                        ": "+ PPApplication.getNotAllowedPreferenceReasonString(context));
                preference.setEnabled(false);
            }
        }
    }

    @Override
    public boolean isRunnable(Context context)
    {

        boolean runnable = super.isRunnable(context);

        runnable = runnable && (!_calendars.isEmpty());
        runnable = runnable && (!_searchString.isEmpty());

        return runnable;
    }

    long computeAlarm(boolean startEvent)
    {
        PPApplication.logE("EventPreferencesCalendar.computeAlarm","startEvent="+startEvent);

        ///// set calendar for startTime and endTime
        Calendar calStartTime = Calendar.getInstance();
        Calendar calEndTime = Calendar.getInstance();

        int gmtOffset = 0; //TimeZone.getDefault().getRawOffset();

        calStartTime.setTimeInMillis(_startTime - gmtOffset);
        calStartTime.set(Calendar.SECOND, -_startBeforeEvent);
        calStartTime.set(Calendar.MILLISECOND, 0);

        calEndTime.setTimeInMillis(_endTime - gmtOffset);
        calEndTime.set(Calendar.SECOND, 0);
        calEndTime.set(Calendar.MILLISECOND, 0);

        long alarmTime;
        if (startEvent)
            alarmTime = calStartTime.getTimeInMillis();
        else
            alarmTime = calEndTime.getTimeInMillis();

        return alarmTime;

    }

    @Override
    public void setSystemEventForStart(Context context)
    {
        // set alarm for state PAUSE

        // this alarm generates broadcast, that change state into RUNNING;
        // from broadcast will by called EventsHandler


        //removeAlarm(true, context);
        removeAlarm(/*false, */context);

        searchEvent(context);

        if (!(isRunnable(context) && _enabled && _eventFound))
            return;

        setAlarm(true, computeAlarm(true), context);
    }

    @Override
    public void setSystemEventForPause(Context context)
    {
        // set alarm for state RUNNING

        // this alarm generates broadcast, that change state into PAUSE;
        // from broadcast will by called EventsHandler

        //removeAlarm(true, context);
        removeAlarm(/*false, */context);

        searchEvent(context);

        if (!(isRunnable(context) && _enabled && _eventFound))
            return;

        setAlarm(false, computeAlarm(false), context);
    }

    @Override
    public void removeSystemEvent(Context context)
    {
        // remove alarms for state STOP

        //removeAlarm(true, context);
        removeAlarm(/*false, */context);

        _eventFound = false;

        PPApplication.logE("EventPreferencesCalendar.removeSystemEvent", "xxx");
    }

    private void removeAlarm(/*boolean startEvent, */Context context)
    {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        if (alarmManager != null) {
            Intent intent = new Intent(context, EventCalendarBroadcastReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), (int) _event._id, intent, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                PPApplication.logE("EventPreferencesCalendar.removeAlarm", "alarm found");

                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    @SuppressLint({"SimpleDateFormat", "NewApi"})
    private void setAlarm(boolean startEvent, long alarmTime, Context context)
    {
        if (PPApplication.logEnabled()) {
            SimpleDateFormat sdf = new SimpleDateFormat("EE d.MM.yyyy HH:mm:ss:S");
            String result = sdf.format(alarmTime);
            if (startEvent)
                PPApplication.logE("EventPreferencesCalendar.setAlarm", "startTime=" + result);
            else
                PPApplication.logE("EventPreferencesCalendar.setAlarm", "endTime=" + result);
        }

        // not set alarm if alarmTime is over.
        Calendar now = Calendar.getInstance();
        if (now.getTimeInMillis() > (alarmTime + Event.EVENT_ALARM_TIME_OFFSET))
            return;

        Intent intent = new Intent(context, EventCalendarBroadcastReceiver.class);

        //intent.putExtra(PPApplication.EXTRA_EVENT_ID, _event._id);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), (int) _event._id, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        if (alarmManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= 23)
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime + Event.EVENT_ALARM_TIME_OFFSET, pendingIntent);
            else if (android.os.Build.VERSION.SDK_INT >= 19)
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime + Event.EVENT_ALARM_TIME_OFFSET, pendingIntent);
            else
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarmTime + Event.EVENT_ALARM_TIME_OFFSET, pendingIntent);
        }
    }

    private void searchEvent(Context context)
    {
        if (!(isRunnable(context) && _enabled && Permissions.checkCalendar(context)))
        {
            _startTime = 0;
            _endTime = 0;
            _eventFound = false;
            DatabaseHandler.getInstance(context).updateEventCalendarTimes(_event);
            return;
        }

        final String[] INSTANCE_PROJECTION = new String[] {
                Instances.BEGIN,           // 0
                Instances.END,			   // 1
                Instances.TITLE,           // 2
                Instances.DESCRIPTION,     // 3
                Instances.CALENDAR_ID,     // 4
                Instances.ALL_DAY,         // 5
                Instances.EVENT_LOCATION,  // 6
                Instances.AVAILABILITY/*,  // 7
            Instances.EVENT_TIMEZONE   // 8 */
        };

        // The indices for the projection array above.
        final int PROJECTION_BEGIN_INDEX = 0;
        final int PROJECTION_END_INDEX = 1;
        //final int PROJECTION_TITLE_INDEX = 2;
        //final int PROJECTION_DESCRIPTION_INDEX = 3;
        final int PROJECTION_CALENDAR_ID_INDEX = 4;
        final int PROJECTION_ALL_DAY_INDEX = 5;
        //final int PROJECTION_EVENT_TIMEZONE_INDEX = 6;

        Cursor cur;
        ContentResolver cr = context.getContentResolver();

        StringBuilder selection = new StringBuilder("(");

        String[] searchStringSplits = _searchString.split("\\|");
        String[] selectionArgs = new String[searchStringSplits.length];
        int argsId = 0;
        selection.append("(");
        for (String split : searchStringSplits) {
            if (!split.isEmpty()) {
                String searchPattern = split;

                // when in searchPattern are not wildcards add %
                if (!(searchPattern.contains("%") || searchPattern.contains("_")))
                    searchPattern = "%"+searchPattern+"%";

                selectionArgs[argsId] = searchPattern;

                if (argsId > 0)
                    selection.append(" OR ");

                switch (_searchField) {
                    case SEARCH_FIELD_TITLE:
                        selection.append("(lower(" + Instances.TITLE + ")" + " LIKE lower(?) ESCAPE '\\')");
                        break;
                    case SEARCH_FIELD_DESCRIPTION:
                        selection.append("(lower(" + Instances.DESCRIPTION + ")" + " LIKE lower(?) ESCAPE '\\')");
                        break;
                    case SEARCH_FIELD_LOCATION:
                        selection.append("(lower(" + Instances.EVENT_LOCATION + ")" + " LIKE lower(?) ESCAPE '\\')");
                        break;
                }

                ++argsId;
            }
        }
        selection.append(")");

        switch (_availability) {
            case AVAILABILITY_BUSY:
                selection.append(" AND (" + Instances.AVAILABILITY + "=" + Instances.AVAILABILITY_BUSY + ")");
                break;
            case AVAILABILITY_FREE:
                selection.append(" AND (" + Instances.AVAILABILITY + "=" + Instances.AVAILABILITY_FREE + ")");
                break;
            case AVAILABILITY_TENTATIVE:
                selection.append(" AND (" + Instances.AVAILABILITY + "=" + Instances.AVAILABILITY_TENTATIVE + ")");
                break;
        }
        selection.append(")");

        /*
        Log.e("** EventPrefCalendar", "selection="+selection);
        for (String arg : selectionArgs)
            Log.e("** EventPrefCalendar", "selectionArgs="+arg);
        */

        // Construct the query with the desired date range.
        Calendar calendar = Calendar.getInstance();
        long now = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, -1);
        long startMillis = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_YEAR, 32);
        long endMillis = calendar.getTimeInMillis();

        Uri.Builder builder = Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        _eventFound = false;
        _startTime = 0;
        _endTime = 0;

        String[] calendarsSplits = _calendars.split("\\|");

        // Submit the query
        try {
            cur = cr.query(builder.build(), INSTANCE_PROJECTION, selection.toString(), selectionArgs, Instances.BEGIN + " ASC");
        } catch (Exception e) {
            cur = null;
        }

        if (cur != null)
        {
            while (cur.moveToNext()) {

                boolean calendarFound = false;
                for (String split : calendarsSplits) {
                    long calendarId = Long.parseLong(split);
                    if (cur.getLong(PROJECTION_CALENDAR_ID_INDEX) == calendarId) {
                        calendarFound = true;
                    }
                }
                if (!calendarFound)
                    continue;

                if ((cur.getInt(PROJECTION_ALL_DAY_INDEX) == 1) && this._ignoreAllDayEvents)
                    continue;

                long beginVal;
                long endVal;
                //String title = null;

                //Log.e("** EventPrefCalendar", "title="+cur.getString(PROJECTION_TITLE_INDEX));

                // Get the field values
                beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
                endVal = cur.getLong(PROJECTION_END_INDEX);

                if (cur.getInt(PROJECTION_ALL_DAY_INDEX) == 1)
                {
                    // get UTC offset
                    Date _now = new Date();
                    int utcOffset = TimeZone.getDefault().getOffset(_now.getTime());

                    beginVal -= utcOffset;
                    endVal -= utcOffset;
                }

                //title = cur.getString(PROJECTION_TITLE_INDEX);

                int gmtOffset = 0; //TimeZone.getDefault().getRawOffset();

                if ((beginVal <= now) && (endVal > now))
                {
                    // event instance is found
                    _eventFound = true;
                    _startTime = beginVal + gmtOffset;
                    _endTime = endVal + gmtOffset;
                    //Log.e("** EventPrefCalendar", "beginVal="+getDate(_startTime));
                    //Log.e("** EventPrefCalendar", "endVal="+getDate(_endTime));
                    break;
                }
                else
                if (beginVal > now)
                {
                    // event instance is found
                    _eventFound = true;
                    _startTime = beginVal + gmtOffset;
                    _endTime = endVal + gmtOffset;
                    //Log.e("** EventPrefCalendar", "beginVal="+getDate(_startTime));
                    //Log.e("** EventPrefCalendar", "endVal="+getDate(_endTime));
                    break;
                }

            }

            cur.close();
        }

        DatabaseHandler.getInstance(context).updateEventCalendarTimes(_event);

    }

    void saveStartEndTime(DataWrapper dataWrapper) {
        //_event._eventPreferencesCalendar.searchEvent(dataWrapper.context);
        //   searchEvent is called from setSystemEventForPause and setSystemEventForStart
        if (_event.getStatus() == Event.ESTATUS_RUNNING)
            _event._eventPreferencesCalendar.setSystemEventForPause(dataWrapper.context);
        if (_event.getStatus() == Event.ESTATUS_PAUSE)
            _event._eventPreferencesCalendar.setSystemEventForStart(dataWrapper.context);
    }

}

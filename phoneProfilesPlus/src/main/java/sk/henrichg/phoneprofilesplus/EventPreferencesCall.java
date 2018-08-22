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

class EventPreferencesCall extends EventPreferences {

    int _callEvent;
    String _contacts;
    String _contactGroups;
    int _contactListType;
    long _startTime;
    boolean _permanentRun;
    int _duration;

    static final String PREF_EVENT_CALL_ENABLED = "eventCallEnabled";
    private static final String PREF_EVENT_CALL_EVENT = "eventCallEvent";
    static final String PREF_EVENT_CALL_CONTACTS = "eventCallContacts";
    static final String PREF_EVENT_CALL_CONTACT_GROUPS = "eventCallContactGroups";
    private static final String PREF_EVENT_CALL_CONTACT_LIST_TYPE = "eventCallContactListType";
    private static final String PREF_EVENT_CALL_PERMANENT_RUN = "eventCallPermanentRun";
    private static final String PREF_EVENT_CALL_DURATION = "eventCallDuration";

    private static final String PREF_EVENT_CALL_CATEGORY = "eventCallCategory";

    static final int CALL_EVENT_RINGING = 0;
    static final int CALL_EVENT_INCOMING_CALL_ANSWERED = 1;
    static final int CALL_EVENT_OUTGOING_CALL_STARTED = 2;
    static final int CALL_EVENT_MISSED_CALL = 3;
    static final int CALL_EVENT_INCOMING_CALL_ENDED = 4;
    static final int CALL_EVENT_OUTGOING_CALL_ENDED = 5;

    static final int CONTACT_LIST_TYPE_WHITE_LIST = 0;
    static final int CONTACT_LIST_TYPE_BLACK_LIST = 1;
    static final int CONTACT_LIST_TYPE_NOT_USE = 2;

    EventPreferencesCall(Event event,
                         boolean enabled,
                         int callEvent,
                         String contacts,
                         String contactGroups,
                         int contactListType,
                         boolean permanentRun,
                         int duration) {
        super(event, enabled);

        this._callEvent = callEvent;
        this._contacts = contacts;
        this._contactGroups = contactGroups;
        this._contactListType = contactListType;
        this._permanentRun = permanentRun;
        this._duration = duration;

        this._startTime = 0;
    }

    @Override
    public void copyPreferences(Event fromEvent) {
        this._enabled = fromEvent._eventPreferencesCall._enabled;
        this._callEvent = fromEvent._eventPreferencesCall._callEvent;
        this._contacts = fromEvent._eventPreferencesCall._contacts;
        this._contactGroups = fromEvent._eventPreferencesCall._contactGroups;
        this._contactListType = fromEvent._eventPreferencesCall._contactListType;
        this._permanentRun = fromEvent._eventPreferencesCall._permanentRun;
        this._duration = fromEvent._eventPreferencesCall._duration;
        this.setSensorPassed(fromEvent._eventPreferencesCall.getSensorPassed());

        this._startTime = 0;
    }

    @Override
    public void loadSharedPreferences(SharedPreferences preferences) {
        Editor editor = preferences.edit();
        editor.putBoolean(PREF_EVENT_CALL_ENABLED, _enabled);
        editor.putString(PREF_EVENT_CALL_EVENT, String.valueOf(this._callEvent));
        editor.putString(PREF_EVENT_CALL_CONTACTS, this._contacts);
        editor.putString(PREF_EVENT_CALL_CONTACT_GROUPS, this._contactGroups);
        editor.putString(PREF_EVENT_CALL_CONTACT_LIST_TYPE, String.valueOf(this._contactListType));
        editor.putBoolean(PREF_EVENT_CALL_PERMANENT_RUN, this._permanentRun);
        editor.putString(PREF_EVENT_CALL_DURATION, String.valueOf(this._duration));
        editor.apply();
    }

    @Override
    public void saveSharedPreferences(SharedPreferences preferences) {
        this._enabled = preferences.getBoolean(PREF_EVENT_CALL_ENABLED, false);
        this._callEvent = Integer.parseInt(preferences.getString(PREF_EVENT_CALL_EVENT, "0"));
        this._contacts = preferences.getString(PREF_EVENT_CALL_CONTACTS, "");
        this._contactGroups = preferences.getString(PREF_EVENT_CALL_CONTACT_GROUPS, "");
        this._contactListType = Integer.parseInt(preferences.getString(PREF_EVENT_CALL_CONTACT_LIST_TYPE, "0"));
        this._permanentRun = preferences.getBoolean(PREF_EVENT_CALL_PERMANENT_RUN, false);
        this._duration = Integer.parseInt(preferences.getString(PREF_EVENT_CALL_DURATION, "5"));
    }

    @Override
    public String getPreferencesDescription(boolean addBullet, boolean addPassStatus, Context context) {
        String descr = "";

        if (!this._enabled) {
            if (!addBullet)
                descr = context.getString(R.string.event_preference_sensor_call_summary);
        } else {
            if (addBullet) {
                descr = descr + "<b>\u2022 ";
                descr = descr + getPassStatusString(context.getString(R.string.event_type_call), addPassStatus, DatabaseHandler.ETYPE_CALL, context);
                descr = descr + ": </b>";
            }

            descr = descr + context.getString(R.string.pref_event_call_event);
            String[] callEvents = context.getResources().getStringArray(R.array.eventCallEventsArray);
            descr = descr + ": " + callEvents[this._callEvent] + "; ";
            descr = descr + context.getString(R.string.pref_event_call_contactListType);
            String[] contactListTypes = context.getResources().getStringArray(R.array.eventCallContactListTypeArray);
            descr = descr + ": " + contactListTypes[this._contactListType];

            if ((this._callEvent == CALL_EVENT_MISSED_CALL) ||
                    (this._callEvent == CALL_EVENT_INCOMING_CALL_ENDED) ||
                    (this._callEvent == CALL_EVENT_OUTGOING_CALL_ENDED)) {
                if (this._permanentRun)
                    descr = descr + "; " + context.getString(R.string.pref_event_permanentRun);
                else
                    descr = descr + "; " + context.getString(R.string.pref_event_duration) + ": " + GlobalGUIRoutines.getDurationString(this._duration);
            }
        }

        return descr;
    }

    @Override
    void setSummary(PreferenceManager prefMng, String key, String value, Context context) {
        if (key.equals(PREF_EVENT_CALL_ENABLED)) {
            CheckBoxPreference preference = (CheckBoxPreference) prefMng.findPreference(key);
            if (preference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, true, preference.isChecked(), false, false, false);
            }
        }

        if (key.equals(PREF_EVENT_CALL_EVENT) || key.equals(PREF_EVENT_CALL_CONTACT_LIST_TYPE)) {
            ListPreference listPreference = (ListPreference) prefMng.findPreference(key);
            if (listPreference != null) {
                int index = listPreference.findIndexOfValue(value);
                CharSequence summary = (index >= 0) ? listPreference.getEntries()[index] : null;
                listPreference.setSummary(summary);
            }
        }
        if (key.equals(PREF_EVENT_CALL_EVENT)) {
            ListPreference listPreference = (ListPreference) prefMng.findPreference(key);
            if (listPreference != null) {
                Preference preferenceDuration = prefMng.findPreference(PREF_EVENT_CALL_DURATION);
                Preference preferencePermanentRun = prefMng.findPreference(PREF_EVENT_CALL_PERMANENT_RUN);
                if (preferenceDuration != null) {
                    boolean enabled = value.equals(String.valueOf(CALL_EVENT_MISSED_CALL)) ||
                            value.equals(String.valueOf(CALL_EVENT_INCOMING_CALL_ENDED)) ||
                            value.equals(String.valueOf(CALL_EVENT_OUTGOING_CALL_ENDED));
                    SharedPreferences preferences = prefMng.getSharedPreferences();
                    enabled = enabled && !preferences.getBoolean(PREF_EVENT_CALL_PERMANENT_RUN, false);
                    preferenceDuration.setEnabled(enabled);
                }
                if (preferencePermanentRun != null)
                    preferencePermanentRun.setEnabled(value.equals(String.valueOf(CALL_EVENT_MISSED_CALL)) ||
                            value.equals(String.valueOf(CALL_EVENT_INCOMING_CALL_ENDED)) ||
                            value.equals(String.valueOf(CALL_EVENT_OUTGOING_CALL_ENDED))
                    );
            }
        }
        if (key.equals(PREF_EVENT_CALL_PERMANENT_RUN)) {
            CheckBoxPreference permanentRunPreference = (CheckBoxPreference) prefMng.findPreference(key);
            if (permanentRunPreference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(permanentRunPreference, true, permanentRunPreference.isChecked(), false, false, false);
            }
            SharedPreferences preferences = prefMng.getSharedPreferences();
            String callEvent = preferences.getString(PREF_EVENT_CALL_EVENT, "-1");
            if (!callEvent.equals(String.valueOf(CALL_EVENT_MISSED_CALL)) &&
                    !callEvent.equals(String.valueOf(CALL_EVENT_INCOMING_CALL_ENDED)) &&
                    !callEvent.equals(String.valueOf(CALL_EVENT_OUTGOING_CALL_ENDED))) {
                Preference preference = prefMng.findPreference(PREF_EVENT_CALL_DURATION);
                if (preference != null) {
                    preference.setEnabled(false);
                }
            } else {
                Preference preference = prefMng.findPreference(PREF_EVENT_CALL_DURATION);
                if (preference != null) {
                    preference.setEnabled(value.equals("false"));
                }
            }
        }
        if (key.equals(PREF_EVENT_CALL_DURATION)) {
            Preference preference = prefMng.findPreference(key);
            int delay;
            try {
                delay = Integer.parseInt(value);
            } catch (Exception e) {
                delay = 0;
            }
            GlobalGUIRoutines.setPreferenceTitleStyle(preference, true, delay > 5, false, false, false);
        }

        Event event = new Event();
        event.createEventPreferences();
        event._eventPreferencesCall.saveSharedPreferences(prefMng.getSharedPreferences());
        boolean isRunnable = event._eventPreferencesCall.isRunnable(context);
        CheckBoxPreference enabledPreference = (CheckBoxPreference)prefMng.findPreference(PREF_EVENT_CALL_ENABLED);
        boolean enabled = (enabledPreference != null) && enabledPreference.isChecked();
        Preference preference = prefMng.findPreference(PREF_EVENT_CALL_CONTACT_GROUPS);
        if (preference != null) {
            boolean bold = !prefMng.getSharedPreferences().getString(PREF_EVENT_CALL_CONTACT_GROUPS, "").isEmpty();
            GlobalGUIRoutines.setPreferenceTitleStyle(preference, enabled, bold, true, !isRunnable, false);
        }
        preference = prefMng.findPreference(PREF_EVENT_CALL_CONTACTS);
        if (preference != null) {
            boolean bold = !prefMng.getSharedPreferences().getString(PREF_EVENT_CALL_CONTACTS, "").isEmpty();
            GlobalGUIRoutines.setPreferenceTitleStyle(preference, enabled, bold, true, !isRunnable, false);
        }
        preference = prefMng.findPreference(PREF_EVENT_CALL_CONTACT_LIST_TYPE);
        if (preference != null)
            GlobalGUIRoutines.setPreferenceTitleStyle(preference, enabled, false, true, !isRunnable, false);
    }

    @Override
    public void setSummary(PreferenceManager prefMng, String key, SharedPreferences preferences, Context context) {
        if (key.equals(PREF_EVENT_CALL_ENABLED) ||
                key.equals(PREF_EVENT_CALL_PERMANENT_RUN)) {
            boolean value = preferences.getBoolean(key, false);
            setSummary(prefMng, key, value ? "true" : "false", context);
        }
        if (key.equals(PREF_EVENT_CALL_EVENT) ||
                key.equals(PREF_EVENT_CALL_CONTACT_LIST_TYPE) ||
                key.equals(PREF_EVENT_CALL_CONTACTS) ||
                key.equals(PREF_EVENT_CALL_CONTACT_GROUPS) ||
                key.equals(PREF_EVENT_CALL_DURATION)) {
            setSummary(prefMng, key, preferences.getString(key, ""), context);
        }
    }

    @Override
    public void setAllSummary(PreferenceManager prefMng, SharedPreferences preferences, Context context) {
        setSummary(prefMng, PREF_EVENT_CALL_ENABLED, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALL_EVENT, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALL_CONTACT_LIST_TYPE, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALL_CONTACTS, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALL_CONTACT_GROUPS, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALL_PERMANENT_RUN, preferences, context);
        setSummary(prefMng, PREF_EVENT_CALL_DURATION, preferences, context);
    }

    @Override
    public void setCategorySummary(PreferenceManager prefMng, /*String key,*/ SharedPreferences preferences, Context context) {
        PreferenceAllowed preferenceAllowed = Event.isEventPreferenceAllowed(PREF_EVENT_CALL_ENABLED, context);
        if (preferenceAllowed.allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
            EventPreferencesCall tmp = new EventPreferencesCall(this._event, this._enabled, this._callEvent, this._contacts, this._contactGroups,
                    this._contactListType, this._permanentRun, this._duration);
            if (preferences != null)
                tmp.saveSharedPreferences(preferences);

            Preference preference = prefMng.findPreference(PREF_EVENT_CALL_CATEGORY);
            if (preference != null) {
                CheckBoxPreference enabledPreference = (CheckBoxPreference)prefMng.findPreference(PREF_EVENT_CALL_ENABLED);
                boolean enabled = (enabledPreference != null) && enabledPreference.isChecked();
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, enabled, tmp._enabled, false, !tmp.isRunnable(context), false);
                preference.setSummary(GlobalGUIRoutines.fromHtml(tmp.getPreferencesDescription(false, false, context)));
            }
        } else {
            Preference preference = prefMng.findPreference(PREF_EVENT_CALL_CATEGORY);
            if (preference != null) {
                preference.setSummary(context.getResources().getString(R.string.profile_preferences_device_not_allowed) +
                        ": " + preferenceAllowed.getNotAllowedPreferenceReasonString(context));
                preference.setEnabled(false);
            }
        }
    }

    @Override
    public boolean isRunnable(Context context) {

        boolean runnable = super.isRunnable(context);

        runnable = runnable && ((_contactListType == CONTACT_LIST_TYPE_NOT_USE) ||
                (!(_contacts.isEmpty() && _contactGroups.isEmpty())));

        return runnable;
    }

    long computeAlarm() {
        PPApplication.logE("EventPreferencesCall.computeAlarm", "xxx");

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
    public void setSystemEventForStart(Context context) {
        // set alarm for state PAUSE

        // this alarm generates broadcast, that will change state into RUNNING;
        // from broadcast will by called EventsHandler

        PPApplication.logE("EventPreferencesCall.setSystemRunningEvent", "xxx");

        Context _context = context;
        if (PhoneProfilesService.getInstance() != null)
            _context = PhoneProfilesService.getInstance();

        removeAlarm(_context);
    }

    @Override
    public void setSystemEventForPause(Context context) {
        // set alarm for state RUNNING

        // this alarm generates broadcast, that will change state into PAUSE;
        // from broadcast will by called EventsHandler

        PPApplication.logE("EventPreferencesCall.setSystemPauseEvent", "xxx");

        Context _context = context;
        if (PhoneProfilesService.getInstance() != null)
            _context = PhoneProfilesService.getInstance();

        removeAlarm(_context);

        if (!(isRunnable(_context) && _enabled))
            return;

        if ((_callEvent == CALL_EVENT_MISSED_CALL) ||
                (_callEvent == CALL_EVENT_INCOMING_CALL_ENDED) ||
                (_callEvent == CALL_EVENT_OUTGOING_CALL_ENDED))
            setAlarm(computeAlarm(), _context);
    }

    @Override
    public void removeSystemEvent(Context context) {
        Context _context = context;
        if (PhoneProfilesService.getInstance() != null)
            _context = PhoneProfilesService.getInstance();

        removeAlarm(_context);

        PPApplication.logE("EventPreferencesCall.removeSystemEvent", "xxx");
    }

    private void removeAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        if (alarmManager != null) {
            //Intent intent = new Intent(context, MissedCallEventEndBroadcastReceiver.class);
            Intent intent = new Intent();
            intent.setAction(PhoneProfilesService.ACTION_MISSED_CALL_EVENT_END_BROADCAST_RECEIVER);
            //intent.setClass(context, MissedCallEventEndBroadcastReceiver.class);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) _event._id, intent, PendingIntent.FLAG_NO_CREATE);
            if (pendingIntent != null) {
                PPApplication.logE("EventPreferencesCall.removeAlarm", "alarm found");

                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        }
    }

    @SuppressLint("NewApi")
    private void setAlarm(long alarmTime, Context context) {
        if (!_permanentRun) {
            if (_startTime > 0) {
                if (PPApplication.logEnabled()) {
                    @SuppressLint("SimpleDateFormat")
                    SimpleDateFormat sdf = new SimpleDateFormat("EE d.MM.yyyy HH:mm:ss:S");
                    String result = sdf.format(alarmTime);
                    PPApplication.logE("EventPreferencesCall.setAlarm", "endTime=" + result);
                }

                //Intent intent = new Intent(context, MissedCallEventEndBroadcastReceiver.class);
                Intent intent = new Intent();
                intent.setAction(PhoneProfilesService.ACTION_MISSED_CALL_EVENT_END_BROADCAST_RECEIVER);
                //intent.setClass(context, MissedCallEventEndBroadcastReceiver.class);

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

    void saveStartTime(DataWrapper dataWrapper) {
        PPApplication.logE("EventPreferencesCall.saveStartTime", "_startTime=" + _startTime);
        if (this._startTime == 0) {
            // alarm for end is not set
            if (Permissions.checkContacts(dataWrapper.context)) {
                PPApplication.logE("EventPreferencesCall.saveStartTime", "contacts permission granted");

                ApplicationPreferences.getSharedPreferences(dataWrapper.context);
                int callEventType = ApplicationPreferences.preferences.getInt(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TYPE, PhoneCallBroadcastReceiver.CALL_EVENT_UNDEFINED);
                long callTime = ApplicationPreferences.preferences.getLong(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TIME, 0);
                String phoneNumber = ApplicationPreferences.preferences.getString(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_PHONE_NUMBER, "");
                PPApplication.logE("EventPreferencesCall.saveStartTime", "callEventType=" + callEventType);
                PPApplication.logE("EventPreferencesCall.saveStartTime", "callTime=" + callTime);
                PPApplication.logE("EventPreferencesCall.saveStartTime", "phoneNumber=" + phoneNumber);

                if (((_callEvent == EventPreferencesCall.CALL_EVENT_MISSED_CALL) && (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_MISSED_CALL)) ||
                    ((_callEvent == EventPreferencesCall.CALL_EVENT_INCOMING_CALL_ENDED) && (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_ENDED)) ||
                    ((_callEvent == EventPreferencesCall.CALL_EVENT_OUTGOING_CALL_ENDED) && (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_OUTGOING_CALL_ENDED))) {

                    boolean phoneNumberFound = false;

                    if (this._contactListType != EventPreferencesCall.CONTACT_LIST_TYPE_NOT_USE) {
                        // find phone number in groups
                        String[] splits = this._contactGroups.split("\\|");
                        for (String split : splits) {
                            String[] projection = new String[]{ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID};
                            String selection = ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID + "=? AND "
                                    + ContactsContract.CommonDataKinds.GroupMembership.MIMETYPE + "='"
                                    + ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE + "'";
                            String[] selectionArgs = new String[]{split};
                            Cursor mCursor = dataWrapper.context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);
                            if (mCursor != null) {
                                while (mCursor.moveToNext()) {
                                    String contactId = mCursor.getString(mCursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID));
                                    String[] projection2 = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};
                                    String selection2 = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?" + " and " +
                                            ContactsContract.CommonDataKinds.Phone.HAS_PHONE_NUMBER + "=1";
                                    String[] selection2Args = new String[]{contactId};
                                    Cursor phones = dataWrapper.context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection2, selection2, selection2Args, null);
                                    if (phones != null) {
                                        while (phones.moveToNext()) {
                                            String _phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                            if (PhoneNumberUtils.compare(_phoneNumber, phoneNumber)) {
                                                phoneNumberFound = true;
                                                break;
                                            }
                                        }
                                        phones.close();
                                    }
                                    if (phoneNumberFound)
                                        break;
                                }
                                mCursor.close();
                            }
                            if (phoneNumberFound)
                                break;
                        }

                        if (!phoneNumberFound) {
                            // find phone number in contacts
                            splits = this._contacts.split("\\|");
                            for (String split : splits) {
                                String[] splits2 = split.split("#");

                                // get phone number from contacts
                                String[] projection = new String[]{ContactsContract.Contacts._ID, ContactsContract.Contacts.HAS_PHONE_NUMBER};
                                String selection = ContactsContract.Contacts.HAS_PHONE_NUMBER + "='1' and " + ContactsContract.Contacts._ID + "=?";
                                String[] selectionArgs = new String[]{splits2[0]};
                                Cursor mCursor = dataWrapper.context.getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, selection, selectionArgs, null);
                                if (mCursor != null) {
                                    while (mCursor.moveToNext()) {
                                        String[] projection2 = new String[]{ContactsContract.CommonDataKinds.Phone._ID, ContactsContract.CommonDataKinds.Phone.NUMBER};
                                        String selection2 = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?" + " and " + ContactsContract.CommonDataKinds.Phone._ID + "=?";
                                        String[] selection2Args = new String[]{splits2[0], splits2[1]};
                                        Cursor phones = dataWrapper.context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection2, selection2, selection2Args, null);
                                        if (phones != null) {
                                            while (phones.moveToNext()) {
                                                String _phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                                                if (PhoneNumberUtils.compare(_phoneNumber, phoneNumber)) {
                                                    phoneNumberFound = true;
                                                    break;
                                                }
                                            }
                                            phones.close();
                                        }
                                        if (phoneNumberFound)
                                            break;
                                    }
                                    mCursor.close();
                                }
                                if (phoneNumberFound)
                                    break;
                            }
                        }

                        if (this._contactListType == EventPreferencesCall.CONTACT_LIST_TYPE_BLACK_LIST)
                            phoneNumberFound = !phoneNumberFound;
                    } else
                        phoneNumberFound = true;

                    if (phoneNumberFound)
                        this._startTime = callTime + (10 * 1000);
                    else
                        this._startTime = 0;
                    PPApplication.logE("EventPreferencesCall.saveStartTime", "_startTime=" + _startTime);

                    DatabaseHandler.getInstance(dataWrapper.context).updateCallStartTime(_event);

                    if (phoneNumberFound) {
                        //if (_event.getStatus() == Event.ESTATUS_RUNNING)
                            setSystemEventForPause(dataWrapper.context);
                    }
                }/* else {
                    PPApplication.logE("EventPreferencesCall.saveStartTime", "_startTime NOT set");

                    _startTime = 0;
                    DatabaseHandler.getInstance(dataWrapper.context).updateCallStartTime(_event);
                }*/
            } else {
                PPApplication.logE("EventPreferencesCall.saveStartTime", "contacts permission NOT granted");

                _startTime = 0;
                DatabaseHandler.getInstance(dataWrapper.context).updateCallStartTime(_event);
            }
        }
    }

}

package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;

import java.util.Arrays;

class EventPreferencesScreen extends EventPreferences {

    int _eventType;
    boolean _whenUnlocked;

    static final int ETYPE_SCREENON = 0;
    //static final int ETYPE_SCREENOFF = 1;

    static final String PREF_EVENT_SCREEN_ENABLED = "eventScreenEnabled";
    private static final String PREF_EVENT_SCREEN_EVENT_TYPE = "eventScreenEventType";
    private static final String PREF_EVENT_SCREEN_WHEN_UNLOCKED = "eventScreenWhenUnlocked";

    private static final String PREF_EVENT_SCREEN_CATEGORY = "eventScreenCategory";

    EventPreferencesScreen(Event event,
                                    boolean enabled,
                                    int eventType,
                                    boolean whenUnlocked)
    {
        super(event, enabled);

        this._eventType = eventType;
        this._whenUnlocked = whenUnlocked;
    }

    @Override
    public void copyPreferences(Event fromEvent)
    {
        this._enabled = fromEvent._eventPreferencesScreen._enabled;
        this._eventType = fromEvent._eventPreferencesScreen._eventType;
        this._whenUnlocked = fromEvent._eventPreferencesScreen._whenUnlocked;
    }

    @Override
    public void loadSharedPreferences(SharedPreferences preferences)
    {
        Editor editor = preferences.edit();
        editor.putBoolean(PREF_EVENT_SCREEN_ENABLED, _enabled);
        editor.putString(PREF_EVENT_SCREEN_EVENT_TYPE, String.valueOf(this._eventType));
        editor.putBoolean(PREF_EVENT_SCREEN_WHEN_UNLOCKED, _whenUnlocked);
        editor.apply();
    }

    @Override
    public void saveSharedPreferences(SharedPreferences preferences)
    {
        this._enabled = preferences.getBoolean(PREF_EVENT_SCREEN_ENABLED, false);
        this._eventType = Integer.parseInt(preferences.getString(PREF_EVENT_SCREEN_EVENT_TYPE, "1"));
        this._whenUnlocked = preferences.getBoolean(PREF_EVENT_SCREEN_WHEN_UNLOCKED, false);
    }

    @Override
    public String getPreferencesDescription(boolean addBullet, Context context)
    {
        String descr = "";

        if (!this._enabled) {
            if (!addBullet)
                descr = context.getString(R.string.event_preference_sensor_screen_summary);
        }
        else
        {
            if (addBullet) {
                descr = descr + "<b>\u2022 </b>";
                descr = descr + "<b>" + context.getString(R.string.event_type_screen) + ": " + "</b>";
            }

            String[] eventListTypeNames = context.getResources().getStringArray(R.array.eventScreenEventTypeArray);
            String[] eventListTypes = context.getResources().getStringArray(R.array.eventScreenEventTypeValues);
            int index = Arrays.asList(eventListTypes).indexOf(Integer.toString(this._eventType));
            descr = descr + eventListTypeNames[index];
            if (this._whenUnlocked)
            {
                if (this._eventType == 0)
                    descr = descr + "; " + context.getString(R.string.pref_event_screen_startWhenUnlocked);
                else
                    descr = descr + "; " + context.getString(R.string.pref_event_screen_endWhenUnlocked);
            }
        }

        return descr;
    }

    @Override
    void setSummary(PreferenceManager prefMng, String key, String value, Context context)
    {
        if (key.equals(PREF_EVENT_SCREEN_EVENT_TYPE))
        {
            ListPreference listPreference = (ListPreference)prefMng.findPreference(key);
            if (listPreference != null) {
                int index = listPreference.findIndexOfValue(value);
                CharSequence summary = (index >= 0) ? listPreference.getEntries()[index] : null;
                listPreference.setSummary(summary);
            }
        }
    }

    @Override
    public void setSummary(PreferenceManager prefMng, String key, SharedPreferences preferences, Context context)
    {
        if (key.equals(PREF_EVENT_SCREEN_EVENT_TYPE))
        {
            setSummary(prefMng, key, preferences.getString(key, ""), context);
        }
    }

    @Override
    public void setAllSummary(PreferenceManager prefMng, SharedPreferences preferences, Context context)
    {
        setSummary(prefMng, PREF_EVENT_SCREEN_EVENT_TYPE, preferences, context);

        setWhenUnlockedTitle(prefMng, _eventType);
    }

    @Override
    public void setCategorySummary(PreferenceManager prefMng, /*String key,*/ SharedPreferences preferences, Context context) {
        PreferenceAllowed preferenceAllowed = Event.isEventPreferenceAllowed(PREF_EVENT_SCREEN_ENABLED, context);
        if (preferenceAllowed.allowed == PPApplication.PREFERENCE_ALLOWED) {
            EventPreferencesScreen tmp = new EventPreferencesScreen(this._event, this._enabled, this._eventType, this._whenUnlocked);
            if (preferences != null)
                tmp.saveSharedPreferences(preferences);

            Preference preference = prefMng.findPreference(PREF_EVENT_SCREEN_CATEGORY);
            if (preference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, tmp._enabled, false, !tmp.isRunnable(context), false);
                preference.setSummary(GlobalGUIRoutines.fromHtml(tmp.getPreferencesDescription(false, context)));
            }
        }
        else {
            Preference preference = prefMng.findPreference(PREF_EVENT_SCREEN_CATEGORY);
            if (preference != null) {
                preference.setSummary(context.getResources().getString(R.string.profile_preferences_device_not_allowed)+
                        ": "+ PPApplication.getNotAllowedPreferenceReasonString(context, preferenceAllowed));
                preference.setEnabled(false);
            }
        }
    }

    @Override
    public void checkPreferences(PreferenceManager prefMng, Context context)
    {
        final Preference eventTypePreference = prefMng.findPreference(PREF_EVENT_SCREEN_EVENT_TYPE);
        final PreferenceManager _prefMng = prefMng;

        if (eventTypePreference != null) {
            eventTypePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String sNewValue = (String) newValue;
                    int iNewValue;
                    if (sNewValue.isEmpty())
                        iNewValue = 100;
                    else
                        iNewValue = Integer.parseInt(sNewValue);

                    setWhenUnlockedTitle(_prefMng, iNewValue);

                    return true;
                }
            });
        }
    }

    private void setWhenUnlockedTitle(PreferenceManager prefMng, int value)
    {
        final CheckBoxPreference whenUnlockedPreference = (CheckBoxPreference)prefMng.findPreference(PREF_EVENT_SCREEN_WHEN_UNLOCKED);

        if (whenUnlockedPreference != null) {
            if (value == 0)
                whenUnlockedPreference.setTitle(R.string.event_preferences_screen_start_when_unlocked);
            else
                whenUnlockedPreference.setTitle(R.string.event_preferences_screen_end_when_unlocked);
        }
    }

    /*
    @Override
    public void setSystemEventForStart(Context context)
    {
    }

    @Override
    public void setSystemEventForPause(Context context)
    {
    }

    @Override
    public void removeSystemEvent(Context context)
    {
    }
    */
}

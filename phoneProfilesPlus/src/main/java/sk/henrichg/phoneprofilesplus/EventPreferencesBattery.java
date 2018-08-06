package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.widget.Toast;

class EventPreferencesBattery extends EventPreferences {

    int _levelLow;
    int _levelHight;
    int _charging;
    boolean _powerSaveMode;

    static final String PREF_EVENT_BATTERY_ENABLED = "eventBatteryEnabled";
    private static final String PREF_EVENT_BATTERY_LEVEL_LOW = "eventBatteryLevelLow";
    private static final String PREF_EVENT_BATTERY_LEVEL_HIGHT = "eventBatteryLevelHight";
    private static final String PREF_EVENT_BATTERY_CHARGING = "eventBatteryCharging";
    private static final String PREF_EVENT_BATTERY_POWER_SAVE_MODE = "eventBatteryPowerSaveMode";

    private static final String PREF_EVENT_BATTERY_CATEGORY = "eventBatteryCategory";

    EventPreferencesBattery(Event event,
                                    boolean enabled,
                                    int levelLow,
                                    int levelHight,
                                    int charging,
                                    boolean powerSaveMode)
    {
        super(event, enabled);

        this._levelLow = levelLow;
        this._levelHight = levelHight;
        this._charging = charging;
        this._powerSaveMode = powerSaveMode;
    }

    @Override
    public void copyPreferences(Event fromEvent)
    {
        super.copyPreferences(fromEvent);

        this._enabled = fromEvent._eventPreferencesBattery._enabled;
        this._levelLow = fromEvent._eventPreferencesBattery._levelLow;
        this._levelHight = fromEvent._eventPreferencesBattery._levelHight;
        this._charging = fromEvent._eventPreferencesBattery._charging;
        this._powerSaveMode = fromEvent._eventPreferencesBattery._powerSaveMode;
    }

    @Override
    public void loadSharedPreferences(SharedPreferences preferences)
    {
        Editor editor = preferences.edit();
        editor.putBoolean(PREF_EVENT_BATTERY_ENABLED, _enabled);
        editor.putString(PREF_EVENT_BATTERY_LEVEL_LOW, String.valueOf(this._levelLow));
        editor.putString(PREF_EVENT_BATTERY_LEVEL_HIGHT, String.valueOf(this._levelHight));
        //editor.putBoolean(PREF_EVENT_BATTERY_CHARGING, this._charging);
        editor.putString(PREF_EVENT_BATTERY_CHARGING, String.valueOf(this._charging));
        editor.putBoolean(PREF_EVENT_BATTERY_POWER_SAVE_MODE, this._powerSaveMode);
        editor.apply();
    }

    @Override
    public void saveSharedPreferences(SharedPreferences preferences)
    {
        this._enabled = preferences.getBoolean(PREF_EVENT_BATTERY_ENABLED, false);

        String sLevel;
        int iLevel;

        sLevel = preferences.getString(PREF_EVENT_BATTERY_LEVEL_LOW, "0");
        if (sLevel.isEmpty()) sLevel = "0";
        iLevel = Integer.parseInt(sLevel);
        if ((iLevel < 0) || (iLevel > 100)) iLevel = 0;
        this._levelLow= iLevel;

        sLevel = preferences.getString(PREF_EVENT_BATTERY_LEVEL_HIGHT, "100");
        if (sLevel.isEmpty()) sLevel = "100";
        iLevel = Integer.parseInt(sLevel);
        if ((iLevel < 0) || (iLevel > 100)) iLevel = 100;
        this._levelHight= iLevel;

        //this._charging = preferences.getBoolean(PREF_EVENT_BATTERY_CHARGING, false);
        this._charging = Integer.parseInt(preferences.getString(PREF_EVENT_BATTERY_CHARGING, "0"));
        this._powerSaveMode = preferences.getBoolean(PREF_EVENT_BATTERY_POWER_SAVE_MODE, false);
    }

    @Override
    public String getPreferencesDescription(boolean addBullet, boolean addPassStatus, Context context)
    {
        String descr = "";

        if (!this._enabled) {
            if (!addBullet)
                descr = context.getString(R.string.event_preference_sensor_battery_summary);
        }
        else
        {
            if (addBullet) {
                descr = descr + "<b>\u2022 ";
                if (addPassStatus && (this._event != null) && (this._event.getStatus() != Event.ESTATUS_STOP))
                    descr = descr + getPassStatusString(context);
                descr = descr + context.getString(R.string.event_type_battery) + ": </b>";
            }

            descr = descr + context.getString(R.string.pref_event_battery_level);
            descr = descr + ": " + this._levelLow + "% - " + this._levelHight + "%";

            if (this._powerSaveMode)
                descr = descr + "; " + context.getString(R.string.pref_event_battery_power_save_mode);
            else {
                descr = descr + "; " + context.getString(R.string.pref_event_battery_charging);
                String[] charging = context.getResources().getStringArray(R.array.eventBatteryChargingArray);
                descr = descr + ": " + charging[this._charging];
            }
        }

        return descr;
    }

    @Override
    void setSummary(PreferenceManager prefMng, String key, String value, Context context)
    {
        if (key.equals(PREF_EVENT_BATTERY_LEVEL_LOW) || key.equals(PREF_EVENT_BATTERY_LEVEL_HIGHT))
        {
            Preference preference = prefMng.findPreference(key);
            if (preference != null)
                preference.setSummary(value + "%");
        }
        if (key.equals(PREF_EVENT_BATTERY_CHARGING)) {
            ListPreference listPreference = (ListPreference) prefMng.findPreference(key);
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
        if (key.equals(PREF_EVENT_BATTERY_LEVEL_LOW) ||
            key.equals(PREF_EVENT_BATTERY_LEVEL_HIGHT) ||
            key.equals(PREF_EVENT_BATTERY_CHARGING))
        {
            setSummary(prefMng, key, preferences.getString(key, ""), context);
        }
    }

    @Override
    public void setAllSummary(PreferenceManager prefMng, SharedPreferences preferences, Context context)
    {
        setSummary(prefMng, PREF_EVENT_BATTERY_LEVEL_LOW, preferences, context);
        setSummary(prefMng, PREF_EVENT_BATTERY_LEVEL_HIGHT, preferences, context);
        setSummary(prefMng, PREF_EVENT_BATTERY_CHARGING, preferences, context);
    }

    @Override
    public void setCategorySummary(PreferenceManager prefMng, /*String key,*/ SharedPreferences preferences, Context context) {
        PreferenceAllowed preferenceAllowed = Event.isEventPreferenceAllowed(PREF_EVENT_BATTERY_ENABLED, context);
        if (preferenceAllowed.allowed == PreferenceAllowed.PREFERENCE_ALLOWED) {
            EventPreferencesBattery tmp = new EventPreferencesBattery(this._event, this._enabled, this._levelLow, this._levelHight, this._charging, this._powerSaveMode);
            if (preferences != null)
                tmp.saveSharedPreferences(preferences);

            Preference preference = prefMng.findPreference(PREF_EVENT_BATTERY_CATEGORY);
            if (preference != null) {
                GlobalGUIRoutines.setPreferenceTitleStyle(preference, tmp._enabled, false, !tmp.isRunnable(context), false);
                preference.setSummary(GlobalGUIRoutines.fromHtml(tmp.getPreferencesDescription(false, false, context)));
            }
        }
        else {
            Preference preference = prefMng.findPreference(PREF_EVENT_BATTERY_CATEGORY);
            if (preference != null) {
                preference.setSummary(context.getResources().getString(R.string.profile_preferences_device_not_allowed)+
                        ": "+ preferenceAllowed.getNotAllowedPreferenceReasonString(context));
                preference.setEnabled(false);
            }
        }
    }

    @Override
    public void checkPreferences(PreferenceManager prefMng, Context context)
    {
        final Preference lowLevelPreference = prefMng.findPreference(PREF_EVENT_BATTERY_LEVEL_LOW);
        final Preference hightLevelPreference = prefMng.findPreference(PREF_EVENT_BATTERY_LEVEL_HIGHT);
        final MaterialListPreference chargingPreference = (MaterialListPreference)prefMng.findPreference(PREF_EVENT_BATTERY_CHARGING);
        final CheckBoxPreference powerSaveModePreference = (CheckBoxPreference)prefMng.findPreference(PREF_EVENT_BATTERY_POWER_SAVE_MODE);
        final PreferenceManager _prefMng = prefMng;
        final Context _context = context.getApplicationContext();

        if (lowLevelPreference != null) {
            lowLevelPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String sNewValue = (String) newValue;
                    int iNewValue;
                    if (sNewValue.isEmpty())
                        iNewValue = 0;
                    else
                        iNewValue = Integer.parseInt(sNewValue);

                    String sHightLevelValue = _prefMng.getSharedPreferences().getString(PREF_EVENT_BATTERY_LEVEL_HIGHT, "100");
                    int iHightLevelValue;
                    if (sHightLevelValue.isEmpty())
                        iHightLevelValue = 100;
                    else
                        iHightLevelValue = Integer.parseInt(sHightLevelValue);

                    boolean OK = ((iNewValue >= 0) && (iNewValue <= iHightLevelValue));

                    if (!OK) {
                        Toast msg = Toast.makeText(_context,
                                _context.getResources().getString(R.string.event_preferences_battery_level_low) + ": " +
                                        _context.getResources().getString(R.string.event_preferences_battery_level_bad_value),
                                Toast.LENGTH_SHORT);
                        msg.show();
                    }

                    return OK;
                }
            });
        }

        if (hightLevelPreference != null) {
            hightLevelPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String sNewValue = (String) newValue;
                    int iNewValue;
                    if (sNewValue.isEmpty())
                        iNewValue = 100;
                    else
                        iNewValue = Integer.parseInt(sNewValue);

                    String sLowLevelValue = _prefMng.getSharedPreferences().getString(PREF_EVENT_BATTERY_LEVEL_LOW, "0");
                    int iLowLevelValue;
                    if (sLowLevelValue.isEmpty())
                        iLowLevelValue = 0;
                    else
                        iLowLevelValue = Integer.parseInt(sLowLevelValue);

                    boolean OK = ((iNewValue >= iLowLevelValue) && (iNewValue <= 100));

                    if (!OK) {
                        Toast msg = Toast.makeText(_context,
                                _context.getResources().getString(R.string.event_preferences_battery_level_hight) + ": " +
                                        _context.getResources().getString(R.string.event_preferences_battery_level_bad_value),
                                Toast.LENGTH_SHORT);
                        msg.show();
                    }

                    return OK;
                }
            });
        }

        if ((chargingPreference != null) && (powerSaveModePreference != null)) {
            chargingPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String sNewValue = (String) newValue;
                    if (!sNewValue.equals("0"))
                        powerSaveModePreference.setChecked(false);
                    return true;
                }
            });
            powerSaveModePreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean bNewValue = (boolean) newValue;
                    if (bNewValue)
                        chargingPreference.setValue("0");
                    return true;
                }
            });
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

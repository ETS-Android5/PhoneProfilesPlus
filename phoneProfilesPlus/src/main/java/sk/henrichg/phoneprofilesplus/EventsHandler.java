package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import android.telephony.TelephonyManager;

import java.util.List;

class EventsHandler {
    
    private final Context context;
    private DataWrapper dataWrapper;
    private String sensorType;

    private int callEventType;
    private static int oldRingerMode;
    private static int oldSystemRingerMode;
    private static int oldZenMode;
    private static String oldRingtone;
    //private static String oldNotificationTone;
    private static int oldSystemRingerVolume;

    private String eventSMSPhoneNumber;
    private long eventSMSDate;
    //private String eventNotificationPostedRemoved;
    private String eventNFCTagName;
    private long eventNFCDate;
    private long eventAlarmClockDate;

    static final String SENSOR_TYPE_RADIO_SWITCH = "radioSwitch";
    static final String SENSOR_TYPE_RESTART_EVENTS = "restartEvents";
    static final String SENSOR_TYPE_RESTART_EVENTS_NOT_UNBLOCK = "restartEventsNotUnblock";
    static final String SENSOR_TYPE_PHONE_CALL = "phoneCall";
    static final String SENSOR_TYPE_CALENDAR_PROVIDER_CHANGED = "calendarProviderChanged";
    static final String SENSOR_TYPE_SEARCH_CALENDAR_EVENTS = "searchCalendarEvents";
    static final String SENSOR_TYPE_SMS = "sms";
    static final String SENSOR_TYPE_NOTIFICATION = "notification";
    static final String SENSOR_TYPE_NFC_TAG = "nfcTag";
    static final String SENSOR_TYPE_EVENT_DELAY_START = "eventDelayStart";
    static final String SENSOR_TYPE_EVENT_DELAY_END = "eventDelayEnd";
    static final String SENSOR_TYPE_BATTERY = "battery";
    static final String SENSOR_TYPE_BLUETOOTH_CONNECTION = "bluetoothConnection";
    static final String SENSOR_TYPE_BLUETOOTH_STATE = "bluetoothState";
    static final String SENSOR_TYPE_DOCK_CONNECTION = "dockConnection";
    static final String SENSOR_TYPE_CALENDAR = "calendar";
    static final String SENSOR_TYPE_TIME = "time";
    static final String SENSOR_TYPE_APPLICATION = "application";
    static final String SENSOR_TYPE_HEADSET_CONNECTION = "headsetConnection";
    //static final String SENSOR_TYPE_NOTIFICATION_EVENT_END = "notificationEventEnd";
    static final String SENSOR_TYPE_SMS_EVENT_END = "smsEventEnd";
    static final String SENSOR_TYPE_WIFI_CONNECTION = "wifiConnection";
    static final String SENSOR_TYPE_WIFI_STATE = "wifiState";
    static final String SENSOR_TYPE_POWER_SAVE_MODE = "powerSaveMode";
    static final String SENSOR_TYPE_GEOFENCES_SCANNER = "geofenceScanner";
    static final String SENSOR_TYPE_LOCATION_MODE = "locationMode";
    static final String SENSOR_TYPE_DEVICE_ORIENTATION = "deviceOrientation";
    static final String SENSOR_TYPE_PHONE_STATE = "phoneState";
    static final String SENSOR_TYPE_NFC_EVENT_END = "nfcEventEnd";
    static final String SENSOR_TYPE_WIFI_SCANNER = "wifiScanner";
    static final String SENSOR_TYPE_BLUETOOTH_SCANNER = "bluetoothScanner";
    static final String SENSOR_TYPE_SCREEN = "screen";
    static final String SENSOR_TYPE_DEVICE_IDLE_MODE = "deviceIdleMode";
    static final String SENSOR_TYPE_PHONE_CALL_EVENT_END = "phoneCallEventEnd";
    static final String SENSOR_TYPE_ALARM_CLOCK = "alarmClock";
    static final String SENSOR_TYPE_ALARM_CLOCK_EVENT_END = "alarmClockEventEnd";

    public EventsHandler(Context context) {
        this.context = context;
    }
    
    void handleEvents(String sensorType) {
        synchronized (PPApplication.eventsHandlerMutex) {
            CallsCounter.logCounter(context, "EventsHandler.handleEvents", "EventsHandler_handleEvents");

            PPApplication.logE("#### EventsHandler.handleEvents", "-- start --------------------------------");

            if (!PPApplication.getApplicationStarted(context, true))
                // application is not started
                return;

            PPApplication.logE("#### EventsHandler.handleEvents", "-- application started --------------------------------");

            //boolean interactive;

            this.sensorType = sensorType;
            PPApplication.logE("#### EventsHandler.handleEvents", "sensorType=" + this.sensorType);
            CallsCounter.logCounterNoInc(context, "EventsHandler.handleEvents->sensorType=" + this.sensorType, "EventsHandler_handleEvents");

            //restartAtEndOfEvent = false;

            // disabled for firstStartEvents
            //if (!PPApplication.getApplicationStarted(context))
            // application is not started
            //	return;

            //PPApplication.setApplicationStarted(context, true);

            dataWrapper = new DataWrapper(context.getApplicationContext(), false, 0, false);

            ApplicationPreferences.getSharedPreferences(context);
            //TODO call sensor to Extender
            //callEventType = ApplicationPreferences.preferences.getInt(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TYPE, PhoneCallBroadcastReceiver.CALL_EVENT_UNDEFINED);

            // save ringer mode, zen mode, ringtone before handle events
            // used by ringing call simulation
            oldRingerMode = ActivateProfileHelper.getRingerMode(context);
            oldZenMode = ActivateProfileHelper.getZenMode(context);
            final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                oldSystemRingerMode = audioManager.getRingerMode();
                oldSystemRingerVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            }

            try {
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
                if (uri != null)
                    oldRingtone = uri.toString();
                else
                    oldRingtone = "";
            } catch (SecurityException e) {
                Permissions.grantPlayRingtoneNotificationPermissions(context, false);
                oldRingtone = "";
            } catch (Exception e) {
                oldRingtone = "";
            }

            /*
            try {
                Uri uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION);
                if (uri != null)
                    oldNotificationTone = uri.toString();
                else
                    oldNotificationTone = "";
            } catch (SecurityException e) {
                Permissions.grantPlayRingtoneNotificationPermissions(context, true, false);
                oldNotificationTone = "";
            } catch (Exception e) {
                oldNotificationTone = "";
            }
            */

            /*
            if (PhoneProfilesService.getInstance() != null) {
                // start of GeofenceScanner
                if (!PhoneProfilesService.isGeofenceScannerStarted())
                    PPApplication.startGeofenceScanner(context);
                // start of CellTowerScanner
                if (!PhoneProfilesService.isPhoneStateScannerStarted()) {
                    PPApplication.logE("EventsHandler.handleEvents", "startPhoneStateScanner");
                    //PPApplication.sendMessageToService(this, PhoneProfilesService.MSG_START_PHONE_STATE_SCANNER);
                    PPApplication.startPhoneStateScanner(context);
                }
            }
            */

            if (!Event.getGlobalEventsRunning(context)) {
                // events are globally stopped

                doEndHandler();
                dataWrapper.invalidateDataWrapper();

                PPApplication.logE("#### EventsHandler.handleEvents", "-- end: events globally stopped --------------------------------");

                return;
            }

            /*
            // start orientation listener only when events exists
            if (PhoneProfilesService.getInstance() != null) {
                if (!PhoneProfilesService.isOrientationScannerStarted()) {
                    if (dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_ORIENTATION) > 0)
                        PPApplication.startOrientationScanner(context);
                }
            }
            */

            if (!eventsExists(sensorType, false)) {
                // events not exists

                doEndHandler();
                dataWrapper.invalidateDataWrapper();

                PPApplication.logE("#### EventsHandler.handleEvents", "-- end: not events found --------------------------------");

                return;
            }

            dataWrapper.fillEventList();

            boolean isRestart = sensorType.equals(SENSOR_TYPE_RESTART_EVENTS);

            //interactive = (!isRestart) || _interactive;

            boolean saveCalendarStartEndTime = false;
            if (isRestart) {
                if (Event.isEventPreferenceAllowed(EventPreferencesCalendar.PREF_EVENT_CALENDAR_ENABLED, context.getApplicationContext()).allowed ==
                        PreferenceAllowed.PREFERENCE_ALLOWED) {
                    int eventCount = DatabaseHandler.getInstance(context.getApplicationContext())
                                        .getTypeEventsCount(DatabaseHandler.ETYPE_CALENDAR, false);
                    if (eventCount > 0)
                        saveCalendarStartEndTime = true;
                }
            }
            if (sensorType.equals(SENSOR_TYPE_CALENDAR_PROVIDER_CHANGED) ||
                    sensorType.equals(SENSOR_TYPE_SEARCH_CALENDAR_EVENTS) ||
                    saveCalendarStartEndTime) {
                // search for calendar events
                PPApplication.logE("[CALENDAR] EventsHandler.handleEvents", "search for calendar events");
                for (Event _event : this.dataWrapper.eventList) {
                    if ((_event._eventPreferencesCalendar._enabled) && (_event.getStatus() != Event.ESTATUS_STOP)) {
                        PPApplication.logE("[CALENDAR] EventsHandler.handleEvents", "event._id=" + _event._id);
                        _event._eventPreferencesCalendar.saveStartEndTime(dataWrapper);
                    }
                }
            }

            if (isRestart) {
                // for restart events, set startTime to 0
                dataWrapper.clearSensorsStartTime(false);
            }
            else {
                if (sensorType.equals(SENSOR_TYPE_SMS)) {
                    // search for sms events, save start time
                    PPApplication.logE("EventsHandler.handleEvents", "search for sms events");
                    for (Event _event : this.dataWrapper.eventList) {
                        if (_event.getStatus() != Event.ESTATUS_STOP) {
                            if (_event._eventPreferencesSMS._enabled) {
                                PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                                _event._eventPreferencesSMS.saveStartTime(dataWrapper, eventSMSPhoneNumber, eventSMSDate);
                            }
                        }
                    }
                }
                if (sensorType.equals(SENSOR_TYPE_NFC_TAG)) {
                    // search for nfc events, save start time
                    PPApplication.logE("EventsHandler.handleEvents", "search for nfc events");
                    for (Event _event : this.dataWrapper.eventList) {
                        if (_event.getStatus() != Event.ESTATUS_STOP) {
                            if (_event._eventPreferencesNFC._enabled) {
                                PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                                _event._eventPreferencesNFC.saveStartTime(dataWrapper, eventNFCTagName, eventNFCDate);
                            }
                        }
                    }
                }
                if (sensorType.equals(SENSOR_TYPE_PHONE_CALL)) {
                    // search for call events, save start time
                    PPApplication.logE("[CALL] EventsHandler.handleEvents", "search for call events");
                    for (Event _event : this.dataWrapper.eventList) {
                        if (_event.getStatus() != Event.ESTATUS_STOP) {
                            if (_event._eventPreferencesCall._enabled &&
                                    ((_event._eventPreferencesCall._callEvent == EventPreferencesCall.CALL_EVENT_MISSED_CALL) ||
                                            (_event._eventPreferencesCall._callEvent == EventPreferencesCall.CALL_EVENT_INCOMING_CALL_ENDED) ||
                                            (_event._eventPreferencesCall._callEvent == EventPreferencesCall.CALL_EVENT_OUTGOING_CALL_ENDED))) {
                                PPApplication.logE("[CALL] EventsHandler.handleEvents", "event._id=" + _event._id);
                                _event._eventPreferencesCall.saveStartTime(dataWrapper);
                            }
                        }
                    }
                }
                if (sensorType.equals(SENSOR_TYPE_ALARM_CLOCK)) {
                    // search for alarm clock events, save start time
                    PPApplication.logE("EventsHandler.handleEvents", "search for alarm clock events");
                    for (Event _event : this.dataWrapper.eventList) {
                        if (_event.getStatus() != Event.ESTATUS_STOP) {
                            if (_event._eventPreferencesAlarmClock._enabled) {
                                PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                                _event._eventPreferencesAlarmClock.saveStartTime(dataWrapper, eventAlarmClockDate);
                            }
                        }
                    }
                }
            }

            boolean forDelayStartAlarm = sensorType.equals(SENSOR_TYPE_EVENT_DELAY_START);
            boolean forDelayEndAlarm = sensorType.equals(SENSOR_TYPE_EVENT_DELAY_END);

            //PPApplication.logE("@@@ EventsHandler.handleEvents","isRestart="+isRestart);
            PPApplication.logE("@@@ EventsHandler.handleEvents", "forDelayStartAlarm=" + forDelayStartAlarm);
            PPApplication.logE("@@@ EventsHandler.handleEvents", "forDelayEndAlarm=" + forDelayEndAlarm);

            // no refresh notification and widgets
            ActivateProfileHelper.lockRefresh = true;

            Profile mergedProfile = DataWrapper.getNonInitializedProfile("", "", 0);
            Profile mergedPausedProfile = DataWrapper.getNonInitializedProfile("", "", 0);

            //Profile activatedProfile0 = null;

            int runningEventCount0;
            int runningEventCountP;
            boolean restartEventsAtEnd = false;
            boolean activateProfileAtEnd = false;
            boolean anyEventPaused = false;
            Event notifyEventEnd = null;

            if (isRestart) {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "restart events");

                //oldActivatedProfile = null;

                // get running events count
                List<EventTimeline> _etl = dataWrapper.getEventTimelineList();
                runningEventCount0 = _etl.size();

                // 1. pause events
                dataWrapper.sortEventsByStartOrderDesc();
                for (Event _event : this.dataWrapper.eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state PAUSE");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP) {
                        // only pause events
                        // pause also paused events

                        boolean running = _event.getStatus() == Event.ESTATUS_RUNNING;
                        dataWrapper.doHandleEvents(_event, true, true, /*interactive,*/ false, forDelayEndAlarm, true, mergedProfile, sensorType);
                        boolean paused = _event.getStatus() == Event.ESTATUS_PAUSE;

                        if (running && paused) {
                            anyEventPaused = true;
                            notifyEventEnd = _event;
                        }

                        /*
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** profileName=" + mergedProfile._name);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** profileId=" + mergedProfile._id);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeRingerMode=" + mergedProfile._volumeRingerMode);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeZenMode=" + mergedProfile._volumeZenMode);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeRingtone=" + mergedProfile._volumeRingtone);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeNotification=" + mergedProfile._volumeNotification);
                        */
                    }
                }

                runningEventCountP = _etl.size();

                // 2. start events
                dataWrapper.sortEventsByStartOrderAsc();
                for (Event _event : this.dataWrapper.eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state RUNNING");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP) {
                        // only start events
                        // start all events
                        dataWrapper.doHandleEvents(_event, false, true, /*interactive,*/ false, forDelayEndAlarm, true, mergedProfile, sensorType);

                        /*
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** profileName=" + mergedProfile._name);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** profileId=" + mergedProfile._id);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeRingerMode=" + mergedProfile._volumeRingerMode);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeZenMode=" + mergedProfile._volumeZenMode);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeRingtone=" + mergedProfile._volumeRingtone);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "**** _volumeNotification=" + mergedProfile._volumeNotification);
                        */
                    }
                }
            } else {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "NO restart events");

                //oldActivatedProfile = dataWrapper.getActivatedProfile();

                //activatedProfile0 = dataWrapper.getActivatedProfileFromDB();

                // get running events count
                List<EventTimeline> _etl = dataWrapper.getEventTimelineList();
                runningEventCount0 = _etl.size();

                //1. pause events
                dataWrapper.sortEventsByStartOrderDesc();
                for (Event _event : this.dataWrapper.eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state PAUSE");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP) {
                        // only pause events
                        // pause only running events

                        boolean running = _event.getStatus() == Event.ESTATUS_RUNNING;
                        //noinspection ConstantConditions
                        dataWrapper.doHandleEvents(_event, true, false, /*interactive,*/ forDelayStartAlarm, forDelayEndAlarm, false, mergedPausedProfile, sensorType);
                        boolean paused = _event.getStatus() == Event.ESTATUS_PAUSE;

                        if (running && paused) {
                            anyEventPaused = true;
                            notifyEventEnd = _event;
                            if (!restartEventsAtEnd && (_event._atEndDo == Event.EATENDDO_RESTART_EVENTS))
                                restartEventsAtEnd = true;
                            if (!activateProfileAtEnd && ((_event._atEndDo == Event.EATENDDO_UNDONE_PROFILE) || (_event._fkProfileEnd != Profile.PROFILE_NO_ACTIVATE)))
                                activateProfileAtEnd = true;
                        }
                    }
                }

                runningEventCountP = _etl.size();

                //2. start events
                mergedProfile.copyProfile(mergedPausedProfile);
                dataWrapper.sortEventsByStartOrderAsc();
                for (Event _event : this.dataWrapper.eventList) {
                    PPApplication.logE("EventsHandler.handleEvents", "state RUNNING");
                    PPApplication.logE("EventsHandler.handleEvents", "event._id=" + _event._id);
                    PPApplication.logE("EventsHandler.handleEvents", "event.getStatus()=" + _event.getStatus());

                    if (_event.getStatus() != Event.ESTATUS_STOP)
                        // only start events
                        // start only paused events
                        //noinspection ConstantConditions
                        dataWrapper.doHandleEvents(_event, false, false, /*interactive,*/ forDelayStartAlarm, forDelayEndAlarm, true, mergedProfile, sensorType);
                }
            }

            ActivateProfileHelper.lockRefresh = false;

            if (mergedProfile._id == 0)
                PPApplication.logE("$$$ EventsHandler.handleEvents", "no profile for activation");
            else
                PPApplication.logE("$$$ EventsHandler.handleEvents", "profileName=" + mergedProfile._name);

            //if ((!restartAtEndOfEvent) || isRestart) {
            //    // No any paused events has "Restart events" at end of event

            //////////////////
            //// when no events are running or manual activation,
            //// activate background profile when no profile is activated

            // get running events count
            List<EventTimeline> eventTimelineList = dataWrapper.getEventTimelineList();
            int runningEventCountE = eventTimelineList.size();

            Profile activatedProfile = dataWrapper.getActivatedProfileFromDB(false, false);
            long backgroundProfileId = Profile.PROFILE_NO_ACTIVATE;
            boolean notifyBackgroundProfile = false;

            if (!dataWrapper.getIsManualProfileActivation(false)) {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "active profile is NOT activated manually");
                PPApplication.logE("$$$ EventsHandler.handleEvents", "runningEventCount0=" + runningEventCount0);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "runningEventCountE=" + runningEventCountE);
                // no manual profile activation
                if ((runningEventCountE == 0) && (!restartEventsAtEnd)) {
                    // activate default profile, only when will not be do restart events from paused events

                    PPApplication.logE("$$$ EventsHandler.handleEvents", "no events running");
                    // no events running
                    backgroundProfileId = Long.valueOf(ApplicationPreferences.applicationBackgroundProfile(context));
                    if (backgroundProfileId != Profile.PROFILE_NO_ACTIVATE) {
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "default profile is set");
                        long activatedProfileId = 0;
                        if (activatedProfile != null)
                            activatedProfileId = activatedProfile._id;

                        if (ApplicationPreferences.applicationBackgroundProfileUsage(context)) {
                            // do not activate default profile when not any event is paused and no any profile is activated
                            // for example for screen on/off broadcast, when no any event is running
                            if (!anyEventPaused && (mergedProfile._id == 0) && (mergedPausedProfile._id == 0))
                                activateProfileAtEnd = true;

                            PPApplication.logE("$$$ EventsHandler.handleEvents", "anyEventPaused=" + anyEventPaused);
                            PPApplication.logE("$$$ EventsHandler.handleEvents", "activatedProfileId=" + activatedProfileId);
                            PPApplication.logE("$$$ EventsHandler.handleEvents", "mergedProfile._id=" + mergedProfile._id);
                            PPApplication.logE("$$$ EventsHandler.handleEvents", "mergedPausedProfile._id=" + mergedPausedProfile._id);
                            PPApplication.logE("$$$ EventsHandler.handleEvents", "isRestart=" + isRestart);
                            PPApplication.logE("$$$ EventsHandler.handleEvents", "activateProfileAtEnd=" + activateProfileAtEnd);
                            if ((activatedProfileId == 0) ||
                                    isRestart ||
                                    // activate default profile when is not activated profile at end of events
                                    ((!activateProfileAtEnd || ((mergedProfile._id != 0) && (mergedPausedProfile._id == 0))) &&
                                            (activatedProfileId != backgroundProfileId))
                                    ) {
                                notifyBackgroundProfile = true;
                                mergedProfile.mergeProfiles(backgroundProfileId, dataWrapper, false);
                                PPApplication.logE("$$$ EventsHandler.handleEvents", "activated default profile");
                            }
                        }
                        else {
                            if ((activatedProfileId != backgroundProfileId) || isRestart) {
                                notifyBackgroundProfile = true;
                                mergedProfile.mergeProfiles(backgroundProfileId, dataWrapper, false);
                                PPApplication.logE("$$$ EventsHandler.handleEvents", "activated default profile");
                            }
                        }
                    }
                }
            } else {
                PPApplication.logE("$$$ EventsHandler.handleEvents", "active profile is activated manually");
                // manual profile activation
                backgroundProfileId = Long.valueOf(ApplicationPreferences.applicationBackgroundProfile(context));
                if (backgroundProfileId != Profile.PROFILE_NO_ACTIVATE) {
                    if (activatedProfile == null) {
                        // if not profile activated, activate Default profile
                        notifyBackgroundProfile = true;
                        mergedProfile.mergeProfiles(backgroundProfileId, dataWrapper, false);
                        PPApplication.logE("$$$ EventsHandler.handleEvents", "activated default profile");
                    }
                }
            }
            ////////////////

            Event notifyEventStart = null;
            String backgroundProfileNotificationSound = "";
            boolean backgroundProfileNotificationVibrate = false;

            if (/*(!isRestart) &&*/ (runningEventCountE > runningEventCountP)) {
                // only running events is increased, play event notification sound

                EventTimeline eventTimeline = eventTimelineList.get(runningEventCountE - 1);
                notifyEventStart = dataWrapper.getEventById(eventTimeline._fkEvent);
            }
            else
            if (/*(!isRestart) &&*/ (backgroundProfileId != Profile.PROFILE_NO_ACTIVATE) && notifyBackgroundProfile) {
                // only when activated is background profile, play event notification sound

                backgroundProfileNotificationSound = ApplicationPreferences.applicationBackgroundProfileNotificationSound(context);
                backgroundProfileNotificationVibrate = ApplicationPreferences.applicationBackgroundProfileNotificationVibrate(context);
            }

            PPApplication.logE("$$$ EventsHandler.handleEvents", "mergedProfile=" + mergedProfile);

            PPApplication.logE("$$$ EventsHandler.handleEvents", "mergedProfile._id=" + mergedProfile._id);
            boolean doSleep = false;
            if (mergedProfile._id != 0) {
                // activate merged profile
                PPApplication.logE("$$$ EventsHandler.handleEvents", "#### profileName=" + mergedProfile._name);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "#### profileId=" + mergedProfile._id);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "#### _volumeRingerMode=" + mergedProfile._volumeRingerMode);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "#### _volumeZenMode=" + mergedProfile._volumeZenMode);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "#### _volumeRingtone=" + mergedProfile._volumeRingtone);
                PPApplication.logE("$$$ EventsHandler.handleEvents", "#### _volumeNotification=" + mergedProfile._volumeNotification);
                DatabaseHandler.getInstance(context.getApplicationContext()).saveMergedProfile(mergedProfile);
                dataWrapper.activateProfileFromEvent(mergedProfile._id, /*interactive,*/ false, true);
                // wait for profile activation
                doSleep = true;
            } else {
                if (!restartEventsAtEnd) {
                    // update only when will not be do restart events from paused events
                    dataWrapper.updateNotificationAndWidgets();
                }
            }

            PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "notifyEventStart=" + notifyEventStart);
            if (notifyEventStart != null) {
                PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "notifyEventStart._name=" + notifyEventStart._name);
                PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "notifyEventStart=" + notifyEventStart.notifyEventStart(context));
            }
            PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "notifyEventEnd=" + notifyEventEnd);
            if (notifyEventEnd != null) {
                PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "notifyEventEnd._name=" + notifyEventEnd._name);
                PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "notifyEventEnd=" + notifyEventEnd.notifyEventEnd());
            }
            PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "backgroundProfileNotificationSound=" + backgroundProfileNotificationSound);

            // notify start of event - only when not restart events
            boolean notify = (!isRestart) && (notifyEventStart != null) && notifyEventStart.notifyEventStart(context);
            if (notify)
                PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "start of event notified");
            if (!notify)
                // notify end of event - only when not restart events
                notify = (!isRestart) && (notifyEventEnd != null) && notifyEventEnd.notifyEventEnd();
            if (notify)
                PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "end of event notified");
            if (!notify) {
                // notify default profile
                if (!backgroundProfileNotificationSound.isEmpty() || backgroundProfileNotificationVibrate) {
                    if (PhoneProfilesService.getInstance() != null) {
                        PhoneProfilesService.getInstance().playNotificationSound(backgroundProfileNotificationSound, backgroundProfileNotificationVibrate);
                        PPApplication.logE("[NOTIFY] EventsHandler.handleEvents", "default profile notified");
                    }
                }
            }

            if (doSleep || notify) {
                //try { Thread.sleep(500); } catch (InterruptedException e) { }
                //SystemClock.sleep(500);
                PPApplication.sleep(500);
            }
            //}

            //restartAtEndOfEvent = false;

            doEndHandler();

            // refresh GUI
            /*Intent refreshIntent = new Intent();
            refreshIntent.setAction(RefreshGUIBroadcastReceiver.INTENT_REFRESH_GUI);
            context.sendBroadcast(refreshIntent);*/
            Intent refreshIntent = new Intent("RefreshGUIBroadcastReceiver");
            LocalBroadcastManager.getInstance(context).sendBroadcast(refreshIntent);


            dataWrapper.invalidateDataWrapper();

            PPApplication.logE("#### EventsHandler.handleEvents", "-- end --------------------------------");
        }
    }

    private boolean eventsExists(String broadcastReceiverType, boolean onlyRunning) {
        int eventType = 0;
        switch (broadcastReceiverType) {
            case SENSOR_TYPE_BATTERY:
                eventType = DatabaseHandler.ETYPE_BATTERY;
                break;
            case SENSOR_TYPE_BLUETOOTH_CONNECTION:
                eventType = DatabaseHandler.ETYPE_BLUETOOTHCONNECTED;
                break;
            case SENSOR_TYPE_BLUETOOTH_SCANNER:
                eventType = DatabaseHandler.ETYPE_BLUETOOTHINFRONT;
                break;
            case SENSOR_TYPE_BLUETOOTH_STATE:
                eventType = DatabaseHandler.ETYPE_BLUETOOTHCONNECTED;
                break;
            case SENSOR_TYPE_CALENDAR_PROVIDER_CHANGED:
                eventType = DatabaseHandler.ETYPE_CALENDAR;
                break;
            case SENSOR_TYPE_DOCK_CONNECTION:
                eventType = DatabaseHandler.ETYPE_PERIPHERAL;
                break;
            /*case SENSOR_TYPE_EVENT_DELAY_START:
                eventType = DatabaseHandler.ETYPE_????;
                break;
            case SENSOR_TYPE_EVENT_DELAY_END:
                eventType = DatabaseHandler.ETYPE_????;
                break;*/
            case SENSOR_TYPE_CALENDAR:
                eventType = DatabaseHandler.ETYPE_CALENDAR;
                break;
            case SENSOR_TYPE_TIME:
                eventType = DatabaseHandler.ETYPE_TIME;
                break;
            case SENSOR_TYPE_APPLICATION:
                eventType = DatabaseHandler.ETYPE_APPLICATION;
                break;
            case SENSOR_TYPE_HEADSET_CONNECTION:
                eventType = DatabaseHandler.ETYPE_PERIPHERAL;
                break;
            case SENSOR_TYPE_NOTIFICATION:
                eventType = DatabaseHandler.ETYPE_NOTIFICATION;
                break;
            /*case SENSOR_TYPE_NOTIFICATION_EVENT_END:
                eventType = DatabaseHandler.ETYPE_NOTIFICATION;
                break;*/
            case SENSOR_TYPE_PHONE_CALL:
                eventType = DatabaseHandler.ETYPE_CALL;
                break;
            case SENSOR_TYPE_PHONE_CALL_EVENT_END:
                eventType = DatabaseHandler.ETYPE_CALL;
                break;
            /*case SENSOR_TYPE_RESTART_EVENTS:
                eventType = DatabaseHandler.ETYPE_???;
                break;*/
            /*// call doEventService for all screen on/off changes
            case SENSOR_TYPE_SCREEN:
                eventType = DatabaseHandler.ETYPE_SCREEN;
                break;*/
            case SENSOR_TYPE_SEARCH_CALENDAR_EVENTS:
                eventType = DatabaseHandler.ETYPE_CALENDAR;
                break;
            case SENSOR_TYPE_SMS:
                eventType = DatabaseHandler.ETYPE_SMS;
                break;
            case SENSOR_TYPE_SMS_EVENT_END:
                eventType = DatabaseHandler.ETYPE_SMS;
                break;
            case SENSOR_TYPE_WIFI_CONNECTION:
                eventType = DatabaseHandler.ETYPE_WIFICONNECTED;
                break;
            case SENSOR_TYPE_WIFI_SCANNER:
                eventType = DatabaseHandler.ETYPE_WIFIINFRONT;
                break;
            case SENSOR_TYPE_WIFI_STATE:
                eventType = DatabaseHandler.ETYPE_WIFICONNECTED;
                break;
            /*case SENSOR_TYPE_DEVICE_IDLE_MODE:
                eventType = DatabaseHandler.ETYPE_????;
                break;*/
            case SENSOR_TYPE_POWER_SAVE_MODE:
                eventType = DatabaseHandler.ETYPE_BATTERY;
                break;
            case SENSOR_TYPE_GEOFENCES_SCANNER:
                eventType = DatabaseHandler.ETYPE_LOCATION;
                break;
            case SENSOR_TYPE_LOCATION_MODE:
                eventType = DatabaseHandler.ETYPE_LOCATION;
                break;
            case SENSOR_TYPE_DEVICE_ORIENTATION:
                eventType = DatabaseHandler.ETYPE_ORIENTATION;
                break;
            case SENSOR_TYPE_PHONE_STATE:
                eventType = DatabaseHandler.ETYPE_MOBILE_CELLS;
                break;
            case SENSOR_TYPE_NFC_TAG:
                eventType = DatabaseHandler.ETYPE_NFC;
                break;
            case SENSOR_TYPE_NFC_EVENT_END:
                eventType = DatabaseHandler.ETYPE_NFC;
                break;
            case SENSOR_TYPE_RADIO_SWITCH:
                eventType = DatabaseHandler.ETYPE_RADIO_SWITCH;
                break;
            case SENSOR_TYPE_ALARM_CLOCK:
                eventType = DatabaseHandler.ETYPE_ALARM_CLOCK;
                break;
            case SENSOR_TYPE_ALARM_CLOCK_EVENT_END:
                eventType = DatabaseHandler.ETYPE_ALARM_CLOCK;
                break;
        }

        if (eventType > 0)
            return DatabaseHandler.getInstance(context.getApplicationContext()).getTypeEventsCount(eventType, onlyRunning) > 0;
        else
            return true;
    }

    private void doEndHandler() {
        PPApplication.logE("EventsHandler.doEndService","sensorType="+sensorType);
        PPApplication.logE("EventsHandler.doEndService","callEventType="+callEventType);

        //TODO call sensor to Extender
        /*
        if (sensorType.equals(SENSOR_TYPE_PHONE_CALL)) {
            boolean linkUnlink = false;
            if (ActivateProfileHelper.getMergedRingNotificationVolumes(context) &&
                    ApplicationPreferences.applicationUnlinkRingerNotificationVolumes(context)) {
                PPApplication.logE("EventsHandler.doEndService","unlink enabled");
                if (!PhoneCallBroadcastReceiver.linkUnlinkExecuted) {
                    PPApplication.logE("EventsHandler.doEndService","profile is not activated from EventsHandler");
                    // no profile is activated from EventsHandler
                    // link, unlink volumes for activated profile
                    if (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_RINGING)
                        linkUnlink = true;
                    if ((callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_ENDED) ||
                            (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_MISSED_CALL))
                        linkUnlink = true;
                    if (linkUnlink) {
                        Profile profile = dataWrapper.getActivatedProfile(false, false);
                        final Profile _profile = Profile.getMappedProfile(profile, context);
                        if (profile != null) {
                            ActivateProfileHelper.executeForVolumes(_profile, false, context);
                        }
                        // wait for link/unlink
                        //PPApplication.sleep(1500);
                    }
                } else
                    PhoneCallBroadcastReceiver.linkUnlinkExecuted = false;
            } else
                PhoneCallBroadcastReceiver.linkUnlinkExecuted = false;

            if (eventsExists(sensorType, true)) {
                // doEndHandler is called even if no event exists, but ringing call simulation is only for running event with call sensor
                if ((android.os.Build.VERSION.SDK_INT >= 21) && (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_RINGING)) {
                    //if (!linkUnlink) {
                        // wait for change ringer mode + volume
                    //    PPApplication.sleep(1500);
                    //}
                    // start PhoneProfilesService for ringing call simulation
                    PPApplication.logE("EventsHandler.doEndService","start simulating ringing call");
                    try {
                        Intent serviceIntent = new Intent(context.getApplicationContext(), PhoneProfilesService.class);
                        serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                        serviceIntent.putExtra(PhoneProfilesService.EXTRA_SIMULATE_RINGING_CALL, true);
                        // add saved ringer mode, zen mode, ringtone before handle events as parameters
                        // ringing call simulator compare this with new (actual values), changed by currently activated profile
                        serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_RINGER_MODE, oldRingerMode);
                        serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_SYSTEM_RINGER_MODE, oldSystemRingerMode);
                        serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_ZEN_MODE, oldZenMode);
                        serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_RINGTONE, oldRingtone);
                        serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_SYSTEM_RINGER_VOLUME, oldSystemRingerVolume);
                        PPApplication.startPPService(context, serviceIntent);
                    } catch (Exception ignored) {
                    }
                }
            }

            if (!PhoneCallBroadcastReceiver.speakerphoneOnExecuted) {
                // no profile is activated from EventsHandler
                // set speakerphone ON for activated profile
                if ((callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_ANSWERED) ||
                        (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_OUTGOING_CALL_ANSWERED)) {
                    Profile profile = dataWrapper.getActivatedProfile(false, false);
                    profile = Profile.getMappedProfile(profile, context);
                    PhoneCallBroadcastReceiver.setSpeakerphoneOn(profile, context);
                }
            } else
                PhoneCallBroadcastReceiver.speakerphoneOnExecuted = false;

            if ((callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_INCOMING_CALL_ENDED) ||
                    (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_OUTGOING_CALL_ENDED) ||
                    (callEventType == PhoneCallBroadcastReceiver.CALL_EVENT_MISSED_CALL)) {
                ApplicationPreferences.getSharedPreferences(context);
                SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                editor.putInt(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TYPE, PhoneCallBroadcastReceiver.CALL_EVENT_UNDEFINED);
                editor.putString(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_PHONE_NUMBER, "");
                editor.putLong(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TIME, 0);
                editor.apply();
            }
        }
        else
        if (sensorType.equals(SENSOR_TYPE_PHONE_CALL_EVENT_END)) {
            ApplicationPreferences.getSharedPreferences(context);
            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
            editor.putInt(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TYPE, PhoneCallBroadcastReceiver.CALL_EVENT_UNDEFINED);
            editor.putString(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_PHONE_NUMBER, "");
            editor.putLong(PhoneCallBroadcastReceiver.PREF_EVENT_CALL_EVENT_TIME, 0);
            editor.apply();
        }
        */
        if (sensorType.equals(SENSOR_TYPE_PHONE_CALL)) {
            TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            if (eventsExists(sensorType, true)) {
                // doEndHandler is called even if no event exists, but ringing call simulation is only for running event with call sensor
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    boolean inRinging = false;
                    if (telephony != null) {
                        int callState = telephony.getCallState();
                        //if (doUnlink) {
                        //if (linkUnlink == PhoneCallBroadcastReceiver.LINKMODE_UNLINK) {
                        inRinging = (callState == TelephonyManager.CALL_STATE_RINGING);
                    }
                    if (inRinging) {
                        // start PhoneProfilesService for ringing call simulation
                        PPApplication.logE("EventsHandler.doEndService", "start simulating ringing call");
                        try {
                            Intent serviceIntent = new Intent(context.getApplicationContext(), PhoneProfilesService.class);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_SIMULATE_RINGING_CALL, true);
                            // add saved ringer mode, zen mode, ringtone before handle events as parameters
                            // ringing call simulator compare this with new (actual values), changed by currently activated profile
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_RINGER_MODE, oldRingerMode);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_SYSTEM_RINGER_MODE, oldSystemRingerMode);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_ZEN_MODE, oldZenMode);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_RINGTONE, oldRingtone);
                            serviceIntent.putExtra(PhoneProfilesService.EXTRA_OLD_SYSTEM_RINGER_VOLUME, oldSystemRingerVolume);
                            PPApplication.startPPService(context, serviceIntent);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            boolean inCall = false;
            if (telephony != null) {
                int callState = telephony.getCallState();
                //if (doUnlink) {
                //if (linkUnlink == PhoneCallBroadcastReceiver.LINKMODE_UNLINK) {
                inCall = (callState == TelephonyManager.CALL_STATE_RINGING) || (callState == TelephonyManager.CALL_STATE_OFFHOOK);
            }
            if (!inCall)
                setEventCallParameters(EventPreferencesCall.PHONE_CALL_EVENT_UNDEFINED, "", 0);
        }
        else
        if (sensorType.equals(SENSOR_TYPE_PHONE_CALL_EVENT_END)) {
            setEventCallParameters(EventPreferencesCall.PHONE_CALL_EVENT_UNDEFINED, "", 0);
        }

        /*else
        if (broadcastReceiverType.equals(SENSOR_TYPE_SMS)) {
            // start PhoneProfilesService for notification tone simulation
            Intent serviceIntent = new Intent(context.getApplicationContext(), PhoneProfilesService.class);
            serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
            serviceIntent.putExtra(EXTRA_SIMULATE_NOTIFICATION_TONE, true);
            serviceIntent.putExtra(EXTRA_OLD_RINGER_MODE, oldRingerMode);
            serviceIntent.putExtra(EXTRA_OLD_SYSTEM_RINGER_MODE, oldSystemRingerMode);
            serviceIntent.putExtra(EXTRA_OLD_ZEN_MODE, oldZenMode);
            serviceIntent.putExtra(EXTRA_OLD_NOTIFICATION_TONE, oldNotificationTone);
            PPApplication.startPPService(context, serviceIntent);
        }
        else
        if (broadcastReceiverType.equals(SENSOR_TYPE_NOTIFICATION)) {
            if ((android.os.Build.VERSION.SDK_INT >= 21) && intent.getStringExtra(EXTRA_EVENT_NOTIFICATION_POSTED_REMOVED).equals("posted")) {
                // start PhoneProfilesService for notification tone simulation
                Intent serviceIntent = new Intent(context.getApplicationContext(), PhoneProfilesService.class);
                serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, false);
                serviceIntent.putExtra(EXTRA_SIMULATE_NOTIFICATION_TONE, true);
                serviceIntent.putExtra(EXTRA_OLD_RINGER_MODE, oldRingerMode);
                serviceIntent.putExtra(EXTRA_OLD_SYSTEM_RINGER_MODE, oldSystemRingerMode);
                serviceIntent.putExtra(EXTRA_OLD_ZEN_MODE, oldZenMode);
                serviceIntent.putExtra(EXTRA_OLD_NOTIFICATION_TONE, oldNotificationTone);
                PPApplication.startPPService(context, serviceIntent);
            }
        }*/
    }

    void setEventSMSParameters(String phoneNumber, long date) {
        eventSMSPhoneNumber = phoneNumber;
        eventSMSDate = date;
    }

    /*
    void setEventNotificationParameters(String postedRemoved) {
        eventNotificationPostedRemoved = postedRemoved;
    }
    */

    void setEventNFCParameters(String tagName, long date) {
        eventNFCTagName = tagName;
        eventNFCDate = date;
    }

    void setEventAlarmClockParameters(long date) {
        eventAlarmClockDate = date;
    }

    void setEventCallParameters(int callEventType, String phoneNumber, long eventTime) {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(EventPreferencesCall.PREF_EVENT_CALL_EVENT_TYPE, callEventType);
        editor.putString(EventPreferencesCall.PREF_EVENT_CALL_PHONE_NUMBER, phoneNumber);
        editor.putLong(EventPreferencesCall.PREF_EVENT_CALL_EVENT_TIME, eventTime);
        editor.apply();
    }

}

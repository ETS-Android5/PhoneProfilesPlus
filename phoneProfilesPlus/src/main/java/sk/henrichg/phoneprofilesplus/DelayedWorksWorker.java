package sk.henrichg.phoneprofilesplus;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

@SuppressWarnings("WeakerAccess")
public class DelayedWorksWorker extends Worker {

    //Context context;

    static final String DELAYED_WORK_HANDLE_EVENTS = "handle_events";
    static final String DELAYED_WORK_START_WIFI_SCAN = "start_wifi_scan";
    static final String DELAYED_WORK_BLOCK_PROFILE_EVENT_ACTIONS = "block_profile_event_actions";
    static final String DELAYED_WORK_PACKAGE_REPLACED = "package_replaced";
    static final String DELAYED_WORK_CLOSE_ALL_APPLICATIONS = "close_all_applications";
    //static final String DELAYED_WORK_CHANGE_FILTER_AFTER_EDITOR_DATA_CHANGE = "change_filter_after_editor_data_change";

    public DelayedWorksWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        //this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            //PPApplication.logE("DelayedWorksWorker.doWork", "xxx");

            //Data outputData;

            // Get the input
            String action = getInputData().getString(PhoneProfilesService.EXTRA_DELAYED_WORK);
            if (action == null)
                return Result.success();

            String sensorType = getInputData().getString(PhoneProfilesService.EXTRA_SENSOR_TYPE);

            //outputData = generateResult(LocationGeofenceEditorActivity.FAILURE_RESULT,
            //                                    getApplicationContext().getString(R.string.event_preferences_location_no_address_found),
            //                                    updateName);

            //return Result.success(outputData);

            Context appContext = getApplicationContext();

            switch (action) {
                case DELAYED_WORK_PACKAGE_REPLACED:
                    PPApplication.logE("PackageReplacedReceiver.doWork", "START");

                    boolean packageReplaced = PPApplication.applicationPackageReplaced; //ApplicationPreferences.applicationPackageReplaced(appContext);
                    PPApplication.logE("PackageReplacedReceiver.doWork", "package replaced=" + packageReplaced);

                    DataWrapper dataWrapper = new DataWrapper(appContext, false, 0, false);

                    if (!packageReplaced) {
                        PhoneProfilesService instance = PhoneProfilesService.getInstance();
                        if (instance != null)
                            instance.setApplicationFullyStarted(true, false);

                        // do restart events, manual profile activation
                        if (Event.getGlobalEventsRunning()) {
                            PPApplication.logE("PackageReplacedReceiver.doWork", "global event run is enabled, first start events");

                            if (!DataWrapper.getIsManualProfileActivation(false)) {
                                ////// unblock all events for first start
                                //     that may be blocked in previous application run
                                dataWrapper.pauseAllEvents(false, false);
                            }

                            dataWrapper.firstStartEvents(true, false);
                            dataWrapper.updateNotificationAndWidgets(true, true);
                        } else {
                            PPApplication.logE("PackageReplacedReceiver.doWork", "global event run is not enabled, manually activate profile");

                            ////// unblock all events for first start
                            //     that may be blocked in previous application run
                            dataWrapper.pauseAllEvents(true, false);

                            dataWrapper.activateProfileOnBoot();
                            dataWrapper.updateNotificationAndWidgets(true, true);
                        }

                        break;
                    }

                    final int oldVersionCode = PPApplication.getSavedVersionCode(appContext);
                    // save version code
                    try {
                        PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
                        int actualVersionCode = PPApplication.getVersionCode(pInfo);
                        PPApplication.setSavedVersionCode(appContext, actualVersionCode);

                        String version = pInfo.versionName + " (" + PPApplication.getVersionCode(pInfo) + ")";
                        PPApplication.addActivityLog(appContext, PPApplication.ALTYPE_APPLICATION_UPGRADE, version, null, null, 0, "");
                    } catch (Exception ignored) {
                    }

                    Permissions.setAllShowRequestPermissions(appContext, true);

                    //WifiBluetoothScanner.setShowEnableLocationNotification(appContext, true, WifiBluetoothScanner.SCANNER_TYPE_WIFI);
                    //WifiBluetoothScanner.setShowEnableLocationNotification(appContext, true, WifiBluetoothScanner.SCANNER_TYPE_BLUETOOTH);
                    //PhoneStateScanner.setShowEnableLocationNotification(appContext, true);
                    //ActivateProfileHelper.setScreenUnlocked(appContext, true);

                    PPApplication.logE("PackageReplacedReceiver.doWork", "oldVersionCode=" + oldVersionCode);
                    int actualVersionCode;
                    try {
                        PackageInfo pInfo = appContext.getPackageManager().getPackageInfo(appContext.getPackageName(), 0);
                        actualVersionCode = PPApplication.getVersionCode(pInfo);
                        PPApplication.logE("PackageReplacedReceiver.doWork", "actualVersionCode=" + actualVersionCode);

                        if (oldVersionCode < actualVersionCode) {
                            PPApplication.logE("PackageReplacedReceiver.doWork", "is new version");

                            //PhoneProfilesService.cancelWork("delayedWorkAfterFirstStartWork", appContext);

                            if (actualVersionCode <= 2322) {
                                // for old packages use Priority in events
                                SharedPreferences.Editor editor = ApplicationPreferences.getEditor(appContext);
                                //PPApplication.logE("PackageReplacedReceiver.doWork", "applicationEventUsePriority=true");
                                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_USE_PRIORITY, true);
                                editor.apply();
                            }
                            if (actualVersionCode <= 2400) {
                                PPApplication.logE("PackageReplacedReceiver.doWork", "donation alarm restart");
                                PPApplication.setDaysAfterFirstStart(appContext, 0);
                                PPApplication.setDonationNotificationCount(appContext, 0);
                                DonationBroadcastReceiver.setAlarm(appContext);
                            }

                            //if (actualVersionCode <= 2500) {
                            //    // for old packages hide profile notification from status bar if notification is disabled
                            //    ApplicationPreferences.getSharedPreferences(appContext);
                            //    if (Build.VERSION.SDK_INT < 26) {
                            //        if (!ApplicationPreferences.preferences.getBoolean(ApplicationPreferences.PREF_NOTIFICATION_STATUS_BAR, true)) {
                            //            SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                            //            PPApplication.logE("PackageReplacedReceiver.onReceive", "notificationShowInStatusBar=false");
                            //            editor.putBoolean(ApplicationPreferences.PREF_NOTIFICATION_SHOW_IN_STATUS_BAR, false);
                            //            editor.apply();
                            //        }
                            //    }
                            //}

                            if (actualVersionCode <= 2700) {
                                SharedPreferences.Editor editor = ApplicationPreferences.getEditor(appContext);

                                //editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EDITOR_SAVE_EDITOR_STATE, true);

                                editor.putBoolean(ActivateProfileActivity.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(ActivateProfileListFragment.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(ActivateProfileListAdapter.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(EditorProfilesActivity.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(EditorProfileListFragment.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS_ORDER, false);
                                editor.putBoolean(EditorProfileListAdapter.PREF_START_TARGET_HELPS_SHOW_IN_ACTIVATOR, false);
                                editor.putBoolean(EditorEventListFragment.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(EditorEventListAdapter.PREF_START_TARGET_HELPS_ORDER, false);
                                editor.putBoolean(ProfilesPrefsActivity.PREF_START_TARGET_HELPS, false);
                                editor.putBoolean(ProfilesPrefsActivity.PREF_START_TARGET_HELPS_SAVE, false);
                                editor.putBoolean(EventsPrefsActivity.PREF_START_TARGET_HELPS, false);
                                editor.apply();
                            }
                            if (actualVersionCode <= 3200) {
                                SharedPreferences.Editor editor = ApplicationPreferences.getEditor(appContext);
                                editor.putBoolean(ProfilesPrefsActivity.PREF_START_TARGET_HELPS, true);
                                editor.apply();
                            }
                            if (actualVersionCode <= 3500) {
                                if (!ApplicationPreferences.getSharedPreferences(appContext).contains(ApplicationPreferences.PREF_APPLICATION_RESTART_EVENTS_ALERT)) {
                                    SharedPreferences.Editor editor = ApplicationPreferences.getEditor(appContext);
                                    editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_RESTART_EVENTS_ALERT, ApplicationPreferences.applicationActivateWithAlert);

                                    String rescan;
                                    rescan = ApplicationPreferences.applicationEventLocationRescan;
                                    if (rescan.equals("0"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_LOCATION_RESCAN, "1");
                                    if (rescan.equals("2"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_LOCATION_RESCAN, "3");
                                    rescan = ApplicationPreferences.applicationEventWifiRescan;
                                    if (rescan.equals("0"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_WIFI_RESCAN, "1");
                                    if (rescan.equals("2"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_WIFI_RESCAN, "3");
                                    rescan = ApplicationPreferences.applicationEventBluetoothRescan;
                                    if (rescan.equals("0"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_BLUETOOTH_RESCAN, "1");
                                    if (rescan.equals("2"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_BLUETOOTH_RESCAN, "3");
                                    rescan = ApplicationPreferences.applicationEventMobileCellsRescan;
                                    if (rescan.equals("0"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_MOBILE_CELLS_RESCAN, "1");
                                    if (rescan.equals("2"))
                                        editor.putString(ApplicationPreferences.PREF_APPLICATION_EVENT_MOBILE_CELLS_RESCAN, "3");
                                    editor.apply();
                                }

                                // continue donation notification
                                if (PPApplication.getDaysAfterFirstStart(appContext) == 8)
                                    PPApplication.setDonationNotificationCount(appContext, 1);
                            }

                            if (actualVersionCode <= 3900) {
                                SharedPreferences preferences = ApplicationPreferences.getSharedPreferences(appContext);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_WIFI_SCAN_IF_WIFI_OFF,
                                        preferences.getBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_WIFI_ENABLE_WIFI, true));
                                editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_BLUETOOTH_SCAN_IF_BLUETOOTH_OFF,
                                        preferences.getBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_BLUETOOTH_ENABLE_BLUETOOTH, true));
                                editor.apply();
                            }

                            //if (actualVersionCode <= 4100) {
                            //    SharedPreferences preferences = appContext.getSharedPreferences(PPApplication.SHARED_PROFILE_PREFS_NAME, Context.MODE_PRIVATE);
                            //    if ((preferences.getInt(Profile.PREF_PROFILE_DEVICE_WIFI_AP, 0) == 3) &&
                            //            (Build.VERSION.SDK_INT >= 26)) {
                            //        // Toggle is not supported for wifi AP in Android 8+
                            //        SharedPreferences.Editor editor = preferences.edit();
                            //        editor.putInt(Profile.PREF_PROFILE_DEVICE_WIFI_AP, 0);
                            //        editor.apply();
                            //    }
                            //}

                            //if (actualVersionCode <= 4200) {
                            //    ApplicationPreferences.getSharedPreferences(appContext);
                            //    SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                            //    editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_FIRST_START, false);
                            //    editor.apply();

                            //    SharedPreferences preferences = appContext.getSharedPreferences(PPApplication.SHARED_PROFILE_PREFS_NAME, Context.MODE_PRIVATE);
                            //    if (preferences.getInt(Profile.PREF_PROFILE_LOCK_DEVICE, 0) == 3) {
                            //        editor = preferences.edit();
                            //        editor.putInt(Profile.PREF_PROFILE_LOCK_DEVICE, 1);
                            //        editor.apply();
                            //    }
                            //}

                            //if (actualVersionCode <= 4400) {
                            //    ApplicationPreferences.getSharedPreferences(appContext);
                            //    if (!ApplicationPreferences.preferences.contains(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_PREF_INDICATOR)) {
                            //        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                            //        editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_PREF_INDICATOR, ApplicationPreferences.applicationWidgetListPrefIndicator(appContext));
                            //        editor.putString(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_BACKGROUND, ApplicationPreferences.applicationWidgetListBackground(appContext));
                            //        editor.putString(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_LIGHTNESS_B, ApplicationPreferences.applicationWidgetListLightnessB(appContext));
                            //        editor.putString(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_LIGHTNESS_T, ApplicationPreferences.applicationWidgetListLightnessT(appContext));
                            //        editor.putString(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_ICON_COLOR, ApplicationPreferences.applicationWidgetListIconColor(appContext));
                            //        editor.putString(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_ICON_LIGHTNESS, ApplicationPreferences.applicationWidgetListIconLightness(appContext));
                            //        editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_ROUNDED_CORNERS, ApplicationPreferences.applicationWidgetListRoundedCorners(appContext));
                            //        editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_BACKGROUND_TYPE, ApplicationPreferences.applicationWidgetListBackgroundType(appContext));
                            //        editor.putString(ApplicationPreferences.PREF_APPLICATION_WIDGET_ONE_ROW_BACKGROUND_COLOR, ApplicationPreferences.applicationWidgetListBackgroundColor(appContext));
                            //        editor.apply();
                            //    }
                            //}

                            if (actualVersionCode <= 4550) {
                                if (Build.VERSION.SDK_INT < 29) {
                                    SharedPreferences preferences = ApplicationPreferences.getSharedPreferences(appContext);
                                    boolean darkBackground = preferences.getBoolean("notificationDarkBackground", false);
                                    if (darkBackground) {
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putString(ApplicationPreferences.PREF_NOTIFICATION_BACKGROUND_COLOR, "1");
                                        editor.apply();
                                    }
                                }
                            }

                            if (actualVersionCode <= 4600) {
                                List<Event> eventList = DatabaseHandler.getInstance(appContext).getAllEvents();
                                for (Event event : eventList) {
                                    if (!event._eventPreferencesCalendar._searchString.isEmpty()) {
                                        String searchStringOrig = event._eventPreferencesCalendar._searchString;
                                        String searchStringNew = "";
                                        String[] searchStringSplits = searchStringOrig.split("\\|");
                                        for (String split : searchStringSplits) {
                                            if (!split.isEmpty()) {
                                                String searchPattern = split;
                                                if (searchPattern.startsWith("!")) {
                                                    searchPattern =  "\\" + searchPattern;
                                                }
                                                if (!searchStringNew.isEmpty())
                                                    //noinspection StringConcatenationInLoop
                                                    searchStringNew = searchStringNew + "|";
                                                //noinspection StringConcatenationInLoop
                                                searchStringNew = searchStringNew + searchPattern;
                                            }
                                        }
                                        event._eventPreferencesCalendar._searchString = searchStringNew;
                                        DatabaseHandler.getInstance(appContext).updateEvent(event);
                                    }
                                }
                            }

                            if (actualVersionCode <= 4870) {
                                SharedPreferences.Editor editor = ApplicationPreferences.getEditor(appContext);
                                editor.putBoolean(EditorProfilesActivity.PREF_START_TARGET_HELPS_FILTER_SPINNER, true);

                                String theme = ApplicationPreferences.applicationTheme(appContext, false);
                                if (!(theme.equals("white") || theme.equals("dark") || theme.equals("night_mode"))) {
                                    String defaultValue = "white";
                                    if (Build.VERSION.SDK_INT >= 28)
                                        defaultValue = "night_mode";
                                    editor.putString(ApplicationPreferences.PREF_APPLICATION_THEME, defaultValue);
                                    GlobalGUIRoutines.switchNightMode(appContext, true);
                                }

                                editor.apply();
                            }

                            if (actualVersionCode <= 5020) {
                                //PPApplication.logE("PackageReplacedReceiver.doWork", "set \"night_mode\" theme");
                                if (Build.VERSION.SDK_INT >= 28) {
                                    SharedPreferences.Editor editor = ApplicationPreferences.getEditor(appContext);
                                    editor.putString(ApplicationPreferences.PREF_APPLICATION_THEME, "night_mode");
                                    GlobalGUIRoutines.switchNightMode(appContext, true);
                                    editor.apply();
                                }
                            }

                            if (actualVersionCode <= 5250) {
                                if (oldVersionCode <= 5210) {
                                    SharedPreferences.Editor editor = ApplicationPreferences.getEditor(appContext);

                                    if (Build.VERSION.SDK_INT >= 26) {
                                        NotificationManager manager = (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
                                        NotificationChannel channel = manager.getNotificationChannel(PPApplication.NOT_USED_MOBILE_CELL_NOTIFICATION_CHANNEL);

                                        editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_EVENT_MOBILE_CELL_NOT_USED_CELLS_DETECTION_NOTIFICATION_ENABLED,
                                                channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
                                    }

                                    int filterEventsSelectedItem = ApplicationPreferences.editorEventsViewSelectedItem;
                                    if (filterEventsSelectedItem == 2)
                                        filterEventsSelectedItem++;
                                    editor.putInt(ApplicationPreferences.EDITOR_EVENTS_VIEW_SELECTED_ITEM, filterEventsSelectedItem);
                                    editor.apply();
                                    ApplicationPreferences.editorEventsViewSelectedItem(appContext);
                                }
                            }

                            if (actualVersionCode <= 5330) {
                                if (oldVersionCode <= 5300) {
                                    // for old packages hide profile notification from status bar if notification is disabled
                                    if (Build.VERSION.SDK_INT < 26) {
                                        SharedPreferences preferences = ApplicationPreferences.getSharedPreferences(appContext);
                                        boolean notificationStatusBar = preferences.getBoolean(ApplicationPreferences.PREF_NOTIFICATION_STATUS_BAR, true);
                                        boolean notificationStatusBarPermanent = preferences.getBoolean(ApplicationPreferences.PREF_NOTIFICATION_STATUS_BAR_PERMANENT, true);
                                        if (!(notificationStatusBar && notificationStatusBarPermanent)) {
                                            SharedPreferences.Editor editor = preferences.edit();
                                            //PPApplication.logE("PackageReplacedReceiver.onReceive", "status bar is not permanent, set it!!");
                                            editor.putBoolean(ApplicationPreferences.PREF_NOTIFICATION_SHOW_IN_STATUS_BAR, false);
                                            editor.putBoolean(ApplicationPreferences.PREF_NOTIFICATION_USE_DECORATION, false);
                                            editor.putString(ApplicationPreferences.PREF_NOTIFICATION_LAYOUT_TYPE, "2");
                                            editor.apply();
                                        }
                                    }
                                }
                            }

                            if (actualVersionCode <= 5430) {
                                SharedPreferences preferences = ApplicationPreferences.getSharedPreferences(appContext);
                                String notificationBackgroundColor = preferences.getString(ApplicationPreferences.PREF_NOTIFICATION_BACKGROUND_COLOR, "0");
                                SharedPreferences.Editor editor = preferences.edit();
                                if (!preferences.contains(ApplicationPreferences.PREF_NOTIFICATION_BACKGROUND_CUSTOM_COLOR))
                                    editor.putInt(ApplicationPreferences.PREF_NOTIFICATION_BACKGROUND_CUSTOM_COLOR, 0xFFFFFFFF);
                                if (!preferences.contains(ApplicationPreferences.PREF_NOTIFICATION_NIGHT_MODE))
                                    editor.putBoolean(ApplicationPreferences.PREF_NOTIFICATION_NIGHT_MODE, false);
                                if (notificationBackgroundColor.equals("2")) {
                                    editor.putBoolean(ApplicationPreferences.PREF_NOTIFICATION_NIGHT_MODE, true);
                                    editor.putString(ApplicationPreferences.PREF_NOTIFICATION_BACKGROUND_COLOR, "1");
                                }
                                else
                                if (notificationBackgroundColor.equals("4")) {
                                    editor.putBoolean(ApplicationPreferences.PREF_NOTIFICATION_NIGHT_MODE, true);
                                    editor.putString(ApplicationPreferences.PREF_NOTIFICATION_BACKGROUND_COLOR, "3");
                                    editor.apply();
                                }
                                editor.apply();
                            }
                        }
                    } catch (Exception ignored) {
                    }

                    PPApplication.loadApplicationPreferences(appContext);
                    PPApplication.loadGlobalApplicationData(appContext);
                    PPApplication.loadProfileActivationData(appContext);

                    /*
                    ApplicationPreferences.getSharedPreferences(appContext);
                    SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
                    editor.putString(ApplicationPreferences.PREF_APPLICATION_THEME, "white");
                    editor.apply();
                    */

                    PPApplication.logE("PackageReplacedReceiver.doWork", "PhoneStateScanner.enabledAutoRegistration="+PhoneStateScanner.enabledAutoRegistration);
                    if (PhoneStateScanner.enabledAutoRegistration) {
                        PhoneStateScanner.stopAutoRegistration(appContext);
                        PPApplication.logE("PackageReplacedReceiver.doWork", "start of wait for end of autoregistration");
                        int count = 0;
                        while (MobileCellsRegistrationService.serviceStarted && (count < 50)) {
                            PPApplication.sleep(100);
                            count++;
                        }
                        PPApplication.logE("PackageReplacedReceiver.doWork", "end of autoregistration");
                    }

                    /*SharedPreferences sharedPreferences = ApplicationPreferences.getSharedPreferences(appContext);
                    if (sharedPreferences != null) {
                        PPApplication.logE("--------------- PackageReplacedReceiver.doWork", "package replaced set to false");
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(ApplicationPreferences.PREF_APPLICATION_PACKAGE_REPLACED, false);
                        editor.apply();
                    }*/
                    PPApplication.applicationPackageReplaced = false;

                    startService(dataWrapper);

                    PPApplication.logE("PackageReplacedReceiver.doWork", "END");
                    break;
                case DELAYED_WORK_HANDLE_EVENTS:
                    if (sensorType != null) {
                        // start events handler
                        EventsHandler eventsHandler = new EventsHandler(appContext);
                        eventsHandler.handleEvents(sensorType);
                    }
                    break;
                case DELAYED_WORK_START_WIFI_SCAN:
                    WifiScanWorker.startScan(appContext);
                    break;
                case DELAYED_WORK_BLOCK_PROFILE_EVENT_ACTIONS:
                    PPApplication.blockProfileEventActions = false;
                    break;
                case DELAYED_WORK_CLOSE_ALL_APPLICATIONS:
                    if (!PPApplication.blockProfileEventActions) {
                        try {
                            /*boolean appFound = false;
                            // intentionally using string value as Context.USAGE_STATS_SERVICE was
                            // strangely only added in API 22 (LOLLIPOP_MR1)
                            @SuppressLint("WrongConstant")
                            UsageStatsManager usm = (UsageStatsManager)appContext.getSystemService("usagestats");
                            long time = System.currentTimeMillis();
                            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,time - 1000 * 1000, time);
                            if (appList != null && appList.size() > 0) {
                                appFound = true;
//                                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
//                                for (UsageStats usageStats : appList) {
//                                    mySortedMap.put(usageStats.getLastTimeUsed(),
//                                            usageStats);
//                                }
//                                if (mySortedMap != null && !mySortedMap.isEmpty()) {
//                                    currentApp = mySortedMap.get(
//                                            mySortedMap.lastKey()).getPackageName();
//                                }
                            }*/

                            /*boolean appFound = false;
                            ActivityManager manager = (ActivityManager)appContext.getSystemService(Context.ACTIVITY_SERVICE);
                            List<ActivityManager.RunningAppProcessInfo> tasks = manager.getRunningAppProcesses();
                            Log.e("DelayedWorksWorker.doWork", "tasks="+tasks);
                            if ((tasks != null) && (!tasks.isEmpty())) {
                                Log.e("DelayedWorksWorker.doWork", "tasks.size()="+tasks.size());
                                for (ActivityManager.RunningAppProcessInfo task : tasks) {
                                    Log.e("DelayedWorksWorker.doWork", "task.processName="+task.processName);
                                    if (task.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                                        Log.e("DelayedWorksWorker.doWork", "IMPORTANCE_FOREGROUND");
                                        appFound = true;
                                        break;
                                    }
                                }
                            }*/
                            //if (appFound) {
                                Intent startMain = new Intent(Intent.ACTION_MAIN);
                                startMain.addCategory(Intent.CATEGORY_HOME);
                                startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                appContext.startActivity(startMain);
                            //}
                        } catch (Exception e) {
                            Log.e("DelayedWorksWorker.doWork", Log.getStackTraceString(e));
                            FirebaseCrashlytics.getInstance().recordException(e);
                            //Crashlytics.logException(e);
                        }
                    }
                    break;
                /*case DELAYED_WORK_CHANGE_FILTER_AFTER_EDITOR_DATA_CHANGE:
                    if (filterSelectedItem != 0) {
                        Activity activity = PPApplication.getEditorActivity();
                        if (activity instanceof EditorProfilesActivity) {
                            final EditorProfilesActivity editorActivity = (EditorProfilesActivity)activity;
                            Fragment fragment = editorActivity.getSupportFragmentManager().findFragmentById(R.id.editor_list_container);
                            if (fragment instanceof EditorProfileListFragment) {
                                EditorProfileListFragment profileFragment = (EditorProfileListFragment) fragment;
                                boolean changeFilter = false;
                                Profile scrollToProfile = DatabaseHandler.getInstance(context).getProfile(profileId, false);
                                if (scrollToProfile != null) {
                                    switch (filterSelectedItem) {
                                        case EditorProfilesActivity.DSI_PROFILES_NO_SHOW_IN_ACTIVATOR:
                                            changeFilter = scrollToProfile._showInActivator;
                                            break;
                                        case EditorProfilesActivity.DSI_PROFILES_SHOW_IN_ACTIVATOR:
                                            changeFilter = !scrollToProfile._showInActivator;
                                            break;
                                    }
                                }
                                if (changeFilter) {
                                    profileFragment.scrollToProfile = scrollToProfile;
                                    Handler handler = new Handler(context.getMainLooper());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((GlobalGUIRoutines.HighlightedSpinnerAdapter) editorActivity.filterSpinner.getAdapter()).setSelection(0);
                                            editorActivity.selectFilterItem(0, 0, false, true);
                                        }
                                    });
                                } else
                                    profileFragment.scrollToProfile = null;
                            }
                            if (fragment instanceof EditorEventListFragment) {
                                EditorEventListFragment eventFragment = (EditorEventListFragment) fragment;
                                boolean changeFilter = false;
                                Event scrollToEvent = DatabaseHandler.getInstance(context).getEvent(eventId);
                                if (scrollToEvent != null) {
                                    switch (filterSelectedItem) {
                                        case EditorProfilesActivity.DSI_EVENTS_NOT_STOPPED:
                                            changeFilter = scrollToEvent.getStatus() == Event.ESTATUS_STOP;
                                            break;
                                        case EditorProfilesActivity.DSI_EVENTS_RUNNING:
                                            changeFilter = scrollToEvent.getStatus() != Event.ESTATUS_RUNNING;
                                            break;
                                        case EditorProfilesActivity.DSI_EVENTS_PAUSED:
                                            changeFilter = scrollToEvent.getStatus() != Event.ESTATUS_PAUSE;
                                            break;
                                        case EditorProfilesActivity.DSI_EVENTS_STOPPED:
                                            changeFilter = scrollToEvent.getStatus() != Event.ESTATUS_STOP;
                                            break;
                                    }
                                }
                                if (changeFilter) {
                                    eventFragment.scrollToEvent = scrollToEvent;
                                    Handler handler = new Handler(context.getMainLooper());
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ((GlobalGUIRoutines.HighlightedSpinnerAdapter) editorActivity.filterSpinner.getAdapter()).setSelection(0);
                                            editorActivity.selectFilterItem(1, 0, false, true);
                                        }
                                    });
                                } else
                                    eventFragment.scrollToEvent = null;
                            }
                        }
                    }
                    break;*/
                default:
                    break;
            }

            return Result.success();
        } catch (Exception e) {
            Log.e("DelayedWorksWorker.doWork", Log.getStackTraceString(e));
            FirebaseCrashlytics.getInstance().recordException(e);
            //Crashlytics.logException(e);
            /*Handler _handler = new Handler(getApplicationContext().getMainLooper());
            Runnable r = new Runnable() {
                public void run() {
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            };
            _handler.postDelayed(r, 1000);*/
            return Result.failure();
        }
    }

    /*
    private Data generateResult(int resultCode, String message, boolean updateName) {
        // Create the output of the work
        PPApplication.logE("FetchAddressWorker.generateResult", "resultCode="+resultCode);
        PPApplication.logE("FetchAddressWorker.generateResult", "message="+message);
        PPApplication.logE("FetchAddressWorker.generateResult", "updateName="+updateName);

        return new Data.Builder()
                .putInt(LocationGeofenceEditorActivity.RESULT_CODE, resultCode)
                .putString(LocationGeofenceEditorActivity.RESULT_DATA_KEY, message)
                .putBoolean(LocationGeofenceEditorActivity.UPDATE_NAME_EXTRA, updateName)
                .build();
    }
    */

    private void startService(DataWrapper dataWrapper) {
        boolean isApplicationStarted = PPApplication.getApplicationStarted(false);
        PPApplication.logE("PackageReplacedReceiver.startService", "isApplicationStarted="+isApplicationStarted);

        PPApplication.exitApp(false, dataWrapper.context, dataWrapper, null, false/*, false, true*/);

        if (isApplicationStarted)
        {
            PPApplication.logE("PackageReplacedReceiver.startService", "start of wait for end of service");
            int count = 0;
            while ((PhoneProfilesService.getInstance() != null) && (count < 50)) {
                PPApplication.sleep(100);
                count++;
            }
            PPApplication.logE("PackageReplacedReceiver.startService", "service ended");

            // start PhoneProfilesService
            //PPApplication.logE("DelayedWorksWorker.doWork", "xxx");
            PPApplication.setApplicationStarted(dataWrapper.context, true);
            Intent serviceIntent = new Intent(dataWrapper.context, PhoneProfilesService.class);
            //serviceIntent.putExtra(PhoneProfilesService.EXTRA_ONLY_START, true);
            //serviceIntent.putExtra(PhoneProfilesService.EXTRA_DEACTIVATE_PROFILE, true);
            serviceIntent.putExtra(PhoneProfilesService.EXTRA_START_ON_PACKAGE_REPLACE, true);
            serviceIntent.putExtra(PhoneProfilesService.EXTRA_ACTIVATE_PROFILES, true);
            PPApplication.startPPService(dataWrapper.context, serviceIntent);
        }
    }

}

package sk.henrichg.phoneprofilesplus;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;

public class ScreenOnOffBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
//        if (intent != null)
//            PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive", "intent.getAction()="+intent.getAction());
//        else
//            PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive", "xxx");

        /*if (intent != null)
            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "intent.getAction()="+intent.getAction());
        else
            return;*/
        if (intent == null)
            return;

        if (!PPApplication.getApplicationStarted(true))
            // application is not started
            return;

        final String action = intent.getAction();
        if (action == null)
            return;

        //PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "before start handler");
        final Context appContext = context.getApplicationContext();
        PPApplication.startHandlerThreadBroadcast(/*"ScreenOnOffBroadcastReceiver.onReceive"*/);
        final Handler __handler = new Handler(PPApplication.handlerThreadBroadcast.getLooper());
        //__handler.post(new PPApplication.PPHandlerThreadRunnable(context.getApplicationContext()) {
        __handler.post(() -> {
//            PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThread", "START run - from=ScreenOnOffBroadcastReceiver.onReceive");

            //Context appContext= appContextWeakRef.get();

            //if (appContext != null) {
                PowerManager powerManager = (PowerManager) appContext.getSystemService(Context.POWER_SERVICE);
                PowerManager.WakeLock wakeLock = null;
                try {
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, PPApplication.PACKAGE_NAME + ":ScreenOnOffBroadcastReceiver_onReceive");
                        wakeLock.acquire(10 * 60 * 1000);
                    }

                    //if (PPApplication.logEnabled()) {
                    //PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "start of handler post");
                    //}

//                    PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", " start wakelock - action="+action);

                    switch (action) {
                        case Intent.ACTION_SCREEN_ON: {
//                            if (PPApplication.logEnabled()) {
//                                PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen on");
//                                PPApplication.logE("[XXX] ScreenOnOffBroadcastReceiver.onReceive", "restartAllScanners");
//                            }

                            PPApplication.isScreenOn = true;

                            if (!ApplicationPreferences.prefLockScreenDisabled) {
//                                PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "action user present - switch keyguard - start");
                                // enable/disable keyguard
                                try {
                                    PhoneProfilesService ppService = PhoneProfilesService.getInstance();
                                    if (ppService != null) {
                                        ppService.switchKeyguard();
                                    }
                                } catch (Exception e) {
                                    PPApplication.recordException(e);
                                }
//                                PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "action user present - switch keyguard - end");
                            }

                            // reset brightness
                            if (PPApplication.deviceIsHuawei && PPApplication.romIsEMUI) {
                                if (ApplicationPreferences.applicationForceSetBrightnessAtScreenOn) {
                                    final Profile profile = DatabaseHandler.getInstance(appContext).getActivatedProfile();
                                    if (profile != null) {
                                        if (profile.getDeviceBrightnessChange()) {
                                            if (Permissions.checkProfileScreenBrightness(appContext, profile, null)) {
                                                try {
//                                                    if (PPApplication.logEnabled()) {
//                                                        int brightnessMode = Settings.System.getInt(appContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
//                                                        int brightness = Settings.System.getInt(appContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
//                                                        float adaptiveBrightness = Settings.System.getFloat(appContext.getContentResolver(), Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, -1);
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (1)", "brightness mode=" + brightnessMode);
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (1)", "manual brightness value=" + brightness);
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (1)", "adaptive brightness value=" + adaptiveBrightness);
//                                                    }
//
//                                                    if (PPApplication.logEnabled()) {
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (2)", "brightness mode=" + profile.getDeviceBrightnessAutomatic());
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (2)", "manual brightness value=" + profile.getDeviceBrightnessManualValue(appContext));
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (2)", "adaptive brightness value=" + profile.getDeviceBrightnessAdaptiveValue(appContext));
//                                                    }
                                                    try {
                                                        if (profile.getDeviceBrightnessAutomatic()) {
                                                            Settings.System.putInt(appContext.getContentResolver(),
                                                                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                                                                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                                                            if (Profile.isProfilePreferenceAllowed(Profile.PREF_PROFILE_DEVICE_ADAPTIVE_BRIGHTNESS, null, null, false, appContext).allowed
                                                                    == PreferenceAllowed.PREFERENCE_ALLOWED) {
                                                                Settings.System.putInt(appContext.getContentResolver(),
                                                                        Settings.System.SCREEN_BRIGHTNESS,
                                                                        profile.getDeviceBrightnessManualValue(appContext));
                                                                try {
                                                                    Settings.System.putFloat(appContext.getContentResolver(),
                                                                            Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ,
                                                                            profile.getDeviceBrightnessAdaptiveValue(appContext));
                                                                } catch (Exception ee) {
                                                                    ActivateProfileHelper.executeRootForAdaptiveBrightness(
                                                                            profile.getDeviceBrightnessAdaptiveValue(appContext),
                                                                            appContext);
                                                                }
                                                            }
                                                        } else {
                                                            Settings.System.putInt(appContext.getContentResolver(),
                                                                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                                                                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                                                            Settings.System.putInt(appContext.getContentResolver(),
                                                                    Settings.System.SCREEN_BRIGHTNESS,
                                                                    profile.getDeviceBrightnessManualValue(appContext));
                                                        }
                                                    } catch (Exception ignored) {
                                                    }
//                                                    if (PPApplication.logEnabled()) {
//                                                        int brightnessMode = Settings.System.getInt(appContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
//                                                        int brightness = Settings.System.getInt(appContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
//                                                        float adaptiveBrightness = Settings.System.getFloat(appContext.getContentResolver(), Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, -1);
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (3)", "brightness mode=" + brightnessMode);
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (3)", "manual brightness value=" + brightness);
//                                                        PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (3)", "adaptive brightness value=" + adaptiveBrightness);
//                                                    }
                                                } catch (Exception e) {
                                                    PPApplication.recordException(e);
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // change screen timeout
                            // WARNING: must be called after PPApplication.isScreenOn = true;
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen on - setScreenTimeout - start");
                            setProfileScreenTimeoutSavedWhenScreenOff(appContext);
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen on - setScreenTimeout - end");

                            // restart scanners for screen on when any is enabled
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen on - restart scanners - start");
                            boolean restart = false;
                            if (ApplicationPreferences.applicationEventLocationEnableScanning)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventWifiEnableScanning)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventBluetoothEnableScanning)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventMobileCellEnableScanning)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventOrientationEnableScanning)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventPeriodicScanningEnableScanning)
                                restart = true;
                            if (restart) {
                                //PPApplication.logE("[RJS] ScreenOnOffBroadcastReceiver.onReceive", "restart all scanners for SCREEN_ON");
                                // for screenOn=true -> used only for Location scanner - start scan with GPS On
                                PPApplication.restartAllScanners(appContext, false);
                            }
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen on - restart scanners - end");
                            break;
                        }
                        case Intent.ACTION_SCREEN_OFF: {
//                        if (PPApplication.logEnabled()) {
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen off");
//                            PPApplication.logE("[XXX] ScreenOnOffBroadcastReceiver.onReceive", "restartAllScanners");
//                        }

                            PPApplication.isScreenOn = false;
//                            PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive", "isScreenOn="+PPApplication.isScreenOn);

//                            if (PPApplication.logEnabled()) {
//                                PPApplication.brightnessModeBeforeScreenOff = Settings.System.getInt(appContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, -1);
//                                PPApplication.brightnessBeforeScreenOff = Settings.System.getInt(appContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, -1);
//                                PPApplication.adaptiveBrightnessBeforeScreenOff = Settings.System.getFloat(appContext.getContentResolver(), Settings.System.SCREEN_AUTO_BRIGHTNESS_ADJ, -1);
//                                PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (1)", "brightness mode=" + PPApplication.brightnessModeBeforeScreenOff);
//                                PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (1)", "manual brightness value=" + PPApplication.brightnessBeforeScreenOff);
//                                PPApplication.logE("[IN_BROADCAST] ScreenOnOffBroadcastReceiver.onReceive (1)", "adaptive brightness value=" + PPApplication.adaptiveBrightnessBeforeScreenOff);
//                            }

                            // call this, because device may not be locked immediately after screen off
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen off - lock device set alarm - start");
                            KeyguardManager keyguardManager = (KeyguardManager) appContext.getSystemService(Context.KEYGUARD_SERVICE);
                            if (keyguardManager != null) {
                                boolean keyguardShowing = keyguardManager.isKeyguardLocked();
//                                PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "keyguardShowing=" + keyguardShowing);

                                if (!keyguardShowing) {
                                    int lockDeviceTime = Settings.Secure.getInt(appContext.getContentResolver(), "lock_screen_lock_after_timeout", 0);
                                    //PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "lockDeviceTime="+lockDeviceTime);
                                    if (lockDeviceTime > 0) {
                                        if (DatabaseHandler.getInstance(appContext).getTypeEventsCount(DatabaseHandler.ETYPE_SCREEN) > 0) {
                                            // call this only when any event with screen sensor exists
                                            // it is not needed to call it again for calendar sensor, because was called for screen off
//                                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "call LockDeviceAfterScreenOffBroadcastReceiver.setAlarm");
                                            LockDeviceAfterScreenOffBroadcastReceiver.setAlarm(lockDeviceTime, appContext);
                                        }
                                    }
                                }
                            }
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen off - lock device set alarm - end");

                            //PPApplication.logE("[RJS] ScreenOnOffBroadcastReceiver.onReceive", "restart all scanners for SCREEN_OFF");

                            // for screen off restart scanners only when it is required for any scanner
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen off - lock device set alarm - restart scanners - start");
                            boolean restart = false;
                            //noinspection RedundantIfStatement
                            if (ApplicationPreferences.applicationEventPeriodicScanningEnableScanning &&
                                    ApplicationPreferences.applicationEventPeriodicScanningScanOnlyWhenScreenIsOn)
                                restart = true;
                            if (ApplicationPreferences.applicationEventLocationEnableScanning &&
                                    ApplicationPreferences.applicationEventLocationScanOnlyWhenScreenIsOn)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventWifiEnableScanning &&
                                    ApplicationPreferences.applicationEventWifiScanOnlyWhenScreenIsOn)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventBluetoothEnableScanning &&
                                    ApplicationPreferences.applicationEventBluetoothScanOnlyWhenScreenIsOn)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventMobileCellEnableScanning &&
                                    ApplicationPreferences.applicationEventMobileCellScanOnlyWhenScreenIsOn)
                                restart = true;
                            else if (ApplicationPreferences.applicationEventOrientationEnableScanning &&
                                    ApplicationPreferences.applicationEventOrientationScanOnlyWhenScreenIsOn)
                                restart = true;
                            if (restart) {
                                // for screenOn=false -> used only for Location scanner - use last usage of GPS for scan
                                //PPApplication.setBlockProfileEventActions(true);
                                PPApplication.restartAllScanners(appContext, false);
                            }
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screen off - lock device set alarm - restart scanners - end");

                            final Handler handler1 = new Handler(appContext.getMainLooper());
                            handler1.post(() -> {
//                                PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThread", "run - from=ScreenOnOffBroadcastReceiver.onReceive - screen off - finish ock actovity - start");
                                if (PPApplication.lockDeviceActivity != null) {
                                    try {
                                        PPApplication.lockDeviceActivity.finish();
                                    } catch (Exception e) {
                                        PPApplication.recordException(e);
                                    }
                                }
//                                PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThread", "run - from=ScreenOnOffBroadcastReceiver.onReceive - screen off - finish ock actovity - end");
                            });

                            break;
                        }
                        case Intent.ACTION_USER_PRESENT:
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "action user present");

                            PPApplication.isScreenOn = true;

                            // change screen timeout
                            // WARNING: must be called after PPApplication.isScreenOn = true;
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "action user present - setScrteenTimeput - start");
                            setProfileScreenTimeoutSavedWhenScreenOff(appContext);
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "action user present - setScrteenTimeput - end");

                            if (ApplicationPreferences.prefLockScreenDisabled) {
//                                PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "action user present - switch keyguard - start");
                                // enable/disable keyguard
                                try {
                                    PhoneProfilesService ppService = PhoneProfilesService.getInstance();
                                    if (ppService != null) {
                                        ppService.switchKeyguard();
                                    }
                                } catch (Exception e) {
                                    PPApplication.recordException(e);
                                }
//                                PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "action user present - switch keyguard - end");
                            }

                            break;
                    }

                    if (Event.getGlobalEventsRunning()) {
                        //PPApplication.logE("****** EventsHandler.handleEvents", "START run - from=ScreenOnOffBroadcastReceiver.onReceive");

//                        PPApplication.logE("[EVENTS_HANDLER_CALL] ScreenOnOffBroadcastReceiver.onReceive", "sensorType=SENSOR_TYPE_SCREEN");
                        EventsHandler eventsHandler = new EventsHandler(appContext);
                        eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_SCREEN);

//                        PPApplication.logE("****** EventsHandler.handleEvents", "END run - from=ScreenOnOffBroadcastReceiver.onReceive");
                    }

                    PhoneProfilesService.drawProfileNotification(false, appContext);

                    /*if (PPApplication.logEnabled()) {
                        PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "end of handler post");
                        PPApplication.logE("PPApplication.startHandlerThread", "END run - from=ScreenOnOffBroadcastReceiver.onReceive");
                    }*/

//                    PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", " end wakelock - action="+action);

                } catch (Exception e) {
//                    PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThread", Log.getStackTraceString(e));
                    PPApplication.recordException(e);
                } finally {
                    if ((wakeLock != null) && wakeLock.isHeld()) {
                        try {
                            wakeLock.release();
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", " wakelock released");
                        } catch (Exception e) {
//                            PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", Log.getStackTraceString(e));
                        }
                    }
                }
            //}

//            PPApplication.logE("[IN_THREAD_HANDLER] PPApplication.startHandlerThread", "END run - from=ScreenOnOffBroadcastReceiver.onReceive");

        });
        //PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "after start handler");
    }

    private void setProfileScreenTimeoutSavedWhenScreenOff(Context appContext) {
        final int screenTimeout = ApplicationPreferences.prefActivatedProfileScreenTimeoutWhenScreenOff;
        //PPApplication.logE("@@@ ScreenOnOffBroadcastReceiver.onReceive", "screenTimeout=" + screenTimeout);
        if ((screenTimeout > 0) && (Permissions.checkScreenTimeout(appContext))) {
            if (PPApplication.screenTimeoutHandler != null) {
                PPApplication.screenTimeoutHandler.post(() -> ActivateProfileHelper.setScreenTimeout(screenTimeout, false, appContext));
            }
        }
    }

}

package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.database.ContentObserver;
import android.media.AudioManager;
import android.os.Handler;
import android.provider.Settings;

class SettingsContentObserver  extends ContentObserver {

    //public static boolean internalChange = false;

    private static int previousVolumeRing = 0;
    private static int previousVolumeNotification = 0;
    //private static int previousVolumeMusic = 0;
    //private static int previousVolumeAlarm = 0;
    //private static int previousVolumeSystem = 0;
    //private static int previousVolumeVoice = 0;
    //private int defaultRingerMode = 0;
    private static int previousScreenTimeout = 0;

    private final Context context;

    SettingsContentObserver(Context c, Handler handler) {
        super(handler);

        context=c;

        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            previousVolumeRing = audioManager.getStreamVolume(AudioManager.STREAM_RING);
            previousVolumeNotification = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
            //previousVolumeMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            //previousVolumeAlarm = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
            //previousVolumeSystem = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);
            //previousVolumeVoice = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        }
    }

    /*
    @Override
    public boolean deliverSelfNotifications() {
        return super.deliverSelfNotifications();
    }
    */

    private int volumeChangeDetect(int volumeStream, int previousVolume, AudioManager audioManager) {

        int currentVolume = audioManager.getStreamVolume(volumeStream);
        PPApplication.logE("### SettingsContentObserver", "channel=" + volumeStream + " currentVolume=" + currentVolume);
        PPApplication.logE("### SettingsContentObserver", "channel=" + volumeStream + " previousVolume=" + previousVolume);
        PPApplication.logE("### SettingsContentObserver", "internalChange="+RingerModeChangeReceiver.internalChange);

        int delta=previousVolume-currentVolume;

        if(delta>0)
        {
            if (!RingerModeChangeReceiver.internalChange) {
                if (volumeStream == AudioManager.STREAM_RING) {
                    ActivateProfileHelper.setRingerVolume(context, currentVolume);
                    PhoneProfilesService.ringingVolume = currentVolume;
                }
                if (volumeStream == AudioManager.STREAM_NOTIFICATION) {
                    ActivateProfileHelper.setNotificationVolume(context, currentVolume);
                    //PhoneProfilesService.notificationVolume = currentVolume;
                }
            }
        }
        else if(delta<0)
        {
            if (!RingerModeChangeReceiver.internalChange) {
                if (volumeStream == AudioManager.STREAM_RING) {
                    ActivateProfileHelper.setRingerVolume(context, currentVolume);
                    PhoneProfilesService.ringingVolume = currentVolume;
                }
                if (volumeStream == AudioManager.STREAM_NOTIFICATION) {
                    ActivateProfileHelper.setNotificationVolume(context, currentVolume);
                    //PhoneProfilesService.notificationVolume = currentVolume;
                }
            }
        }
        return currentVolume;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);

        CallsCounter.logCounter(context, "SettingsContentObserver.onChange", "SettingsContentObserver_onChange");

        ////// volume change
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int audioMode = audioManager.getMode();

            if ((audioMode == AudioManager.MODE_NORMAL) || (audioMode == AudioManager.MODE_RINGTONE)) {
                previousVolumeRing = volumeChangeDetect(AudioManager.STREAM_RING, previousVolumeRing, audioManager);
                previousVolumeNotification = volumeChangeDetect(AudioManager.STREAM_NOTIFICATION, previousVolumeNotification, audioManager);
                //previousVolumeMusic = volumeChangeDetect(AudioManager.STREAM_MUSIC, previousVolumeMusic, audioManager);
                //previousVolumeAlarm = volumeChangeDetect(AudioManager.STREAM_ALARM, previousVolumeAlarm, audioManager);
                //previousVolumeSystem = volumeChangeDetect(AudioManager.STREAM_SYSTEM, previousVolumeSystem, audioManager);
            }
            //previousVolumeVoice = volumeChangeDetect(AudioManager.STREAM_VOICE_CALL, previousVolumeVoice, audioManager);
            //////////////
        }

        ////// screen timeout change
        int screenTimeout = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 0);
        if (!ActivateProfileHelper.disableScreenTimeoutInternalChange) {
            if (previousScreenTimeout != screenTimeout) {
                if (Permissions.checkScreenTimeout(context)) {
                    ActivateProfileHelper.setActivatedProfileScreenTimeout(context, 0);
                    if (PPApplication.screenTimeoutHandler != null) {
                        PPApplication.screenTimeoutHandler.post(new Runnable() {
                            public void run() {
                                ActivateProfileHelper.screenTimeoutUnlock(context);
                            }
                        });
                    }// else
                    //    ActivateProfileHelper.screenTimeoutUnlock(context);
                }
            }
        }
        previousScreenTimeout = screenTimeout;
        /////////////
    }

}

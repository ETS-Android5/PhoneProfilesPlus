package sk.henrichg.phoneprofilesplus;

import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.support.PersistableBundleCompat;

import java.util.concurrent.TimeUnit;

class GeofenceScannerJob extends Job {

    static final String JOB_TAG  = "GeofenceScannerJob";
    //static final String JOB_TAG_START  = "GeofenceScannerJob_start";

    //private static CountDownLatch countDownLatch = null;

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        PPApplication.logE("GeofenceScannerJob.onRunJob", "xxx");

        Context context = getContext();

        CallsCounter.logCounter(context, "GeofenceScannerJob.onRunJob", "GeofenceScannerJob_onRunJob");

        //countDownLatch = new CountDownLatch(1);

        //boolean isPowerSaveMode = PPApplication.isPowerSaveMode;
        boolean isPowerSaveMode = DataWrapper.isPowerSaveMode(context);
        if (isPowerSaveMode && ApplicationPreferences.applicationEventLocationUpdateInPowerSaveMode(context).equals("2")) {
            PPApplication.logE("GeofenceScannerJob.onRunJob", "update in power save mode is not allowed = cancel job");
            GeofenceScannerJob.cancelJob(false,null);
            return Result.SUCCESS;
        }

        if (Event.getGlobalEventsRunning(context)) {
            if ((!params.getExtras().getBoolean("shortInterval", false)) ||
                params.getExtras().getBoolean("notShortIsExact", true)) {

                boolean geofenceScannerUpdatesStarted = false;
                synchronized (PPApplication.geofenceScannerMutex) {
                    if ((PhoneProfilesService.instance != null) && (PhoneProfilesService.instance.getGeofencesScanner() != null)) {
                        if (PhoneProfilesService.instance.getGeofencesScanner().mUpdatesStarted) {
                            PPApplication.logE("GeofenceScannerJob.onRunJob", "location updates started - save to DB");

                            if ((PhoneProfilesService.instance != null) && PhoneProfilesService.instance.isGeofenceScannerStarted())
                                PhoneProfilesService.instance.getGeofencesScanner().updateGeofencesInDB();

                            geofenceScannerUpdatesStarted = true;
                        }
                    }
                }

                if (geofenceScannerUpdatesStarted) {
                    PPApplication.logE("GeofenceScannerJob.onRunJob", "location updates started - start EventsHandler");

                    // start events handler
                    EventsHandler eventsHandler = new EventsHandler(context);
                    eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_GEOFENCES_SCANNER/*, false*/);

                }
            }
        }

        GeofenceScannerJob.scheduleJob(context, false, null, false/*, false*/);

        /*
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
        countDownLatch = null;
        PPApplication.logE("GeofenceScannerJob.onRunJob", "return");
        */
        return Result.SUCCESS;
    }

    private static void _scheduleJob(final Context context, boolean shortInterval/*, final boolean forScreenOn*/) {
        JobManager jobManager = null;
        try {
            jobManager = JobManager.instance();
        } catch (Exception ignored) {
        }

        if (jobManager != null) {
            final JobRequest.Builder jobBuilder;

            int interval;
            synchronized (PPApplication.geofenceScannerMutex) {
                if ((PhoneProfilesService.instance != null) && PhoneProfilesService.instance.isGeofenceScannerStarted())
                    PPApplication.logE("GeofenceScannerJob.scheduleJob", "mUpdatesStarted=" + PhoneProfilesService.instance.getGeofencesScanner().mUpdatesStarted);
                else
                    PPApplication.logE("GeofenceScannerJob.scheduleJob", "mUpdatesStarted=false");

                // look at GeofenceScanner:UPDATE_INTERVAL_IN_MILLISECONDS
                //int updateDuration = 30;

                if ((PhoneProfilesService.instance != null) && PhoneProfilesService.instance.isGeofenceScannerStarted() &&
                        PhoneProfilesService.instance.getGeofencesScanner().mUpdatesStarted) {
                    interval = ApplicationPreferences.applicationEventLocationUpdateInterval(context) * 60;
                    PPApplication.logE("GeofenceScannerJob.scheduleJob", "interval=" + interval);
                    //boolean isPowerSaveMode = PPApplication.isPowerSaveMode;
                    boolean isPowerSaveMode = DataWrapper.isPowerSaveMode(context);
                    if (isPowerSaveMode && ApplicationPreferences.applicationEventLocationUpdateInPowerSaveMode(context).equals("1"))
                        interval = 2 * interval;
                    //interval = interval - updateDuration;
                } else {
                    interval = 5;
                    shortInterval = true;
                }
            }

            if (!shortInterval) {

                //jobManager.cancelAllForTag(JOB_TAG_START);

                jobBuilder = new JobRequest.Builder(JOB_TAG);

                if (TimeUnit.SECONDS.toMillis(interval) < JobRequest.MIN_INTERVAL) {
                    jobManager.cancelAllForTag(JOB_TAG);
                    jobBuilder.setExact(TimeUnit.SECONDS.toMillis(interval));
                } else {
                    int requestsForTagSize = jobManager.getAllJobRequestsForTag(JOB_TAG).size();
                    PPApplication.logE("GeofenceScannerJob.scheduleJob", "requestsForTagSize=" + requestsForTagSize);
                    if (requestsForTagSize == 0) {
                        if (TimeUnit.SECONDS.toMillis(interval) < JobRequest.MIN_INTERVAL)
                            // must be set min interval because:
                            //   java.lang.IllegalArgumentException: intervalMs is out of range of [900000, 9223372036854775807] (too low)
                            jobBuilder.setPeriodic(JobRequest.MIN_INTERVAL);
                        else
                            jobBuilder.setPeriodic(TimeUnit.SECONDS.toMillis(interval));
                    } else
                        return;
                }
            } else {
                _cancelJob();
                jobBuilder = new JobRequest.Builder(JOB_TAG/*_START*/);
                /*if (forScreenOn)
                    jobBuilder.setExact(TimeUnit.SECONDS.toMillis(5));
                else
                    jobBuilder.setExact(TimeUnit.SECONDS.toMillis(5));*/
                jobBuilder.startNow();
            }

            PPApplication.logE("GeofenceScannerJob.scheduleJob", "build and schedule");

            try {
                PersistableBundleCompat bundleCompat = new PersistableBundleCompat();
                bundleCompat.putBoolean("shortInterval", shortInterval);
                bundleCompat.putBoolean("notShortIsExact", TimeUnit.MINUTES.toMillis(interval) < JobRequest.MIN_INTERVAL);

                jobBuilder
                        .setUpdateCurrent(false) // don't update current, it would cancel this currently running job
                        .build()
                        .scheduleAsync();
            } catch (Exception ignored) {
            }
        }
    }

    static void scheduleJob(final Context context,
                            @SuppressWarnings("SameParameterValue") final boolean useHandler,
                            final Handler _handler, final boolean startScanning/*, final boolean forScreenOn*/) {
        PPApplication.logE("GeofenceScannerJob.scheduleJob", "startScanning="+startScanning);

        //if ((PhoneProfilesService.instance != null) && PhoneProfilesService.isGeofenceScannerStarted()) {
            if (useHandler && (_handler == null)) {
                PPApplication.startHandlerThread("GeofenceScannerJob.scheduleJob");
                final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        _scheduleJob(context, startScanning/*, forScreenOn*/);
                        /*if (countDownLatch != null)
                            countDownLatch.countDown();*/
                    }
                });
            }
            else {
                _scheduleJob(context, startScanning/*, forScreenOn*/);
                /*if (countDownLatch != null)
                    countDownLatch.countDown();*/
            }
        //}
        //else
        //    PPApplication.logE("GeofenceScannerJob.scheduleJob", "scanner is not started");
    }

    private static void _cancelJob(/*final Context context*/) {
        try {
            JobManager jobManager = JobManager.instance();
            //jobManager.cancelAllForTag(JOB_TAG_START);
            jobManager.cancelAllForTag(JOB_TAG);
        } catch (Exception ignored) {}
    }

    static void cancelJob(/*final Context context, */final boolean useHandler, final Handler _handler) {
        PPApplication.logE("GeofenceScannerJob.cancelJob", "xxx");

        if (useHandler && (_handler == null)) {
            PPApplication.startHandlerThread("GeofenceScannerJob.cancelJob");
            final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    _cancelJob();
                    /*if (countDownLatch != null)
                        countDownLatch.countDown();*/
                }
            });
        }
        else {
            _cancelJob();
            /*if (countDownLatch != null)
                countDownLatch.countDown();*/
        }
    }

    static boolean isJobScheduled() {
        PPApplication.logE("GeofenceScannerJob.isJobScheduled", "xxx");

        try {
            JobManager jobManager = JobManager.instance();
            return (jobManager.getAllJobRequestsForTag(JOB_TAG).size() != 0)/* ||
                    (jobManager.getAllJobRequestsForTag(JOB_TAG_START).size() != 0)*/;
        } catch (Exception e) {
            return false;
        }
    }

}

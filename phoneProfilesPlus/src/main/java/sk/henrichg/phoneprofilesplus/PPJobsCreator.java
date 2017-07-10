package sk.henrichg.phoneprofilesplus;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

class PPJobsCreator implements JobCreator {

    @Override
    public Job create(String tag) {
        switch (tag) {
            case SearchCalendarEventsJob.JOB_TAG:
                return new SearchCalendarEventsJob();
            case SearchCalendarEventsJob.JOB_TAG_SHORT:
                return new SearchCalendarEventsJob();
            case WifiScanJob.JOB_TAG:
                return new WifiScanJob();
            case WifiScanJob.JOB_TAG_SHORT:
                return new WifiScanJob();
            case BluetoothScanJob.JOB_TAG:
                return new BluetoothScanJob();
            case BluetoothScanJob.JOB_TAG_SHORT:
                return new BluetoothScanJob();
            case GeofenceScannerJob.JOB_TAG:
                return new GeofenceScannerJob();
            case GeofenceScannerJob.JOB_TAG_START:
                return new GeofenceScannerJob();
            default:
                return null;
        }
    }

}

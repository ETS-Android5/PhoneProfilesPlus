package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;

/*
 This service is called only from WifiSSIDPreference and BluetoothNamePreference, not needed to convert it to job.
 */

public class WifiBluetoothScannerService extends IntentService
{
    //private Context context;

    static final String EXTRA_SCANNER_TYPE = "scanner_type";

    public WifiBluetoothScannerService()
    {
        super("WifiBluetoothScannerService");

        // if enabled is true, onStartCommand(Intent, int, int) will return START_REDELIVER_INTENT,
        // so if this process dies before onHandleIntent(Intent) returns, the process will be restarted
        // and the intent redelivered. If multiple Intents have been sent, only the most recent one
        // is guaranteed to be redelivered.
        // -- but restarted service has intent == null??
        setIntentRedelivery(true);
    }

    @SuppressLint("NewApi")
    @Override
    protected void onHandleIntent(Intent intent) {
        //context = getApplicationContext();
        CallsCounter.logCounter(this, "WifiBluetoothScannerService.doWakefulWork", "ScannerService_doWakefulWork");

        if (intent == null) {
            PPApplication.logE("%%%% WifiBluetoothScannerService.doWakefulWork", "intent=null");
            return;
        }

        PPApplication.logE("%%%% WifiBluetoothScannerService.doWakefulWork", "-- START ------------");

        String scannerType = intent.getStringExtra(EXTRA_SCANNER_TYPE);
        PPApplication.logE("%%%% WifiBluetoothScannerService.onHandleIntent", "scannerType="+scannerType);

        WifiBluetoothScanner wifiBluetoothScanner = new WifiBluetoothScanner(this);
        wifiBluetoothScanner.doScan(scannerType);

        PPApplication.logE("%%%% WifiBluetoothScannerService.doWakefulWork", "-- END ------------");

    }

}
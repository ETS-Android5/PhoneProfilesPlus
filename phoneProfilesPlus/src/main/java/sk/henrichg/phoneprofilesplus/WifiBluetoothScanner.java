package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;

import java.util.List;

class WifiBluetoothScanner {

    private final Context context;

    private static final int wifiScanDuration = 25;      // 25 seconds for wifi scan
    private static final int classicBTScanDuration = 20; // 20 seconds for classic bluetooth scan

    static List<BluetoothDeviceData> tmpBluetoothScanResults = null;
    static boolean bluetoothDiscoveryStarted = false;
    static BluetoothLeScanner bluetoothLEScanner = null;
    //static BluetoothLEScanCallback18 bluetoothLEScanCallback18 = null;
    //static BluetoothLEScanCallback21 bluetoothLEScanCallback21 = null;

    static final String SCANNER_TYPE_WIFI = "wifi";
    static final String SCANNER_TYPE_BLUETOOTH = "bluetooth";

    private static final String PREF_FORCE_ONE_BLUETOOTH_SCAN = "forceOneBluetoothScanInt";
    private static final String PREF_FORCE_ONE_LE_BLUETOOTH_SCAN = "forceOneLEBluetoothScanInt";
    private static final String PREF_FORCE_ONE_WIFI_SCAN = "forceOneWifiScanInt";

    static final int FORCE_ONE_SCAN_DISABLED = 0;
    static final int FORCE_ONE_SCAN_FROM_PREF_DIALOG = 3;

    private static final String PREF_SHOW_ENABLE_LOCATION_NOTIFICATION = "show_enable_location_notification";

    public WifiBluetoothScanner(Context context) {
        this.context = context;
    }

    void doScan(String scannerType) {
        synchronized (PPApplication.wifiBluetoothscannerMutex) {
            CallsCounter.logCounter(context, "WifiBluetoothScanner.doScan", "Scanner_doScan");

            if (!PPApplication.getApplicationStarted(context, true))
                // application is not started
                return;

            PPApplication.logE("%%%% WifiBluetoothScanner.doScan", "-- START ------------");

            DataWrapper dataWrapper;

            PPApplication.logE("%%%% WifiBluetoothScanner.doScan", "scannerType=" + scannerType);

            // for Airplane mode ON, no scan
            if (android.os.Build.VERSION.SDK_INT >= 17) {
                if (Settings.Global.getInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0) != 0) {
                    PPApplication.logE("%%%% WifiBluetoothScanner.doScan", "-- END - airplane mode ON -------");
                    return;
                }
            } else {
                if (Settings.System.getInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) != 0) {
                    PPApplication.logE("%%%% WifiBluetoothScanner.doScan", "-- END - airplane mode ON -------");
                    return;
                }
            }

            // check power save mode
            //boolean isPowerSaveMode = PPApplication.isPowerSaveMode;
            boolean isPowerSaveMode = DataWrapper.isPowerSaveMode(context);
            if (isPowerSaveMode) {
                int forceScan = getForceOneWifiScan(context);
                if (forceScan != FORCE_ONE_SCAN_FROM_PREF_DIALOG) {
                    if (scannerType.equals(SCANNER_TYPE_WIFI) &&
                            ApplicationPreferences.applicationEventWifiScanInPowerSaveMode(context).equals("2")) {
                        // not scan wi-fi in power save mode
                        PPApplication.logE("%%%% WifiBluetoothScanner.doScan", "-- END - power save mode ON -------");
                        return;
                    }
                    if (scannerType.equals(SCANNER_TYPE_BLUETOOTH) &&
                            ApplicationPreferences.applicationEventBluetoothScanInPowerSaveMode(context).equals("2")) {
                        // not scan bluetooth in power save mode
                        PPApplication.logE("%%%% WifiBluetoothScanner.doScan", "-- END - power save mode ON -------");
                        return;
                    }
                }
            }

            PhoneProfilesService.startHandlerThread();
            final Handler wifiBluetoothChangeHandler = new Handler(PhoneProfilesService.handlerThread.getLooper());

            PPApplication.logE("$$$ WifiBluetoothScanner.doScan", "before synchronized block - scannerType=" + scannerType);

            synchronized (PPApplication.radioChangeStateMutex) {

                PPApplication.logE("$$$ WifiBluetoothScanner.doScan", "in synchronized block - start - scannerType=" + scannerType);

                if (scannerType.equals(SCANNER_TYPE_WIFI)) {
                    PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "start wifi scan");

                    WifiScanJob.fillWifiConfigurationList(context);

                    boolean canScan = Event.isEventPreferenceAllowed(EventPreferencesWifi.PREF_EVENT_WIFI_ENABLED, context) == PPApplication.PREFERENCE_ALLOWED;
                    if (canScan) {
                        canScan = !WifiApManager.isWifiAPEnabled(context);
                        PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "canScan=" + canScan);
                        PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "isWifiAPEnabled=" + !canScan);
                    }

                    if (canScan) {

                        dataWrapper = new DataWrapper(context, false, false, 0);

                        // check if wifi scan events exists
                        //lock();
                        boolean wifiEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_WIFIINFRONT) > 0;
                        //unlock();
                        int forceScan = getForceOneWifiScan(context);
                        boolean scan = (wifiEventsExists || (forceScan == FORCE_ONE_SCAN_FROM_PREF_DIALOG));
                        if (scan) {
                            if (wifiEventsExists)
                                scan = isLocationEnabled(context, scannerType);
                        }
                        PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "wifiEventsExists=" + wifiEventsExists);
                        PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "forceScan=" + forceScan);
                        if (!scan) {
                            // wifi scan events not exists
                            PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "alarms removed");
                            WifiScanJob.cancelJob(context, null);
                        } else {
                            PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "can scan");

                            if (WifiScanJob.wifi == null)
                                WifiScanJob.wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

                            if (WifiScanJob.getWifiEnabledForScan(context)) {
                                // service restarted during scanning, disable wifi
                                PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "disable wifi - service restarted");
                                wifiBluetoothChangeHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        //lock();
                                        WifiScanJob.wifi.setWifiEnabled(false);
                                        WifiScanJob.setScanRequest(context, false);
                                        WifiScanJob.setWaitForResults(context, false);
                                        WifiScanJob.setWifiEnabledForScan(context, false);
                                    }
                                });
                                //try { Thread.sleep(700); } catch (InterruptedException e) { }
                                //SystemClock.sleep(700);
                                PPApplication.sleep(700);
                                //unlock();
                            }

                            if (true /*canScanWifi(dataWrapper)*/) { // scan even if wifi is connected

                                PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "scan started");

                                WifiScanJob.setScanRequest(context, false);
                                WifiScanJob.setWaitForResults(context, false);
                                WifiScanJob.setWifiEnabledForScan(context, false);

                                WifiScanJob.unlock();

                                // start scan

                                //lock();

                                // enable wifi
                                int wifiState;
                                wifiState = enableWifi(dataWrapper, WifiScanJob.wifi, wifiBluetoothChangeHandler);

                                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                                    PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "startScan");
                                    WifiScanJob.startScan(context);
                                } else if (wifiState != WifiManager.WIFI_STATE_ENABLING) {
                                    WifiScanJob.setScanRequest(context, false);
                                    WifiScanJob.setWaitForResults(context, false);
                                    WifiScanJob.setWifiEnabledForScan(context, false);
                                    setForceOneWifiScan(context, FORCE_ONE_SCAN_DISABLED);
                                }

                                if ((WifiScanJob.getScanRequest(context)) ||
                                        (WifiScanJob.getWaitForResults(context))) {
                                    PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "waiting for scan end");

                                    // wait for scan end
                                    waitForWifiScanEnd(context, null);

                                    PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "scan ended");

                                    if (WifiScanJob.getWaitForResults(context)) {
                                        PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "no data received from scanner");
                                        if (getForceOneWifiScan(context) != WifiBluetoothScanner.FORCE_ONE_SCAN_FROM_PREF_DIALOG) // not start service for force scan
                                        {
                                            // start job
                                            final Context appContext = context.getApplicationContext();
                                            PhoneProfilesService.startHandlerThread();
                                            final Handler handler = new Handler(PhoneProfilesService.handlerThread.getLooper());
                                            handler.postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    //EventsHandlerJob.startForSensor(context, EventsHandler.SENSOR_TYPE_WIFI_SCANNER);
                                                    EventsHandler eventsHandler = new EventsHandler(appContext);
                                                    eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_WIFI_SCANNER, false);
                                                }
                                            }, 5000);
                                            //WifiScanBroadcastReceiver.setAlarm(context);
                                        }
                                    }
                                }

                                WifiScanJob.unlock();
                                //unlock();
                            }
                        }

                        wifiBluetoothChangeHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (WifiScanJob.getWifiEnabledForScan(context)) {
                                    PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "disable wifi");
                                    //lock();
                                    WifiScanJob.wifi.setWifiEnabled(false);
                                    WifiScanJob.setWifiEnabledForScan(context, false);
                                } else
                                    PPApplication.logE("$$$W WifiBluetoothScanner.doScan", "keep enabled wifi");
                            }
                        });
                        //try { Thread.sleep(700); } catch (InterruptedException e) { }
                        //SystemClock.sleep(700);
                        PPApplication.sleep(700);
                        //unlock();
                    }

                    setForceOneWifiScan(context, FORCE_ONE_SCAN_DISABLED);
                    WifiScanJob.setWaitForResults(context, false);
                    WifiScanJob.setScanRequest(context, false);

                    WifiScanJob.unlock();
                    //unlock();

                } else if (scannerType.equals(SCANNER_TYPE_BLUETOOTH)) {

                    if (Event.isEventPreferenceAllowed(EventPreferencesBluetooth.PREF_EVENT_BLUETOOTH_ENABLED, context) == PPApplication.PREFERENCE_ALLOWED) {

                        PPApplication.logE("$$$B WifiBluetoothScanner.doScan", "start bt scan");

                        dataWrapper = new DataWrapper(context, false, false, 0);

                        // check if bluetooth scan events exists
                        //lock();
                        boolean bluetoothEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_BLUETOOTHINFRONT) > 0;
                        int forceScan = getForceOneBluetoothScan(dataWrapper.context);
                        int forceScanLE = getForceOneLEBluetoothScan(context);
                        boolean classicDevicesScan = dataWrapper.getDatabaseHandler().getBluetoothDevicesTypeCount(EventPreferencesBluetooth.DTYPE_CLASSIC, forceScanLE) > 0;
                        boolean leDevicesScan;
                        if (bluetoothLESupported(context))
                            leDevicesScan = dataWrapper.getDatabaseHandler().getBluetoothDevicesTypeCount(EventPreferencesBluetooth.DTYPE_LE, forceScanLE) > 0;
                        else
                            leDevicesScan = false;
                        //unlock();
                        boolean scan = (bluetoothEventsExists || (forceScan == FORCE_ONE_SCAN_FROM_PREF_DIALOG) ||
                                (forceScanLE == FORCE_ONE_SCAN_FROM_PREF_DIALOG));
                        if (scan) {
                            if (leDevicesScan)
                                leDevicesScan = isLocationEnabled(context, scannerType);
                        }
                        if (!scan) {
                            // bluetooth scan events not exists
                            PPApplication.logE("$$$B WifiBluetoothScanner.doScan", "no bt scan events");
                            BluetoothScanJob.cancelJob(context, null);
                        } else {
                            PPApplication.logE("$$$B WifiBluetoothScanner.doScan", "scan=true");

                            if (BluetoothScanJob.bluetooth == null)
                                BluetoothScanJob.bluetooth = BluetoothScanJob.getBluetoothAdapter(context);

                            if (BluetoothScanJob.bluetooth != null) {
                                if (BluetoothScanJob.getBluetoothEnabledForScan(context)) {
                                    // service restarted during scanning, disable Bluetooth
                                    PPApplication.logE("$$$B WifiBluetoothScanner.doScan", "disable BT - service restarted");
                                    wifiBluetoothChangeHandler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            //lock();
                                            BluetoothScanJob.bluetooth.disable();
                                            BluetoothScanJob.setScanRequest(context, false);
                                            BluetoothScanJob.setLEScanRequest(context, false);
                                            BluetoothScanJob.setWaitForResults(context, false);
                                            BluetoothScanJob.setWaitForLEResults(context, false);
                                            BluetoothScanJob.setBluetoothEnabledForScan(context, false);
                                        }
                                    });
                                    //try { Thread.sleep(700); } catch (InterruptedException e) { }
                                    //SystemClock.sleep(700);
                                    PPApplication.sleep(700);
                                    //unlock();
                                }

                                if (true /*canScanBluetooth(dataWrapper)*/) {  // scan even if bluetooth is connected
                                    BluetoothScanJob.setScanRequest(context, false);
                                    BluetoothScanJob.setLEScanRequest(context, false);
                                    BluetoothScanJob.setWaitForResults(context, false);
                                    BluetoothScanJob.setWaitForLEResults(context, false);
                                    BluetoothScanJob.setBluetoothEnabledForScan(context, false);

                                    int bluetoothState;

                                    if (classicDevicesScan) {
                                        ///////// Classic BT scan

                                        PPApplication.logE("$$$BCL WifiBluetoothScanner.doScan", "classic devices scan");

                                        //lock();

                                        // enable bluetooth
                                        bluetoothState = enableBluetooth(dataWrapper,
                                                BluetoothScanJob.bluetooth,
                                                wifiBluetoothChangeHandler,
                                                false);
                                        PPApplication.logE("$$$BCL WifiBluetoothScanner.doScan", "bluetoothState=" + bluetoothState);

                                        if (bluetoothState == BluetoothAdapter.STATE_ON) {
                                            PPApplication.logE("$$$BCL WifiBluetoothScanner.doScan", "start classic scan");
                                            BluetoothScanJob.startCLScan(context);
                                        } else if (bluetoothState != BluetoothAdapter.STATE_TURNING_ON) {
                                            BluetoothScanJob.setScanRequest(context, false);
                                            BluetoothScanJob.setWaitForResults(context, false);
                                            BluetoothScanJob.setBluetoothEnabledForScan(context, false);
                                            setForceOneBluetoothScan(context, FORCE_ONE_SCAN_DISABLED);
                                        }

                                        if ((BluetoothScanJob.getScanRequest(context)) ||
                                                (BluetoothScanJob.getWaitForResults(context))) {
                                            PPApplication.logE("$$$BCL WifiBluetoothScanner.doScan", "waiting for classic scan end");

                                            // wait for scan end
                                            waitForBluetoothScanEnd(context, null);

                                            PPApplication.logE("$$$BCL WifiBluetoothScanner.doScan", "classic scan ended");

                                        }

                                        //unlock();

                                        setForceOneBluetoothScan(context, FORCE_ONE_SCAN_DISABLED);
                                        BluetoothScanJob.setWaitForResults(context, false);
                                        BluetoothScanJob.setLEScanRequest(context, false);

                                        ///////// Classic BT scan - end
                                    }

                                    if (leDevicesScan) {
                                        ///////// LE BT scan

                                        PPApplication.logE("$$$BLE WifiBluetoothScanner.doScan", "LE devices scan");

                                    /*if (android.os.Build.VERSION.SDK_INT < 21)
                                        // for old BT LE scan must by acquired lock
                                        lock();*/

                                        // enable bluetooth
                                        bluetoothState = enableBluetooth(dataWrapper,
                                                BluetoothScanJob.bluetooth,
                                                wifiBluetoothChangeHandler,
                                                true);

                                        if (bluetoothState == BluetoothAdapter.STATE_ON) {
                                            PPApplication.logE("$$$BLE WifiBluetoothScanner.doScan", "start LE scan");
                                            BluetoothScanJob.startLEScan(context);
                                        } else if (bluetoothState != BluetoothAdapter.STATE_TURNING_ON) {
                                            BluetoothScanJob.setLEScanRequest(context, false);
                                            BluetoothScanJob.setWaitForLEResults(context, false);
                                            BluetoothScanJob.setBluetoothEnabledForScan(context, false);
                                            setForceOneLEBluetoothScan(context, FORCE_ONE_SCAN_DISABLED);
                                        }

                                        if ((BluetoothScanJob.getLEScanRequest(context)) ||
                                                (BluetoothScanJob.getWaitForLEResults(context))) {
                                            PPApplication.logE("$$$BLE WifiBluetoothScanner.doScan", "waiting for LE scan end");

                                            // wait for scan end
                                            waitForLEBluetoothScanEnd(context, null);

                                            // send broadcast for start EventsHandler
                                        /*Intent btLEIntent = new Intent(context, BluetoothLEScanBroadcastReceiver.class);
                                        sendBroadcast(btLEIntent);*/
                                            Intent btLEIntent = new Intent("BluetoothLEScanBroadcastReceiver");
                                            LocalBroadcastManager.getInstance(context).sendBroadcast(btLEIntent);

                                            PPApplication.logE("$$$BLE WifiBluetoothScanner.doScan", "LE scan ended");
                                        }

                                        //unlock();

                                        setForceOneLEBluetoothScan(context, FORCE_ONE_SCAN_DISABLED);
                                        BluetoothScanJob.setWaitForLEResults(context, false);
                                        BluetoothScanJob.setLEScanRequest(context, false);

                                        ///////// LE BT scan - end
                                    }
                                }

                                wifiBluetoothChangeHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (BluetoothScanJob.getBluetoothEnabledForScan(context)) {
                                            PPApplication.logE("$$$B WifiBluetoothScanner.doScan", "disable bluetooth");
                                            //lock();
                                            BluetoothScanJob.bluetooth.disable();
                                            BluetoothScanJob.setBluetoothEnabledForScan(context, false);
                                        } else
                                            PPApplication.logE("$$$B WifiBluetoothScanner.doScan", "keep enabled bluetooth");
                                    }
                                });
                                //try { Thread.sleep(700); } catch (InterruptedException e) { }
                                //SystemClock.sleep(700);
                                PPApplication.sleep(700);
                                //unlock();
                            }
                        }
                    }

                    setForceOneBluetoothScan(context, FORCE_ONE_SCAN_DISABLED);
                    setForceOneLEBluetoothScan(context, FORCE_ONE_SCAN_DISABLED);
                    BluetoothScanJob.setWaitForResults(context, false);
                    BluetoothScanJob.setWaitForLEResults(context, false);
                    BluetoothScanJob.setScanRequest(context, false);
                    BluetoothScanJob.setLEScanRequest(context, false);

                    //unlock();
                }

                PPApplication.logE("$$$ WifiBluetoothScanner.doScan", "in synchronized block - end - scannerType=" + scannerType);

            }

            PPApplication.logE("$$$ WifiBluetoothScanner.doScan", "after synchronized block - scannerType=" + scannerType);

            PPApplication.logE("%%%% WifiBluetoothScanner.doScan", "-- END ------------");
        }
    }

    static int getForceOneWifiScan(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_FORCE_ONE_WIFI_SCAN, FORCE_ONE_SCAN_DISABLED);
    }

    static void setForceOneWifiScan(Context context, int forceScan)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_FORCE_ONE_WIFI_SCAN, forceScan);
        editor.apply();
    }

    static int getForceOneBluetoothScan(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_FORCE_ONE_BLUETOOTH_SCAN, FORCE_ONE_SCAN_DISABLED);
    }

    static void setForceOneBluetoothScan(Context context, int forceScan)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_FORCE_ONE_BLUETOOTH_SCAN, forceScan);
        editor.apply();
    }

    static int getForceOneLEBluetoothScan(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getInt(PREF_FORCE_ONE_LE_BLUETOOTH_SCAN, FORCE_ONE_SCAN_DISABLED);
    }

    static void setForceOneLEBluetoothScan(Context context, int forceScan)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putInt(PREF_FORCE_ONE_LE_BLUETOOTH_SCAN, forceScan);
        editor.apply();
    }

    /*
    private void lock() {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (wakeLock == null)
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ScanWakeLock");
        try {
            if (!wakeLock.isHeld())
                wakeLock.acquire(10 * 60 * 1000);
            PPApplication.logE("$$$ WifiBluetoothScanner.lock","xxx");
        } catch(Exception e) {
            Log.e("WifiBluetoothScanner.lock", "Error getting Lock: " + e.getMessage());
            PPApplication.logE("$$$ WifiBluetoothScanner.lock", "Error getting Lock: " + e.getMessage());
        }
    }

    private void unlock() {
        if ((wakeLock != null) && (wakeLock.isHeld())) {
            PPApplication.logE("$$$ WifiBluetoothScanner.unlock","xxx");
            wakeLock.release();
        }
    }
    */

    @SuppressLint("NewApi")
    private int enableWifi(DataWrapper dataWrapper, WifiManager wifi, Handler wifiBluetoothChangeHandler)
    {
        PPApplication.logE("@@@ WifiBluetoothScanner.enableWifi","xxx");

        int wifiState = wifi.getWifiState();
        int forceScan = getForceOneWifiScan(dataWrapper.context);

        //if ((!dataWrapper.getIsManualProfileActivation()) || forceScan)
        //{
        if (wifiState != WifiManager.WIFI_STATE_ENABLING)
        {
            boolean isWifiEnabled = (wifiState == WifiManager.WIFI_STATE_ENABLED);
            boolean isScanAlwaysAvailable = false;
            if (forceScan != FORCE_ONE_SCAN_FROM_PREF_DIALOG) {
                // from dialog preference wifi must be enabled for saved wifi configuration
                // (see WifiScanJobBroadcastReceiver.fillWifiConfigurationList)

                // this must be disabled because scanning not working, when wifi is disabled after disabled WiFi AP
                // Tested and scanning working ;-)
                if (android.os.Build.VERSION.SDK_INT >= 18)
                    isScanAlwaysAvailable = wifi.isScanAlwaysAvailable();
            }
            PPApplication.logE("@@@ WifiBluetoothScanner.enableWifi","isScanAlwaysAvailable="+isScanAlwaysAvailable);
            isWifiEnabled = isWifiEnabled || isScanAlwaysAvailable;
            if (!isWifiEnabled)
            {
                if (ApplicationPreferences.applicationEventWifiEnableWifi(dataWrapper.context) || (forceScan != FORCE_ONE_SCAN_DISABLED))
                {
                    boolean wifiEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_WIFIINFRONT) > 0;
                    boolean scan = ((wifiEventsExists && ApplicationPreferences.applicationEventWifiEnableWifi(dataWrapper.context)) ||
                            (forceScan == FORCE_ONE_SCAN_FROM_PREF_DIALOG));
                    if (scan)
                    {
                        WifiScanJob.setWifiEnabledForScan(dataWrapper.context, true);
                        WifiScanJob.setScanRequest(dataWrapper.context, true);
                        WifiScanJob.lock();
                        final WifiManager _wifi = wifi;
                        wifiBluetoothChangeHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                PPApplication.logE("$$$ WifiBluetoothScanner.enableWifi", "before enable wifi");
                                _wifi.setWifiEnabled(true);
                                PPApplication.logE("$$$ WifiBluetoothScanner.enableWifi", "after enable wifi");
                            }
                        });
                        PPApplication.logE("@@@ WifiBluetoothScanner.enableWifi","set enabled");

                            /*
                            try {
                                Thread.sleep(700);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            */

                        return WifiManager.WIFI_STATE_ENABLING;
                    }
                }
            }
            else
            {
                if (isScanAlwaysAvailable) {
                    PPApplication.logE("@@@ WifiBluetoothScanner.enableWifi", "scan always available");
                    wifiState =  WifiManager.WIFI_STATE_ENABLED;
                }
                return wifiState;
            }
        }
        //}

        return wifiState;
    }

    @SuppressLint("NewApi")
    private int enableBluetooth(DataWrapper dataWrapper,
                                BluetoothAdapter bluetooth,
                                Handler wifiBluetoothChangeHandler,
                                boolean forLE)
    {
        PPApplication.logE("@@@ WifiBluetoothScanner.enableBluetooth","xxx");

        int bluetoothState = bluetooth.getState();
        int forceScan;
        if (!forLE)
            forceScan = getForceOneBluetoothScan(dataWrapper.context);
        else
            forceScan = getForceOneLEBluetoothScan(dataWrapper.context);

        //if ((!dataWrapper.getIsManualProfileActivation()) || forceScan)
        //{
        boolean isBluetoothEnabled = bluetoothState == BluetoothAdapter.STATE_ON;
        if (!isBluetoothEnabled)
        {
            if (ApplicationPreferences.applicationEventBluetoothEnableBluetooth(dataWrapper.context) || (forceScan != FORCE_ONE_SCAN_DISABLED))
            {
                boolean bluetoothEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_BLUETOOTHINFRONT) > 0;
                boolean scan = ((bluetoothEventsExists && ApplicationPreferences.applicationEventBluetoothEnableBluetooth(dataWrapper.context)) ||
                        (forceScan == FORCE_ONE_SCAN_FROM_PREF_DIALOG));
                if (scan)
                {
                    PPApplication.logE("@@@ WifiBluetoothScanner.enableBluetooth","set enabled");
                    BluetoothScanJob.setBluetoothEnabledForScan(dataWrapper.context, true);
                    if (!forLE)
                        BluetoothScanJob.setScanRequest(dataWrapper.context, true);
                    else
                        BluetoothScanJob.setLEScanRequest(dataWrapper.context, true);
                    final BluetoothAdapter _bluetooth = bluetooth;
                    wifiBluetoothChangeHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            //lock(); // lock is required for enabling bluetooth
                            _bluetooth.enable();
                        }
                    });
                    return BluetoothAdapter.STATE_TURNING_ON;
                }
            }
        }
        else
        {
            PPApplication.logE("@@@ WifiBluetoothScanner.enableBluetooth","already enabled");
            return bluetoothState;
        }
        //}

        return bluetoothState;
    }

    static void waitForWifiScanEnd(Context context, AsyncTask<Void, Integer, Void> asyncTask)
    {
        long start = SystemClock.uptimeMillis();
        do {
            //PPApplication.logE("$$$ WifiAP", "WifiBluetoothScanner.waitForWifiScanEnd-getScanRequest="+WifiScanJobBroadcastReceiver.getScanRequest(context));
            //PPApplication.logE("$$$ WifiAP", "WifiBluetoothScanner.waitForWifiScanEnd-getWaitForResults="+WifiScanJobBroadcastReceiver.getWaitForResults(context));

            if (!((WifiScanJob.getScanRequest(context)) ||
                    (WifiScanJob.getWaitForResults(context)))) {
                break;
            }
            if (asyncTask != null)
            {
                if (asyncTask.isCancelled())
                    break;
            }

            //try { Thread.sleep(100); } catch (InterruptedException e) { }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() - start < wifiScanDuration * 1000);
    }

    private static void waitForBluetoothScanEnd(Context context, AsyncTask<Void, Integer, Void> asyncTask)
    {
        long start = SystemClock.uptimeMillis();
        do {
            if (!((BluetoothScanJob.getScanRequest(context)) ||
                    (BluetoothScanJob.getWaitForResults(context))))
                break;
            if (asyncTask != null)
            {
                if (asyncTask.isCancelled())
                    break;
            }

            //try { Thread.sleep(100); } catch (InterruptedException e) { }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() - start < classicBTScanDuration * 1000);

        BluetoothScanJob.finishScan(context);
        BluetoothScanJob.stopCLScan(context);
    }

    private static void waitForLEBluetoothScanEnd(Context context, AsyncTask<Void, Integer, Void> asyncTask)
    {
        if (bluetoothLESupported(context)) {
            long start = SystemClock.uptimeMillis();
            do {
                if (!((BluetoothScanJob.getLEScanRequest(context)) ||
                        (BluetoothScanJob.getWaitForLEResults(context))))
                    break;
                if (asyncTask != null) {
                    if (asyncTask.isCancelled())
                        break;
                }

                //try { Thread.sleep(100); } catch (InterruptedException e) { }
                SystemClock.sleep(100);
            } while (SystemClock.uptimeMillis() - start < ApplicationPreferences.applicationEventBluetoothLEScanDuration(context) * 1000);

            BluetoothScanJob.finishLEScan(context);
            BluetoothScanJob.stopLEScan(context);
        }
    }

    static void waitForForceOneBluetoothScanEnd(Context context, AsyncTask<Void, Integer, Void> asyncTask) {
        long start = SystemClock.uptimeMillis();
        do {
            if (getForceOneBluetoothScan(context) == FORCE_ONE_SCAN_DISABLED)
                break;
            if (asyncTask != null)
            {
                if (asyncTask.isCancelled())
                    break;
            }

            //try { Thread.sleep(100); } catch (InterruptedException e) { }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() - start < classicBTScanDuration * 1000);
        BluetoothScanJob.finishScan(context);
        BluetoothScanJob.stopCLScan(context);

        if (asyncTask != null)
        {
            if (asyncTask.isCancelled())
                return;
        }

        start = SystemClock.uptimeMillis();
        do {
            if (getForceOneLEBluetoothScan(context) == FORCE_ONE_SCAN_DISABLED)
                break;
            if (asyncTask != null) {
                if (asyncTask.isCancelled())
                    break;
            }

            //try { Thread.sleep(100); } catch (InterruptedException e) { }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() - start < ApplicationPreferences.applicationEventBluetoothLEScanDuration(context) * 1000);
        BluetoothScanJob.finishLEScan(context);
        BluetoothScanJob.stopLEScan(context);
    }

    @SuppressLint("InlinedApi")
    static boolean bluetoothLESupported(Context context) {
        return ((android.os.Build.VERSION.SDK_INT >= 18) &&
                PPApplication.hasSystemFeature(context, PackageManager.FEATURE_BLUETOOTH_LE));
    }

    private static boolean isLocationEnabled(Context context, String scanType) {
        if (Build.VERSION.SDK_INT >= 23) {
            // check for Location Settings

            //Log.e("WifiBluetoothScanner.isLocationEnabled","scanType="+scanType);

            int locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF);
            boolean isScanAlwaysAvailable = true;

            if (scanType.equals(SCANNER_TYPE_WIFI)) {
                if (WifiScanJob.wifi == null)
                    WifiScanJob.wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                int wifiState = WifiScanJob.wifi.getWifiState();
                boolean isWifiEnabled = (wifiState == WifiManager.WIFI_STATE_ENABLED);
                isScanAlwaysAvailable = isWifiEnabled || WifiScanJob.wifi.isScanAlwaysAvailable();
            }

            //if (locationMode == Settings.Secure.LOCATION_MODE_OFF)
            //    Log.e("WifiBluetoothScanner.isLocationEnabled","locationMode=LOCATION_MODE_OFF");
            //Log.e("WifiBluetoothScanner.isLocationEnabled","isScanAlwaysAvailable="+isScanAlwaysAvailable);

            if ((locationMode == Settings.Secure.LOCATION_MODE_OFF) || (!isScanAlwaysAvailable)) {
                // Location settings are not properly set, show notification about it

                if (getShowEnableLocationNotification(context)) {
                    Intent notificationIntent = new Intent(context, PhoneProfilesPreferencesActivity.class);
                    notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    String notificationText;
                    String notificationBigText;

                    if (scanType.equals(SCANNER_TYPE_WIFI)) {
                        notificationText = context.getString(R.string.phone_profiles_pref_eventWiFiScanningSystemSettings);
                        notificationBigText = context.getString(R.string.phone_profiles_pref_eventWiFiScanningSystemSettings_summary);
                    } else {
                        notificationText = context.getString(R.string.phone_profiles_pref_eventBluetoothScanningSystemSettings);
                        notificationBigText = context.getString(R.string.phone_profiles_pref_eventBluetoothScanningSystemSettings_summary);
                    }

                    String nTitle = notificationText;
                    String nText = notificationBigText;
                    if (android.os.Build.VERSION.SDK_INT < 24) {
                        nTitle = context.getString(R.string.app_name);
                        nText = notificationText+": "+notificationBigText;
                    }
                    NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                            .setSmallIcon(R.drawable.ic_exclamation_notify) // notification icon
                            .setContentTitle(nTitle) // title for notification
                            .setContentText(nText) // message for notification
                            .setAutoCancel(true); // clear notification after click
                    mBuilder.setStyle(new NotificationCompat.BigTextStyle().bigText(nText));

                    int requestCode;
                    if (scanType.equals(SCANNER_TYPE_WIFI)) {
                        notificationIntent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO, "wifiScanningCategory");
                        requestCode = 1;
                    } else {
                        notificationIntent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO, "bluetoothScanninCategory");
                        requestCode = 2;
                    }
                    //notificationIntent.putExtra(PhoneProfilesPreferencesActivity.EXTRA_SCROLL_TO_TYPE, "screen");

                    PendingIntent pi = PendingIntent.getActivity(context, requestCode, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(pi);
                    mBuilder.setPriority(Notification.PRIORITY_MAX);
                    if (android.os.Build.VERSION.SDK_INT >= 21) {
                        mBuilder.setCategory(Notification.CATEGORY_RECOMMENDATION);
                        mBuilder.setVisibility(Notification.VISIBILITY_PUBLIC);
                    }
                    NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    if (mNotificationManager != null) {
                        if (scanType.equals(SCANNER_TYPE_WIFI))
                            mNotificationManager.notify(PPApplication.LOCATION_SETTINGS_FOR_WIFI_SCANNING_NOTIFICATION_ID, mBuilder.build());
                        else
                            mNotificationManager.notify(PPApplication.LOCATION_SETTINGS_FOR_BLUETOOTH_SCANNING_NOTIFICATION_ID, mBuilder.build());
                    }

                    setShowEnableLocationNotification(context, false);
                }

                return false;
            }
            else {
                setShowEnableLocationNotification(context, true);
                return true;
            }

        }
        else {
            setShowEnableLocationNotification(context, true);
            return true;
        }
    }

    static private boolean getShowEnableLocationNotification(Context context)
    {
        ApplicationPreferences.getSharedPreferences(context);
        return ApplicationPreferences.preferences.getBoolean(PREF_SHOW_ENABLE_LOCATION_NOTIFICATION, true);
    }

    static void setShowEnableLocationNotification(Context context, boolean show)
    {
        ApplicationPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = ApplicationPreferences.preferences.edit();
        editor.putBoolean(PREF_SHOW_ENABLE_LOCATION_NOTIFICATION, show);
        editor.apply();
    }

}

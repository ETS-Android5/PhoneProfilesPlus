package sk.henrichg.phoneprofilesplus;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.PowerManager;

import static android.content.Context.POWER_SERVICE;

public class BluetoothStateChangedBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        PPApplication.logE("##### BluetoothStateChangedBroadcastReceiver.onReceive", "xxx");

        CallsCounter.logCounter(context, "BluetoothStateChangedBroadcastReceiver.onReceive", "BluetoothStateChangedBroadcastReceiver_onReceive");

        final Context appContext = context.getApplicationContext();

        if (!PPApplication.getApplicationStarted(appContext, true))
            // application is not started
            return;

        if (intent == null)
            return;

        //BluetoothJob.startForStateChangedBroadcast(appContext, intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR));

        String action = intent.getAction();
        if ((action != null) && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            // BluetoothStateChangedBroadcastReceiver

            final int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            PPApplication.startHandlerThread("BluetoothStateChangedBroadcastReceiver.onReceive");
            final Handler handler = new Handler(PPApplication.handlerThread.getLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {

                    PowerManager powerManager = (PowerManager) appContext.getSystemService(POWER_SERVICE);
                    PowerManager.WakeLock wakeLock = null;
                    if (powerManager != null) {
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BluetoothStateChangedBroadcastReceiver.onReceive");
                        wakeLock.acquire(10 * 60 * 1000);
                    }

                    // remove connected devices list
                    if (bluetoothState == BluetoothAdapter.STATE_OFF) {
                        BluetoothConnectionBroadcastReceiver.clearConnectedDevices(appContext, false);
                        BluetoothConnectionBroadcastReceiver.saveConnectedDevices(appContext);
                    }

                    if (Event.getGlobalEventsRunning(appContext))
                    {
                        PPApplication.logE("@@@ BluetoothStateChangedBroadcastReceiver.onReceive","state="+bluetoothState);

                        if ((bluetoothState == BluetoothAdapter.STATE_ON) || (bluetoothState == BluetoothAdapter.STATE_OFF)) {

                            if (bluetoothState == BluetoothAdapter.STATE_ON)
                            {
                                //if ((!dataWrapper.getIsManualProfileActivation()) || PPApplication.getForceOneBluetoothScan(appContext))
                                //{
                                if (BluetoothScanJob.getScanRequest(appContext))
                                {
                                    PPApplication.logE("@@@ BluetoothStateChangedBroadcastReceiver.onReceive", "start classic scan");
                                    BluetoothScanJob.startCLScan(appContext);
                                }
                                else
                                if (BluetoothScanJob.getLEScanRequest(appContext))
                                {
                                    PPApplication.logE("@@@ BluetoothStateChangedBroadcastReceiver.onReceive", "start LE scan");
                                    BluetoothScanJob.startLEScan(appContext);
                                }
                                else
                                if (!(BluetoothScanJob.getWaitForResults(appContext) ||
                                        BluetoothScanJob.getWaitForLEResults(appContext)))
                                {
                                    // refresh bounded devices
                                    BluetoothScanJob.fillBoundedDevicesList(appContext);
                                }
                                //}
                            }

                            if (!((BluetoothScanJob.getScanRequest(appContext)) ||
                                    (BluetoothScanJob.getLEScanRequest(appContext)) ||
                                    (BluetoothScanJob.getWaitForResults(appContext)) ||
                                    (BluetoothScanJob.getWaitForLEResults(appContext)) ||
                                    (BluetoothScanJob.getBluetoothEnabledForScan(appContext)))) {

                                // start events handler
                                EventsHandler eventsHandler = new EventsHandler(appContext);
                                eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_RADIO_SWITCH/*, false*/);

                                //}

                                /*DataWrapper dataWrapper = new DataWrapper(appContext, false, false, 0);
                                boolean bluetoothEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_BLUETOOTHCONNECTED) > 0;
                                dataWrapper.invalidateDataWrapper();

                                if (bluetoothEventsExists) {
                                    PPApplication.logE("@@@ BluetoothJob.onRunJob", "BluetoothStateChangedBroadcastReceiver: bluetoothEventsExists=" + bluetoothEventsExists);
                                */

                                // start events handler
                                eventsHandler.handleEvents(EventsHandler.SENSOR_TYPE_BLUETOOTH_STATE/*, false*/);

                                //}
                            }

                        }
                    }

                    if ((wakeLock != null) && wakeLock.isHeld()) {
                        try {
                            wakeLock.release();
                        } catch (Exception ignored) {}
                    }
                }
            });
        }

    }
}

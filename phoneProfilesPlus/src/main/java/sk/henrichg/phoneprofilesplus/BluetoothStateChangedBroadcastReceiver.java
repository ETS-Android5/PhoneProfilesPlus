package sk.henrichg.phoneprofilesplus;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.WakefulBroadcastReceiver;

public class BluetoothStateChangedBroadcastReceiver extends WakefulBroadcastReceiver {

    public static final String BROADCAST_RECEIVER_TYPE = "bluetoothState";

    @Override
    public void onReceive(Context context, Intent intent) {

        //Thread.setDefaultUncaughtExceptionHandler(new TopExceptionHandler());

        PPApplication.logE("##### BluetoothStateChangedBroadcastReceiver.onReceive", "xxx");

        Context appContext = context.getApplicationContext();

        int bluetoothState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

        // remove connected devices list
        if (bluetoothState == BluetoothAdapter.STATE_OFF) {
            BluetoothConnectionBroadcastReceiver.clearConnectedDevices(appContext, false);
            BluetoothConnectionBroadcastReceiver.saveConnectedDevices(appContext);
        }

        if (!PPApplication.getApplicationStarted(appContext, true))
            // application is not started
            return;

        //PPApplication.loadPreferences(appContext);

        if (Event.getGlobalEventsRuning(appContext))
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
                    // required for Bluetooth ConnectionType="Not connected"

                    //if ((bluetoothState == BluetoothAdapter.STATE_ON) || (bluetoothState == BluetoothAdapter.STATE_OFF)) {
                        /*Intent broadcastIntent = new Intent(appContext, RadioSwitchBroadcastReceiver.class);
                        broadcastIntent.putExtra(EventsService.EXTRA_EVENT_RADIO_SWITCH_TYPE, EventPreferencesRadioSwitch.RADIO_TYPE_BLUETOOTH);
                        broadcastIntent.putExtra(EventsService.EXTRA_EVENT_RADIO_SWITCH_STATE, bluetoothState == BluetoothAdapter.STATE_ON);
                        appContext.sendBroadcast(broadcastIntent);*/
                        LocalBroadcastManager.getInstance(appContext).registerReceiver(PPApplication.radioSwitchBroadcastReceiver, new IntentFilter("RadioSwitchBroadcastReceiver"));
                        Intent broadcastIntent = new Intent("RadioSwitchBroadcastReceiver");
                        broadcastIntent.putExtra(EventsService.EXTRA_EVENT_RADIO_SWITCH_TYPE, EventPreferencesRadioSwitch.RADIO_TYPE_BLUETOOTH);
                        broadcastIntent.putExtra(EventsService.EXTRA_EVENT_RADIO_SWITCH_STATE, bluetoothState == BluetoothAdapter.STATE_ON);
                        LocalBroadcastManager.getInstance(appContext).sendBroadcast(broadcastIntent);

                    //}

                    /*DataWrapper dataWrapper = new DataWrapper(appContext, false, false, 0);
                    boolean bluetoothEventsExists = dataWrapper.getDatabaseHandler().getTypeEventsCount(DatabaseHandler.ETYPE_BLUETOOTHCONNECTED) > 0;
                    dataWrapper.invalidateDataWrapper();

                    if (bluetoothEventsExists) {
                        PPApplication.logE("@@@ BluetoothStateChangedBroadcastReceiver.onReceive", "bluetoothEventsExists=" + bluetoothEventsExists);
                    */
                        // start service
                        Intent eventsServiceIntent = new Intent(appContext, EventsService.class);
                        eventsServiceIntent.putExtra(EventsService.EXTRA_BROADCAST_RECEIVER_TYPE, BROADCAST_RECEIVER_TYPE);
                        startWakefulService(appContext, eventsServiceIntent);
                        //}
                }

            }
        }
    }
}

package sk.henrichg.phoneprofilesplus;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {

    protected ResultReceiver mReceiver;
    private boolean updateName;

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(LocationGeofenceEditorActivity.RECEIVER);

        // Check if receiver was properly registered.
        if (mReceiver == null) {
            Log.wtf("FetchAddressIntentService", "No receiver received. There is nowhere to send the results.");
            return;
        }

        // Get the location passed to this service through an extra.
        Location location = intent.getParcelableExtra(LocationGeofenceEditorActivity.LOCATION_DATA_EXTRA);


        // Make sure that the location data was really sent over through an extra. If it wasn't,
        // send an error error message and return.
        if (location == null) {
            Log.wtf("FetchAddressIntentService", errorMessage);
            deliverResultToReceiver(LocationGeofenceEditorActivity.FAILURE_RESULT, "No location data provided");
            return;
        }

        updateName = intent.getBooleanExtra(LocationGeofenceEditorActivity.UPDATE_NAME_EXTRA, false);

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(location.getLatitude(),
                                                 location.getLongitude(),
                                                 // In this sample, get just a single address.
                                                 1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            Log.e("FetchAddressIntentService", "Service not available", ioException);
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            Log.e("FetchAddressIntentService", "Invalid location. " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                Log.e("FetchAddressIntentService", "No address found");
            }
            deliverResultToReceiver(LocationGeofenceEditorActivity.FAILURE_RESULT,
                    getApplicationContext().getString(R.string.event_preferences_location_no_address_found));
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i("FetchAddressIntentService", "Address found");
            deliverResultToReceiver(LocationGeofenceEditorActivity.SUCCESS_RESULT,
                    TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }

    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(LocationGeofenceEditorActivity.RESULT_DATA_KEY, message);
        bundle.putBoolean(LocationGeofenceEditorActivity.UPDATE_NAME_EXTRA, updateName);
        mReceiver.send(resultCode, bundle);
    }

}

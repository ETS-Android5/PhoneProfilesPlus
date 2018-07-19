package sk.henrichg.phoneprofilesplus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageButton;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;

public class LocationGeofenceEditorActivity extends AppCompatActivity
                                     implements GoogleApiClient.ConnectionCallbacks,
                                                GoogleApiClient.OnConnectionFailedListener,
                                                OnMapReadyCallback
{
    private GoogleApiClient mGoogleApiClient;
    private FusedLocationProviderClient mFusedLocationClient;
    private LocationCallback mLocationCallback;
    private GoogleMap mMap;
    private Marker editedMarker;
    private Circle editedRadius;
    private Circle lastLocationRadius;

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;
    private static final String PACKAGE_NAME = "sk.henrichg.phoneprofilesplus";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String LOCATION_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";
    public static final String UPDATE_NAME_EXTRA = PACKAGE_NAME + ".UPDATE_NAME_EXTRA";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    private Location mLastLocation;
    private Location mLocation;
    private LocationRequest mLocationRequest;

    private long geofenceId;
    private Geofence geofence;

    private AddressResultReceiver mResultReceiver;
    //private boolean mAddressRequested = false;

    private EditText geofenceNameEditText;
    private AppCompatImageButton addressButton;
    private TextView addressText;
    private Button okButton;
    private TextView radiusLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        GlobalGUIRoutines.setTheme(this, false, false, false);
        GlobalGUIRoutines.setLanguage(getBaseContext());

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_location_geofence_editor);

        mResultReceiver = new AddressResultReceiver(new Handler(getMainLooper()));

        // Create a GoogleApiClient instance
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                PPApplication.logE("LocationGeofenceEditorActivity.LocationCallback","xxx");
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    mLastLocation = location;
                    PPApplication.logE("LocationGeofenceEditorActivity.LocationCallback","location="+location);

                    if (mLocation == null) {
                        mLocation = new Location(mLastLocation);
                        refreshActivity(true);
                    }
                    else
                        updateEditedMarker(false);
                }
            }
        };

        createLocationRequest();

        mResolvingError = savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        Intent intent = getIntent();
        geofenceId = intent.getLongExtra(LocationGeofencePreference.EXTRA_GEOFENCE_ID, 0);

        if (geofenceId > 0) {
            geofence = DatabaseHandler.getInstance(getApplicationContext()).getGeofence(geofenceId);
            mLocation = new Location("LOC");
            mLocation.setLatitude(geofence._latitude);
            mLocation.setLongitude(geofence._longitude);
        }
        if (geofence == null) {
            geofenceId = 0;
            geofence = new Geofence();
            geofence._name = getString(R.string.event_preferences_location_new_location_name) + "_" +
                                String.valueOf(DatabaseHandler.getInstance(getApplicationContext()).getGeofenceCount()+1);
            geofence._radius = 100;
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.location_editor_map);
        mapFragment.getMapAsync(this);

        radiusLabel = findViewById(R.id.location_pref_dlg_radius_seekbar_label);
        SeekBar radiusSeekBar = findViewById(R.id.location_pref_dlg_radius_seekbar);
        radiusSeekBar.setProgress(Math.round(geofence._radius / (float)20.0)-1);
        radiusSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                geofence._radius = (progress+1) * 20;
                updateEditedMarker(false);
                //Log.d("LocationGeofenceEditorActivity.onProgressChanged", "radius="+geofence._radius);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        geofenceNameEditText = findViewById(R.id.location_editor_geofence_name);
        geofenceNameEditText.setText(geofence._name);

        addressText = findViewById(R.id.location_editor_address_text);

        okButton = findViewById(R.id.location_editor_ok);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = geofenceNameEditText.getText().toString();
                if ((!name.isEmpty()) && (mLocation != null)) {
                    geofence._name = name;
                    geofence._latitude = mLocation.getLatitude();
                    geofence._longitude = mLocation.getLongitude();

                    if (geofenceId > 0) {
                        DatabaseHandler.getInstance(getApplicationContext()).updateGeofence(geofence);
                    } else {
                        DatabaseHandler.getInstance(getApplicationContext()).addGeofence(geofence);
                        /*synchronized (PPApplication.geofenceScannerMutex) {
                            // start location updates
                            if ((PhoneProfilesService.getInstance() != null) && PhoneProfilesService.isGeofenceScannerStarted())
                                PhoneProfilesService.getGeofencesScanner().connectForResolve();
                        }*/
                    }

                    DatabaseHandler.getInstance(getApplicationContext()).checkGeofence(String.valueOf(geofence._id), 1);

                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(LocationGeofencePreference.EXTRA_GEOFENCE_ID, geofence._id);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                }
            }
        });

        Button cancelButton = findViewById(R.id.location_editor_cancel);
        //cancelButton.setAllCaps(false);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
            }
        });

        AppCompatImageButton myLocationButton = findViewById(R.id.location_editor_my_location);
        myLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLastLocation != null)
                    mLocation = new Location(mLastLocation);
                refreshActivity(true);
            }
        });

        addressButton = findViewById(R.id.location_editor_address_btn);
        addressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGeofenceAddress(/*true*/);
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Within {@code onPause()}, we pause location updates, but leave the
        // connection to GoogleApiClient intact.  Here, we resume receiving
        // location updates if the user has requested them.
        try {
            if (mGoogleApiClient.isConnected()) {
                startLocationUpdates();
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        try {
            if (mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                try {
                    if (!mGoogleApiClient.isConnecting() &&
                            !mGoogleApiClient.isConnected()) {
                        mGoogleApiClient.connect();
                    }
                } catch (Exception ignored) {}
            }
        }
        else
        if (requestCode == Permissions.REQUEST_CODE + Permissions.GRANT_TYPE_LOCATION_GEOFENCE_EDITOR_ACTIVITY) {
            getLastLocation();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLastLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
        Log.i("LocationGeofenceEditorActivity", "Connection suspended");
        //mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        //noinspection StatementWithEmptyBody
        if (mResolvingError) {
            // Already attempting to resolve an error.
            //return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            mResolvingError = true;
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mMap != null) {
            mMap.getUiSettings().setMapToolbarEnabled(false);
            updateEditedMarker(true);

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng point) {
                    //Log.d("Map", "Map clicked");
                    if (mLocation == null)
                        mLocation = new Location("LOC");
                    mLocation.setLatitude(point.latitude);
                    mLocation.setLongitude(point.longitude);
                    refreshActivity(false);
                }
            });
        }
    }

    private void updateEditedMarker(boolean setMapCamera) {
        if (mMap != null) {

            if (mLastLocation != null) {
                LatLng lastLocationGeofence = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                if (lastLocationRadius == null) {
                    lastLocationRadius = mMap.addCircle(new CircleOptions()
                            .center(lastLocationGeofence)
                            .radius(mLastLocation.getAccuracy())
                            .strokeColor(ContextCompat.getColor(this, R.color.map_last_location_marker_stroke))
                            .fillColor(ContextCompat.getColor(this, R.color.map_last_location_marker_fill))
                            .strokeWidth(5)
                            .zIndex(1));
                } else {
                    lastLocationRadius.setRadius(mLastLocation.getAccuracy());
                    lastLocationRadius.setCenter(lastLocationGeofence);
                }
            }

            if (mLocation != null) {
                LatLng editedGeofence = new LatLng(mLocation.getLatitude(), mLocation.getLongitude());
                if (editedMarker == null) {
                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(editedGeofence);
                    editedMarker = mMap.addMarker(markerOptions);
                } else
                    editedMarker.setPosition(editedGeofence);
                editedMarker.setTitle(geofenceNameEditText.getText().toString());

                if (editedRadius == null) {
                    editedRadius = mMap.addCircle(new CircleOptions()
                            .center(editedGeofence)
                            .radius(geofence._radius)
                            .strokeColor(ContextCompat.getColor(this, R.color.map_edited_location_marker_stroke))
                            .fillColor(ContextCompat.getColor(this, R.color.map_edited_location_marker_fill))
                            .strokeWidth(5)
                            .zIndex(2));
                } else {
                    editedRadius.setRadius(geofence._radius);
                    editedRadius.setCenter(editedGeofence);
                }
                radiusLabel.setText(String.valueOf(Math.round(geofence._radius)));

                if (setMapCamera)
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(editedGeofence));
            }
        }
    }

    //----------------------------------------------------

    private void refreshActivity(boolean setMapCamera) {
        boolean enableAddressButton = false;
        if (mLocation != null) {
            // Determine whether a geo-coder is available.
            if (Geocoder.isPresent()) {
                startIntentService(false);
                enableAddressButton = true;
            }
        }
        if (addressButton.isEnabled())
            GlobalGUIRoutines.setImageButtonEnabled(enableAddressButton, addressButton, R.drawable.ic_action_location_address, getApplicationContext());
        String name = geofenceNameEditText.getText().toString();

        updateEditedMarker(setMapCamera);

        okButton.setEnabled((!name.isEmpty()) && (mLocation != null));
    }

    @SuppressLint("MissingPermission")
    private void getLastLocation() {
        if (Permissions.grantLocationGeofenceEditorPermissions(getApplicationContext(), this)) {
            try {
                mFusedLocationClient.getLastLocation()
                        .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                // Got last known location. In some rare situations this can be null.
                                PPApplication.logE("LocationGeofenceEditorActivity.getLastLocation","location="+location);
                                if (location != null) {
                                    mLastLocation = location;
                                }
                                if (mLastLocation == null)
                                    startLocationUpdates();
                                else if (mLocation == null)
                                    mLocation = new Location(mLastLocation);
                                refreshActivity(true);
                            }
                        });
            } catch (Exception ignored) {}
        }
    }

    private void createLocationRequest() {
        mLocationRequest = LocationRequest.create();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    /**
     * Requests location updates from the FusedLocationApi.
     */
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (Permissions.grantLocationGeofenceEditorPermissions(getApplicationContext(), this)) {
            try {
                if (mFusedLocationClient != null)
                    mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        if (mFusedLocationClient != null)
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
    }

    private  void getGeofenceAddress(/*boolean updateName*/) {
        try {
            // Only start the service to fetch the address if GoogleApiClient is
            // connected.
            if (mGoogleApiClient.isConnected() && mLocation != null) {
                startIntentService(true);
            }
            // If GoogleApiClient isn't connected, process the user's request by
            // setting mAddressRequested to true. Later, when GoogleApiClient connects,
            // launch the service to fetch the address. As far as the user is
            // concerned, pressing the Fetch Address button
            // immediately kicks off the process of getting the address.
            //mAddressRequested = true;
        } catch (Exception ignored) {}
    }

    private void startIntentService(boolean updateName) {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(RECEIVER, mResultReceiver);
        intent.putExtra(LOCATION_DATA_EXTRA, mLocation);
        intent.putExtra(UPDATE_NAME_EXTRA, updateName);
        startService(intent);
    }

    @SuppressLint("ParcelCreator")
    private class AddressResultReceiver extends ResultReceiver {

        AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            boolean enableAddressButton = false;
            if (resultCode == LocationGeofenceEditorActivity.SUCCESS_RESULT) {
                // Display the address string
                // or an error message sent from the intent service.
                String addressOutput = resultData.getString(RESULT_DATA_KEY);
                addressText.setText(addressOutput);

                if (resultData.getBoolean(UPDATE_NAME_EXTRA, false))
                    geofenceNameEditText.setText(addressOutput);

                updateEditedMarker(false);

                enableAddressButton = true;
            }

            GlobalGUIRoutines.setImageButtonEnabled(enableAddressButton, addressButton, R.drawable.ic_action_location_address, getApplicationContext());

            //mAddressRequested = false;
        }
    }


    //------------------------------------------

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), "errorDialog");
    }

    /* Called from ErrorDialogFragment when the dialog is dismissed. */
    private void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = -999;
            if (this.getArguments() != null)
                errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (getActivity() != null)
                ((LocationGeofenceEditorActivity) getActivity()).onDialogDismissed();
        }
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        try {
            super.startActivityForResult(intent, requestCode);
        } catch (NullPointerException e) {
            // fixes Google Maps bug: http://stackoverflow.com/a/20905954/2075875
            String pkg = null;
            if (intent != null)
                pkg = intent.getPackage();
            if (intent == null || (pkg != null && pkg.equals("com.android.vending")))
                Log.e("LocationGeofenceEditorActivity", "ignoring startActivityForResult exception ", e);
            else
                throw e;
        }
    }

}


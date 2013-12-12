package com.example.hellogooglemaps;

/**
 * Created by xdai on 11/13/13.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.example.hellogooglemaps.location.LocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

/**
 * A placeholder fragment containing a simple view.
 */
public  class MainFragment extends SherlockFragment  implements
        LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

        // A request to connect to Location Services
        private LocationRequest mLocationRequest;

        // Stores the current instantiation of the location client in this object
        private LocationClient mLocationClient;

        // Handles to UI widgets
        private TextView mLatLng;
        private TextView mAddress;
        private ProgressBar mActivityIndicator;
        private TextView mConnectionState;
        private TextView mConnectionStatus;
        private Button mGetLocation;
        private Button mStartUpdate;
        private Button mStopUpdate;
        private Button mGetAddress;
        // Handle to SharedPreferences for this app
        SharedPreferences mPrefs;

        // Handle to a SharedPreferences editor
        SharedPreferences.Editor mEditor;

    /*
     * Note if updates have been turned on. Starts out as "false"; is set to "true" in the
     * method handleRequestSuccess of LocationUpdateReceiver.
     *
     */
    boolean mUpdatesRequested = false;
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    public static MainFragment newInstance(){
        Bundle args = new Bundle();

        MainFragment mainFragment = new MainFragment();
        mainFragment.setArguments(args);

        return mainFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.main_fragment, container, false);
        // Get handles to the UI view objects
        mLatLng = (TextView) rootView.findViewById(R.id.lat_lng);
        mAddress = (TextView) rootView.findViewById(R.id.address);
        mActivityIndicator = (ProgressBar) rootView.findViewById(R.id.address_progress);
        mConnectionState = (TextView) rootView.findViewById(R.id.text_connection_state);
        mConnectionStatus = (TextView) rootView.findViewById(R.id.text_connection_status);
        mGetLocation= (Button)rootView.findViewById(R.id.get_location_button);
        mStartUpdate= (Button)rootView.findViewById(R.id.start_updates);
        mStopUpdate= (Button)rootView.findViewById(R.id.stop_updates);
        mGetAddress= (Button)rootView.findViewById(R.id.get_address_button);
        // Create a new global location parameters object
        mLocationRequest = LocationRequest.create();

        /*
         * Set the update interval
         */
        mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Set the interval ceiling to one minute
        mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);

        // Note that location updates are off until the user turns them on
        mUpdatesRequested = false;

        // Open Shared Preferences
        mPrefs = getSherlockActivity().getSharedPreferences(LocationUtils.SHARED_PREFERENCES, Context.MODE_PRIVATE);

        // Get an editor
        mEditor = mPrefs.edit();

        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(this.getSherlockActivity(), this, this);
        mGetLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getLocation(view);
            }
        });
        mGetAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAddress(view);
            }
        });
        mStartUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startUpdates(view);
            }
        });
        mStopUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopUpdates(view);
            }
        });
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    /*
     * Called when the Activity is restarted, even before it becomes visible.
     */
    @Override
    public void onStart() {

        super.onStart();
        /*
         * Connect the client. Don't re-start any requests here;
         * instead, wait for onResume()
         */
        mLocationClient.connect();

    }
    /*
     * Called when the system detects that this Activity is now visible.
     */
    @Override
    public void onResume() {
        super.onResume();

        // If the app already has a setting for getting location updates, get it
        if (mPrefs.contains(LocationUtils.KEY_UPDATES_REQUESTED)) {
            mUpdatesRequested = mPrefs.getBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);

            // Otherwise, turn off location updates until requested
        } else {
            mEditor.putBoolean(LocationUtils.KEY_UPDATES_REQUESTED, false);
            mEditor.commit();
        }

    }

    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * LocationUpdateRemover and LocationUpdateRequester may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {
            // If the request code matches the code sent in onConnectionFailed
            case LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :
                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:
                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.resolved));

                        // Display the result
                        mConnectionState.setText(R.string.connected);
                        mConnectionStatus.setText(R.string.resolved);
                        break;

                    // If any other result was returned by Google Play services
                    default:
                        // Log the result
                        Log.d(LocationUtils.APPTAG, getString(R.string.no_resolution));

                        // Display the result
                        mConnectionState.setText(R.string.disconnected);
                        mConnectionStatus.setText(R.string.no_resolution);

                        break;
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(LocationUtils.APPTAG,
                        getString(R.string.unknown_activity_request_code, requestCode));

                break;
        }
    }

    /**
     * Verify that Google Play services is available before making a request.
     *
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getSherlockActivity());


        if (ConnectionResult.SUCCESS == resultCode) {
            Log.d(LocationUtils.APPTAG, getString(R.string.play_services_available));
            return true;
        } else {

            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this.getSherlockActivity(), 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSherlockActivity().getSupportFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }

    /**
     * Invoked by the "Get Location" button.
     *
     * Calls getLastLocation() to get the current location
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void getLocation(View v) {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Display the current location in the UI
            mLatLng.setText(LocationUtils.getLatLng(this.getSherlockActivity(), currentLocation));
        }
    }

    /**
     * Invoked by the "Get Address" button.
     * Get the address of the current location, using reverse geocoding. This only works if
     * a geocoding service is available.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    // For Eclipse with ADT, suppress warnings about Geocoder.isPresent()
    @SuppressLint("NewApi")
    public void getAddress(View v) {

        // In Gingerbread and later, use Geocoder.isPresent() to see if a geocoder is available.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD && !Geocoder.isPresent()) {
            // No geocoder is present. Issue an error message
            Toast.makeText(this.getSherlockActivity(), R.string.no_geocoder_available, Toast.LENGTH_LONG).show();
            return;
        }

        if (servicesConnected()) {

            Location currentLocation = mLocationClient.getLastLocation();
            // Turn the indefinite activity indicator on
            mActivityIndicator.setVisibility(View.VISIBLE);
            // Start the background task
            (new MainFragment.GetAddressTask(this.getSherlockActivity())).execute(currentLocation);
        }
    }

    /**
     * Invoked by the "Start Updates" button
     * Sends a request to start location updates
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void startUpdates(View v) {
        mUpdatesRequested = true;

        if (servicesConnected()) {
            startPeriodicUpdates();
        }
    }

    /**
     * Invoked by the "Stop Updates" button
     * Sends a request to remove location updates
     * request them.
     *
     * @param v The view object associated with this method, in this case a Button.
     */
    public void stopUpdates(View v) {
        mUpdatesRequested = false;

        if (servicesConnected()) {
            stopPeriodicUpdates();
        }
    }

    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle bundle) {
        mConnectionStatus.setText(R.string.connected);

        if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
    }

    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        mConnectionStatus.setText(R.string.disconnected);
    }

    /*
     * Called by Location Services if the attempt to
     * Location Services fails.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this.getSherlockActivity(),
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
    }

    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {

        // Report to the UI that the location was updated
        mConnectionStatus.setText(R.string.location_updated);

        // In the UI, set the latitude and longitude to the value received
        mLatLng.setText(LocationUtils.getLatLng(this.getSherlockActivity(), location));
    }


    /**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {

        mLocationClient.requestLocationUpdates(mLocationRequest, (com.google.android.gms.location.LocationListener)this.getSherlockActivity());
        mConnectionState.setText(R.string.location_requested);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates((com.google.android.gms.location.LocationListener)this.getSherlockActivity());
        mConnectionState.setText(R.string.location_updates_stopped);
    }

    /**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                errorCode,
                this.getSherlockActivity(),
                LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {
            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(getSherlockActivity().getSupportFragmentManager(), LocationUtils.APPTAG);
        }
    }


    /**
     * An AsyncTask that calls getFromLocation() in the background.
     * The class uses the following generic types:
     * Location - A {@link android.location.Location} object containing the current location,
     *            passed as the input parameter to doInBackground()
     * Void     - indicates that progress units are not used by this subclass
     * String   - An address passed to onPostExecute()
     */
    protected class GetAddressTask extends AsyncTask<Location, Void, String> {
        // Store the context passed to the AsyncTask when the system instantiates it.
       Context mContext;

        // Constructor called by the system to instantiate the task
        public GetAddressTask(Context context) {
            // Required by the semantics of AsyncTask
            super();
            mContext = context;
        }

        @Override
        protected String doInBackground(Location... params) {
           return new AddressFetchr(mContext).getAdress(params);
        }

        /**
         * A method that's called once doInBackground() completes. Set the text of the
         * UI element that displays the address. This method runs on the UI thread.
         */
        @Override
        protected void onPostExecute(String address) {
            // Turn off the progress bar
            mActivityIndicator.setVisibility(View.GONE);
            // Set the address in the UI
            mAddress.setText(address);
        }
    }

}

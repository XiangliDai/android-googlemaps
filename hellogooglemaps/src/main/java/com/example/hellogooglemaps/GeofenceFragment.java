package com.example.hellogooglemaps;

/**
 * Created by xdai on 11/13/13.
 */

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.example.hellogooglemaps.geofence.GeofenceRemover;
import com.example.hellogooglemaps.geofence.GeofenceRequester;
import com.example.hellogooglemaps.geofence.GeofenceUtils;
import com.example.hellogooglemaps.geofence.ReceiveTransitionsIntentService;
import com.example.hellogooglemaps.geofence.SimpleGeofence;
import com.example.hellogooglemaps.geofence.SimpleGeofenceStore;
import com.example.hellogooglemaps.location.LocationUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public  class GeofenceFragment extends SherlockFragment  implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationClient.OnAddGeofencesResultListener{
        /*
        * Use to set an expiration time for a geofence. After this amount
        * of time Location Services will stop tracking the geofence.
        * Remember to unregister a geofence when you're finished with it.
        * Otherwise, your app will use up battery. To continue monitoring
        * a geofence indefinitely, set the expiration time to
        * Geofence#NEVER_EXPIRE.
        */
        private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
        private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
                GEOFENCE_EXPIRATION_IN_HOURS * DateUtils.HOUR_IN_MILLIS;
        // Holds the location client
        private LocationClient mLocationClient;
        // Stores the PendingIntent used to request geofence monitoring
        private PendingIntent mGeofenceRequestIntent;
        // Defines the allowable request types.
        //public enum REQUEST_TYPE = {ADD};

        // Store the current request
        private GeofenceUtils.REQUEST_TYPE mRequestType;

        // Store the current type of removal
        private GeofenceUtils.REMOVE_TYPE mRemoveType;

        // Persistent storage for geofences
        private SimpleGeofenceStore mPrefs;

        // Store a list of geofences to add
        List<Geofence> mCurrentGeofences;

        // Add geofences handler
        private GeofenceRequester mGeofenceRequester;
        // Remove geofences handler
        private GeofenceRemover mGeofenceRemover;
        // Handle to geofence 1 latitude in the UI
        private EditText mLatitude1;

        // Handle to geofence 1 longitude in the UI
        private EditText mLongitude1;

        // Handle to geofence 1 radius in the UI
        private EditText mRadius1;

        // Handle to geofence 2 latitude in the UI
        private EditText mLatitude2;

        // Handle to geofence 2 longitude in the UI
        private EditText mLongitude2;

        // Handle to geofence 2 radius in the UI
        private EditText mRadius2;

        private Button mRegister;
        private Button mUnregisterByPendingIntent;
        private Button mUnregisterGeofence1;
        private String lat, lng;
        private Button mUnregisterGeofence2;
        /*
         * Internal lightweight geofence objects for geofence 1 and 2
         */
        private SimpleGeofence mUIGeofence1;
        private SimpleGeofence mUIGeofence2;

        // decimal formats for latitude, longitude, and radius
        private DecimalFormat mLatLngFormat;
        private DecimalFormat mRadiusFormat;

        /*
         * An instance of an inner class that receives broadcasts from listeners and from the
         * IntentService that receives geofence transition events
         */
        private GeofenceSampleReceiver mBroadcastReceiver;

        // An intent filter for the broadcast receiver
        private IntentFilter mIntentFilter;

        // Store the list of geofences to remove
        private List<String> mGeofenceIdsToRemove;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Set the pattern for the latitude and longitude format
            String latLngPattern = getString(R.string.lat_lng_pattern);
            lat  = (String)getArguments().getSerializable(MainFragment.EXTRA_LAT);
            lng  = (String)getArguments().getSerializable(MainFragment.EXTRA_LNG);
            // Set the format for latitude and longitude
            mLatLngFormat = new DecimalFormat(latLngPattern);

            // Localize the format
            mLatLngFormat.applyLocalizedPattern(mLatLngFormat.toLocalizedPattern());

            // Set the pattern for the radius format
            String radiusPattern = getString(R.string.radius_pattern);

            // Set the format for the radius
            mRadiusFormat = new DecimalFormat(radiusPattern);

            // Localize the pattern
            mRadiusFormat.applyLocalizedPattern(mRadiusFormat.toLocalizedPattern());

            // Create a new broadcast receiver to receive updates from the listeners and service
            mBroadcastReceiver = new GeofenceSampleReceiver();

            // Create an intent filter for the broadcast receiver
            mIntentFilter = new IntentFilter();

            // Action for broadcast Intents that report successful addition of geofences
            mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);

            // Action for broadcast Intents that report successful removal of geofences
            mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);

            // Action for broadcast Intents containing various types of geofencing errors
            mIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);

            // All Location Services sample apps use this category
            mIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

            // Instantiate a new geofence storage area
            mPrefs = new SimpleGeofenceStore(this.getSherlockActivity());

            // Instantiate the current List of geofences
            mCurrentGeofences = new ArrayList<Geofence>();

            // Instantiate a Geofence requester
            mGeofenceRequester = new GeofenceRequester(this.getSherlockActivity());

            // Instantiate a Geofence remover
            mGeofenceRemover = new GeofenceRemover(this.getSherlockActivity());


        }

    public static GeofenceFragment newInstance(String lat, String lng){
        Bundle args = new Bundle();
        args.putSerializable(MainFragment.EXTRA_LAT, lat);
        args.putSerializable(MainFragment.EXTRA_LNG, lng);
        GeofenceFragment geofenceFragment = new GeofenceFragment();
        geofenceFragment.setArguments(args);

        return geofenceFragment;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.geofence_fragment, container, false);
        // Get handles to the UI view objects
        // Get handles to the Geofence editor fields in the UI
        mLatitude1 = (EditText) rootView.findViewById(R.id.value_latitude_1);
        mLongitude1 = (EditText) rootView.findViewById(R.id.value_longitude_1);
        mRadius1 = (EditText) rootView.findViewById(R.id.value_radius_1);
        mLatitude2 = (EditText) rootView.findViewById(R.id.value_latitude_2);
        mLongitude2 = (EditText) rootView.findViewById(R.id.value_longitude_2);
        mRadius2 = (EditText) rootView.findViewById(R.id.value_radius_2);
        mLatitude1.setText(lat);
        mLongitude1.setText(lng);
        mRegister= (Button)rootView.findViewById(R.id.register);
        mUnregisterByPendingIntent= (Button)rootView.findViewById(R.id.unregister_by_pending_intent);
        mUnregisterGeofence1= (Button)rootView.findViewById(R.id.unregister_geofence1);
        mUnregisterGeofence2= (Button)rootView.findViewById(R.id.unregister_geofence2);
        mRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRegisterClicked(view);
            }
        });
        mUnregisterByPendingIntent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUnregisterByPendingIntentClicked(view);
            }
        });
        mUnregisterGeofence1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUnregisterGeofence1Clicked(view);
            }
        });
        mUnregisterGeofence2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onUnregisterGeofence2Clicked(view);
            }
        });
        return rootView;
    }

        /*
         * Handle results returned to this Activity by other Activities started with
         * startActivityForResult(). In particular, the method onConnectionFailed() in
         * GeofenceRemover and GeofenceRequester may call startResolutionForResult() to
         * start an Activity that handles Google Play services problems. The result of this
         * call returns here, to onActivityResult.
         * calls
         */
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent intent) {
            // Choose what to do based on the request code
            switch (requestCode) {

                // If the request code matches the code sent in onConnectionFailed
                case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                    switch (resultCode) {
                        // If Google Play services resolved the problem
                        case Activity.RESULT_OK:

                            // If the request was to add geofences
                            if (GeofenceUtils.REQUEST_TYPE.ADD == mRequestType) {

                                // Toggle the request flag and send a new request
                                mGeofenceRequester.setInProgressFlag(false);

                                // Restart the process of adding the current geofences
                                mGeofenceRequester.addGeofences(mCurrentGeofences);

                                // If the request was to remove geofences
                            } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == mRequestType ){

                                // Toggle the removal flag and send a new removal request
                                mGeofenceRemover.setInProgressFlag(false);

                                // If the removal was by Intent
                                if (GeofenceUtils.REMOVE_TYPE.INTENT == mRemoveType) {

                                    // Restart the removal of all geofences for the PendingIntent
                                    mGeofenceRemover.removeGeofencesByIntent(
                                            mGeofenceRequester.getRequestPendingIntent());

                                    // If the removal was by a List of geofence IDs
                                } else {

                                    // Restart the removal of the geofence list
                                    mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);
                                }
                            }
                            break;

                        // If any other result was returned by Google Play services
                        default:

                            // Report that Google Play services was unable to resolve the problem.
                            Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                    }

                    // If any other request code was received
                default:
                    // Report that this Activity received an unknown requestCode
                    Log.d(GeofenceUtils.APPTAG,
                            getString(R.string.unknown_activity_request_code, requestCode));

                    break;
            }
        }

        /*
         * Whenever the Activity resumes, reconnect the client to Location
         * Services and reload the last geofences that were set
         */
        @Override
        public void onResume() {
            super.onResume();
            // Register the broadcast receiver to receive status updates
            LocalBroadcastManager.getInstance(this.getSherlockActivity()).registerReceiver(mBroadcastReceiver, mIntentFilter);
        /*
         * Get existing geofences from the latitude, longitude, and
         * radius values stored in SharedPreferences. If no values
         * exist, null is returned.
         */
            mUIGeofence1 = mPrefs.getGeofence("1");
            mUIGeofence2 = mPrefs.getGeofence("2");
        /*
         * If the returned geofences have values, use them to set
         * values in the UI, using the previously-defined number
         * formats.
         */
            if (mUIGeofence1 != null) {
                mLatitude1.setText(
                        mLatLngFormat.format(
                                mUIGeofence1.getLatitude()));
                mLongitude1.setText(
                        mLatLngFormat.format(
                                mUIGeofence1.getLongitude()));
                mRadius1.setText(
                        mRadiusFormat.format(
                                mUIGeofence1.getRadius()));
            }
            if (mUIGeofence2 != null) {
                mLatitude2.setText(
                        mLatLngFormat.format(
                                mUIGeofence2.getLatitude()));
                mLongitude2.setText(
                        mLatLngFormat.format(
                                mUIGeofence2.getLongitude()));
                mRadius2.setText(
                        mRadiusFormat.format(
                                mUIGeofence2.getRadius()));
            }
        }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu,inflater);
        inflater.inflate(R.menu.main, menu);
    }

    /*
         * Respond to menu item selections
*/
        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            // Handle item selection
            switch (item.getItemId()) {

                // Request to clear the geofence1 settings in the UI
                case R.id.menu_item_clear_geofence1:
                    mLatitude1.setText(GeofenceUtils.EMPTY_STRING);
                    mLongitude1.setText(GeofenceUtils.EMPTY_STRING);
                    mRadius1.setText(GeofenceUtils.EMPTY_STRING);
                    return true;

                // Request to clear the geofence2 settings in the UI
                case R.id.menu_item_clear_geofence2:
                    mLatitude2.setText(GeofenceUtils.EMPTY_STRING);
                    mLongitude2.setText(GeofenceUtils.EMPTY_STRING);
                    mRadius2.setText(GeofenceUtils.EMPTY_STRING);
                    return true;

                // Request to clear both geofence settings in the UI
                case R.id.menu_item_clear_geofences:
                    mLatitude1.setText(GeofenceUtils.EMPTY_STRING);
                    mLongitude1.setText(GeofenceUtils.EMPTY_STRING);
                    mRadius1.setText(GeofenceUtils.EMPTY_STRING);

                    mLatitude2.setText(GeofenceUtils.EMPTY_STRING);
                    mLongitude2.setText(GeofenceUtils.EMPTY_STRING);
                    mRadius2.setText(GeofenceUtils.EMPTY_STRING);
                    return true;

                // Remove all geofences from storage
                case R.id.menu_item_clear_geofence_history:
                    mPrefs.clearGeofence("1");
                    mPrefs.clearGeofence("2");
                    return true;

                // Pass through any other request
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

        /*
         * Save the current geofence settings in SharedPreferences.
         */
        @Override
        public void onPause() {
            super.onPause();
            mPrefs.setGeofence("1", mUIGeofence1);
            mPrefs.setGeofence("2", mUIGeofence2);
        }

        /**
         * Verify that Google Play services is available before making a request.
         *
         * @return true if Google Play services is available, otherwise false
         */
        private boolean servicesConnected() {

            // Check that Google Play services is available
            int resultCode =
                    GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getSherlockActivity());

            // If Google Play services is available
            if (ConnectionResult.SUCCESS == resultCode) {

                // In debug mode, log the status
                Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));

                // Continue
                return true;

                // Google Play services was not available for some reason
            } else {

                // Display an error dialog
                showErrorDialog(resultCode);
                return false;
            }
        }

        /**
         * Called when the user clicks the "Remove geofences" button
         *
         * @param view The view that triggered this callback
         */
        public void onUnregisterByPendingIntentClicked(View view) {
        /*
         * Remove all geofences set by this app. To do this, get the
         * PendingIntent that was added when the geofences were added
         * and use it as an argument to removeGeofences(). The removal
         * happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done
         */

        /*
         * Record the removal as remove by Intent. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
            // Record the type of removal
            mRemoveType = GeofenceUtils.REMOVE_TYPE.INTENT;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
            if (!servicesConnected()) {

                return;
            }

            // Try to make a removal request
            try {
        /*
         * Remove the geofences represented by the currently-active PendingIntent. If the
         * PendingIntent was removed for some reason, re-create it; since it's always
         * created with FLAG_UPDATE_CURRENT, an identical PendingIntent is always created.
         */
                mGeofenceRemover.removeGeofencesByIntent(mGeofenceRequester.getRequestPendingIntent());

            } catch (UnsupportedOperationException e) {
                // Notify user that previous request hasn't finished.
                Toast.makeText(this.getSherlockActivity(), R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
            }

        }

        /**
         * Called when the user clicks the "Remove geofence 1" button
         * @param view The view that triggered this callback
         */
        public void onUnregisterGeofence1Clicked(View view) {
        /*
         * Remove the geofence by creating a List of geofences to
         * remove and sending it to Location Services. The List
         * contains the id of geofence 1 ("1").
         * The removal happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done.
         */

            // Create a List of 1 Geofence with the ID "1" and store it in the global list
            mGeofenceIdsToRemove = Collections.singletonList("1");

        /*
         * Record the removal as remove by list. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
            mRemoveType = GeofenceUtils.REMOVE_TYPE.LIST;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
            if (!servicesConnected()) {

                return;
            }

            // Try to remove the geofence
            try {
                mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);

                // Catch errors with the provided geofence IDs
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (UnsupportedOperationException e) {
                // Notify user that previous request hasn't finished.
                Toast.makeText(this.getSherlockActivity(), R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Called when the user clicks the "Remove geofence 2" button
         * @param view The view that triggered this callback
         */
        public void onUnregisterGeofence2Clicked(View view) {
        /*
         * Remove the geofence by creating a List of geofences to
         * remove and sending it to Location Services. The List
         * contains the id of geofence 2, which is "2".
         * The removal happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done.
         */

        /*
         * Record the removal as remove by list. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
            mRemoveType = GeofenceUtils.REMOVE_TYPE.LIST;

            // Create a List of 1 Geofence with the ID "2" and store it in the global list
            mGeofenceIdsToRemove = Collections.singletonList("2");

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
            if (!servicesConnected()) {

                return;
            }

            // Try to remove the geofence
            try {
                mGeofenceRemover.removeGeofencesById(mGeofenceIdsToRemove);

                // Catch errors with the provided geofence IDs
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (UnsupportedOperationException e) {
                // Notify user that previous request hasn't finished.
                Toast.makeText(this.getSherlockActivity(), R.string.remove_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
            }
        }

        /**
         * Called when the user clicks the "Register geofences" button.
         * Get the geofence parameters for each geofence and add them to
         * a List. Create the PendingIntent containing an Intent that
         * Location Services sends to this app's broadcast receiver when
         * Location Services detects a geofence transition. Send the List
         * and the PendingIntent to Location Services.
         */
        public void onRegisterClicked(View view) {

        /*
         * Record the request as an ADD. If a connection error occurs,
         * the app can automatically restart the add request if Google Play services
         * can fix the error
         */
            mRequestType = GeofenceUtils.REQUEST_TYPE.ADD;

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
            if (!servicesConnected()) {

                return;
            }

        /*
         * Check that the input fields have values and that the values are with the
         * permitted range
         */
            if (!checkInputFields()) {
                return;
            }

        /*
         * Create a version of geofence 1 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         */
            mUIGeofence1 = new SimpleGeofence(
                    "1",
                    // Get latitude, longitude, and radius from the UI
                    Double.valueOf(mLatitude1.getText().toString()),
                    Double.valueOf(mLongitude1.getText().toString()),
                    Float.valueOf(mRadius1.getText().toString()),
                    // Set the expiration time
                    GEOFENCE_EXPIRATION_IN_MILLISECONDS,
                    // Only detect entry transitions
                    Geofence.GEOFENCE_TRANSITION_ENTER);

            // Store this flat version in SharedPreferences
            mPrefs.setGeofence("1", mUIGeofence1);

        /*
         * Create a version of geofence 2 that is "flattened" into individual fields. This
         * allows it to be stored in SharedPreferences.
         */
            mUIGeofence2 = new SimpleGeofence(
                    "2",
                    // Get latitude, longitude, and radius from the UI
                    Double.valueOf(mLatitude2.getText().toString()),
                    Double.valueOf(mLongitude2.getText().toString()),
                    Float.valueOf(mRadius2.getText().toString()),
                    // Set the expiration time
                    GEOFENCE_EXPIRATION_IN_MILLISECONDS,
                    // Detect both entry and exit transitions
                    Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT
            );

            // Store this flat version in SharedPreferences
            mPrefs.setGeofence("2", mUIGeofence2);

        /*
         * Add Geofence objects to a List. toGeofence()
         * creates a Location Services Geofence object from a
         * flat object
         */
            mCurrentGeofences.add(mUIGeofence1.toGeofence());
            mCurrentGeofences.add(mUIGeofence2.toGeofence());

            // Start the request. Fail if there's already a request in progress
            try {
                // Try to add geofences
                mGeofenceRequester.addGeofences(mCurrentGeofences);
            } catch (UnsupportedOperationException e) {
                // Notify user that previous request hasn't finished.
                Toast.makeText(this.getSherlockActivity(), R.string.add_geofences_already_requested_error,
                        Toast.LENGTH_LONG).show();
            }
        }
        /**
         * Check all the input values and flag those that are incorrect
         * @return true if all the widget values are correct; otherwise false
         */
        private boolean checkInputFields() {
            // Start with the input validity flag set to true
            boolean inputOK = true;

        /*
         * Latitude, longitude, and radius values can't be empty. If they are, highlight the input
         * field in red and put a Toast message in the UI. Otherwise set the input field highlight
         * to black, ensuring that a field that was formerly wrong is reset.
         */
            if (TextUtils.isEmpty(mLatitude1.getText())) {
                mLatitude1.setBackgroundColor(Color.RED);
                Toast.makeText(this.getSherlockActivity(), R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                mLatitude1.setBackgroundColor(Color.BLACK);
            }

            if (TextUtils.isEmpty(mLongitude1.getText())) {
                mLongitude1.setBackgroundColor(Color.RED);
                Toast.makeText(this.getSherlockActivity(), R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                mLongitude1.setBackgroundColor(Color.BLACK);
            }
            if (TextUtils.isEmpty(mRadius1.getText())) {
                mRadius1.setBackgroundColor(Color.RED);
                Toast.makeText(this.getSherlockActivity(), R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                mRadius1.setBackgroundColor(Color.BLACK);
            }

            if (TextUtils.isEmpty(mLatitude2.getText())) {
                mLatitude2.setBackgroundColor(Color.RED);
                Toast.makeText(this.getSherlockActivity(), R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                mLatitude2.setBackgroundColor(Color.BLACK);
            }
            if (TextUtils.isEmpty(mLongitude2.getText())) {
                mLongitude2.setBackgroundColor(Color.RED);
                Toast.makeText(this.getSherlockActivity(), R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                mLongitude2.setBackgroundColor(Color.BLACK);
            }
            if (TextUtils.isEmpty(mRadius2.getText())) {
                mRadius2.setBackgroundColor(Color.RED);
                Toast.makeText(this.getSherlockActivity(), R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                mRadius2.setBackgroundColor(Color.BLACK);
            }

        /*
         * If all the input fields have been entered, test to ensure that their values are within
         * the acceptable range. The tests can't be performed until it's confirmed that there are
         * actual values in the fields.
         */
            if (inputOK) {

            /*
             * Get values from the latitude, longitude, and radius fields.
             */
                double lat1 = Double.valueOf(mLatitude1.getText().toString());
                double lng1 = Double.valueOf(mLongitude1.getText().toString());
                double lat2 = Double.valueOf(mLatitude1.getText().toString());
                double lng2 = Double.valueOf(mLongitude1.getText().toString());
                float rd1 = Float.valueOf(mRadius1.getText().toString());
                float rd2 = Float.valueOf(mRadius2.getText().toString());

            /*
             * Test latitude and longitude for minimum and maximum values. Highlight incorrect
             * values and set a Toast in the UI.
             */

                if (lat1 > GeofenceUtils.MAX_LATITUDE || lat1 < GeofenceUtils.MIN_LATITUDE) {
                    mLatitude1.setBackgroundColor(Color.RED);
                    Toast.makeText(
                            this.getSherlockActivity(),
                            R.string.geofence_input_error_latitude_invalid,
                            Toast.LENGTH_LONG).show();

                    // Set the validity to "invalid" (false)
                    inputOK = false;
                } else {

                    mLatitude1.setBackgroundColor(Color.BLACK);
                }

                if ((lng1 > GeofenceUtils.MAX_LONGITUDE) || (lng1 < GeofenceUtils.MIN_LONGITUDE)) {
                    mLongitude1.setBackgroundColor(Color.RED);
                    Toast.makeText(
                            this.getSherlockActivity(),
                            R.string.geofence_input_error_longitude_invalid,
                            Toast.LENGTH_LONG).show();

                    // Set the validity to "invalid" (false)
                    inputOK = false;
                } else {

                    mLongitude1.setBackgroundColor(Color.BLACK);
                }

                if (lat2 > GeofenceUtils.MAX_LATITUDE || lat2 < GeofenceUtils.MIN_LATITUDE) {
                    mLatitude2.setBackgroundColor(Color.RED);
                    Toast.makeText(
                            this.getSherlockActivity(),
                            R.string.geofence_input_error_latitude_invalid,
                            Toast.LENGTH_LONG).show();

                    // Set the validity to "invalid" (false)
                    inputOK = false;
                } else {

                    mLatitude2.setBackgroundColor(Color.BLACK);
                }

                if ((lng2 > GeofenceUtils.MAX_LONGITUDE) || (lng2 < GeofenceUtils.MIN_LONGITUDE)) {
                    mLongitude2.setBackgroundColor(Color.RED);
                    Toast.makeText(
                            this.getSherlockActivity(),
                            R.string.geofence_input_error_longitude_invalid,
                            Toast.LENGTH_LONG).show();

                    // Set the validity to "invalid" (false)
                    inputOK = false;
                } else {

                    mLongitude2.setBackgroundColor(Color.BLACK);
                }
                if (rd1 < GeofenceUtils.MIN_RADIUS) {
                    mRadius1.setBackgroundColor(Color.RED);
                    Toast.makeText(
                            this.getSherlockActivity(),
                            R.string.geofence_input_error_radius_invalid,
                            Toast.LENGTH_LONG).show();

                    // Set the validity to "invalid" (false)
                    inputOK = false;
                } else {

                    mRadius1.setBackgroundColor(Color.BLACK);
                }
                if (rd2 < GeofenceUtils.MIN_RADIUS) {
                    mRadius2.setBackgroundColor(Color.RED);
                    showToast(R.string.geofence_input_error_radius_invalid);

                    // Set the validity to "invalid" (false)
                    inputOK = false;
                } else {

                    mRadius2.setBackgroundColor(Color.BLACK);
                }
            }

            // If everything passes, the validity flag will still be true, otherwise it will be false.
            return inputOK;
        }

    private void showToast(int restId){
        Toast.makeText(this.getSherlockActivity(), restId, Toast.LENGTH_LONG).show();
    }
    /*
         * Create a PendingIntent that triggers an IntentService in your
         * app when a geofence transition occurs.
         */
    private PendingIntent getTransitionPendingIntent() {
        // Create an explicit Intent
        Intent intent = new Intent(this.getSherlockActivity(),
                ReceiveTransitionsIntentService.class);
        /*
         * Return the PendingIntent
         */
        return PendingIntent.getService(
                this.getSherlockActivity(),
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
    @Override
    public void onConnected(Bundle bundle) {
        switch (mRequestType) {
            case ADD :
                // Get the PendingIntent for the request
                mGeofenceRequestIntent = getTransitionPendingIntent();
                // Send a request to add the current geofences
                mLocationClient.addGeofences(mCurrentGeofences, mGeofenceRequestIntent, this);
                break;
            case REMOVE_INTENT:
                mLocationClient.removeGeofences(mGeofenceRequestIntent, (LocationClient.OnRemoveGeofencesResultListener) this.getSherlockActivity());
                break;
    }
    }
    /*
      * Implement ConnectionCallbacks.onDisconnected()
      * Called by Location Services once the location client is
      * disconnected.
      */
    @Override
    public void onDisconnected() {
        // Turn off the request flag
        //mInProgress = false;
        // Destroy the current location client
        mLocationClient = null;
    }

   /*
     * Provide the implementation of
     * OnAddGeofencesResultListener.onAddGeofencesResult.
     * Handle the result of adding the geofences
     *
     */
        @Override
        public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
            // If adding the geofences was successful
            if (LocationStatusCodes.SUCCESS == statusCode) {
            /*
             * Handle successful addition of geofences here.
             * You can send out a broadcast intent or update the UI.
             * geofences into the Intent's extended data.
             */
                Toast.makeText(this.getSherlockActivity(), "onAddGeofencesResult", Toast.LENGTH_LONG).show();

            } else {
                // If adding the geofences failed
            /*
             * Report errors here.
             * You can log the error using Log.e() or update
             * the UI.
             */
            }
            // Turn off the in progress flag and disconnect the client
            //mInProgress = false;
            mLocationClient.disconnect();
        }
    // Implementation of OnConnectionFailedListener.onConnectionFailed
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
 * Define a Broadcast receiver that receives updates from connection listeners and
 * the geofence transition service.
 */
public class GeofenceSampleReceiver extends BroadcastReceiver {
    /*
     * Define the required method for broadcast receivers
     * This method is invoked when a broadcast Intent triggers the receiver
     */
    @Override
    public void onReceive(Context context, Intent intent) {

        // Check the action code and determine what to do
        String action = intent.getAction();

        // Intent contains information about errors in adding or removing geofences
        if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {

            handleGeofenceError(context, intent);

            // Intent contains information about successful addition or removal of geofences
        } else if (
                TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)
                        ||
                        TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {

            handleGeofenceStatus(context, intent);

            // Intent contains information about a geofence transition
        } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {

            handleGeofenceTransition(context, intent);

            // The Intent contained an invalid action
        } else {
            Log.e(GeofenceUtils.APPTAG, getString(R.string.invalid_action_detail, action));
            Toast.makeText(context, R.string.invalid_action, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * If you want to display a UI message about adding or removing geofences, put it here.
     *
     * @param context A Context for this component
     * @param intent The received broadcast Intent
     */
    private void handleGeofenceStatus(Context context, Intent intent) {

    }

    /**
     * Report geofence transitions to the UI
     *
     * @param context A Context for this component
     * @param intent The Intent containing the transition
     */
    private void handleGeofenceTransition(Context context, Intent intent) {
            /*
             * If you want to change the UI when a transition occurs, put the code
             * here. The current design of the app uses a notification to inform the
             * user that a transition has occurred.
             */
    }

    /**
     * Report addition or removal errors to the UI, using a Toast
     *
     * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
     */
    private void handleGeofenceError(Context context, Intent intent) {
        String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
        Log.e(GeofenceUtils.APPTAG, msg);
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }
}

}

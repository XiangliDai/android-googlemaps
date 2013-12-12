package com.example.hellogooglemaps;

/**
 * Created by xdai on 12/12/13.
 */

import android.content.Context;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.util.Log;

import com.example.hellogooglemaps.location.LocationUtils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class AddressFetchr {


    Context localContext;


    public AddressFetchr(Context context) {
        localContext = context;
    }

    /**
     * Get a geocoding service instance, pass latitude and longitude to it, format the returned
     * address, and return the address to the UI thread.
     */
    protected String getAdress(Location... params) {
            /*
             * Get a new geocoding service instance, set for localized addresses. This example uses
             * android.location.Geocoder, but other geocoders that conform to address standards
             * can also be used.
             */
        Resources resources = localContext.getResources();

        Geocoder geocoder = new Geocoder(localContext, Locale.getDefault());

        // Get the current location from the input parameter list
        Location location = params[0];

        // Create a list to contain the result address
        List<Address> addresses = null;

        // Try to get an address for the current location. Catch IO or network problems.
        try {

                /*
                 * Call the synchronous getFromLocation() method with the latitude and
                 * longitude of the current location. Return at most 1 address.
                 */
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);

            // Catch network or other I/O problems.
        } catch (IOException exception1) {


            Log.e(LocationUtils.APPTAG, resources.getString(R.string.IO_Exception_getFromLocation));
            // print the stack trace
            exception1.printStackTrace();

            return (resources.getString(R.string.IO_Exception_getFromLocation));

        } catch (IllegalArgumentException exception2) {
            // Construct a message containing the invalid arguments
            String errorString = resources.getString(
                    R.string.illegal_argument_exception,
                    location.getLatitude(),
                    location.getLongitude()
            );

            Log.e(LocationUtils.APPTAG, errorString);
            exception2.printStackTrace();

            return errorString;
        }
        // If the reverse geocode returned an address
        if (addresses != null && addresses.size() > 0) {

            Address address = addresses.get(0);
            // Format the first line of address
            String addressText = resources.getString(R.string.address_output_string,
                    // If there's a street address, add it
                    address.getMaxAddressLineIndex() > 0 ?
                            address.getAddressLine(0) : "",
                    // Locality is usually a city
                    address.getLocality(),
                    // The country of the address
                    address.getCountryName()
            );

            return addressText;

        } else {
            return resources.getString(R.string.no_address_found);
        }
    }

}

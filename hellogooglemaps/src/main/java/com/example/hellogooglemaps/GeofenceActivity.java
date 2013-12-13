/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.hellogooglemaps;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * UI handler for the Location Services Geofence sample app.
 * Allow input of latitude, longitude, and radius for two geofences.
 * When registering geofences, check input and then send the geofences to Location Services.
 * Also allow removing either one of or both of the geofences.
 * The menu allows you to clear the screen or delete the geofences stored in persistent memory.
 */
public class GeofenceActivity extends SingleFrameActivity {

    @Override
    protected SherlockFragment createFragment() {
        String lat = (String)getIntent().getSerializableExtra(MainFragment.EXTRA_LAT);
        String lng = (String)getIntent().getSerializableExtra(MainFragment.EXTRA_LNG);
        return  GeofenceFragment.newInstance(lat, lng);
    }
}


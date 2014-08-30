package com.vfdev.android.geopoints;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.vfdev.android.GeoTracker.GeoTracker;


public class GeoPointEdit extends Activity implements GeoTracker.OnLocationUpdateListener {

    private TextView mCurrentLocationTV;
    private GeoTracker mGeoTracker = null;


    @Override
    public void onLocationUpdate(Location location) {
        setCurrentLocation(
                (float) location.getLatitude(),
                (float) location.getLongitude()
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_point_edit);

        mCurrentLocationTV = (TextView) findViewById(R.id.gpLocation);

        // Setup geo tracker :
        mGeoTracker = GeoTracker.getInstance(getApplicationContext());
        // setup this activity as OnLocationUpdateListener for GeoTracker
        mGeoTracker.setOnLocationUpdateListener(this);
        // Show the location provider :
        showLocationProvider();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.geo_point_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);

    }


    @Override
    protected void onResume() {
        super.onResume();
        mGeoTracker.requestLocationUpdates();
    }

    @Override
    protected void onPause() {
        mGeoTracker.stopLocationUpdates();
        super.onPause();
    }

    protected void setCurrentLocation(float lat, float lng) {
        mCurrentLocationTV.setText(String.valueOf(lat) + ", " + String.valueOf(lng));
    }

    public void showLocationProvider() {
        String provider = mGeoTracker.getProvider();
        if (!provider.isEmpty()) {
            Toast.makeText(
                    getApplicationContext(),
                    "Provider : " + provider,
                    Toast.LENGTH_LONG
            ).show();
        } else {
            Toast.makeText(
                    getApplicationContext(),
                    "No location provider is found ",
                    Toast.LENGTH_LONG
            ).show();
        }
    }




}

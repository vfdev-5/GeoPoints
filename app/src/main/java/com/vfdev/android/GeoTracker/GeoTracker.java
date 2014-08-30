package com.vfdev.android.GeoTracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

/**
 * Created by FV on 30.08.2014.
 */
public class GeoTracker extends Service implements LocationListener {

    private static final int LOCATION_MIN_TIME_UPDATE=5000; // in millis
    private static final int LOCATION_MIN_DIST_UPDATE=10; // in meters

    private LocationManager mLocationManager = null;
    private String mProvider = "";
    private Location mCurrentLocation = null;

    // flags :
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
    boolean canGetLocation = false;
    boolean isEnabled = false;


    // Callback for location updates
    public interface OnLocationUpdateListener {
        public void onLocationUpdate(Location loc);
    };
    private OnLocationUpdateListener mListener = null;

    public void setOnLocationUpdateListener(OnLocationUpdateListener listener) {
        mListener = listener;
    }


    // Singleton instance
    private static GeoTracker mInstance = null;

    public static GeoTracker getInstance(Context context) {
        if (mInstance == null && context != null) {
            mInstance = new GeoTracker(context);
        }
        return mInstance;
    }
    public static GeoTracker getInstance() {
        return mInstance;
    }


    private GeoTracker(Context context) {

        mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (mLocationManager == null) {
            return;
        }
        canGetLocation=true;
        isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (isGPSEnabled || isNetworkEnabled){
            isEnabled = true;
            requestLocationUpdates();
        }

    }

    public boolean canGetLocation() {
        return canGetLocation;
    }
    public boolean isEnabled() {
        return isEnabled;
    }

    public void requestLocationUpdates() {
        // Firstly request
        if (isNetworkEnabled) {
            mProvider = LocationManager.NETWORK_PROVIDER;
        } else if (isGPSEnabled) {
            mProvider = LocationManager.GPS_PROVIDER;
        }
        mLocationManager.requestLocationUpdates(
                mProvider,
                LOCATION_MIN_TIME_UPDATE,
                LOCATION_MIN_DIST_UPDATE,
                this
        );
    }

    public void stopLocationUpdates() {
        mLocationManager.removeUpdates(this);
    }

    public double getLatitude() {
        if (mCurrentLocation != null) {
            return mCurrentLocation.getLatitude();
        }
        return 0.0;
    }

    public double getLongitude() {
        if (mCurrentLocation != null) {
            return mCurrentLocation.getLongitude();
        }
        return 0.0;
    }

    public String getProvider() {
        return mProvider;
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
        mProvider = provider;
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
        mProvider = provider;
    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        if (mListener != null) {
            mListener.onLocationUpdate(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    @Override
    public IBinder onBind(Intent arg) {
        return null;
    }


}

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
import android.util.Log;
import android.widget.Toast;

import java.util.List;

/**
 * Created by FV on 30.08.2014.
 */
public class GeoTracker extends Service implements LocationListener {

    private static final String TAG = GeoTracker.class.getName();

    private static final float MAX_ACCURACY = 50.0f; // meters
    private static final int MAX_TIME_INTERVAL = 5*60*1000; // 5 minutes = interval between last measured location and current time

    private long mLocationMinTimeUpdate = 10 * 1000;
    private float mLocationMinDistUpdate = 10.0f;

    private LocationManager mLocationManager = null;
    private String mProvider = "";
    private String mAvailableProviders = "";
    private Location mCurrentLocation = null;

    // flags :
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled = false;
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

    public static GeoTracker getInstance() {
        if (mInstance == null) {
            mInstance = new GeoTracker();
        }
        return mInstance;
    }

    public boolean init(Context context) {

        mLocationManager = (LocationManager) context.getSystemService(LOCATION_SERVICE);
        if (mLocationManager == null) {
            return false;
        }
        checkProviders();
        if (isGPSEnabled || isNetworkEnabled){
            isEnabled = true;
//            requestLocationUpdates();
        }
        return true;
    }


    private void checkProviders() {
        isGPSEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        mAvailableProviders="";
        if (isGPSEnabled)
            mAvailableProviders += "gps ";
        if (isNetworkEnabled)
            mAvailableProviders += "network ";

//        Toast.makeText(this, "Check providers, available providers : " + mAvailableProviders, Toast.LENGTH_LONG).show();
        Log.i(TAG, "Check providers, available providers : " + mAvailableProviders);

    }

    private GeoTracker() {
        // empty
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public long getLocationMinTimeUpdate() {
        return mLocationMinTimeUpdate;
    }
    public void setLocationMinTimeUpdate(int time) {
        mLocationMinTimeUpdate = time;
    }

    public float getLocationMinDistUpdate() {
        return mLocationMinDistUpdate;
    }
    public void setLocationMinDistUpdate(int dist) {
        mLocationMinDistUpdate = dist;
    }


    protected boolean isInitOK() {
        if (mLocationManager == null) {
            Log.w(TAG, "Class is not initialized or Location services are not available");
            return false;
        }
        return true;
    }


    public void requestLocationUpdates() {

        if (!isInitOK()) return;

        checkProviders();
        Log.i(TAG, "requestLocationUpdates");
        if (isNetworkEnabled) {
            mProvider = LocationManager.NETWORK_PROVIDER;
            mLocationManager.requestLocationUpdates(
                    mProvider,
                    mLocationMinTimeUpdate,
                    mLocationMinDistUpdate,
                    this
            );
        }

        if (isGPSEnabled) {
            mProvider = LocationManager.GPS_PROVIDER;
            mLocationManager.requestLocationUpdates(
                    mProvider,
                    mLocationMinTimeUpdate,
                    mLocationMinDistUpdate,
                    this
            );
        }
    }

    public void stopLocationUpdates() {
        if (!isInitOK()) return;
        mLocationManager.removeUpdates(this);
    }

    public Location getCurrentLocation() {
        return mCurrentLocation;
    }

    public Location getLastKnownLocation() {
        if (!isInitOK()) return null;

        float accuracy = Float.MAX_VALUE;
        long time = Long.MIN_VALUE;
        Location output = null;

        List<String> providers = mLocationManager.getAllProviders();
        for (String provider: providers) {

            Location loc = mLocationManager.getLastKnownLocation(provider);
            if (loc != null) {

                float a = loc.getAccuracy();
                if (a < accuracy) {
                    accuracy = a;
                    time = loc.getTime();
                    output = loc;
                }
            }

        }

        if (accuracy > MAX_ACCURACY ||
                System.currentTimeMillis() - time > MAX_TIME_INTERVAL) {
            output = null;
        }
        return output;
    }

    public String getProvider() {
        return mProvider;
    }

    public String getAvailableProviders() {
        return mAvailableProviders;
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
        mProvider = provider;
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
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

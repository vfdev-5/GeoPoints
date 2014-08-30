package com.vfdev.android.geopoints;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.vfdev.android.DB.GeoDBConf;
import com.vfdev.android.DB.GeoDBHandler;
import com.vfdev.android.GeoTracker.GeoTracker;

public class GeoPointList extends ListActivity {

    private static final String TAG=GeoPointList.class.getName();

    private GeoDBHandler mGeoDBHandler = null;
    private GeoTracker mGeoTracker = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_point_list);

        // setup GeoDB Handler :
        mGeoDBHandler = GeoDBHandler.getInstance(getApplicationContext());

        // setup GeoTracker :
        mGeoTracker = GeoTracker.getInstance(this);
        if (!mGeoTracker.canGetLocation()) {
            // TODO: Alert user and close application
            Toast.makeText(
                    getApplicationContext(),
                    "No location service is available",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        }

        if (!mGeoTracker.isEnabled()) {
            askToEnableLocationService();
        }


        // setup item context menu
        registerForContextMenu(getListView());
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }



    @Override
    protected void onResume() {
        super.onResume();
        // (re-)fill data from GeoDB
        fillList();

    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.geo_point_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_point) {
            // go to GeoPointEdit activity
            createGeoPoint();
            return true;
        }
        //else if (id == R.id.action_settings) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView,view,position,id);
        editGeoPoint(id);
    }

    protected void createGeoPoint() {
        Intent i = new Intent(this, GeoPointEdit.class);
        startActivity(i);
    }

    protected void editGeoPoint(long id) {
        Intent i = new Intent(this, GeoPointEdit.class);
        // Put extra info about selected item : key_rowid and id
        //i.putExtra()
        startActivity(i);
    }

    protected void fillList() {

        Cursor cursor = mGeoDBHandler.getAllDataFromTable(GeoDBConf.GEODB_TABLE_GEOPOINTS);
        startManagingCursor(cursor);

        // Create an array to specify the fields we want to display in the list (only TITLE)
        String[] from = new String[]{
                GeoDBConf.COMMON_KEY_NAME,
                GeoDBConf.COMMON_KEY_DESC
        };

        // and an array of the fields we want to bind those fields to (in this case just text1)
        int[] to = new int[]{
                R.id.gpLIName,
                R.id.gpLIDescription
        };

        // Now create a simple cursor adapter and set it to display
        SimpleCursorAdapter points =
                new SimpleCursorAdapter(
                        this,
                        R.layout.layout_geopoint_row,
                        cursor,
                        from,
                        to,
                        CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
        setListAdapter(points);
    }


    protected void askToEnableLocationService() {



        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);

    }


}


/*
public class GeoPointList extends ListActivity implements LocationListener {


    private static final int LOCATION_MIN_TIME_UPDATE=5000; // in millis
    private static final int LOCATION_MIN_DIST_UPDATE=10; // in meters

    private LocationManager mLocationManager;
    private String mProvider;
    private TextView mCurrentLocationTV;
    private PointF mCurrentLocation = new PointF();


    protected void showLocationProvider() {
        if (!mProvider.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Provider : " + mProvider,
                    Toast.LENGTH_LONG);
        } else {
            Toast.makeText(getApplicationContext(),
                    "No location provider is found ",
                    Toast.LENGTH_LONG);
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_point_list);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Check if GPS provider is enabled otherwise ask to activate
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        // Define the criteria how to select the locatioin provider -> use default
        // 2nd arg : if true then only a provider that is currently enabled is returned
        Criteria criteria = new Criteria();
        mProvider = mLocationManager.getBestProvider(criteria, true);

        // Show the provider :
        showLocationProvider();

        // Initialize the location fields
        Location location = mLocationManager.getLastKnownLocation(mProvider);
        if (location != null) {
            onLocationChanged(location);
        } else {
            setCurrentLocation(0, 0);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationManager.requestLocationUpdates(
                mProvider,
                LOCATION_MIN_TIME_UPDATE,
                LOCATION_MIN_DIST_UPDATE,
                this
        );
    }

    @Override
    protected void onPause() {
        mLocationManager.removeUpdates(this);
        super.onPause();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.geo_point_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this, "Enabled new provider " + provider, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(this, "Disabled provider " + provider, Toast.LENGTH_SHORT).show();
    }

    protected void setCurrentLocation(float lat, float lng) {
        mCurrentLocationTV.setText( String.valueOf(lat) + ", " + String.valueOf(lng) );
        mCurrentLocation.set(lat, lng);
    }

    @Override
    public void onLocationChanged(Location location) {
        float lat = (float) (location.getLatitude());
        float lng = (float) (location.getLongitude());
        setCurrentLocation(lat, lng);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }





    public void onAddClicked(View view) {

        // add current location to the DB

    }


}
*/
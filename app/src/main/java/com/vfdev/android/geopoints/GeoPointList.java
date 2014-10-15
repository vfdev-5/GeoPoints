package com.vfdev.android.geopoints;

import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.vfdev.android.DB.GeoDBConf;
import com.vfdev.android.DB.GeoDBHandler;
import com.vfdev.android.GeoTracker.GeoTracker;

public class GeoPointList extends ListActivity implements GeoTracker.OnLocationUpdateListener {

    private static final String TAG=GeoPointList.class.getName();

    private GeoDBHandler mGeoDBHandler = null;
    private GeoTracker mGeoTracker = null;
    private Location mCurrentLocation = null;
    private TextView mCurrentLocationTV = null;


    @Override
    public void onLocationUpdate(Location location) {
        if (location != null) {
            handleLocation(location);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);

        // setup GeoDB Handler :
        mGeoDBHandler = GeoDBHandler.getInstance();
        mGeoDBHandler.init(getApplicationContext());

        // setup GeoTracker :
        mGeoTracker = GeoTracker.getInstance();
        if (!mGeoTracker.init(getApplicationContext())) {
            // TODO: Alert user and close application
            Toast.makeText(
                    getApplicationContext(),
                    "No location service is available",
                    Toast.LENGTH_LONG
            ).show();
            finish();
        }

        // connect to geotracker location update as listener
        mGeoTracker.setOnLocationUpdateListener(this);

        setContentView(R.layout.activity_geo_point_list);
        mCurrentLocationTV = (TextView) findViewById(R.id.currentLocation);

        if (!mGeoTracker.isEnabled()) {
            askToEnableLocationService();
        }

        // get last known most accurate location :
        Location location = mGeoTracker.getLastKnownLocation();
        if (location != null) {
            handleLocation(location);
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
        // Show the location provider :
        showLocationProvider();
        // request location updates:
        mGeoTracker.requestLocationUpdates();
        // (re-)fill data from GeoDB:
        fillList();

    }

    @Override
    protected void onPause() {
        mGeoTracker.stopLocationUpdates();
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
        else if (id == R.id.action_settings) {
            fillList();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView,view,position,id);
        editGeoPoint(id);
    }

    protected void createGeoPoint() {
        if (mCurrentLocation != null) {
            Intent i = new Intent(this, GeoPointEdit.class);
            i.putExtra("CurrentLatitude", mCurrentLocation.getLatitude());
            i.putExtra("CurrentLongitude", mCurrentLocation.getLongitude());
            i.putExtra("Table", GeoDBConf.GEODB_TABLE_GEOPOINTS);
            startActivity(i);
        } else {
            Toast.makeText(
                    this,
                    "Your current location is not available yet",
                    Toast.LENGTH_LONG
            ).show();
            mGeoTracker.requestLocationUpdates();
        }
    }

    protected void editGeoPoint(long id) {
        Intent i = new Intent(this, GeoPointEdit.class);
        // Put extra info about selected item : key_rowid and id
        i.putExtra(GeoDBConf.COMMON_KEY_ID, id);
        i.putExtra("Table", GeoDBConf.GEODB_TABLE_GEOPOINTS);
        startActivity(i);
    }

    protected void fillList() {

        Cursor cursor = mGeoDBHandler.getAllDataFromTable(GeoDBConf.GEODB_TABLE_GEOPOINTS);

        if (cursor==null) return;

        startManagingCursor(cursor);
        cursor.moveToFirst();

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

    protected void handleLocation(Location location) {

        mCurrentLocation = location;
        mCurrentLocationTV.setText(
                String.valueOf(location.getLatitude()) + ", " +
                        String.valueOf(location.getLongitude()));
    }


    protected void showLocationProvider() {

        String availableProviders = mGeoTracker.getAvailableProviders();
        String provider = mGeoTracker.getProvider();
        if (!provider.isEmpty()) {
            Toast.makeText(
                    getApplicationContext(),
                    "Available providers: " + availableProviders + "\n" + "Provider : " + provider,
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


package com.vfdev.android.geopoints;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PointF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vfdev.android.DB.GeoDBConf;
import com.vfdev.android.DB.GeoDBHandler;
import com.vfdev.android.GeoTracker.GeoTracker;

import java.sql.Timestamp;
import java.util.Date;


public class GeoPointEdit extends Activity {

    private static final String TAG=GeoPointEdit.class.getName();

//    private static final int REMOVE_ID = Menu.FIRST;

    private String mTable;
    private long mRowId;
    private TextView mCurrentLocationTV;
    private LatLng mCurrentLocation;
    private EditText mGPName;
    private EditText mGPDescription;
    private TextView mGPTime;

    private GoogleMap mMap;
    private GeoDBHandler mGeoDBHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geo_point_edit);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        mCurrentLocationTV = (TextView) findViewById(R.id.gpLocation);
        mGPTime = (TextView) findViewById(R.id.gpTime);
        mGPName = (EditText) findViewById(R.id.gpName);
        mGPDescription = (EditText) findViewById(R.id.gpDescription);

        mGeoDBHandler = GeoDBHandler.getInstance();

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mMap = mapFragment.getMap();
        if (mMap == null) {
            Toast.makeText(
                    this,
                    "Google map is not available. Too bad :(",
                    Toast.LENGTH_LONG
            ).show();
        }


        Bundle extras = getIntent().getExtras();
        double lat = 0.0, lng = 0.0;
        if (extras != null) {
            // Test firstly if row id is passed from GeoDB:
            mTable = extras.getString("Table", "");
            mRowId = extras.getLong(GeoDBConf.COMMON_KEY_ID, -1);
            if (mRowId > -1 && !mTable.isEmpty()) {
                // Edit geopoint :
                populateFields();
            } else {
                // No id from GeoDB is passed -> add new geopoint
                lat = extras.getDouble("CurrentLatitude", -12345);
                lng = extras.getDouble("CurrentLongitude", -12345);
                if (lat > -12345 && lng > -12345) {
                    setCurrentLocation(lat, lng);
                    if (mMap != null) {
                        putMarkerOnMap("You are here", "", mCurrentLocation);
                    }
                }
                // set current date/time :
                Time now = new Time();
                now.setToNow();
                setCurrentDateTime(now);
            }
        }

        // Avoid on-screen keyboard auto popping up
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }


    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.geo_point_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            saveModifications();
            exitActivity();
            return true;
        } else if (id == R.id.action_remove) {
            removeGeoPoint();
            exitActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    /** Save instance state */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);

        // Store temporary modifications in the outState
//        outState.putSerializable(NotesDbAdapter.KEY_ROWID, mRowId);
//        outState.putString(NotesDbAdapter.KEY_TITLE, mTitleText.getText().toString());
//        outState.putString(NotesDbAdapter.KEY_BODY, mBodyText.getText().toString());
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        Log.i(TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedState);
        // Restore row id from saved state
        if (savedState != null) {
//            mRowId = (Long) savedState.getSerializable(NotesDbAdapter.KEY_ROWID);
//            mTitleText.setText((String) savedState.getString(NotesDbAdapter.KEY_TITLE));
//            mBodyText.setText((String) savedState.getString(NotesDbAdapter.KEY_BODY));
            return;
        }
//        mRowId = null;
    }

    /** Handles Back button
     * Save modification */
    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed()");
        saveModifications();
        super.onBackPressed();
    }

    protected void exitActivity() {
        Log.i(TAG, "exitActivity()");
        navigateUpTo(new Intent(this, GeoPointList.class));
    }


    private void populateFields() {
        if (mRowId > -1) {
            Cursor data = mGeoDBHandler.getDataOnRowIdFromTable(mRowId, mTable);
            if (data == null) return;

            startManagingCursor(data);
            data.moveToFirst();

            Log.i(TAG,"count : " + String.valueOf(data.getCount()));

            mGPName.setText(data.getString(
                    data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_NAME)
                    )
            );

            mGPDescription.setText(data.getString(
                            data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_DESC)
                    )
            );

            double lat = data.getFloat(data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LAT));
            double lng = data.getFloat(data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LON));
            setCurrentLocation(lat, lng);
            putMarkerOnMap(
                    mGPName.getText().toString(),
                    mGPDescription.getText().toString(),
                    mCurrentLocation
            );

            mGPTime.setText(data.getString(data.getColumnIndexOrThrow(GeoDBConf.GEOPOINTS_KEY_TIME)));

        }
    }

    private void saveModifications() {

        ContentValues data = new ContentValues();
        data.put(GeoDBConf.COMMON_KEY_NAME, mGPName.getText().toString());
        data.put(GeoDBConf.COMMON_KEY_DESC, mGPDescription.getText().toString());
        data.put(GeoDBConf.COMMON_KEY_LAT, mCurrentLocation.latitude);
        data.put(GeoDBConf.COMMON_KEY_LON, mCurrentLocation.longitude);
        data.put(GeoDBConf.GEOPOINTS_KEY_TIME, mGPTime.getText().toString());

        if (mRowId == -1) {
            long id = mGeoDBHandler.createDataInTable(mTable, data);
            if (id > 0) {
                mRowId = id;
            } else {
                Log.e(TAG, "Failed to insert data in the table");
            }
        } else {
            mGeoDBHandler.updateDataInTable(mTable, mRowId, data);
        }
    }

    protected void removeGeoPoint() {
        Log.i(TAG, "onRemoveClicked()");
        // remove current item from DB
        if (mRowId != -1) {
            mGeoDBHandler.deleteDataInTable(mTable, mRowId);
        }
    }



    protected void setCurrentLocation(double lat, double lng) {
        mCurrentLocation = new LatLng(lat, lng);
        mCurrentLocationTV.setText(String.valueOf(lat) + ", " + String.valueOf(lng));
    }

    protected void setCurrentDateTime(Time time) {
        mGPTime.setText(time.format("%Y/%m/%d - %H:%M:%S"));
    }

    // Local methods:
    private void putMarkerOnMap(String name, String description, LatLng ll) {

        // Add a marker of that location to the map
        if (mMap != null) {
            // clear all previous markers:
            mMap.clear();
            // add new marker:
            Marker marker = mMap.addMarker(new MarkerOptions().position(ll));
            marker.setSnippet(description);
            marker.setTitle(name);
            mMap.moveCamera(CameraUpdateFactory.
                            newCameraPosition(CameraPosition.fromLatLngZoom(
                                            ll,
                                    (float) 16.0)
                            )
            );
        }
    }



}

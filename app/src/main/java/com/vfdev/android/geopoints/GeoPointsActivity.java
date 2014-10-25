package com.vfdev.android.geopoints;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.vfdev.android.DB.GeoDBConf;
import com.vfdev.android.DB.GeoDBHandler;
import com.vfdev.android.GeoTracker.GeoTracker;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.Vector;


/*
    Main application activity.
*/

public class GeoPointsActivity extends Activity implements ActionBar.TabListener,
        GeoTracker.OnLocationUpdateListener
{

    private static final String TAG=GeoPointsActivity.class.getName();
    private static final int POINTS_PAGE = 0;
    private static final int MAP_PAGE = 1;

    // UI
    private ViewPager mViewPager=null;
    private TwoPagesAdapter mPagerAdapter=null;
    private ActionBar mActionBar=null;
    private ListFragment mGeoPointListFragment=null;
    private MapFragment mGeoPointsMapFragment=null;
    private GoogleMap mMap=null;

    // DB & Geo
    private GeoDBHandler mGeoDBHandler = null;
    private GeoTracker mGeoTracker = null;
    private Location mCurrentLocation = null;

    // Local data
    protected class GeoPoint {
        public String name;
        public String description;
        public LatLng latLng;
        GeoPoint(String name, String description, LatLng latLng) {
            this.name = name;
            this.description = description;
            this.latLng = latLng;
        }
    }
    private Vector<GeoPoint> mGeoPoints;


    // ------ Activity methods -------

    public GeoPointsActivity() {
        super();
        Log.i(TAG, "GeoPointsActivity");
        mGeoPointsMapFragment = new MapFragment() {
            @Override
            public void onViewCreated(View view, Bundle savedInstanceState) {
                super.onViewCreated(view, savedInstanceState);
                Log.i(TAG, "MapFragement : onViewCreated");
                setupMap();
            }
        };
        mGeoPointListFragment = new ListFragment() {
            @Override
            public void onListItemClick(ListView listView, View view, int position, long id) {
                super.onListItemClick(listView, view, position, id);
                Log.i(TAG, "ListFragment : onListItemClick");
                editGeoPoint(id);
            }
        };
        mGeoPoints = new Vector<GeoPoint>();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate()");

//        Log.w(TAG, "super != null : " + String.valueOf(this != null));
//        Log.w(TAG, "savedInstanceState != null : " + String.valueOf(savedInstanceState != null));
        // Prevent from a crash if screen orientation changes
        // Need a proper handling
        super.onCreate(null);

        setContentView(R.layout.activity_geo_points);

        setupPagerUI();

        setupDbAndGeo();

        // get last known most accurate location :
        Location location = mGeoTracker.getLastKnownLocation();
        if (location != null) {
            handleLocation(location);
        }

    }
    @Override
    public void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();

        // Show the location provider :
        showLocationProvider();
        // request location updates:
        mGeoTracker.requestLocationUpdates();
        // (re-)fill data from GeoDB:
        fillList();
//        // restore map page
//        if (mViewPager.getCurrentItem() == MAP_PAGE) {
//            setupMap();
//        }

    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause()");
        mGeoTracker.stopLocationUpdates();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");
        super.onDestroy();
    }

    /** Save instance state */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);
    }


    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        Log.i(TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedState);

        // get last known most accurate location :
        Location location = mGeoTracker.getCurrentLocation();
        if (location != null) {
            handleLocation(location);
        }
    }

    /** Handles Back button
     * Go to Points Tab if on the Map page */
    @Override
    public void onBackPressed() {
        Log.i(TAG, "onBackPressed()");

        if (mViewPager.getCurrentItem() == MAP_PAGE) {
            mViewPager.setCurrentItem(0);
        } else {
            super.onBackPressed();
        }
    }

    // ----- OnLocationUpdateListener implementation ------
    @Override
    public void onLocationUpdate(Location location) {
        if (location != null) {
            handleLocation(location);
        }
    }

    // ------ Tabs handling -------
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        // setup map
        if (tab.getPosition() == MAP_PAGE) {
            setupMap();
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // setup map
        if (tab.getPosition() == MAP_PAGE) {
            setupMap();
        }
    }

    // ------ Menu handling -------
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
//        else if (id == R.id.action_settings) {
//            fillList();
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }



    // ------ other helping methods -------
    protected void initTabs() {

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            mActionBar.addTab(
                    mActionBar.newTab()
                            .setText(mPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }

    }

    protected void setupPagerUI()
    {
        Log.i(TAG, "setupPagerUI()");
        // Set up the action bar.
        mActionBar = getActionBar();
        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mPagerAdapter = new TwoPagesAdapter(getFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                Log.i(TAG,"onPageSelected : " + String.valueOf(position));
                mActionBar.setSelectedNavigationItem(position);
            }
        });

        initTabs();

    }


    protected void setupDbAndGeo()
    {
        Log.i(TAG, "setupDbAndGeo");
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
        }
        // connect to geotracker location update as listener
        mGeoTracker.setOnLocationUpdateListener(this);
        if (!mGeoTracker.isEnabled()) {
            askToEnableLocationService();
        }

        // create app external storage directory:
        File appDir = new File(Environment.getExternalStorageDirectory().toString()+File.separator+
                getResources().getString(R.string.title_activity_geo_point_list)
        );
        if (!appDir.exists()) {
            if (!appDir.mkdirs()) {
                Log.w(TAG,"setupDbAndGeo : failed to create dirs");
            }
        }

    }

    protected void askToEnableLocationService() {

        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);

    }

    protected void handleLocation(Location location) {
        mCurrentLocation = location;
    }


    protected void setupMap() {
        mMap = mGeoPointsMapFragment.getMap();
        if (mMap != null) {
            // clear previous points:
            mMap.clear();
            putPointsToMap();
            if (mCurrentLocation != null) {
                putMarkerOnMap("You are here",
                        "Here is your current location",
                        new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
            }
        }
    }

    protected void createGeoPoint() {
        if (mCurrentLocation != null) {
            Log.i(TAG, "createGeoPoint()");
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


        String[] fieldNames = {
                GeoDBConf.COMMON_KEY_ID,
                GeoDBConf.COMMON_KEY_NAME,
                GeoDBConf.COMMON_KEY_DESC,
                GeoDBConf.COMMON_KEY_LAT,
                GeoDBConf.COMMON_KEY_LON
        };

        Cursor cursor = mGeoDBHandler.getAllDataFromTable(GeoDBConf.GEODB_TABLE_GEOPOINTS, fieldNames);

        if (cursor == null) return;

        startManagingCursor(cursor);
        cursor.moveToFirst();

        // Create an array to specify the fields we want to display in the list
        String[] from = {
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


        // set points to the list fragment
        mGeoPointListFragment.setListAdapter(points);

        // set points to memory
        mGeoPoints.clear();

        if (cursor.getCount() == 0) return;

        cursor.moveToFirst();
        do {
            String gpName = cursor.getString(
                    cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_NAME));

            String gpDescription = cursor.getString(
                    cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_DESC));

            double lat = cursor.getFloat(cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LAT));
            double lng = cursor.getFloat(cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LON));

            mGeoPoints.add(new GeoPoint(gpName, gpDescription, new LatLng(lat, lng)));

        } while (cursor.moveToNext());

    }


    protected void putPointsToMap() {


        if (mMap == null)
            return;

        for (GeoPoint gp : mGeoPoints) {

            // add new marker:
            Marker marker = mMap.addMarker(new MarkerOptions().position(gp.latLng));
            marker.setAlpha(0.7f);
            marker.setTitle(gp.name);
            marker.setSnippet(gp.description);
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        }

        /*
        String[] fieldNames = {
                GeoDBConf.COMMON_KEY_NAME,
                GeoDBConf.COMMON_KEY_DESC,
                GeoDBConf.COMMON_KEY_LAT,
                GeoDBConf.COMMON_KEY_LON
        };

        Cursor data = mGeoDBHandler.getAllDataFromTable(GeoDBConf.GEODB_TABLE_GEOPOINTS, fieldNames);
        if (data==null) return;
        startManagingCursor(data);

        if (!data.moveToFirst()) {
            Log.i(TAG, "setPointsToMap : no data");
            return;
        }

        if (mMap == null)
            return;

        do {
            String gpName = data.getString(
                    data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_NAME));


            String gpDescription = data.getString(
                    data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_DESC));

            double lat = data.getFloat(data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LAT));
            double lng = data.getFloat(data.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LON));
            // add new marker:
            Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng)));
            marker.setAlpha(0.7f);
            marker.setSnippet(gpDescription);
            marker.setTitle(gpName);
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        } while (data.moveToNext());
        */

    }

    private void putMarkerOnMap(String name, String description, LatLng ll) {

        // Add a marker of that location to the map
        if (mMap != null) {
            // add new marker:
            Marker marker = mMap.addMarker(new MarkerOptions().position(ll));
            marker.setAlpha(0.7f);
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


    public void onShowOnMap(View view) {
        mViewPager.setCurrentItem(MAP_PAGE);
        // Zoom on selected point
        ListView listView = mGeoPointListFragment.getListView();
        int position = listView.getPositionForView(view);
//        Toast.makeText(getApplicationContext(), "Position : " + String.valueOf(position), Toast.LENGTH_SHORT).show();

        if (mMap != null) {
            mMap.moveCamera(CameraUpdateFactory.
                            newCameraPosition(CameraPosition.fromLatLngZoom(
                                            mGeoPoints.elementAt(position).latLng,
                                            (float) 16.0)
                            )
            );
        }

    }



    /*
     *  Subclass FragmentPagerAdapter to display 2 static fragments
     */
    public class TwoPagesAdapter extends FragmentPagerAdapter {

        public TwoPagesAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case POINTS_PAGE:
                    return mGeoPointListFragment;
                case MAP_PAGE:
                    return mGeoPointsMapFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case POINTS_PAGE:
                    return getResources().getString(R.string.list_tab_name).toUpperCase(l);
                case MAP_PAGE:
                    return getResources().getString(R.string.map_tab_name).toUpperCase(l);
            }
            return null;
        }

    }


}

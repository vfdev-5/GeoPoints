package com.vfdev.android.geopoints;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ListFragment;
import android.content.DialogInterface;
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
        GeoTracker.OnLocationUpdateListener,
        GoogleMap.OnInfoWindowClickListener,
        GoogleMap.OnMarkerClickListener
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
    private int mMapType = GoogleMap.MAP_TYPE_NORMAL;

    // DB & Geo
    private GeoDBHandler mGeoDBHandler = null;
    private GeoTracker mGeoTracker = null;
    private Location mCurrentLocation = null;

    // Map markers
    private Marker mCurrentLocationMarker=null;

    // Local data
    protected class GeoPoint {
        public String name;
        public String description;
        public LatLng latLng;
        public long id;
        GeoPoint(long id, String name, String description, LatLng latLng) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.latLng = latLng;
        }
        GeoPoint(Marker marker) {
            this.id=-1;
            this.name = marker.getTitle();
            this.description = marker.getSnippet();
            this.latLng = marker.getPosition();
        }

        @Override
        public boolean equals(Object pointObj){
            if (!(pointObj instanceof GeoPoint)) return false;
            GeoPoint point = (GeoPoint) pointObj;

            if (point.id < 0 || this.id < 0) {
                return this.latLng.equals(point.latLng) &&
                        this.name.equals(point.name) &&
                        this.description.equals(point.description);
            } else {
                return this.id == point.id;
            }
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
        // refresh Map_page (e.g. if item is deleted passing directly from Map_page)
        if (mViewPager.getCurrentItem() == MAP_PAGE) {
            setupMap();
        }

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

        outState.putInt("MapType", mMapType);

    }


    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        Log.i(TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedState);

        mMapType = savedState.getInt("MapType");

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

    // ----- GoogleMap.OnMarkerClickListener implementation ------
    @Override
    public boolean onMarkerClick(Marker marker) {
        if (marker.getTitle().isEmpty() && marker.getSnippet().isEmpty()) {
            onInfoWindowClick(marker);
        }
        return false;
    }

    // ----- GoogleMap.OnInfoWindowClickListener implementation ------
    @Override
    public void onInfoWindowClick(Marker marker) {
        // Open GeoPointEdit activity
        int position = mGeoPoints.indexOf(new GeoPoint(marker));
        if (position > -1) {
            editGeoPoint(mGeoPoints.elementAt(position).id);
        }
    }

    // ------ Tabs handling -------
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
        // setup map
//        if (tab.getPosition() == MAP_PAGE) {
//            setupMap();
//        }
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
        else if (id == R.id.action_map_select_type) {
            startMapSettingsDialog();
            return true;
        }

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
        if (mCurrentLocationMarker != null && mMap != null) {
            mCurrentLocationMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
            zoomOnPoint(mMap, mCurrentLocationMarker.getPosition());
        }
    }


    protected void setupMap() {
        // setup map and its configuration
        if (mMap == null) {
            mMap = mGeoPointsMapFragment.getMap();
            if (mMap != null) {
                mMap.setOnInfoWindowClickListener(this);
                mMap.setOnMarkerClickListener(this);
            }
        }
        // set points
        if (mMap != null) {
            // TODO: Think about !!!
            Log.w(TAG,"Redraw points");
            // clear previous points:
            mMap.clear();
            mMap.setMapType(mMapType);
            putPointsToMap();
            if (mCurrentLocation != null) {
                mCurrentLocationMarker = createCurrentPositionMarker(mMap,
                        "You are here",
                        "Here is your current location",
                        new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
                zoomOnPoint(mMap, mCurrentLocationMarker.getPosition());
//                mCurrentLocationMarker = putMarkerOnMap("You are here",
//                        "Here is your current location",
//                        new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));
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
        // Put extra info about selected item : id and table name
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
            long id = cursor.getLong(
                    cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_ID));

            String gpName = cursor.getString(
                    cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_NAME));

            String gpDescription = cursor.getString(
                    cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_DESC));

            double lat = cursor.getFloat(cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LAT));
            double lng = cursor.getFloat(cursor.getColumnIndexOrThrow(GeoDBConf.COMMON_KEY_LON));

            mGeoPoints.add(new GeoPoint(id, gpName, gpDescription, new LatLng(lat, lng)));

        } while (cursor.moveToNext());
    }

    protected void putPointsToMap() {
        if (mMap == null)
            return;
        for (GeoPoint gp : mGeoPoints) {
            // add new marker:
            Marker marker = createMarker(mMap, gp.name, gp.description, gp.latLng);
            marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            marker.setFlat(true);
        }
    }

    protected Marker createMarker(GoogleMap map, String name, String description, LatLng ll) {
        return map.addMarker(new MarkerOptions()
                .position(ll)
                .alpha(0.7f)
                .title(name)
                .snippet(description)
        );
    }


    protected Marker createCurrentPositionMarker(GoogleMap map, String name, String description, LatLng ll) {

        Marker output = createMarker(map, name, description, ll);
        output.setAlpha(1.0f);
        output.setIcon(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_myplaces));

        return output;
    }

    protected Marker putMarkerOnMap(String name, String description, LatLng ll) {

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
            return marker;
        }
        return null;
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
            zoomOnPoint(mMap, mGeoPoints.elementAt(position).latLng);
        }

    }

    protected void zoomOnPoint(GoogleMap map, LatLng latLng) {
            map.moveCamera(CameraUpdateFactory.
                            newCameraPosition(CameraPosition.fromLatLngZoom(
                                            latLng,
                                            (float) 16.0)
                            )
            );
    }

    private void startMapSettingsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.map_select_type_dialog);
        builder.setItems(R.array.maptypes, new DialogInterface.OnClickListener(){

            private static final int TYPE_NORMAL = 0;
            private static final int TYPE_HYBRID = 1;
            private static final int TYPE_SATELLITE = 2;
            private static final int TYPE_TERRAIN = 3;

            @Override
            public void onClick(DialogInterface dialog, int which) {

                switch(which){
                    case TYPE_NORMAL:
                        mMapType=GoogleMap.MAP_TYPE_NORMAL;
                        break;
                    case TYPE_HYBRID:
                        mMapType=GoogleMap.MAP_TYPE_HYBRID;
                        break;
                    case TYPE_SATELLITE:
                        mMapType=GoogleMap.MAP_TYPE_SATELLITE;
                        break;
                    case TYPE_TERRAIN:
                        mMapType=GoogleMap.MAP_TYPE_TERRAIN;
                        break;
                }
                if (mMap != null) {
                    mMap.setMapType(mMapType);
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
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

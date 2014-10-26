package com.vfdev.android.geopoints;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.DialogPreference;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;


public class GeoPointEdit extends Activity {

    private static final String TAG=GeoPointEdit.class.getName();
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private String mTable;
    private long mRowId = -1;
    private TextView mCurrentLocationTV;
    private LatLng mCurrentLocation;
    private EditText mGPName;
    private EditText mGPDescription;
    private TextView mGPTime;
    private ImageView mGPImageView;
    private Bitmap mGPImageIcon = null;
    private String mGPImagePath = "";

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
        mGPImageView = (ImageView) findViewById(R.id.gpImage);
        mGPImageView.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_camera));

        mGeoDBHandler = GeoDBHandler.getInstance();

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
                addNewPoint(lat, lng);
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

            // TODO: Alert ask user
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.confirmRemoval);
            builder.setMessage(R.string.confirmRemovalMsg);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    removeGeoPoint();
                    exitActivity();
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog,int which) {
                    dialog.cancel();
                    exitActivity();
                }
            });

            AlertDialog dialog = builder.create();
            dialog.show();

//            removeGeoPoint();
//            exitActivity();
        }
        return super.onOptionsItemSelected(item);
    }

    /** Save instance state */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.i(TAG, "onSaveInstanceState()");
        super.onSaveInstanceState(outState);

        // Store temporary modifications in the outState
        outState.putString("Table", mTable);
        outState.putLong(GeoDBConf.COMMON_KEY_ID, mRowId);
        outState.putSerializable(GeoDBConf.COMMON_KEY_NAME, mGPName.getText().toString());
        outState.putSerializable(GeoDBConf.COMMON_KEY_DESC, mGPDescription.getText().toString());
        outState.putDouble(GeoDBConf.COMMON_KEY_LAT, mCurrentLocation.latitude);
        outState.putDouble(GeoDBConf.COMMON_KEY_LON, mCurrentLocation.longitude);
        outState.putSerializable(GeoDBConf.GEOPOINTS_KEY_TIME, mGPTime.getText().toString());
        if (mGPImageIcon != null) {
            outState.putByteArray(GeoDBConf.GEOPOINTS_IMAGE_ICON, getBitmapAsByteArray(mGPImageIcon));
        }
        outState.putString(GeoDBConf.GEOPOINTS_IMAGE_PATH, mGPImagePath);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedState) {
        Log.i(TAG, "onRestoreInstanceState()");
        super.onRestoreInstanceState(savedState);
        // Restore row id from saved state
        if (savedState != null) {
            mTable = savedState.getString("Table");
            mRowId = savedState.getLong(GeoDBConf.COMMON_KEY_ID);
            mGPName.setText((String) savedState.getSerializable(GeoDBConf.COMMON_KEY_NAME));
            mGPDescription.setText((String) savedState.getSerializable(GeoDBConf.COMMON_KEY_DESC));
            double lat = savedState.getDouble(GeoDBConf.COMMON_KEY_LAT);
            double lng = savedState.getDouble(GeoDBConf.COMMON_KEY_LON);
            setCurrentLocation(lat, lng);
            mGPTime.setText((String) savedState.getSerializable(GeoDBConf.GEOPOINTS_KEY_TIME));

            byte [] imageIconData = savedState.getByteArray(GeoDBConf.GEOPOINTS_IMAGE_ICON);
            if (imageIconData != null && imageIconData.length > 0) {
                mGPImageIcon = BitmapFactory.decodeByteArray(imageIconData, 0, imageIconData.length);
                mGPImageView.setImageBitmap(mGPImageIcon);
            }
            mGPImagePath = savedState.getString(GeoDBConf.GEOPOINTS_IMAGE_PATH);

            return;
        }
        mRowId = -1;
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


            if (data.getCount() < 1) {
                Log.w(TAG, "count is zero or negative -> delete table entry");
                removeGeoPoint();
                return;
            }

            startManagingCursor(data);
            // !!! CAN CRASH HERE
            data.moveToFirst();
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

            mGPTime.setText(data.getString(data.getColumnIndexOrThrow(GeoDBConf.GEOPOINTS_KEY_TIME)));

            // restore image bitmap :
            byte [] imageIconData = data.getBlob(data.getColumnIndexOrThrow(GeoDBConf.GEOPOINTS_IMAGE_ICON));
            if (imageIconData != null && imageIconData.length > 0) {
                mGPImageIcon = BitmapFactory.decodeByteArray(imageIconData, 0, imageIconData.length);
                mGPImageView.setImageBitmap(mGPImageIcon);
            } else {
                mGPImageView.setImageDrawable(getResources().getDrawable(android.R.drawable.ic_menu_camera));
            }
            mGPImagePath = data.getString(data.getColumnIndexOrThrow(GeoDBConf.GEOPOINTS_IMAGE_PATH));

        }
    }

    private void addNewPoint(double lat, double lng) {

        if (lat > -12345 && lng > -12345) {
            setCurrentLocation(lat, lng);
//                    if (mMap != null) {
//                        putMarkerOnMap("You are here", "", mCurrentLocation);
//                    }
        }
        // set current date/time :
        Time now = new Time();
        now.setToNow();
        setCurrentDateTime(now);
    }

    private void saveModifications() {

        ContentValues data = new ContentValues();
        data.put(GeoDBConf.COMMON_KEY_NAME, mGPName.getText().toString());
        data.put(GeoDBConf.COMMON_KEY_DESC, mGPDescription.getText().toString());
        data.put(GeoDBConf.COMMON_KEY_LAT, mCurrentLocation.latitude);
        data.put(GeoDBConf.COMMON_KEY_LON, mCurrentLocation.longitude);
        data.put(GeoDBConf.GEOPOINTS_KEY_TIME, mGPTime.getText().toString());
        data.put(GeoDBConf.GEOPOINTS_IMAGE_ICON, getBitmapAsByteArray(mGPImageIcon));
        data.put(GeoDBConf.GEOPOINTS_IMAGE_PATH, mGPImagePath);

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
            // remove stored file:
            removeFile(mGPImagePath);
        }
    }



    protected void setCurrentLocation(double lat, double lng) {
        mCurrentLocation = new LatLng(lat, lng);
        mCurrentLocationTV.setText(String.valueOf(lat) + ", " + String.valueOf(lng));
    }

    protected void setCurrentDateTime(Time time) {
        mGPTime.setText(time.format("%Y/%m/%d - %H:%M:%S"));
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + ".jpg";
        String appDir = Environment.getExternalStorageDirectory().toString()+File.separator+
                getResources().getString(R.string.title_activity_geo_point_list);
        File image = new File(appDir,imageFileName);
        if(!image.createNewFile()){
            return null;
        }
        // Save a file: path for use with ACTION_VIEW intents
        mGPImagePath = image.getAbsolutePath();
        return image;
    }

    public void onTakePictureEvent(View v) {

        if (mGPImageIcon == null) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE_SECURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "onTakePictureEvent : Failed to create an image file");
                    return;
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }

        } else {
            // Display full image
            if (!mGPImagePath.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(new File(mGPImagePath)), "image/*");
                startActivity(intent);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            // Get the dimensions of the View
//            int targetW = mGPImageView.getWidth() - 10;
//            int targetH = mGPImageView.getHeight() - 10;
            int targetW = 250;
            int targetH = 250;

            // Get the dimensions of the bitmap
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            bmOptions.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(mGPImagePath, bmOptions);
            int photoW = bmOptions.outWidth;
            int photoH = bmOptions.outHeight;

            // Determine how much to scale down the image
            int scaleFactor = (int) Math.ceil(Math.max(photoW*1.0/targetW, photoH*1.0/targetH)) + 1;

            // Decode the image file into a Bitmap sized to fill the View
            bmOptions.inJustDecodeBounds = false;
            bmOptions.inSampleSize = scaleFactor;
            bmOptions.inPurgeable = true;

            mGPImageIcon = BitmapFactory.decodeFile(mGPImagePath, bmOptions);
            mGPImageView.setImageBitmap(mGPImageIcon);

        } else {
            // delete created temp file :
            removeFile(mGPImagePath);
        }
    }


    protected void removeFile(String filename) {
        File fileToDel = new File(filename);
        if (!fileToDel.exists() || !fileToDel.delete()) {
            Log.w(TAG, "onActivityResult : failed to remove empty temp image file");
        }
    }

    protected static byte[] getBitmapAsByteArray(Bitmap bitmap) {
        if (bitmap == null)
            return "".getBytes();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, outputStream);
        return outputStream.toByteArray();
    }





}

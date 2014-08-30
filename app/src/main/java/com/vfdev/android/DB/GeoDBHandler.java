package com.vfdev.android.DB;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/** Singleton */
public class GeoDBHandler extends SQLiteOpenHelper {

    private static final String TAG=GeoDBHandler.class.getName();

    private static GeoDBHandler mInstance=null;

    static public GeoDBHandler getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new GeoDBHandler(context);
        }
        return mInstance;
    }

    private GeoDBHandler(Context context) {
        super(context, GeoDBConf.GEODB_NAME, null, GeoDBConf.GEODB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate db");
        db.execSQL(GeoDBConf.GEODB_CREATE_GEOPOINTS);
        db.execSQL(GeoDBConf.GEODB_CREATE_GEOREMINDERS);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade db from " + String.valueOf(oldVersion) + " to " + String.valueOf(newVersion));
        db.execSQL(GeoDBConf.GEODB_CLEAR);
        onCreate(db);
    }

    public Cursor getAllDataFromTable(String tableName) {

        // Field names can be null -> all fields are given, however it is discouraged
        // http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
        String[] fieldNames=null;
        return getReadableDatabase().query(tableName,
                fieldNames,
                null,
                null,
                null,
                null,
                null);
    }


}

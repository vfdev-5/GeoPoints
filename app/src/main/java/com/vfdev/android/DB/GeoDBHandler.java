package com.vfdev.android.DB;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

/** Singleton */
public class GeoDBHandler {

    private static final String TAG = GeoDBHandler.class.getName();
    private static GeoDBHandler mInstance = null;
    private GeoDBHandlerPrivate mHandlerPrivate;

    static public GeoDBHandler getInstance() {
        if (mInstance == null) {
            mInstance = new GeoDBHandler();
        }
        return mInstance;
    }

    public boolean init(Context context) {
        mHandlerPrivate = new GeoDBHandlerPrivate(context);
        return true;
    }

    private GeoDBHandler() {

    }

    private class GeoDBHandlerPrivate extends SQLiteOpenHelper {


        public GeoDBHandlerPrivate(Context context) {
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
            db.execSQL(GeoDBConf.GEODB_GEOPOINTS);
            db.execSQL(GeoDBConf.GEODB_GEOREMINDERS);
            onCreate(db);
        }

    };


    public Cursor getAllDataFromTable(String tableName) {

        if (mHandlerPrivate == null) {
            return null;
        }
        // Field names can be null -> all fields are given, however it is discouraged
        // http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
        String[] fieldNames = null;
        return mHandlerPrivate.getReadableDatabase().query(tableName,
                fieldNames,
                null,
                null,
                null,
                null,
                null);
    }

    public Cursor getAllDataFromTable(String tableName, String[] fieldNames) {

        if (mHandlerPrivate == null) {
            return null;
        }
        // Field names can be null -> all fields are given, however it is discouraged
        // http://developer.android.com/reference/android/database/sqlite/SQLiteDatabase.html
        return mHandlerPrivate.getReadableDatabase().query(tableName,
                fieldNames,
                null,
                null,
                null,
                null,
                null);
    }


    public Cursor getDataOnRowIdFromTable(long id, String tableName) {
        if (mHandlerPrivate == null) {
            return null;
        }
        return mHandlerPrivate.getReadableDatabase().query(
                true,
                tableName,
                null, // field names
                GeoDBConf.COMMON_KEY_ID + "=" + String.valueOf(id),
                null,
                null,
                null,
                null,
                null
                );

    }


    public long createDataInTable(String tableName, ContentValues data) {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        return db.insert(tableName, null, data);
    }


    public boolean updateDataInTable(String tableName, long id, ContentValues data) {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        return db.update(
                tableName,
                data,
                GeoDBConf.COMMON_KEY_ID + "=" + String.valueOf(id),
                null) > 0;

    }

    public boolean deleteDataInTable(String tableName, long id)
    {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        return db.delete(
                tableName,
                GeoDBConf.COMMON_KEY_ID + "=" + String.valueOf(id),
                null) > 0;
    }


    public boolean deleteDataInTable(String tableName, long[] ids)
    {
        SQLiteDatabase db = mHandlerPrivate.getWritableDatabase();
        String values = "";
        for (int i=0; i<ids.length-1; i++) {
            long id = ids[i];
            values += String.valueOf(id) + ", ";
        }
        values += String.valueOf(ids[ids.length-1]);
        return db.delete(
                tableName,
                GeoDBConf.COMMON_KEY_ID + " in (" + values + ")",
                null) > 0;
    }



}
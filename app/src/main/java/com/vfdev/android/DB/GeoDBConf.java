package com.vfdev.android.DB;

/**
 * Created by vfomin on 29/08/2014.
 */
public class GeoDBConf {
    /** Geo Database info : name, version, tables, field names etc */
    public static final String GEODB_NAME="GeoDB";
    public static final int GEODB_VERSION=2;
    public static final String GEODB_TABLE_GEOPOINTS="GeoPoints";
    public static final String GEODB_TABLE_GEOREMINDERS="GeoReminders";

    public static final String COMMON_KEY_ID="_id";
    public static final String COMMON_KEY_NAME="gpName";
    public static final String COMMON_KEY_DESC="gpDesc";
    public static final String COMMON_KEY_LAT="gpLat";
    public static final String COMMON_KEY_LON="gpLon";

    /** GEODB_TABLE_GEOPOINTS field names : */
    public static final String GEOPOINTS_KEY_TIME="gpTime";

    /** GEODB_TABLE_GEOREMINDERS field names : */
    public static final String GEOREMINDERS_KEY_RADIUS="grRadius";
    public static final String GEOREMINDERS_KEY_ACTION="grAction";


    /** Commands to create tables */
    public static final String GEODB_CREATE_GEOPOINTS=
            "CREATE TABLE " + GEODB_TABLE_GEOPOINTS +
                    "(" + COMMON_KEY_ID + " integer primary key autoincrement, " +
                    COMMON_KEY_NAME + " text not null, " +
                    COMMON_KEY_DESC + " text not null, " +
                    COMMON_KEY_LAT + " float not null, " +
                    COMMON_KEY_LON + " float not null, " +
                    GEOPOINTS_KEY_TIME + " text not null" +
                    ");";

    public static final String GEODB_CREATE_GEOREMINDERS=
            "CREATE TABLE " + GEODB_TABLE_GEOREMINDERS +
                    "(" + COMMON_KEY_ID + " integer primary key autoincrement, " +
                    COMMON_KEY_NAME + " text not null, " +
                    COMMON_KEY_DESC + " text not null, " +
                    COMMON_KEY_LAT + " float not null, " +
                    COMMON_KEY_LON + " float not null, " +
                    GEOREMINDERS_KEY_RADIUS + " float not null, " +
                    GEOREMINDERS_KEY_ACTION + " text not null" +
                    ");";

    /** Commands to drop off all tables */
    public static final String GEODB_GEOPOINTS=
            "DROP TABLE IF EXISTS " +
                    GEODB_TABLE_GEOPOINTS + ";";

    public static final String GEODB_GEOREMINDERS=
            "DROP TABLE IF EXISTS " +
                    GEODB_TABLE_GEOREMINDERS + ";";
}

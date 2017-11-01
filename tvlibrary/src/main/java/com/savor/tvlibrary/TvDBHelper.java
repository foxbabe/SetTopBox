package com.savor.tvlibrary;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteTransactionListener;
import android.util.Log;

import java.util.ArrayList;

/**
 * Created by zhang.haiqiang on 2017/10/31.
 */

public class TvDBHelper extends SQLiteOpenHelper {

    private static final String TAG = "TvDBHelper";

    /**
     * 数据库名称
     */
    public static final String DB_NAME = "tv.db";
    private static final int DB_VERSION = 1;

    private static TvDBHelper dbHelper = null;
    private Context mContext;

    public static class FieldName {

        public static final String ID = "_id";
        public static final String CHANNEL_ID = "channel_id";
        public static final String CHANNEL_NAME = "channel_name";
        public static final String CHANNEL_NUM = "channel_num";
    }

    public static class TableName {
        public static final String ATV_CHANNEL = "atv_channel_table";
    }


    private TvDBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);

        mContext = context;
    }


    public synchronized static TvDBHelper getInstance(Context context) {
        if (dbHelper == null) {
            dbHelper = new TvDBHelper(context);
        }
        return dbHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        // 创建电视频道表
        createTable_atvChannel(db);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }


    private void createTable_atvChannel(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + TableName.ATV_CHANNEL
                + " ("
                + FieldName.ID + " INTEGER PRIMARY KEY, "
                + FieldName.CHANNEL_ID + " TEXT, "
                + FieldName.CHANNEL_NUM + " INTEGER, "
                + FieldName.CHANNEL_NAME + " TEXT "
                + ");";
        db.execSQL(DATABASE_CREATE);
    }

    void cleanAtcChannels() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TableName.ATV_CHANNEL, null, null);
        db.close();
    }

    void insertChannels() {

    }

    void setAtvChannels(ArrayList<AtvChannel> newChannels) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransactionWithListener(new SQLiteTransactionListener() {
            @Override
            public void onBegin() {
                Log.d(TAG, "setAtvChannels onBegin");
            }

            @Override
            public void onCommit() {
                Log.d(TAG,"setAtvChannels onCommit");
            }

            @Override
            public void onRollback() {
                Log.d(TAG,"setAtvChannels onRollback");
            }
        });
        db.delete(TableName.ATV_CHANNEL, null, null);
        if (newChannels != null) {
            for (AtvChannel channel :
                    newChannels) {
                ContentValues contentValues = new ContentValues();
//                contentValues.put(FieldName.CHANNEL_ID, channel.getFreq());
//                contentValues.put(FieldName.CHANNEL_NUM, channel.getChannelNum());
                contentValues.put(FieldName.CHANNEL_NAME, channel.getChannelName());
                db.insert(TableName.ATV_CHANNEL, null, contentValues);
            }
        }

        db.setTransactionSuccessful();
        db.endTransaction();
        db.close();
    }

    ArrayList<AtvChannel> getAtvChannels() {
        ArrayList<AtvChannel> channels = null;
        Cursor cursor = null;
        SQLiteDatabase db = getReadableDatabase();
        try {
            cursor = db.query(TableName.ATV_CHANNEL, null, null, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                channels = new ArrayList<>();
                do {
                    AtvChannel bean = new AtvChannel();
//                    bean.setChannelNum(cursor.getInt(cursor.getColumnIndex(FieldName.CHANNEL_NUM)));
                    bean.setChannelName(cursor.getString(cursor.getColumnIndex(FieldName.CHANNEL_NAME)));
//                    bean.setFreq(cursor.getInt(cursor.getColumnIndex(FieldName.CHANNEL_ID)));

                    channels.add(bean);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        db.close();
        return channels;
    }
}

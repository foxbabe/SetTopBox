package com.savor.ads.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.OnDemandBean;
import com.savor.ads.bean.PlayListBean;
import com.savor.ads.bean.SetTopBoxBean;
import com.savor.ads.bean.VersionInfo;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanghq on 2016/12/9.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";
    private SQLiteDatabase db=null;
    private static DBHelper dbHelper=null;
    public static class MediaDBInfo {

        public static class FieldName {
            public static final String ID = "id";
            public static final String LOCATION_ID = "location_id";
            public static final String PERIOD = "period";
            public static final String ADS_ORDER = "ads_order";
            public static final String VID = "vid";
            public static final String MD5 = "md5";
            public static final String MEDIANAME = "media_name";
            public static final String MEDIATYPE = "media_type";
            public static final String MEDIA_PATH = "media_path";
            public static final String CHINESE_NAME = "chinese_name";
            public static final String SURFIX = "surfix";
            public static final String DURATION = "duration";
            public static final String CREATETIME = "create_time";
            public static final String UPDATETIME = "update_time";
            public static final String DOWNLOADED = "downloaded";

            public static final String START_DATE = "start_date";
            public static final String END_DATE = "end_date";

            /**
             * 长视频信息
             */

            public static final String TITLE = "title";
            public static final String TIME = "time";
            public static final String CATAGORY = "catagory";
            public static final String VIDEOURL = "videourl";
            public static final String PICURL = "picurl";
            public static final String PICMD5 = "picmd5";
            public static final String LENGTHCLASSIFY = "lengthcalssify";
            public static final String AREAID = "areadid";
            public static final String PICNAME = "picname";

        }

        public static class TableName {
            public static final String NEWPLAYLIST = "newplaylist_talbe";
            public static final String PLAYLIST = "playlist_talbe";
            public static final String NEWADSLIST = "new_adslist_table";
            public static final String ADSLIST = "adslist_table";
            public static final String MULTICASTMEDIALIB = "multicastmedialib_table";
        }
    }

    /**
     * 数据库名称
     */
    public static final String DATABASE_NAME = "dbsavor.db";


    private static final int DB_VERSION = 14;

    private Context mContext;

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);

        mContext = context;
        open();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LogFileUtil.writeApInfo("-------Database onCreate-------");
        /**
         * 创建新一期的播放列表
         * */
        createTable_newplaylistTrace(db);
        /**
         * 创建正在播放的播放列表
         */
        createTable_playlistTrace(db);
        /**
         *创建新的一期广告内容表
         */
        createTable_newAdsListTrace(db);
        /**
         * 创建正在播放的广告内容表
         */
        createTable_adsListTrace(db);

        /**
         * 创建点播下载表
         */
        createTable_multicastTrace(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        LogFileUtil.writeApInfo("-------Database onUpgrade-------oldVersion=" + oldVersion + ", newVersion=" + newVersion);

        if (newVersion == 14) {
            // 14版本加入视频Location_id属性来给广告表做匹配
            try {
                String alterPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.PLAYLIST + " ADD " + MediaDBInfo.FieldName.LOCATION_ID +" TEXT;";
                sqLiteDatabase.execSQL(alterPlaylist);

                String alterNewPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.NEWPLAYLIST + " ADD " + MediaDBInfo.FieldName.LOCATION_ID +" TEXT;";
                sqLiteDatabase.execSQL(alterNewPlaylist);

                /**
                 * 创建存储新的一期的广告内容表
                 */
                createTable_newAdsListTrace(sqLiteDatabase);
                /**
                 * 创建当前播放的完整的广告内容表
                 */
                createTable_adsListTrace(sqLiteDatabase);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

//    @Override
//    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        LogFileUtil.writeApInfo("-------Database onUpgrade-------oldVersion=" + oldVersion + ", newVersion=" + newVersion);
//        try {
//            if (newVersion < 14) {
//                String alterPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.PLAYLIST + " DROP COLUMN " + MediaDBInfo.FieldName.LOCATION_ID;
//                db.execSQL(alterPlaylist);
//
//                String alterNewPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.NEWPLAYLIST + " DROP COLUMN " + MediaDBInfo.FieldName.LOCATION_ID;
//                db.execSQL(alterNewPlaylist);
//
//                String dropAdsTable = "DROP TABLE " + MediaDBInfo.TableName.ADSLIST;
//                db.execSQL(dropAdsTable);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    private void createTable_newplaylistTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.NEWPLAYLIST
                + " (id INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.ADS_ORDER + " INTEGER, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.SURFIX + " TEXT, "
                + MediaDBInfo.FieldName.DURATION + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT " + ");";
        db.execSQL(DATABASE_CREATE);
    }
    private void upgradeTable_newplaylistTrace(SQLiteDatabase db) {
        createTempTable(db,MediaDBInfo.TableName.NEWPLAYLIST,MediaDBInfo.TableName.NEWPLAYLIST+"_temp");
        createTable_newplaylistTrace(db);
        String column = "id,"
                + MediaDBInfo.FieldName.VID + ","
                + MediaDBInfo.FieldName.PERIOD + ","
                + MediaDBInfo.FieldName.ADS_ORDER + ","
                + MediaDBInfo.FieldName.MEDIANAME + ","
                + MediaDBInfo.FieldName.MEDIATYPE + ","
                + MediaDBInfo.FieldName.CREATETIME + ","
                + MediaDBInfo.FieldName.SURFIX + ","
                + MediaDBInfo.FieldName.DURATION + ","
                + MediaDBInfo.FieldName.MEDIA_PATH + ","
                + MediaDBInfo.FieldName.MD5;
        copyTempDataToTable(db,MediaDBInfo.TableName.NEWPLAYLIST,column,MediaDBInfo.TableName.NEWPLAYLIST + "_temp",column);
    }

    private void createTable_playlistTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.PLAYLIST
                + " (id INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.ADS_ORDER + " INTEGER, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.SURFIX + " TEXT, "
                + MediaDBInfo.FieldName.DURATION + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT " + ");";

        db.execSQL(DATABASE_CREATE);
    }
    private void upgradeTable_playlistTrace(SQLiteDatabase db) {
        createTempTable(db,MediaDBInfo.TableName.PLAYLIST,MediaDBInfo.TableName.PLAYLIST+"_temp");
        createTable_playlistTrace(db);
        String column = "id,"
                + MediaDBInfo.FieldName.VID + ","
                + MediaDBInfo.FieldName.PERIOD + ","
                + MediaDBInfo.FieldName.ADS_ORDER + ","
                + MediaDBInfo.FieldName.MEDIANAME + ","
                + MediaDBInfo.FieldName.MEDIATYPE + ","
                + MediaDBInfo.FieldName.CREATETIME + ","
                + MediaDBInfo.FieldName.SURFIX + ","
                + MediaDBInfo.FieldName.DURATION + ","
                + MediaDBInfo.FieldName.MEDIA_PATH + ","
                + MediaDBInfo.FieldName.MD5;
        copyTempDataToTable(db,MediaDBInfo.TableName.PLAYLIST,column,MediaDBInfo.TableName.PLAYLIST + "_temp",column);
    }
    /**
     * 创建广告表
     * @param db
     */
    private void createTable_newAdsListTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.NEWADSLIST
                + " (id INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CHINESE_NAME + "TEXT,"
                + MediaDBInfo.FieldName.MD5 + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.ADS_ORDER + " INTEGER, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.SURFIX + " TEXT, "
                + MediaDBInfo.FieldName.DURATION + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT, "
                + MediaDBInfo.FieldName.START_DATE + " TEXT, "
                + MediaDBInfo.FieldName.END_DATE + " TEXT " + ");";
        db.execSQL(DATABASE_CREATE);
    }
    /**
     * 创建广告表
     * @param db
     */
    private void createTable_adsListTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.ADSLIST
                + " (id INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CHINESE_NAME + "TEXT,"
                + MediaDBInfo.FieldName.MD5 + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.ADS_ORDER + " INTEGER, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.SURFIX + " TEXT, "
                + MediaDBInfo.FieldName.DURATION + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT, "
                + MediaDBInfo.FieldName.START_DATE + " TEXT, "
                + MediaDBInfo.FieldName.END_DATE + " TEXT " + ");";
        db.execSQL(DATABASE_CREATE);
    }
    /**
     * 更新广告表
     * @param db
     */
    private void upgradeTable_adsListTrace(SQLiteDatabase db) {
        createTempTable(db,MediaDBInfo.TableName.ADSLIST,MediaDBInfo.TableName.ADSLIST+"_temp");
        createTable_playlistTrace(db);
        String column = MediaDBInfo.FieldName.ID + ","
                + MediaDBInfo.FieldName.VID + ","
                + MediaDBInfo.FieldName.LOCATION_ID + ","
                + MediaDBInfo.FieldName.MEDIANAME + ","
                + MediaDBInfo.FieldName.MEDIATYPE + ","
                + MediaDBInfo.FieldName.CHINESE_NAME + ","
                + MediaDBInfo.FieldName.MD5 + ","
                + MediaDBInfo.FieldName.PERIOD + ","
                + MediaDBInfo.FieldName.ADS_ORDER + ","
                + MediaDBInfo.FieldName.CREATETIME + ","
                + MediaDBInfo.FieldName.SURFIX + ","
                + MediaDBInfo.FieldName.DURATION + ","
                + MediaDBInfo.FieldName.MEDIA_PATH + ","
                + MediaDBInfo.FieldName.START_DATE + ","
                + MediaDBInfo.FieldName.END_DATE ;
        copyTempDataToTable(db,MediaDBInfo.TableName.ADSLIST,column,MediaDBInfo.TableName.ADSLIST + "_temp",column);
    }

    private void createTable_multicastTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.MULTICASTMEDIALIB
                + " (id INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.TITLE + " TEXT, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.CATAGORY + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.PICNAME + " TEXT, "
                + MediaDBInfo.FieldName.PICMD5 + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT, "
                + MediaDBInfo.FieldName.AREAID + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.DOWNLOADED + " TEXT, "
                + MediaDBInfo.FieldName.LENGTHCLASSIFY + " TEXT " + ");";
        db.execSQL(DATABASE_CREATE);
    }
    private void upgradeTable_multicastTrace(SQLiteDatabase db) {
        createTempTable(db,MediaDBInfo.TableName.MULTICASTMEDIALIB,MediaDBInfo.TableName.MULTICASTMEDIALIB+"_temp");
        createTable_multicastTrace(db);
        String column = "id,"
                + MediaDBInfo.FieldName.TITLE + ","
                + MediaDBInfo.FieldName.VID + ","
                + MediaDBInfo.FieldName.CATAGORY + ","
                + MediaDBInfo.FieldName.MEDIANAME + ","
                + MediaDBInfo.FieldName.PICNAME + ","
                + MediaDBInfo.FieldName.PICMD5 + ","
                + MediaDBInfo.FieldName.MD5 + ","
                + MediaDBInfo.FieldName.AREAID + ","
                + MediaDBInfo.FieldName.PERIOD + ","
                + MediaDBInfo.FieldName.MEDIATYPE + ","
                + MediaDBInfo.FieldName.DOWNLOADED + ","
                + MediaDBInfo.FieldName.LENGTHCLASSIFY;
        copyTempDataToTable(db,MediaDBInfo.TableName.MULTICASTMEDIALIB,column,MediaDBInfo.TableName.MULTICASTMEDIALIB + "_temp",column);
    }

    /**
     * 创建临时表
     * @param srcTable
     * @param tempTable
     */
    private void createTempTable(SQLiteDatabase db,String srcTable,String tempTable){

        String DATABASE_CREATE_TEMP = "ALTER TABLE "
                + srcTable
                + " RENAME TO "
                + tempTable;
        db.execSQL(DATABASE_CREATE_TEMP);
    }

    /**
     * 将临时表中的数据复制回原表
     * @param db
     * @param srcTable
     * @param srcColumn
     * @param tempTable
     * @param tempColumn
     */
    private void copyTempDataToTable(SQLiteDatabase db,String srcTable,String srcColumn,String tempTable,String tempColumn){
        String DATABASE_COPY_DATA = "insert into "
                + srcTable
                + "("
                + srcColumn
                + ") select "
                + tempColumn
                + " from "
                + tempTable;
        db.execSQL(DATABASE_COPY_DATA);
        db.execSQL("DROP TABLE IF EXISTS " + tempTable);
    }

    /**
     * 打开数据库
     *
     * @return
     * @throws SQLException
     */
    public SQLiteDatabase open(){
        if (db == null || !db.isOpen()) {
            db = getWritableDatabase();
            db.enableWriteAheadLogging();
        }
        return db;
    }

    public synchronized static DBHelper get(Context context){
            if (dbHelper == null){
                dbHelper = new DBHelper(context);
            }
        return dbHelper;
    }

    /**
     * 关闭数据库
     */
    public void close() {
        if (db!=null&&db.isOpen()){
            db.close();
            db = null;
        }
    }



    /**
     * 向广告播放列表数据库中插入数据
     *
     * @param playList
     * @throws JSONException
     */
    public boolean insertOrUpdateNewPlayListLib(PlayListBean playList,int id) {
        if (playList == null) {
            return false;
        }
        long in = 0;
        try{
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.VID, playList.getVid());
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, playList.getMedia_name());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, playList.getMedia_type());
            initialValues.put(MediaDBInfo.FieldName.SURFIX, playList.getSurfix());
            initialValues.put(MediaDBInfo.FieldName.CREATETIME, AppUtils.getCurTime("yyyyMMddHHmm"));
            initialValues.put(MediaDBInfo.FieldName.MD5, playList.getMd5());
            initialValues.put(MediaDBInfo.FieldName.PERIOD, playList.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.ADS_ORDER, playList.getOrder());
            initialValues.put(MediaDBInfo.FieldName.DURATION, playList.getDuration());
            initialValues.put(MediaDBInfo.FieldName.LOCATION_ID, playList.getLocation_id());
            initialValues.put(MediaDBInfo.FieldName.MEDIA_PATH, playList.getMediaPath());
            if (-1!=id){
                String selection=DBHelper.MediaDBInfo.FieldName.ID + "=? ";
                String[] selectionArgs = new String[]{String.valueOf(id)};
                in = db.update(MediaDBInfo.TableName.NEWPLAYLIST,initialValues,selection,selectionArgs);
            }else {
                in = db.insert(MediaDBInfo.TableName.NEWPLAYLIST, null, initialValues);
            }

            if (in > 0) {
                return true;
            } else {
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }
        return  false;
    }
    //查询播放列表本期数据是否存在
    public List<PlayListBean> findNewPlayListByWhere(String selection, String[] selectionArgs) throws SQLException {

        Cursor cursor = null;
        List<PlayListBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.NEWPLAYLIST, null,
                    selection, selectionArgs, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    PlayListBean bean = new PlayListBean();
                    bean.setId(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ID)));
                    bean.setVid(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MD5)));
                    bean.setMedia_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setSurfix(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.SURFIX)));
                    bean.setPeriod(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.PERIOD)));
                    bean.setOrder(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ADS_ORDER)));
                    playList.add(bean);
                }
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        } finally {
            try {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e2) {
                LogUtils.e(e2.toString());
            }

        }
        return playList;
    }
    //查询播放列表本期数据是否存在
    public List<PlayListBean> findPlayListByWhere(String selection, String[] selectionArgs) throws SQLException {

        String[] columns = new String[]{
                MediaDBInfo.FieldName.ID,
                DBHelper.MediaDBInfo.FieldName.MD5,
                DBHelper.MediaDBInfo.FieldName.MEDIANAME,
                DBHelper.MediaDBInfo.FieldName.VID,
                DBHelper.MediaDBInfo.FieldName.SURFIX,
                MediaDBInfo.FieldName.PERIOD};
        String groupBy = null;
        String having = null;
        String orderBy = null;
        Cursor cursor = null;
        List<PlayListBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.PLAYLIST, columns,
                    selection, selectionArgs, groupBy, having, orderBy, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    PlayListBean bean = new PlayListBean();
                    bean.setId(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ID)));
                    bean.setVid(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MD5)));
                    bean.setMedia_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setSurfix(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.SURFIX)));
                    bean.setPeriod(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.PERIOD)));
                    playList.add(bean);
                }
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        } finally {
            try {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e2) {
                LogUtils.e(e2.toString());
            }

        }
        return playList;
    }


    public List<PlayListBean> findAdsByWhere(String selection, String[] selectionArgs) throws SQLException {
        Cursor cursor = null;
        List<PlayListBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.ADSLIST, null,
                    selection, selectionArgs, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    PlayListBean bean = new PlayListBean();
                    bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MD5)));
                    bean.setMedia_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setMedia_type(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                    bean.setMediaPath(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIA_PATH)));
                    bean.setSurfix(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.SURFIX)));
                    bean.setPeriod(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.PERIOD)));
                    bean.setDuration(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.DURATION)));
                    bean.setOrder(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ADS_ORDER)));
                    bean.setLocation_id(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.LOCATION_ID)));
                    bean.setStart_date(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.START_DATE)));
                    bean.setEnd_date(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.END_DATE)));
                    playList.add(bean);
                }
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        } finally {
            try {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e2) {
                LogUtils.e(e2.toString());
            }

        }
        return playList;
    }

    public List<PlayListBean> findNewAdsByWhere(String selection, String[] selectionArgs) throws SQLException {
        Cursor cursor = null;
        List<PlayListBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.NEWADSLIST, null,
                    selection, selectionArgs, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    PlayListBean bean = new PlayListBean();
                    bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MD5)));
                    bean.setMedia_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setMedia_type(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                    bean.setMediaPath(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIA_PATH)));
                    bean.setSurfix(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.SURFIX)));
                    bean.setPeriod(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.PERIOD)));
                    bean.setDuration(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.DURATION)));
                    bean.setOrder(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ADS_ORDER)));
                    bean.setLocation_id(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.LOCATION_ID)));
                    bean.setStart_date(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.START_DATE)));
                    bean.setEnd_date(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.END_DATE)));
                    playList.add(bean);
                }
            }
        } catch (Exception e) {
            LogUtils.e(e.toString());
        } finally {
            try {
                if (cursor != null && !cursor.isClosed()) {
                    cursor.close();
                }
            } catch (Exception e2) {
                LogUtils.e(e2.toString());
            }

        }
        return playList;
    }

    /**
     * 广告数据单独入库
     * @param playList
     * @param id
     * @return
     */
    public boolean insertOrUpdateNewAdsList(PlayListBean playList,int id) {
        if (playList == null) {
            return false;
        }
        long in = 0;
        try{
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.VID, playList.getVid());
            initialValues.put(MediaDBInfo.FieldName.LOCATION_ID, playList.getLocation_id());
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, playList.getMedia_name());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, playList.getMedia_type());
            initialValues.put(MediaDBInfo.FieldName.MD5, playList.getMd5());
            initialValues.put(MediaDBInfo.FieldName.PERIOD, playList.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.ADS_ORDER, playList.getOrder());
            initialValues.put(MediaDBInfo.FieldName.CREATETIME, AppUtils.getCurTime("yyyyMMddHHmm"));
            initialValues.put(MediaDBInfo.FieldName.SURFIX, playList.getSurfix());
            initialValues.put(MediaDBInfo.FieldName.DURATION, playList.getDuration());
            initialValues.put(MediaDBInfo.FieldName.MEDIA_PATH, playList.getMediaPath());
            initialValues.put(MediaDBInfo.FieldName.START_DATE, playList.getStart_date());
            initialValues.put(MediaDBInfo.FieldName.END_DATE, playList.getEnd_date());
            if (-1!=id){
                String selection=DBHelper.MediaDBInfo.FieldName.ID + "=? ";
                String[] selectionArgs = new String[]{String.valueOf(id)};
                in = db.update(MediaDBInfo.TableName.NEWADSLIST,initialValues,selection,selectionArgs);
            }else {
                in = db.insert(MediaDBInfo.TableName.NEWADSLIST, null, initialValues);
            }

            if (in > 0) {
                return true;
            } else {
                return false;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }
        return  false;
    }

    /**
     * 按顺序查询播放表
     * @return
     */
    public ArrayList<PlayListBean> getOrderedPlayList() {
        ArrayList<PlayListBean> playList = null;
        Cursor cursor=null;
        Session session = Session.get(mContext);
        if (!TextUtils.isEmpty(session.getProPeriod())
                && !TextUtils.isEmpty(session.getAdvPeriod())) {
            try {
                //            open();
                // 拼接查询条件
                String selection = MediaDBInfo.FieldName.PERIOD + "=? OR " + MediaDBInfo.FieldName.PERIOD + "=?";
                String[] args = new String[]{session.getProPeriod(), session.getAdvPeriod()};
//                ArrayList<VersionInfo> playListVersion = Session.get(mContext).getPlayListVersion();
//                for (int i = 0; i < playListVersion.size(); i++) {
//                    VersionInfo version = playListVersion.get(i);
//                    if (i != 0) {
//                        selection += " or ";
//                    }
//                    selection += "(" + MediaDBInfo.FieldName.PERIOD + "=? and " + MediaDBInfo.FieldName.MEDIATYPE + "=?)";
//                    args[i * 2] = version.getVersion();
//                    args[i * 2 + 1] = version.getType();
//                }

                cursor = db.query(MediaDBInfo.TableName.PLAYLIST, null, selection, args, null, null, MediaDBInfo.FieldName.ADS_ORDER);
                if (cursor != null && cursor.moveToFirst()) {
                    playList = new ArrayList<>();
                    do {
                        PlayListBean bean = new PlayListBean();
                        bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                        bean.setMd5(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MD5)));
                        bean.setMedia_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                        bean.setMedia_type(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                        bean.setMediaPath(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIA_PATH)));
                        bean.setSurfix(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.SURFIX)));
                        bean.setPeriod(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.PERIOD)));
                        bean.setDuration(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.DURATION)));
                        bean.setOrder(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ADS_ORDER)));
                        bean.setLocation_id(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.LOCATION_ID)));

                        playList.add(bean);
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
        }
        return playList;
    }

    /**
     * 数据删除
     */
    public boolean deleteDataByWhere(String DBtable, String selection, String[] selectionArgs) {
        boolean flag = false;
        try {
//            open();
            flag = db.delete(DBtable, selection, selectionArgs) > 0;
        }catch (Exception e){
            e.printStackTrace();
        }finally {

        }

        return flag;
    }

    /**
     * 清空某张表的数据
     */
    public boolean deleteAllData(String DBtable) {
        boolean flag =false;
        try{
//            open();
            flag = db.delete(DBtable, null, null) > 0;

        }catch (Exception e){
            LogUtils.d(e.toString());
        }finally {

        }

        return flag;
    }

    /**
     * 复制数据
     * @param fromTable
     * @param toTable
     */
    public void copyTableMethod(String fromTable, String toTable) {
        try{
//            open();
            db.delete(toTable, "1=1", null);
            db.execSQL("insert into " + toTable + " select * from  " + fromTable);
        }catch (Exception e){
           e.printStackTrace();
        }


    }

    /**
     * 向点播下载表插入数据
     *
     * @throws JSONException
     */
    public boolean insertOrUpdateMulticastLib(MediaLibBean bean,boolean isUpdate) {
        if (bean == null) {
            return false;
        }
        boolean flag = false;
        try {
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.TITLE, bean.getName());
            initialValues.put(MediaDBInfo.FieldName.VID, bean.getVid());
            initialValues.put(MediaDBInfo.FieldName.MD5, bean.getMd5());
            initialValues.put(MediaDBInfo.FieldName.DOWNLOADED, "0");
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, bean.getName());
            initialValues.put(MediaDBInfo.FieldName.AREAID, bean.getArea_id());
            initialValues.put(MediaDBInfo.FieldName.PERIOD, bean.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, bean.getType());
            long success = 0;
            if (isUpdate){
                success = db.update(MediaDBInfo.TableName.MULTICASTMEDIALIB,
                        initialValues,MediaDBInfo.FieldName.TITLE+"=? ",
                        new String[]{bean.getName()});
            }else {
                success = db.insert(MediaDBInfo.TableName.MULTICASTMEDIALIB,
                        null,
                        initialValues);
            }

            if (success > 0) {
                flag = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
        }

        return flag;
    }

    /**
     * 查询点播表
     * @param selection
     * @param selectionArgs
     * @return
     * @throws SQLException
     */
    public List<OnDemandBean> findMutlicastMediaLibByWhere(String selection, String[] selectionArgs) throws SQLException {
        List<OnDemandBean> list = null;
        synchronized (dbHelper){
            Cursor cursor = null;
            try{
//                open();
//                db.beginTransaction();
                cursor = db.query(MediaDBInfo.TableName.MULTICASTMEDIALIB, null,
                        selection, selectionArgs, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        list = new ArrayList<>();
                        do {
                            OnDemandBean bean = new OnDemandBean();
                            bean.setTitle(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.TITLE)));
                            bean.setVodId(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                            bean.setCatagory(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.CATAGORY)));
                            bean.setPicUrlMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.PICMD5)));
                            bean.setMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MD5)));
                            bean.setLengthClassify(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.LENGTHCLASSIFY)));
                            bean.setAreaId(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.AREAID)));
                            bean.setPeriod(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.PERIOD)));
                            bean.setMedia_type(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MEDIATYPE)));
                            list.add(bean);
                        } while (cursor.moveToNext());
                    }
//                    db.setTransactionSuccessful();

                }
            }catch (Exception e){
                e.printStackTrace();
            }finally {
                try{
                    if (cursor!=null&&!cursor.isClosed()){
                        cursor.close();
                        cursor = null;
                    }
//                    db.endTransaction();
//                    db.close();
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

}

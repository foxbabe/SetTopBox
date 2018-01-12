package com.savor.ads.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import com.savor.ads.bean.MediaLibBean;
import com.savor.ads.bean.RstrSpecialty;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanghq on 2016/12/9.
 */

public class DBHelper extends SQLiteOpenHelper {

    private static final String TAG = "DBHelper";
    private SQLiteDatabase db = null;
    private static DBHelper dbHelper = null;

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

            public static final String START_DATE = "start_date";
            public static final String END_DATE = "end_date";

            public static final String FOOD_ID = "food_id";

            public static final String ADMASTER_SIN = "admaster_sin";
        }

        public static class TableName {
            public static final String NEWPLAYLIST = "newplaylist_talbe";
            public static final String PLAYLIST = "playlist_talbe";
            public static final String NEWADSLIST = "new_adslist_table";
            public static final String ADSLIST = "adslist_table";
            public static final String MULTICASTMEDIALIB = "multicastmedialib_table";
            public static final String SPECIALTY = "specialty_table";
            public static final String RTB_ADS = "rtb_ads_table";
        }
    }

    /**
     * 数据库名称
     */
    public static final String DATABASE_NAME = "dbsavor.db";


    private static final int DB_VERSION = 16;

    private Context mContext;

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DB_VERSION);

        mContext = context;
        open();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        LogFileUtil.writeKeyLogInfo("-------Database onCreate-------");
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
        /**
         * 创建特色菜表
         */
        createTable_specialty(db);
        /**
         * 创建实时竞价广告表
         */
        createTable_rtbads(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        LogFileUtil.writeKeyLogInfo("-------Database onUpgrade-------oldVersion=" + oldVersion + ", newVersion=" + newVersion);

        if (oldVersion < 16) {
            // 16版本加入RTB广告，同时轮播视频表和点播视频表加上<中文名称>列
            try {
                /**
                 * 创建实时竞价广告表
                 */
                createTable_rtbads(sqLiteDatabase);

                String alterPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.PLAYLIST + " ADD " + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT;";
                sqLiteDatabase.execSQL(alterPlaylist);

                String alterNewPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.NEWPLAYLIST + " ADD " + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT;";
                sqLiteDatabase.execSQL(alterNewPlaylist);

                String alterAdslist = "ALTER TABLE " + MediaDBInfo.TableName.ADSLIST + " ADD " + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT;";
                sqLiteDatabase.execSQL(alterAdslist);

                String alterNewAdslist = "ALTER TABLE " + MediaDBInfo.TableName.NEWADSLIST + " ADD " + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT;";
                sqLiteDatabase.execSQL(alterNewAdslist);

                String alterMulticast = "ALTER TABLE " + MediaDBInfo.TableName.MULTICASTMEDIALIB + " ADD " + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT;";
                sqLiteDatabase.execSQL(alterMulticast);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (oldVersion < 15) {
            // 15版本加入特色菜
            /**
             * 创建特色菜表
             */
            createTable_specialty(sqLiteDatabase);
        }

        if (oldVersion < 14) {
            // 14版本加入视频Location_id属性来给广告表做匹配
            try {
                String alterPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.PLAYLIST + " ADD " + MediaDBInfo.FieldName.LOCATION_ID + " TEXT;";
                sqLiteDatabase.execSQL(alterPlaylist);

                String alterNewPlaylist = "ALTER TABLE " + MediaDBInfo.TableName.NEWPLAYLIST + " ADD " + MediaDBInfo.FieldName.LOCATION_ID + " TEXT;";
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

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        LogFileUtil.writeKeyLogInfo("-------Database onUpgrade-------oldVersion=" + oldVersion + ", newVersion=" + newVersion);
    }

    private void createTable_newplaylistTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.NEWPLAYLIST
                + " (id INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.ADS_ORDER + " INTEGER, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.SURFIX + " TEXT, "
                + MediaDBInfo.FieldName.DURATION + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT " + ");";
        db.execSQL(DATABASE_CREATE);
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
                + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.SURFIX + " TEXT, "
                + MediaDBInfo.FieldName.DURATION + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT " + ");";

        db.execSQL(DATABASE_CREATE);
    }

    /**
     * 创建广告表
     *
     * @param db
     */
    private void createTable_newAdsListTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.NEWADSLIST
                + " (" + MediaDBInfo.FieldName.ID + " INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT, "
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
     *
     * @param db
     */
    private void createTable_adsListTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.ADSLIST
                + " (" + MediaDBInfo.FieldName.ID + " INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT, "
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

    private void createTable_multicastTrace(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.MULTICASTMEDIALIB
                + " (" + MediaDBInfo.FieldName.ID + " INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT);";
        db.execSQL(DATABASE_CREATE);
    }

    private void createTable_specialty(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.SPECIALTY
                + " (" + MediaDBInfo.FieldName.ID + " INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.FOOD_ID + " TEXT);";
        db.execSQL(DATABASE_CREATE);
    }

    private void createTable_rtbads(SQLiteDatabase db) {
        String DATABASE_CREATE = "create table "
                + MediaDBInfo.TableName.RTB_ADS
                + " (" + MediaDBInfo.FieldName.ID + " INTEGER PRIMARY KEY, "
                + MediaDBInfo.FieldName.VID + " TEXT, "
                + MediaDBInfo.FieldName.LOCATION_ID + " TEXT, "
                + MediaDBInfo.FieldName.MEDIANAME + " TEXT, "
                + MediaDBInfo.FieldName.MEDIATYPE + " TEXT, "
                + MediaDBInfo.FieldName.CHINESE_NAME + " TEXT, "
                + MediaDBInfo.FieldName.MD5 + " TEXT, "
                + MediaDBInfo.FieldName.PERIOD + " TEXT, "
                + MediaDBInfo.FieldName.ADS_ORDER + " INTEGER, "
                + MediaDBInfo.FieldName.CREATETIME + " TEXT, "
                + MediaDBInfo.FieldName.SURFIX + " TEXT, "
                + MediaDBInfo.FieldName.DURATION + " TEXT, "
                + MediaDBInfo.FieldName.ADMASTER_SIN + " TEXT, "
                + MediaDBInfo.FieldName.MEDIA_PATH + " TEXT" + ");";
        db.execSQL(DATABASE_CREATE);
    }

    /**
     * 创建临时表
     *
     * @param srcTable
     * @param tempTable
     */
    private void createTempTable(SQLiteDatabase db, String srcTable, String tempTable) {

        String DATABASE_CREATE_TEMP = "ALTER TABLE "
                + srcTable
                + " RENAME TO "
                + tempTable;
        db.execSQL(DATABASE_CREATE_TEMP);
    }

    /**
     * 将临时表中的数据复制回原表
     *
     * @param db
     * @param srcTable
     * @param srcColumn
     * @param tempTable
     * @param tempColumn
     */
    private void copyTempDataToTable(SQLiteDatabase db, String srcTable, String srcColumn, String tempTable, String tempColumn) {
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
    public SQLiteDatabase open() {
        if (db == null || !db.isOpen()) {
            db = getWritableDatabase();
            db.enableWriteAheadLogging();
        }
        return db;
    }

    public synchronized static DBHelper get(Context context) {
        if (dbHelper == null) {
            dbHelper = new DBHelper(context);
        }
        return dbHelper;
    }

    /**
     * 关闭数据库
     */
    public void close() {
        if (db != null && db.isOpen()) {
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
    public boolean insertOrUpdateNewPlayListLib(MediaLibBean playList, int id) {
        if (playList == null) {
            return false;
        }
        long in = 0;
        try {
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.VID, playList.getVid());
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, playList.getName());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, playList.getType());
            initialValues.put(MediaDBInfo.FieldName.CHINESE_NAME, playList.getChinese_name());
            initialValues.put(MediaDBInfo.FieldName.SURFIX, playList.getSurfix());
            initialValues.put(MediaDBInfo.FieldName.CREATETIME, AppUtils.getCurTime("yyyyMMddHHmm"));
            initialValues.put(MediaDBInfo.FieldName.MD5, playList.getMd5());
            initialValues.put(MediaDBInfo.FieldName.PERIOD, playList.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.ADS_ORDER, playList.getOrder());
            initialValues.put(MediaDBInfo.FieldName.DURATION, playList.getDuration());
            initialValues.put(MediaDBInfo.FieldName.LOCATION_ID, playList.getLocation_id());
            initialValues.put(MediaDBInfo.FieldName.MEDIA_PATH, playList.getMediaPath());
            if (-1 != id) {
                String selection = DBHelper.MediaDBInfo.FieldName.ID + "=? ";
                String[] selectionArgs = new String[]{String.valueOf(id)};
                in = db.update(MediaDBInfo.TableName.NEWPLAYLIST, initialValues, selection, selectionArgs);
            } else {
                in = db.insert(MediaDBInfo.TableName.NEWPLAYLIST, null, initialValues);
            }

            if (in > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return false;
    }

    //查询播放列表本期数据是否存在
    public List<MediaLibBean> findNewPlayListByWhere(String selection, String[] selectionArgs) throws SQLException {

        Cursor cursor = null;
        List<MediaLibBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.NEWPLAYLIST, null,
                    selection, selectionArgs, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    MediaLibBean bean = new MediaLibBean();
                    bean.setId(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ID)));
                    bean.setVid(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MD5)));
                    bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setType(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                    bean.setChinese_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.CHINESE_NAME)));
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
    public List<MediaLibBean> findPlayListByWhere(String selection, String[] selectionArgs) throws SQLException {

        String[] columns = null;
        String groupBy = null;
        String having = null;
        String orderBy = null;
        Cursor cursor = null;
        List<MediaLibBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.PLAYLIST, columns,
                    selection, selectionArgs, groupBy, having, orderBy, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    MediaLibBean bean = new MediaLibBean();
                    bean.setId(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ID)));
                    bean.setVid(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MD5)));
                    bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setType(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                    bean.setChinese_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.CHINESE_NAME)));
                    bean.setMediaPath(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIA_PATH)));
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


    public List<MediaLibBean> findAdsByWhere(String selection, String[] selectionArgs) throws SQLException {
        Cursor cursor = null;
        List<MediaLibBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.ADSLIST, null,
                    selection, selectionArgs, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    MediaLibBean bean = new MediaLibBean();
                    bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MD5)));
                    bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setType(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                    bean.setChinese_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.CHINESE_NAME)));
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

    public List<MediaLibBean> findNewAdsByWhere(String selection, String[] selectionArgs) throws SQLException {
        Cursor cursor = null;
        List<MediaLibBean> playList = null;
        try {
//            open();
            cursor = db.query(MediaDBInfo.TableName.NEWADSLIST, null,
                    selection, selectionArgs, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                playList = new ArrayList<>();
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
                    MediaLibBean bean = new MediaLibBean();
                    bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                    bean.setMd5(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MD5)));
                    bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                    bean.setType(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                    bean.setMediaPath(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIA_PATH)));
                    bean.setChinese_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.CHINESE_NAME)));
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
     *
     * @param playList
     * @param id
     * @return
     */
    public boolean insertOrUpdateNewAdsList(MediaLibBean playList, int id) {
        if (playList == null) {
            return false;
        }
        long in = 0;
        try {
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.VID, playList.getVid());
            initialValues.put(MediaDBInfo.FieldName.LOCATION_ID, playList.getLocation_id());
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, playList.getName());
            initialValues.put(MediaDBInfo.FieldName.CHINESE_NAME, playList.getChinese_name());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, playList.getType());
            initialValues.put(MediaDBInfo.FieldName.MD5, playList.getMd5());
            initialValues.put(MediaDBInfo.FieldName.PERIOD, playList.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.ADS_ORDER, playList.getOrder());
            initialValues.put(MediaDBInfo.FieldName.CREATETIME, AppUtils.getCurTime("yyyyMMddHHmm"));
            initialValues.put(MediaDBInfo.FieldName.SURFIX, playList.getSurfix());
            initialValues.put(MediaDBInfo.FieldName.DURATION, playList.getDuration());
            initialValues.put(MediaDBInfo.FieldName.MEDIA_PATH, playList.getMediaPath());
            initialValues.put(MediaDBInfo.FieldName.START_DATE, playList.getStart_date());
            initialValues.put(MediaDBInfo.FieldName.END_DATE, playList.getEnd_date());
            if (-1 != id) {
                String selection = DBHelper.MediaDBInfo.FieldName.ID + "=? ";
                String[] selectionArgs = new String[]{String.valueOf(id)};
                in = db.update(MediaDBInfo.TableName.NEWADSLIST, initialValues, selection, selectionArgs);
            } else {
                in = db.insert(MediaDBInfo.TableName.NEWADSLIST, null, initialValues);
            }

            if (in > 0) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }
        return false;
    }

    /**
     * RTB广告数据单独入库
     *
     * @param playList
     * @param isUpdate
     * @return
     */
    public boolean insertOrUpdateRTBAdsList(MediaLibBean playList, boolean isUpdate) {
        if (playList == null) {
            return false;
        }
        long in = 0;
        try {
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.VID, playList.getVid());
            initialValues.put(MediaDBInfo.FieldName.LOCATION_ID, playList.getLocation_id());
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, playList.getName());
            initialValues.put(MediaDBInfo.FieldName.CHINESE_NAME, playList.getChinese_name());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, playList.getType());
            initialValues.put(MediaDBInfo.FieldName.MD5, playList.getMd5());
            initialValues.put(MediaDBInfo.FieldName.PERIOD, playList.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.ADS_ORDER, playList.getOrder());
            initialValues.put(MediaDBInfo.FieldName.CREATETIME, AppUtils.getCurTime("yyyyMMddHHmm"));
            initialValues.put(MediaDBInfo.FieldName.SURFIX, playList.getSurfix());
            initialValues.put(MediaDBInfo.FieldName.DURATION, playList.getDuration());
            initialValues.put(MediaDBInfo.FieldName.MEDIA_PATH, playList.getMediaPath());
            initialValues.put(MediaDBInfo.FieldName.ADMASTER_SIN, playList.getAdmaster_sin());

            long successCount = 0;
            if (isUpdate) {
                successCount = db.update(MediaDBInfo.TableName.RTB_ADS,
                        initialValues, MediaDBInfo.FieldName.VID + "=? ",
                        new String[]{playList.getVid()});
            } else {
                successCount = db.insert(MediaDBInfo.TableName.RTB_ADS,
                        null,
                        initialValues);
            }

            return successCount > 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 按顺序查询播放表
     *
     * @return
     */
    public ArrayList<MediaLibBean> getOrderedPlayList() {
        ArrayList<MediaLibBean> playList = null;
        Cursor cursor = null;
        Session session = Session.get(mContext);
        if (!TextUtils.isEmpty(session.getProPeriod())
                && !TextUtils.isEmpty(session.getAdvPeriod())) {
            try {
                // 拼接查询条件
                String selection = null;
                String[] args = null;
                //            open();
                if (session.isStandalone()){
                    // 拼接查询条件
                    selection = MediaDBInfo.FieldName.PERIOD + "=?";
                    args = new String[]{session.getProPeriod()};
                }else{
                    // 拼接查询条件
                    selection = MediaDBInfo.FieldName.PERIOD + "=? OR " + MediaDBInfo.FieldName.PERIOD + "=?";
                    args = new String[]{session.getProPeriod(), session.getAdvPeriod()};
                }




                cursor = db.query(MediaDBInfo.TableName.PLAYLIST, null, selection, args, null, null, MediaDBInfo.FieldName.ADS_ORDER);
                if (cursor != null && cursor.moveToFirst()) {
                    playList = new ArrayList<>();
                    do {
                        MediaLibBean bean = new MediaLibBean();
                        bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                        bean.setMd5(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MD5)));
                        bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                        bean.setChinese_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.CHINESE_NAME)));
                        bean.setType(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
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
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

        return flag;
    }

    /**
     * 清空某张表的数据
     */
    public boolean deleteAllData(String DBtable) {
        boolean flag = false;
        try {
//            open();
            flag = db.delete(DBtable, null, null) > 0;

        } catch (Exception e) {
            LogUtils.d(e.toString());
        } finally {

        }

        return flag;
    }

    /**
     * 复制数据
     *
     * @param fromTable
     * @param toTable
     */
    public void copyTableMethod(String fromTable, String toTable) {
        try {
//            open();
            db.delete(toTable, "1=1", null);
            db.execSQL("insert into " + toTable + " select * from  " + fromTable);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 向点播下载表插入数据
     *
     * @throws JSONException
     */
    public boolean insertOrUpdateMulticastLib(MediaLibBean bean, boolean isUpdate) {
        if (bean == null) {
            return false;
        }
        boolean flag = false;
        try {
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.VID, bean.getVid());
            initialValues.put(MediaDBInfo.FieldName.MD5, bean.getMd5());
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, bean.getName());
            initialValues.put(MediaDBInfo.FieldName.CHINESE_NAME, bean.getChinese_name());
            initialValues.put(MediaDBInfo.FieldName.PERIOD, bean.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, bean.getType());
            long success = 0;
            if (isUpdate) {
                success = db.update(MediaDBInfo.TableName.MULTICASTMEDIALIB,
                        initialValues, MediaDBInfo.FieldName.VID + "=? ",
                        new String[]{bean.getVid()});
            } else {
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
     * 向点特色菜表插入数据
     *
     * @throws JSONException
     */
    public boolean insertOrUpdateSpecialtyLib(RstrSpecialty bean, boolean isUpdate) {
        if (bean == null) {
            return false;
        }
        boolean flag = false;
        try {
//            open();
            ContentValues initialValues = new ContentValues();
            initialValues.put(MediaDBInfo.FieldName.VID, bean.getVid());
            initialValues.put(MediaDBInfo.FieldName.FOOD_ID, bean.getFood_id());
            initialValues.put(MediaDBInfo.FieldName.MD5, bean.getMd5());
            initialValues.put(MediaDBInfo.FieldName.MEDIANAME, bean.getName());
            initialValues.put(MediaDBInfo.FieldName.MEDIA_PATH, bean.getMedia_path());
            initialValues.put(MediaDBInfo.FieldName.CREATETIME, AppUtils.getCurTime("yyyyMMddHHmm"));
            initialValues.put(MediaDBInfo.FieldName.PERIOD, bean.getPeriod());
            initialValues.put(MediaDBInfo.FieldName.MEDIATYPE, bean.getType());
            long success = 0;
            if (isUpdate) {
                success = db.update(MediaDBInfo.TableName.SPECIALTY,
                        initialValues, MediaDBInfo.FieldName.FOOD_ID + "=? ",
                        new String[]{bean.getFood_id()});
            } else {
                success = db.insert(MediaDBInfo.TableName.SPECIALTY,
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
     *
     * @param selection
     * @param selectionArgs
     * @return
     * @throws SQLException
     */
    public List<MediaLibBean> findMutlicastMediaLibByWhere(String selection, String[] selectionArgs) throws SQLException {
        List<MediaLibBean> list = null;
        synchronized (dbHelper) {
            Cursor cursor = null;
            try {
//                open();
//                db.beginTransaction();
                cursor = db.query(MediaDBInfo.TableName.MULTICASTMEDIALIB, null,
                        selection, selectionArgs, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        list = new ArrayList<>();
                        do {
                            MediaLibBean bean = new MediaLibBean();
                            bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                            bean.setChinese_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.CHINESE_NAME)));
                            bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                            bean.setMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MD5)));
                            bean.setPeriod(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.PERIOD)));
                            list.add(bean);
                        } while (cursor.moveToNext());
                    }
//                    db.setTransactionSuccessful();

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                        cursor = null;
                    }
//                    db.endTransaction();
//                    db.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    /**
     * 查询RTB广告表
     *
     * @param selection
     * @param selectionArgs
     * @return
     * @throws SQLException
     */
    public List<MediaLibBean> findRtbadsMediaLibByWhere(String selection, String[] selectionArgs) throws SQLException {
        List<MediaLibBean> list = null;
        synchronized (dbHelper) {
            Cursor cursor = null;
            try {
//                open();
//                db.beginTransaction();
                cursor = db.query(MediaDBInfo.TableName.RTB_ADS, null,
                        selection, selectionArgs, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        list = new ArrayList<>();
                        do {
                            MediaLibBean bean = new MediaLibBean();
                            bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                            bean.setMd5(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MD5)));
                            bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                            bean.setChinese_name(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.CHINESE_NAME)));
                            bean.setType(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                            bean.setMediaPath(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIA_PATH)));
                            bean.setSurfix(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.SURFIX)));
                            bean.setPeriod(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.PERIOD)));
                            bean.setDuration(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.DURATION)));
                            bean.setOrder(cursor.getInt(cursor.getColumnIndex(MediaDBInfo.FieldName.ADS_ORDER)));
                            bean.setLocation_id(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.LOCATION_ID)));
                            bean.setAdmaster_sin(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.ADMASTER_SIN)));
                            list.add(bean);
                        } while (cursor.moveToNext());
                    }
//                    db.setTransactionSuccessful();

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                        cursor = null;
                    }
//                    db.endTransaction();
//                    db.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

    /**
     * 查询特色菜表
     *
     * @param selection
     * @param selectionArgs
     * @return
     * @throws SQLException
     */
    public List<RstrSpecialty> findSpecialtyByWhere(String selection, String[] selectionArgs) throws SQLException {
        List<RstrSpecialty> list = null;
        synchronized (dbHelper) {
            Cursor cursor = null;
            try {
//                open();
//                db.beginTransaction();
                cursor = db.query(MediaDBInfo.TableName.SPECIALTY, null,
                        selection, selectionArgs, null, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        list = new ArrayList<>();
                        do {
                            RstrSpecialty bean = new RstrSpecialty();
                            bean.setName(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIANAME)));
                            bean.setVid(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.VID)));
                            bean.setMd5(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.MD5)));
                            bean.setPeriod(cursor.getString(cursor.getColumnIndex(DBHelper.MediaDBInfo.FieldName.PERIOD)));
                            bean.setMedia_type(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIATYPE)));
                            bean.setMedia_path(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.MEDIA_PATH)));
                            bean.setFood_id(cursor.getString(cursor.getColumnIndex(MediaDBInfo.FieldName.FOOD_ID)));
                            list.add(bean);
                        } while (cursor.moveToNext());
                    }
//                    db.setTransactionSuccessful();

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (cursor != null && !cursor.isClosed()) {
                        cursor.close();
                        cursor = null;
                    }
//                    db.endTransaction();
//                    db.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }

}

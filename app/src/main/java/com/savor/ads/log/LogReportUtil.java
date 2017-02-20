	package com.savor.ads.log;

import android.content.Context;
import android.text.TextUtils;

import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


public class LogReportUtil {
	private static BlockingQueue<LogReportParam> mLogQueue = null;
	Context mContext=null;
	Session session = null;
	private static String boiteName=null;
	private static String roomName=null;
	private static String mac=null;
	private static LogReportUtil instance=null;
	private LogReportUtil(Context context) {
		mLogQueue = new ArrayBlockingQueue<LogReportParam>(15);
		this.mContext = context;
		session = Session.get(mContext);
		boiteName = session.getBoiteName();
		roomName = session.getRoomName();
		mac = session.getMacAddress();
		initLog();
	}

	public static LogReportUtil get(Context context){
		if (instance==null){
			instance = new LogReportUtil(context);
		}
		return instance;
	}

	private void initLog() {
		String time = String.valueOf(System.currentTimeMillis());
		String boiteId = "";
		String roomId = "";

		LogReportParam mLogEn = new LogReportParam();
		mLogEn.setUUid(time);
		if (!TextUtils.isEmpty(session.getBoiteId())){
			boiteId = session.getBoiteId();
		}
		if (!TextUtils.isEmpty(session.getRoomId())){
			roomId = session.getRoomId();
		}
		mLogEn.setUUid(String.valueOf(System.currentTimeMillis()));
		mLogEn.setHotel_id(boiteId);
		mLogEn.setRoom_id(roomId);
		mLogEn.setTime(time);
		mLogEn.setAction("poweron");
		mLogEn.setType("");
		mLogEn.setMedia_id("");
		mLogEn.setMobile_id("");
		mLogEn.setApk_version(session.getVersionName());
		mLogEn.setAdsPeriod(session.getAdvertMediaPeriod());
		mLogEn.setVodPeriod(session.getMulticastMediaPeriod());
		mLogEn.setCustom(AppUtils.getInputType(session.getTvInputSource()));
		mLogEn.setBoxId(session.getBoxId());
		mLogEn.setLogHour("");
		offer(mLogEn);
		/*******开机即生成一条信号源日志*******/
		time = String.valueOf(System.currentTimeMillis());
		mLogEn.setUUid(String.valueOf(System.currentTimeMillis()));
		mLogEn.setHotel_id(boiteId);
		mLogEn.setRoom_id(roomId);
		mLogEn.setTime(time);
		mLogEn.setAction("Signal");
		mLogEn.setType("system");
		mLogEn.setMedia_id("");
		mLogEn.setMobile_id("");
		mLogEn.setApk_version(session.getVersionName());
		mLogEn.setAdsPeriod(session.getAdvertMediaPeriod());
		mLogEn.setVodPeriod(session.getMulticastMediaPeriod());
		mLogEn.setCustom(AppUtils.getInputType(session.getTvInputSource()));
		offer(mLogEn);
	}


	private static void offer(LogReportParam arg) {
		if (mLogQueue != null) {
			mLogQueue.offer(arg);

		}
	}
	public LogReportParam poll() {
		return mLogQueue.poll();
	}

	public LogReportParam take() throws InterruptedException {
		if(mLogQueue!=null)
			return mLogQueue.take();
		else
			return null;
	}

	public static int getNum() {
		return mLogQueue.size();
	}

	/**
	 *
	 * @param uuid 视频播放完整性
	 * @param hotel_id 酒楼ID
	 * @param room_id 包间ID
	 * @param time 发生时间
	 * @param action 动作
	 * @param type 类型
	 * @param media_id 视频ID
	 * @param mobile_id 手机ID
	 * @param apk_version apk版本
	 * @param ads_period 广告期号
     * @param vod_period 点播期号
     * @param custom 通用参数，目前包括播放器音量，电视切换时间，电视信号源
     */
	
	public void sendAdsLog(String uuid ,
						   String hotel_id,
						   String room_id,
						   String time,
						   String action,
						   String type,
						   String media_id,
						   String mobile_id,
						   String apk_version,
						   String ads_period,
						   String vod_period,
						   String custom) {
		LogReportParam mLogEn = new LogReportParam();

		mLogEn.setUUid(uuid);
		mLogEn.setHotel_id(hotel_id+"");
		mLogEn.setRoom_id(room_id+"");
		mLogEn.setTime(time);
		mLogEn.setAction(action+"");
		mLogEn.setType(type);
		mLogEn.setMedia_id(media_id+"");
		mLogEn.setMobile_id(mobile_id+"");
		mLogEn.setApk_version(apk_version+"");
		mLogEn.setAdsPeriod(ads_period+"");
		mLogEn.setVodPeriod(vod_period+"");
		mLogEn.setCustom(custom+"");
		offer(mLogEn);
	}
	
}

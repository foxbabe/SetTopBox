package com.savor.ads.log;

import android.content.Context;
import android.text.TextUtils;

import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogUtils;

import java.io.FileWriter;
import java.io.IOException;

/**
 * 生产日志任务
 */
public class LogProduceService {
	private boolean running = false;
	private boolean producing = false;
	private FileWriter mwriter = null;
	private LogReportParam mparam = null;
	private Context mContext=null;
	private String logTime=null;
	private String logFileName = null;
	private LogReportUtil logReportUtil = null;
	private Session session;
	public LogProduceService (Context context){
		this.mContext = context;
		session = Session.get(context);
		logReportUtil = LogReportUtil.get(mContext);
	}

	/**
	 * 1.当卡被拔出的时候停止生产日志
	 * 2、当应用停掉的时候停止生产日志
	 */

	public void run() {
		LogUtils.i("run");
		running = true;
		new Thread() {
			@Override
			public void run() {
				while (running) {
					// 生成日志文件
					// 更新id
					createfile();
					producing = true;
					while (producing){
						if (mwriter==null){
							break;
						}
						if (!logTime.equals(AppUtils.getTime("hour"))){
							break;
						}
						int num = LogReportUtil.getNum();
						LogUtils.i("num:" + num);
						if (num > 0) {
							try {
								mparam = logReportUtil.take();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							if (mparam != null) {
								String log = makeLog();
								try {
									mwriter.write(log);
									mwriter.flush();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
						try {
							Thread.sleep(5*1000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					closeWriter();
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
	}

	private void closeWriter() {
		if (mwriter != null) {
			try {
				mwriter.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			mwriter = null;
		}
	}

	/**
	 * 获取日志内容
	 */
	private String makeLog(){
		String boxId="";
		String logHour = "";
		if ("poweron".equals(mparam.getAction())){
			boxId = mparam.getBoxId();
			logHour = mparam.getLogHour();
		}else {
			boxId = session.getEthernetMac();
			logHour = logTime;
		}
		String ret = mparam.getUUid() + ","
					+ mparam.getHotel_id() + ","
					+ mparam.getRoom_id() + ","
					+ mparam.getTime() + ","
					+ mparam.getAction() + ","
					+ mparam.getType()+ ","
					+ mparam.getMedia_id() + ","
					+ mparam.getMobile_id() + ","
					+ mparam.getApk_version() + ","
					+ mparam.getAdsPeriod() + ","
					+ mparam.getVodPeriod() + ","
					+ mparam.getCustom() + ","
					+ boxId + ","
					+ logHour
					+ "\r\n";
        LogUtils.d("makeLog=="+ret);
		return ret;
	}
	/**
	 * 创建日志
	 */
	private void createfile() {
		try {
			String roomId = session.getRoomId();
			String boiteid = session.getBoiteId();
			String boxId = session.getEthernetMac();
			if (TextUtils.isEmpty(roomId)
					||TextUtils.isEmpty(boiteid)
					||TextUtils.isEmpty(boxId)){
				producing = false;
					return;
			}
			String time = AppUtils.getTime("hour");
			String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.log);
			logFileName = boxId + "_" + time + ".blog";
			logTime = time;
			mwriter = new FileWriter(path + logFileName,true);
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		if (mwriter!=null){
			producing = true;
		}else {
			producing = false;
		}
	}
	
	public void stop() {
		LogUtils.i("stop");
		producing = false;

	}



}

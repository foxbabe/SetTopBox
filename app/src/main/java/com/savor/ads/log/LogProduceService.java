package com.savor.ads.log;

import android.content.Context;
import android.text.TextUtils;

import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 生产日志任务
 */
public class LogProduceService {
	private FileWriter mLogWriter = null;
	private Context mContext=null;
	private String logTime=null;
	private LogReportUtil logReportUtil = null;
	private Session session;
	private File file;
	//单机版
	private String standalone="standalone";
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
		new Thread() {
			@Override
			public void run() {
				while (true) {
					// 生成日志文件
					createFile();

					while (true) {
                        if (TextUtils.isEmpty(logTime) || !logTime.equals(AppUtils.getCurTime("yyyyMMddHH"))){
                            break;
                        }
                        if (mLogWriter != null) {
                            if (LogReportUtil.getLogNum() > 0) {
                                try {
                                    LogReportParam mparam = logReportUtil.take();
                                    if (mparam != null) {
                                        String log = makeLog(mparam);
                                        LogUtils.i("log:" + log);
                                        try {
                                            mLogWriter.write(log);
                                            mLogWriter.flush();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        } else {
							LogFileUtil.write("Log FileWriter is null, will recreate file.");
							createFile();
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
		if (mLogWriter != null) {
			try {
				mLogWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			mLogWriter = null;
		}
	}

	/**
	 * 获取日志内容
	 */
	private String makeLog(LogReportParam mparam){
		String boxId="";
		String logHour = "";
		String end = "";
		if ("poweron".equals(mparam.getAction())){
			boxId = mparam.getBoxId();
			logHour = mparam.getLogHour();
		}else {
			boxId = session.getEthernetMac();
			logHour = logTime;
		}
		if (file.getName().contains("standalone")){
			end = ",standalone"+"\r\n";
		}else {
			end = "\r\n";
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
					+ end;
		return ret;
	}

	/**
	 * 生成餐厅端日志内容
	 */
	private String makeRstrLog(RestaurantLogBean mparam){
		String ret = mparam.getUUid() + ","
					+ mparam.getHotel_id() + ","
					+ mparam.getRoom_id() + ","
					+ mparam.getTime() + ","
					+ mparam.getAction() + ","
					+ mparam.getType()+ ","
					+ mparam.getMobile_id() + ","
					+ mparam.getApk_version() + ","
					+ mparam.getDuration() + ","
					+ mparam.getPptSize() + ","
					+ mparam.getPptInterval() + ","
					+ mparam.getInnerType() + ","
					+ mparam.getCustom1() + ","
					+ mparam.getCustom2() + ","
					+ mparam.getCustom3() + ","
					+ mparam.getBoxId() + ","
					+ logTime
					+ "\r\n";
		return ret;
	}
	/**
	 * 创建日志
	 */
	private void createFile() {
		try {
//			String roomId = session.getRoomId();
//			String boiteid = session.getBoiteId();
			String boxMac = session.getEthernetMac();
//			if (TextUtils.isEmpty(roomId)
//					||TextUtils.isEmpty(boiteid)
//					||TextUtils.isEmpty(boxId)){
//					return;
//			}
			String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.log);
			logTime = AppUtils.getCurTime("yyyyMMddHH");
			if (session.isStandalone()){
				mLogWriter = new FileWriter(path + boxMac + "_" + logTime +"_"+standalone+".blog",true);
				file = new File(path + boxMac + "_" + logTime + "_" +standalone +".blog");
			}else {
				mLogWriter = new FileWriter(path + boxMac + "_" + logTime + ".blog",true);
				file = new File(path + boxMac + "_" + logTime + ".blog");
			}
		} catch (Exception e2) {
			e2.printStackTrace();
			LogFileUtil.writeException(e2);

            AppApi.reportSDCardState(mContext, null, 1);
		}
	}



}

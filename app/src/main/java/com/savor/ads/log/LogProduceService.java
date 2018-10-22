package com.savor.ads.log;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.savor.ads.core.ApiRequestListener;
import com.savor.ads.core.AppApi;
import com.savor.ads.core.Session;
import com.savor.ads.service.HeartbeatService;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.ConstantValues;
import com.savor.ads.utils.LogFileUtil;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Handler;

/**
 * 生产日志任务
 */
public class LogProduceService {
	private FileWriter mLogWriter = null;
	private FileWriter mQRCodeLogWriter = null;
	private Context mContext=null;
	private String logTime=null;
	private LogReportUtil logReportUtil = null;
	private Session session;
	private File file;
	private long tempTime;
	//单机版
	private String standalone="standalone";
	private boolean isNewRequestSmall=false;
	private boolean isNewRequestBig=false;
	private boolean isNewRequestCall=false;
	public LogProduceService (Context context){
		this.mContext = context;
		session = Session.get(context);
		logReportUtil = LogReportUtil.get(mContext);
	}
	android.os.Handler handler=new android.os.Handler(Looper.getMainLooper());
	/**
	 * 1.当卡被拔出的时候停止生产日志
	 * 2、当应用停掉的时候停止生产日志
	 */

	public void run() {
		new Thread() {
			@Override
			public void run() {
				while (true) {
					while (TextUtils.isEmpty(AppUtils.getMainMediaPath())) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					// 生成日志文件
					createFile();
					Random random =new Random();
					int index = random.nextInt(200);
					handler.postDelayed(new Runnable() {
						@Override
						public void run() {
							downloadMiniProgramIcon();
						}
					},index*1000);

					while (true) {
//						try {
//							long nowTime = System.currentTimeMillis();
//							long time = (nowTime-tempTime)/1000/60;
//							if (time>5){
//								break;
//							}
//						}catch (Exception e){
//							e.printStackTrace();
//						}

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
                                            if (!TextUtils.isEmpty(e.getMessage())){
												if (e.getMessage().contains("No space left on device")){
													Log.e("LogProduceService","内存卡满，无法写入");
												}else{
													AppApi.reportSDCardState(mContext, null, 1);
												}
											}
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

//						if (mQRCodeLogWriter!=null){
//                        	if (logReportUtil.getQRCodeLogNum()>0){
//								try {
//									QRCodeLogBean logBean = logReportUtil.takeCodeLog();
//									if (logBean != null) {
//										String log = makeCodeLog(logBean);
//										LogUtils.i("log:" + log);
//										try {
//											mQRCodeLogWriter.write(log);
//											mQRCodeLogWriter.flush();
//										} catch (IOException e) {
//											e.printStackTrace();
//										}
//									}
//								} catch (InterruptedException e) {
//									e.printStackTrace();
//								}
//							}
//						}else {
//							createFile();
//						}

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

	/**
	 * 小程序码下载到本地
	 */
	private void downloadMiniProgramIcon(){
        String box_mac = Session.get(mContext).getEthernetMac();

		String urlSmall = AppApi.API_URLS.get(AppApi.Action.CP_MINIPROGRAM_DOWNLOAD_QRCODE_JSON)+"?box_mac="+ box_mac+"&type="+ ConstantValues.MINI_PROGRAM_SMALL_TYPE;
        String pathSmall = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + ConstantValues.MINI_PROGRAM_SMALL_NAME;
        File tarFile = new File(pathSmall);
        if (tarFile.exists()) {
            tarFile.delete();
        }
        Session.get(mContext).setDownloadMiniProgramSmallIcon(false);
        AppApi.downloadQRCodeSmallImg(urlSmall,mContext,apiRequestListener,pathSmall);
        isNewRequestSmall = true;
        //-----------------------------------
        String urlBig = AppApi.API_URLS.get(AppApi.Action.CP_MINIPROGRAM_DOWNLOAD_QRCODE_JSON)+"?box_mac="+ box_mac+"&type="+ ConstantValues.MINI_PROGRAM_BIG_TYPE;
        String pathBig = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + ConstantValues.MINI_PROGRAM_BIG_NAME;
        tarFile = new File(urlBig);
        if (tarFile.exists()) {
            tarFile.delete();
        }
        Session.get(mContext).setDownloadMiniProgramBigIcon(false);
        AppApi.downloadQRCodeBigImg(urlBig,mContext,apiRequestListener,pathBig);
		isNewRequestBig = true;
        //-----------------------------------
        String urlCall = AppApi.API_URLS.get(AppApi.Action.CP_MINIPROGRAM_DOWNLOAD_QRCODE_JSON)+"?box_mac="+ box_mac+"&type="+ ConstantValues.MINI_PROGRAM_CALL_TYPE;
		String pathCall = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + ConstantValues.MINI_PROGRAM_CALL_NAME;
        tarFile = new File(urlCall);
        if (tarFile.exists()) {
            tarFile.delete();
        }
        Session.get(mContext).setDownloadMiniProgramCallIcon(false);
        AppApi.downloadQRCodeCallImg(urlCall,mContext,apiRequestListener,pathCall);
        isNewRequestCall = true;
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
	 * 创建小程序码显示日志
	 * @param codeLogBean
	 * @return
	 */
	private String makeCodeLog(QRCodeLogBean codeLogBean){
		String end = "\r\n";
		String ret = codeLogBean.getHotel_id()+","
				+ codeLogBean.getRoom_id()+","
				+ codeLogBean.getBoxId()+","
				+ codeLogBean.getAction()+","
				+ codeLogBean.getTime()+","
				+ end;
		return ret;
	}
	/**
	 * 创建日志
	 */
	private void createFile() {
		try {

			String boxMac = session.getEthernetMac();

			File file1 = new File(AppUtils.getMainMediaPath());
			if (!file1.exists()) {
				LogFileUtil.writeKeyLogInfo("createFile() MainMediaPath is not exist!!!");
			}
			String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.log);
			logTime = AppUtils.getCurTime("yyyyMMddHH");
			tempTime = System.currentTimeMillis();
			if (session.isStandalone()){
				mLogWriter = new FileWriter(path + boxMac + "_" + logTime +"_"+standalone+".blog",true);
				file = new File(path + boxMac + "_" + logTime + "_" +standalone +".blog");
			}else {
				mLogWriter = new FileWriter(path + boxMac + "_" + logTime + ".blog",true);
				file = new File(path + boxMac + "_" + logTime + ".blog");
			}

//			String pathCode = AppUtils.getFilePath(mContext,AppUtils.StorageFile.qrcode_log);
//			mQRCodeLogWriter = new FileWriter(pathCode+boxMac+"_"+logTime+".blog",true);
		} catch (Exception e2) {
			e2.printStackTrace();
			LogFileUtil.writeException(e2);

            AppApi.reportSDCardState(mContext, null, 1);
		}
	}


	ApiRequestListener apiRequestListener = new ApiRequestListener() {
		@Override
		public void onSuccess(AppApi.Action method, Object obj) {
			switch (method){
                case SP_GET_QRCODE_SMALL_JSON:
					if (obj instanceof File){
						Session.get(mContext).setDownloadMiniProgramSmallIcon(true);
					}
					break;
                case SP_GET_QRCODE_BIG_JSON:
                    if (obj instanceof File){
                        Session.get(mContext).setDownloadMiniProgramBigIcon(true);
                    }
                    break;
                case SP_GET_QRCODE_CALL_JSON:
                    if (obj instanceof File){
                        Session.get(mContext).setDownloadMiniProgramCallIcon(true);
                    }
                    break;
			}

		}

		@Override
		public void onError(AppApi.Action method, Object obj) {
			String box_mac = Session.get(mContext).getEthernetMac();
			switch (method){
				case SP_GET_QRCODE_SMALL_JSON:
					if (isNewRequestSmall){

						String urlSmall = AppApi.API_URLS.get(AppApi.Action.CP_MINIPROGRAM_DOWNLOAD_QRCODE_JSON)+"?box_mac="+ box_mac+"&type="+ ConstantValues.MINI_PROGRAM_SMALL_TYPE;
						String pathSmall = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + ConstantValues.MINI_PROGRAM_SMALL_NAME;
						File tarFile = new File(pathSmall);
						if (tarFile.exists()) {
							tarFile.delete();
						}
						Session.get(mContext).setDownloadMiniProgramSmallIcon(false);
						AppApi.downloadQRCodeSmallImg(urlSmall,mContext,apiRequestListener,pathSmall);
						isNewRequestSmall = false;
					}

					break;
				case SP_GET_QRCODE_BIG_JSON:
					if (isNewRequestBig){
						String urlBig = AppApi.API_URLS.get(AppApi.Action.CP_MINIPROGRAM_DOWNLOAD_QRCODE_JSON)+"?box_mac="+ box_mac+"&type="+ ConstantValues.MINI_PROGRAM_BIG_TYPE;
						String pathBig = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + ConstantValues.MINI_PROGRAM_BIG_NAME;
						File tarFile = new File(urlBig);
						if (tarFile.exists()) {
							tarFile.delete();
						}
						Session.get(mContext).setDownloadMiniProgramBigIcon(false);
						AppApi.downloadQRCodeBigImg(urlBig,mContext,apiRequestListener,pathBig);
						isNewRequestBig = false;
					}

					break;
				case SP_GET_QRCODE_CALL_JSON:
					if (isNewRequestCall){
						String urlCall = AppApi.API_URLS.get(AppApi.Action.CP_MINIPROGRAM_DOWNLOAD_QRCODE_JSON)+"?box_mac="+ box_mac+"&type="+ ConstantValues.MINI_PROGRAM_CALL_TYPE;
						String pathCall = AppUtils.getFilePath(mContext, AppUtils.StorageFile.cache) + ConstantValues.MINI_PROGRAM_CALL_NAME;
						File tarFile = new File(urlCall);
						if (tarFile.exists()) {
							tarFile.delete();
						}
						Session.get(mContext).setDownloadMiniProgramCallIcon(false);
						AppApi.downloadQRCodeCallImg(urlCall,mContext,apiRequestListener,pathCall);
						isNewRequestCall = false;
					}

					break;
			}
		}

		@Override
		public void onNetworkFailed(AppApi.Action method) {

		}
	};

}

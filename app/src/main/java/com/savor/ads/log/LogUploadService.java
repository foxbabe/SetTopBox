package com.savor.ads.log;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.common.OSSLog;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSPlainTextAKSKCredentialProvider;
import com.savor.ads.BuildConfig;
import com.savor.ads.core.Session;
import com.savor.ads.oss.OSSValues;
import com.savor.ads.oss.ResuambleUpload;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.LogUtils;

import java.io.File;
import java.util.Hashtable;

public class LogUploadService{

	private final static String TAG = "LogUploadSer";
	private static Hashtable<String,String> mLogLocalList = new Hashtable<String,String>();
	private Context context;
	private Session session;
	private OSS oss;
	public LogUploadService(Context context) {
		this.context = context;
		session = Session.get(context);

		initOSSClient();
	}

	private void initOSSClient(){
		OSSCredentialProvider credentialProvider = new OSSPlainTextAKSKCredentialProvider(OSSValues.accessKeyId, OSSValues.accessKeySecret);

		ClientConfiguration conf = new ClientConfiguration();
		conf.setConnectionTimeout(15 * 1000); // 连接超时，默认15秒
		conf.setSocketTimeout(15 * 1000); // socket超时，默认15秒
		conf.setMaxConcurrentRequest(5); // 最大并发请求书，默认5个
		conf.setMaxErrorRetry(2); // 失败后最大重试次数，默认2次
		OSSLog.enableLog();
		oss = new OSSClient(context, BuildConfig.OSS_ENDPOINT, credentialProvider, conf);
	}
	public void start() {

		new Thread() {
			@Override
			public void run() {

				try {
					sleep(1000 * 60*10);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				//只保留loged目录下面当月以及上月的日志
				File[] fileds = getLoged();
				if (fileds != null) {
					for (File file : fileds) {
						if (!file.getName().endsWith(".blog")){
							continue;
						}
						String path = file.getPath();
						String name = file.getName();
						String[] split = name.split("_");
						if (split != null && split.length == 4){
							String logDate = split[3].substring(4, 6);

							String currentDate = AppUtils.getTime("month");
							String month = currentDate.substring(4,6);
							if (Integer.parseInt(logDate) != Integer.parseInt(month)
									&& Integer.parseInt(logDate) != Integer.parseInt(month)-1) {
								boolean delete = file.delete();
								LogUtils.d(delete + "");
							}
						}
					}
				}
				uploadLotteryRecordFile();
				while (true){
					uploadFile();
					try {
						Thread.sleep(1000*5);
//						Thread.sleep(1000*60*60);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			}
		}.start();

	}

	private void uploadLotteryRecordFile(){
		File[] files = getAllLogInfo(AppUtils.StorageFile.lottery);
		if (files!=null&&files.length>0){
			for (final File file:files){
				final String name = file.getName();
				final String path = file.getPath();
				if (file.isFile()) {

					if (name.contains(AppUtils.getTime("date"))){
						continue;
					}
					final String archive = path + ".zip";
					try {
						AppUtils.zipFile(new File(path), new File(archive), name+".zip");
					} catch (Exception e) {
						e.printStackTrace();
					}
					if(new File(archive).exists()){
						final String object_key = archive.substring(1,archive.length());
						String oss_file_path = OSSValues.uploadLotteryPath + name + ".zip";
						new ResuambleUpload(oss,
								BuildConfig.OSS_BUCKET_NAME,
								oss_file_path,
								object_key,
								new LogUploadService.UploadResult() {
									@Override
									public void isSuccessOSSUpload(boolean flag) {
										if (flag){
											file.delete();
										}
										if (new File(archive).exists()){
											new File(archive).delete();
										}
									}
								}).resumableUpload();
					}
				}
			}
		}

	}

	private void uploadFile(){
		File[] files = getAllLogInfo(AppUtils.StorageFile.log);
		if (files!=null&&files.length>0){
			for (File file:files){
				final String name = file.getName();
				final String path = file.getPath();
				if (file.isFile()) {
					String[] split = name.split("_");
					if (split.length!=2){
						continue;
					}
					final String time = split[1].substring(0, split[1].length() - 5);
					if (time.equals(AppUtils.getTime("hour"))){
						continue;
					}
					final String archive = path + ".zip";

					if (/*!TextUtils.isEmpty(session.getOss_bucket())
							&&*/!TextUtils.isEmpty(session.getOss_file_path())) {

						try {
							AppUtils.zipFile(new File(path), new File(archive), name+".zip");
						} catch (Exception e) {
							e.printStackTrace();
						}
						if(new File(archive).exists()){
							final String object_key = archive.substring(1,archive.length());
							String oss_file_path = session.getOss_file_path()+name+".zip";
							new ResuambleUpload(oss,
									BuildConfig.OSS_BUCKET_NAME,
									oss_file_path,
									object_key,
									new LogUploadService.UploadResult() {
										@Override
										public void isSuccessOSSUpload(boolean flag) {
											if (flag){
												afterOSSUpload(name,time);
											}
											if (new File(archive).exists()){
												new File(archive).delete();
											}
										}
									}).resumableUpload();
						}

					}
				}
			}
		}

	}


	/**
	 * 获取log目录下所有日志
	 */
	private File[] getAllLogInfo(AppUtils.StorageFile storage){
		String path = AppUtils.getFilePath(context, storage);
		File[] files = new File(path).listFiles();
		if (files == null || files.length <= 0)
			return null;
		for (File f:files) {
			if (f.isFile()&&f.exists()) {
				String filePath = f.getPath();
				String fileName = f.getName();
				if (fileName.contains(".zip")){
					f.delete();
					continue;
				}
			}
		}
		files = new File(path).listFiles();
		return files;
	}

	/**
	 * 获取日志目录下的文件
	 * @return
     */
	private File[] getLoged() {
		String path = AppUtils.getFilePath(context, AppUtils.StorageFile.loged);
		if (path == null || path.length() <= 0){
			return null;
		}
		File[] files = new File(path).listFiles();
		if (files == null || files.length <= 0)
			return null;
		return files;
	}

	private void afterOSSUpload(String fileName,String time){
		if (TextUtils.isEmpty(fileName)||TextUtils.isEmpty(time)){
			return;
		}
		String filepath = AppUtils.getFilePath(context, AppUtils.StorageFile.log)+fileName;
		String currentTime = AppUtils.getTime("hour");
		if (!time.equals(currentTime)&&new File(filepath).exists()) {
			String deskPath = AppUtils.getFilePath(context, AppUtils.StorageFile.loged);
			new File(filepath).renameTo(new File(deskPath+fileName));
		}

	}


	public interface UploadResult{
		void isSuccessOSSUpload(boolean flag);
	}
}

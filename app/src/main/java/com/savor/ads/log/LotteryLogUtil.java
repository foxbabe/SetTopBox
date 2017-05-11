package com.savor.ads.log;

import android.content.Context;

import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;
import com.savor.ads.utils.GlobalValues;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by zhanghq on 2017/5/11.
 */

public class LotteryLogUtil {

    private FileWriter mWriter = null;

    private Context mContext;
    private Session mSession;

    private static LotteryLogUtil lotteryLogUtil;
    private LotteryLogUtil(Context context) {
        mContext = context;
        mSession = Session.get(mContext);
    }

    public static LotteryLogUtil getInstance(Context context) {
        if (lotteryLogUtil == null) {
            lotteryLogUtil = new LotteryLogUtil(context);
        }
        return lotteryLogUtil;
    }

    public void writeLotteryUpdate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lotteryLogUtil) {
                    if (mWriter == null){
                        createLotteryRecordFile();

                        if (mWriter != null){
                            try {
                                String lotteryLog = System.currentTimeMillis() + "," + mSession.getBoiteId() + "," + mSession.getRoomId() + "," +
                                        mSession.getBoxId() + ",update," + ",,," ;
                                mWriter.write(lotteryLog);
                                closeWriter();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
        }).start();
    }

    public void writeLotteryRecord(final int prizeId, final String prizeName, final String prize_time){
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lotteryLogUtil) {
                    if (mWriter == null){
                        createLotteryRecordFile();

                        if (mWriter != null){
                            try {
                                String lotteryLog = System.currentTimeMillis() + "," + mSession.getBoiteId() + "," + mSession.getRoomId() + "," +
                                        mSession.getBoxId() + ",lottery," + GlobalValues.CURRENT_PROJECT_DEVICE_ID + "," + GlobalValues.CURRENT_PROJECT_DEVICE_NAME +
                                        "," + prizeId + "," + prizeName + "," + prize_time;
                                mWriter.write(lotteryLog);
                                closeWriter();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }
            }
        }).start();

    }

    private void createLotteryRecordFile(){
        String time = AppUtils.getTime("date");
        String recordFileName = time + ".blog";
        String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.lottery);
        try {
            mWriter = new FileWriter(path+recordFileName,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeWriter() {
        if (mWriter != null) {
            try {
                mWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mWriter = null;
        }
    }
}

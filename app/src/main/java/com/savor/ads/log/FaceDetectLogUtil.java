package com.savor.ads.log;

import android.content.Context;

import com.savor.ads.bean.FaceLogBean;
import com.savor.ads.core.Session;
import com.savor.ads.utils.AppUtils;

import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by zhanghq on 2017/5/11.
 */

public class FaceDetectLogUtil {

    private FileWriter mWriter = null;

    private Context mContext;
    private Session mSession;

    private static FaceDetectLogUtil faceDetectUtil;
    private FaceDetectLogUtil(Context context) {
        mContext = context;
        mSession = Session.get(mContext);
    }

    public static FaceDetectLogUtil getInstance(Context context) {
        if (faceDetectUtil == null) {
            faceDetectUtil = new FaceDetectLogUtil(context);
        }
        return faceDetectUtil;
    }

    public void writeFaceRecord(final FaceLogBean bean){
        if (bean != null) {
            synchronized (faceDetectUtil) {
                if (mWriter == null){
                    createFaceRecordFile();

                    if (mWriter != null){
                        try {
                            String faceLog = System.currentTimeMillis() + "|" + mSession.getBoiteId() + "|" + mSession.getRoomId() + "|" +
                                    mSession.getBoxId() + "|" + bean.getUuid() + "|" + bean.getTrackId() + "|" + bean.getStartTime() +
                                    "|" + bean.getEndTime() + "|" + bean.getTotalSeconds() + "|" + bean.getMediaIds() + "\r\n";
                            mWriter.write(faceLog);
                            mWriter.flush();
                            closeWriter();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            }
        }
    }

    private void createFaceRecordFile(){
        String time = AppUtils.getCurTime("yyyyMMddHH");
        String recordFileName = time + "_" + mSession.getEthernetMac() + ".blog";
        String path = AppUtils.getFilePath(mContext, AppUtils.StorageFile.face);
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

package com.savor.ads.utils.tv;

import android.content.Context;
import android.util.Log;

import com.google.gson.annotations.SerializedName;
import com.savor.ads.bean.AtvProgramInfo;
import com.savor.ads.bean.TvProgramResponse;
import com.savor.ads.core.Session;
import com.savor.ads.utils.LogFileUtil;
import com.tvos.atv.AtvManager;
import com.tvos.atv.AtvPlayer;
import com.tvos.atv.AtvScanManager;
import com.tvos.atv.vo.AtvEventScan;
import com.tvos.common.AudioManager;
import com.tvos.common.ChannelManager;
import com.tvos.common.TvManager;
import com.tvos.common.exception.TvCommonException;
import com.tvos.common.vo.ProgramInfo;
import com.tvos.common.vo.ProgramInfoQueryCriteria;
import com.tvos.common.vo.TvOsType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by zhanghq on 2016/12/12.
 */

public class TvOperate {
    private static int ATV_MIN_FREQ = 48250;
    private static int ATV_MAX_FREQ = 877250;
    private static int ATV_EVENT_INTERVAL = 500 * 1000;// every 500ms to show

    //    private TVHandler mhandler = null;

//    public void setTvhandler(TVHandler handler){
//        mhandler = handler;
//    }

    public void switchATVChannel(int channelNum) {

        int count, mchannelNum;
//        mchannelNum = channelNum;
        ChannelManager cm = TvManager.getChannelManager();
        try {
            count = cm.getProgramCount(TvOsType.EnumProgramCountType.E_COUNT_ATV_DTV);
        } catch (TvCommonException e1) {
            e1.printStackTrace();
        }

        try {
            cm.selectProgram(channelNum - 1, (short) 0, 0x00);
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
    }

    public void autoTuning(final AutoTurningCallback turningCallback) {
        makeSourceATV();

        AtvManager.getAtvPlayerManager().setOnAtvPlayerEventListener(new AtvPlayer.OnAtvPlayerEventListener() {

            @Override
            public boolean onSignalUnLock(int arg0) {
                return false;
            }

            @Override
            public boolean onSignalLock(int arg0) {
                return false;
            }

            @Override
            public boolean onAtvProgramInfoReady(int arg0) {
                return false;
            }

            @Override
            public boolean onAtvManualTuningScanInfo(int arg0, AtvEventScan arg1) {
                return false;
            }

            @Override
            public boolean onAtvAutoTuningScanInfo(int what, AtvEventScan extra) {
                if (turningCallback != null) {
                    turningCallback.onProgressUpdate(extra.percent);
                }
                if ((extra.percent >= 100) || (extra.frequencyKHz > ATV_MAX_FREQ)) {
                    interruptTuning();
                    if (turningCallback != null) {
                        turningCallback.onComplete();
                    }
                }

                return false;
            }
        });

        AtvScanManager asm = AtvManager.getAtvScanManager();
        try {
            asm.setAutoTuningStart(ATV_EVENT_INTERVAL, ATV_MIN_FREQ,
                    ATV_MAX_FREQ, AtvScanManager.EnumAutoScanState.E_NONE_NTSC_AUTO_SCAN);
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
    }

    private void makeSourceATV() {
        try {
            if (TvManager.getCurrentInputSource() != TvOsType.EnumInputSource.E_INPUT_SOURCE_ATV) {
                TvManager.setInputSource(TvOsType.EnumInputSource.E_INPUT_SOURCE_ATV);
            }
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
    }

    public void interruptTuning() {

        try {
            AtvManager.getAtvScanManager().setAutoTuningEnd();
            TvManager.getChannelManager().changeToFirstService(
                    ChannelManager.EnumFirstServiceInputType.E_FIRST_SERVICE_ATV,
                    ChannelManager.EnumFirstServiceType.E_DEFAULT);
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
    }
    public AtvProgramInfo[] getAllProgramInfoT() {
        AtvProgramInfo[] progList = new AtvProgramInfo[10];
        for (int i = 0; i < 10; i++) {
            progList[i] = new AtvProgramInfo();
            progList[i].setChannelName("频道" + i);
            progList[i].setChennalNum(i);
            progList[i].setFreq(500 + i);
            progList[i].setAudioStandard(600 + i);
            progList[i].setVideoStandard(700 + i);

        }
        return progList;
    }

    public AtvProgramInfo[] getAllProgramInfo() {
        int count = getAtvProgramCount();
        AtvProgramInfo[] progList = new AtvProgramInfo[count];

        ChannelManager cm = TvManager.getChannelManager();
        AtvScanManager asm = AtvManager.getAtvScanManager();
        for (int i = 0; i < count; i++) {
            AtvProgramInfo pi = new AtvProgramInfo();
            pi.setChennalNum(i);

            ProgramInfoQueryCriteria qc = new ProgramInfoQueryCriteria();
            qc.queryIndex = i;
            try {
                ProgramInfo info = cm.getProgramInfo(qc, TvOsType.EnumProgramInfoType.E_INFO_DATABASE_INDEX);
                pi.setChannelName(info.serviceName);

                pi.setFreq(pllToFreqKHz(asm.getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_PROGRAM_PLL_DATA, i)));
//                int soundindx = AtvManager.getAtvScanManager().getAtvProgramInfo(
//                        AtvScanManager.EnumGetProgramInfo.E_GET_AUDIO_STANDARD, i);
//                pi.audioStandard = AudioManager.EnumAtvSystemStandard.getOrdinalThroughValue(soundindx);
                pi.setAudioStandard(AtvManager.getAtvScanManager().getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_AUDIO_STANDARD, i));
                pi.setVideoStandard(asm.getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_VIDEO_STANDARD_OF_PROGRAM, i));
            } catch (TvCommonException e) {
                e.printStackTrace();
            }
            progList[i] = pi;
        }

        return progList;
    }

    private static int freqKHzToPLL(int freq) {
        return (freq / 50);
    }

    private static int pllToFreqKHz(int freq) {
        return (freq * 50);
    }

    private int getAtvProgramCount() {
        int count = 0;
        ChannelManager cm = TvManager.getChannelManager();
        try {
            count = cm.getProgramCount(TvOsType.EnumProgramCountType.E_COUNT_ATV_DTV);
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
        return count;
    }

    private AtvProgramInfo getProgramInfo(int index) {
        AtvProgramInfo info = new AtvProgramInfo();
        info.setChennalNum(index);

        ChannelManager cm = TvManager.getChannelManager();
        AtvScanManager asm = AtvManager.getAtvScanManager();
        try {
            ProgramInfoQueryCriteria qc = new ProgramInfoQueryCriteria();
            qc.queryIndex = index;
            //		info.audioStandard = EnumAtvSystemStandard.getOrdinalThroughValue(soundindx);
            //		int soundindx = AtvManager.getAtvScanManager().getAtvProgramInfo(EnumGetProgramInfo.E_GET_AUDIO_STANDARD, chennalNum);
            info.setChannelName(cm.getProgramInfo(qc, TvOsType.EnumProgramInfoType.E_INFO_DATABASE_INDEX).serviceName);
            info.setFreq(pllToFreqKHz(asm.getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_PROGRAM_PLL_DATA, index)));
            info.setAudioStandard(AtvManager.getAtvScanManager().getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_AUDIO_STANDARD, index));
            info.setVideoStandard(asm.getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_VIDEO_STANDARD_OF_PROGRAM, index));
        } catch (TvCommonException e) {
            e.printStackTrace();
        }

        return info;
    }

    private boolean writeToDatabase(AtvProgramInfo[] progList) {
        if (progList == null || progList.length == 0) {
            return false;
        }

        AtvScanManager asm = AtvManager.getAtvScanManager();
        AudioManager am = TvManager.getAudioManager();
        try {
            asm.setProgramControl(AtvScanManager.EnumSetProgramCtrl.E_SET_CURRENT_PROGRAM_NUMBER, 0, 0);
            asm.setProgramControl(AtvScanManager.EnumSetProgramCtrl.E_RESET_CHANNEL_DATA, 0, 0);
            for (int i = 0; i < progList.length; i++) {
                progList[i].setChennalNum(progList[i].getChennalNum() - 1);
                asm.setProgramControl(AtvScanManager.EnumSetProgramCtrl.E_SET_CURRENT_PROGRAM_NUMBER, progList[i].getChennalNum(), 0);
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_NEED_AFT, progList[i].getChennalNum(), 0);
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_SKIP_PROGRAM, progList[i].getChennalNum(), 0);
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_LOCK_PROGRAM, progList[i].getChennalNum(), 0);
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_ENABLE_REALTIME_AUDIO_DETECTION, progList[i].getChennalNum(), 0);
                asm.setAtvStationName(progList[i].getChennalNum(), progList[i].getChannelName());
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_SET_AUDIO_STANDARD, progList[i].getChennalNum(), progList[i].getAudioStandard());
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_SET_VIDEO_STANDARD_OF_PROGRAM, progList[i].getChennalNum(), progList[i].getVideoStandard());
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_SET_AFT_OFFSET, progList[i].getChennalNum(), 0);
                asm.setAtvProgramInfo(AtvScanManager.EnumSetProgramInfo.E_SET_PROGRAM_PLL_DATA, progList[i].getChennalNum(), freqKHzToPLL(progList[i].getFreq()));
                am.setAtvMtsMode(AudioManager.EnumAtvAudioModeType.E_ATV_AUDIOMODE_FORCED_MONO);
            }
            asm.setProgramControl(AtvScanManager.EnumSetProgramCtrl.E_SET_CURRENT_PROGRAM_NUMBER, 0, 0);
            return true;
        } catch (TvCommonException e) {
            e.printStackTrace();
        } finally {
        }
        return false;

    }

    private AtvProgramInfo readDatabase(int index) {

        AtvProgramInfo p = new AtvProgramInfo();


        AtvScanManager asm = AtvManager.getAtvScanManager();
        AudioManager am = TvManager.getAudioManager();

        try {
            //	asm.getProgramControl(EnumSetProgramCtrl.E_SET_CURRENT_PROGRAM_NUMBER, 0, 0);
            //	asm.setProgramControl(EnumSetProgramCtrl.E_RESET_CHANNEL_DATA, 0, 0);

            //asm.setProgramControl(EnumSetProgramCtrl.E_SET_CURRENT_PROGRAM_NUMBER, p.chennalNum, 0);
            p.setAudioStandard(asm.getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_AUDIO_STANDARD, index));
            p.setFreq(asm.getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_PROGRAM_PLL_DATA, index));
            p.setVideoStandard(asm.getAtvProgramInfo(AtvScanManager.EnumGetProgramInfo.E_GET_VIDEO_STANDARD_OF_PROGRAM, index));
            p.setChannelName(asm.getAtvStationName(index));
            p.setChennalNum(index);


        } catch (TvCommonException e) {
            e.printStackTrace();
        }


        return p;

    }

    public void readWriteTest() {
        AtvProgramInfo[] oldlist = getAllProgramInfo();
        ArrayList<AtvProgramInfo> newlist = new ArrayList<AtvProgramInfo>(Arrays.asList(oldlist));
        AtvProgramInfo p = getProgramInfo(6);
        p.setChennalNum(newlist.size());
        newlist.add(p);

//		for (AtvProgramInfo info : newlist) {
//			LogUtils.i(("chennalNum=" + info.chennalNum);
//			LogUtils.i(("name=" + info.channelName);
//			LogUtils.i(("freq=" + info.freq);
//			LogUtils.i(("videosystem=" + info.videoStandard);
//			LogUtils.i(("audiosystem=" + info.audioStandard);
//		}

        writeToDatabase(newlist.toArray(new AtvProgramInfo[newlist.size()]));
    }

    public void updateProgram(Context context, TvProgramResponse programResponse) {
//        JSONArray roomList = null;
//        JSONObject jsonObj = null;
//        LogUtils.i("tx", "list " + list);
//
//
//        JSONObject jsonObject = null;
//
//
//        try {
//            jsonObject = new JSONObject(list);
//        } catch (JSONException e2) {
//            e2.printStackTrace();
//        }
//
//
//        try {
//            roomList = jsonObject.getJSONArray("channelInfo");
//            // roomList = new JSONArray(list1);
//            LogUtils.i("tx", "roomList size " + roomList.length());
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//        if (roomList.length() <= 0)
//            return;
        if (programResponse == null || programResponse.getTvChannelList() == null || programResponse.getTvChannelList().isEmpty())
            return;
//        ArrayList<AtvProgramInfo> newlist = new ArrayList<AtvProgramInfo>();
//        for (int i = 0; i < roomList.length(); i++) {
//
//            try {
//                jsonObj = roomList.getJSONObject(i);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//            AtvProgramInfo p = new AtvProgramInfo();//getProgramInfo(0);
//            String chennalNum = jsonObj.optString("chennalNum");
//            String freq = jsonObj.optString("freq");
//            String audioStandard = jsonObj.optString("audioStandard");
//            String videoStandard = jsonObj.optString("videoStandard");
//            p.setChennalNum(Integer.parseInt(chennalNum) - 1);
//            p.setFreq(Integer.parseInt(freq));
//            p.setChannelName(jsonObj.optString("channelName"));
//            p.setAudioStandard(Integer.parseInt(audioStandard));
//            p.setVideoStandard(Integer.parseInt(videoStandard));
//
//            newlist.add(p);
//        }
//        writeToDatabase(newlist.toArray(new AtvProgramInfo[newlist.size()]));
//        String defNum = jsonObject.optString("defaultNum");

        LogFileUtil.write("TvOperate will write channel list To Database");
        writeToDatabase(programResponse.getTvChannelList().toArray(new AtvProgramInfo[programResponse.getTvChannelList().size()]));
        LogFileUtil.write("TvOperate will setTvDefaultChannelNumber");
        Session.get(context).setTvDefaultChannelNumber(programResponse.getLockingChannelNum());
    }

    public TvOsType.EnumInputSource getCurrentInputSource() {
        TvOsType.EnumInputSource curInputSource = TvOsType.EnumInputSource.E_INPUT_SOURCE_NONE;
        try {
            curInputSource = TvManager.getCurrentInputSource();
        } catch (TvCommonException e) {
            e.printStackTrace();
        }
        return curInputSource;
    }

    public interface AutoTurningCallback {
        void onProgressUpdate(int percent);

        void onComplete();
    }
}

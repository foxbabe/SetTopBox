package com.savor.tvlibrary;

import android.os.SystemProperties;
import android.view.SurfaceHolder;

import com.hisilicon.android.tvapi.HitvManager;
import com.hisilicon.android.tvapi.constant.EnumSourceIndex;
import com.hisilicon.android.tvapi.impl.CusExImpl;
import com.hisilicon.android.tvapi.listener.OnChannelScanListener;
import com.hisilicon.android.tvapi.listener.TVMessage;
import com.hisilicon.android.tvapi.vo.ChannelScanInfo;
import com.hisilicon.android.tvapi.vo.RectInfo;
import com.hisilicon.android.tvapi.vo.TvProgram;

import java.util.ArrayList;

/**
 * Created by zhang.haiqiang on 2017/7/19.
 */

class V600TVOperator implements ITVOperator {
    @Override
    public void setDisplay(SurfaceHolder holder) {
        SystemProperties.set("persist.sys.uishow", "0");

        holder.setType(SurfaceHolder.SURFACE_TYPE_HISI_TRANSPARENT);
        //设置显示区域
        RectInfo rect = new RectInfo();
        rect.setX(0);
        rect.setY(0);
        rect.setW(1920);
        rect.setH(1080);
        HitvManager.getInstance().getSourceManager().setWindowRect(rect, 0);
        //TODO 切换信号源就可以显示TV画面
    }

    @Override
    public void switchATVChannel(int channelNum) {
        HitvManager.getInstance().getAtvChannel().selectProg(channelNum);
    }

    MyChannelScanListener mOnChannelScanListener = new MyChannelScanListener();
    @Override
    public void autoTuning(final AutoTurningCallback turningCallback) {
        final int total = HitvManager.getInstance().getAtvChannel().getMaxTuneFreq() - HitvManager.getInstance().getAtvChannel().getMinTuneFreq();
        mOnChannelScanListener.setFreqRange(total);
        mOnChannelScanListener.setTurningCallback(turningCallback);

        HitvManager.getInstance().registerListener(
                TVMessage.HI_TV_EVT_SCAN_PROGRESS, mOnChannelScanListener);
        HitvManager.getInstance().registerListener(
                TVMessage.HI_TV_EVT_SCAN_LOCK, mOnChannelScanListener);
        HitvManager.getInstance().registerListener(
                TVMessage.HI_TV_EVT_SCAN_FINISH, mOnChannelScanListener);
        HitvManager.getInstance().getAtvChannel().exitScan();
        HitvManager.getInstance().getAtvChannel().autoScan();

        CusExImpl.getInstance().cus_set_preset_frequency(0);
    }

    @Override
    public void interruptTuning() {
        HitvManager.getInstance().unregisterListener(
                TVMessage.HI_TV_EVT_SCAN_PROGRESS, mOnChannelScanListener);
        HitvManager.getInstance().unregisterListener(
                TVMessage.HI_TV_EVT_SCAN_LOCK, mOnChannelScanListener);
        HitvManager.getInstance().unregisterListener(
                TVMessage.HI_TV_EVT_SCAN_FINISH, mOnChannelScanListener);
        HitvManager.getInstance().getAtvChannel().exitScan();
    }

    @Override
    public AtvChannel[] getAtvChannels() {
        AtvChannel[] atvChannels = null;
        ArrayList<TvProgram> tvPrograms = HitvManager.getInstance().getAtvChannel().getProgList();
        if (tvPrograms != null) {
            atvChannels = new AtvChannel[tvPrograms.size()];
            for (int i = 0; i < tvPrograms.size(); i++) {
                TvProgram program = tvPrograms.get(i);
                AtvChannel channel = new AtvChannel();
                channel.setChennalNum(i);
                channel.setFreq(program.getlFreq());
                channel.setChannelName(program.getStrName());
                atvChannels[i] = channel;
            }
        }
        return atvChannels;
    }

    @Override
    public void setAtvChannels(AtvChannel[] channels) {
//        HitvManager.getInstance().getAtvChannel().
    }

    @Override
    public TVSignal getCurrentSignalSource() {
        int sourceId = HitvManager.getInstance().getSourceManager().getCurSourceId(0);
        TVSignal signal = TVSignal.ATV;
        switch (sourceId) {
            case EnumSourceIndex.SOURCE_ATV:
                signal = TVSignal.ATV;
                break;
            case EnumSourceIndex.SOURCE_CVBS1:
                signal = TVSignal.AVI;
                break;
            case EnumSourceIndex.SOURCE_HDMI1:
                signal = TVSignal.HDMI;
                break;
        }
        return signal;
    }

    @Override
    public void setSignalSource(TVSignal signal) {
        int source = 0;
        switch (signal) {
            case ATV:
                source = EnumSourceIndex.SOURCE_ATV;
                break;
            case AVI:
                source = EnumSourceIndex.SOURCE_CVBS1;
                break;
            case HDMI:
                source = EnumSourceIndex.SOURCE_HDMI1;
                break;
        }
        HitvManager.getInstance().getSourceManager().deselectSource(HitvManager.getInstance().getSourceManager().getCurSourceId(0), true);
        HitvManager.getInstance().getSourceManager().selectSource(source, 0);
    }

    @Override
    public void exitTv() {
        HitvManager.getInstance().getSourceManager().deselectSource(HitvManager.getInstance().getSourceManager().getCurSourceId(0), true);
        HitvManager.getInstance().getSourceManager().selectSource(13, 0);
        SystemProperties.set("persist.sys.uishow", "1");
    }

    class MyChannelScanListener extends OnChannelScanListener {
        AutoTurningCallback turningCallback;
        int freqRange;

        public void setTurningCallback(AutoTurningCallback turningCallback) {
            this.turningCallback = turningCallback;
        }

        public void setFreqRange(int freqRange) {
            this.freqRange = freqRange;
        }

        @Override
        public void onChannelScanFinish() {
            SystemProperties.set("persist.atv.scanlock", "0");
            if (turningCallback != null) {
                turningCallback.onComplete();
            }
        }

        @Override
        public void onChannelScanStart() {
        }

        @Override
        public void onChannelScanLock(TvProgram arg0) {
            SystemProperties.set("persist.atv.scanlock", "1");
        }

        @Override
        public void onChannelScanProgress(ChannelScanInfo arg0) {
            SystemProperties.set("persist.atv.scanlock", "1");
            if (turningCallback != null) {
                turningCallback.onProgressUpdate(arg0.getCurrFreq() * 100 / freqRange);
            }
        }

    }
}

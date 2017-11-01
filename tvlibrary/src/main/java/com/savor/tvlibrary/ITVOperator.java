package com.savor.tvlibrary;

import android.media.tv.TvView;
import android.view.SurfaceHolder;

import java.util.ArrayList;

/**
 * Created by zhang.haiqiang on 2017/7/19.
 */

public interface ITVOperator {
    void setDisplay(SurfaceHolder holder);
    void switchATVChannel(int channelNum);
    void autoTuning(AutoTurningCallback turningCallback);
    void interruptTuning();
    ArrayList<AtvChannel> getAtvChannels();
    void setAtvChannels(ArrayList<AtvChannel> channels);
    TVSignal getCurrentSignalSource();
    void setSignalSource(TVSignal signal);
    void exitTv(TvView tvView);
}

package com.savor.tvlibrary;

import android.view.SurfaceHolder;

/**
 * Created by zhang.haiqiang on 2017/7/19.
 */

public interface ITVOperator {
    void setDisplay(SurfaceHolder holder);
    void switchATVChannel(int channelNum);
    void autoTuning(AutoTurningCallback turningCallback);
    void interruptTuning();
    AtvChannel[] getAtvChannels();
    void setAtvChannels(AtvChannel[] channels);
    TVSignal getCurrentSignalSource();
    void setSignalSource(TVSignal signal);
    void exitTv();
}

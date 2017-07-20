package com.savor.tvlibrary;

/**
 * 电视频道
 * Created by zhanghq on 2016/12/27.
 */
public class AtvChannel {
    /** 频道序号 */
    private int chennalNum;
    /** 节目频段 */
    private long freq;
    /** 音频标准 */
    private int audioStandard;
    /** 视频标准 */
    private int videoStandard;
    /** 频道名称 */
    private String channelName;

    public int getChennalNum() {
        return chennalNum;
    }

    public void setChennalNum(int chennalNum) {
        this.chennalNum = chennalNum;
    }

    public long getFreq() {
        return freq;
    }

    public void setFreq(long freq) {
        this.freq = freq;
    }

    public int getAudioStandard() {
        return audioStandard;
    }

    public void setAudioStandard(int audioStandard) {
        this.audioStandard = audioStandard;
    }

    public int getVideoStandard() {
        return videoStandard;
    }

    public void setVideoStandard(int videoStandard) {
        this.videoStandard = videoStandard;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}

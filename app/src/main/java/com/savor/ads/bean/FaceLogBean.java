package com.savor.ads.bean;

import com.savor.ads.utils.AppUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by zhang.haiqiang on 2017/7/17.
 */

public class FaceLogBean {
    private String uuid;
    private long newestFrameIndex;
    private long startTime;
    private long endTime;
    private long totalSeconds;
    private int trackId;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }


    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
        this.totalSeconds = (this.endTime - this.startTime) / 1000;
    }

    public long getNewestFrameIndex() {
        return newestFrameIndex;
    }

    public void setNewestFrameIndex(long newestFrameIndex) {
        this.newestFrameIndex = newestFrameIndex;
    }

    public int getTrackId() {
        return trackId;
    }

    public void setTrackId(int trackId) {
        this.trackId = trackId;
    }

    @Override
    public String toString() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return "{" +
                "uuid='" + uuid + '\'' +
                ", trackId=" + trackId + '\'' +
                ", startTime='" + df.format(new Date(startTime)) + '\'' +
                ", endTime='" + df.format(new Date(endTime)) + '\'' +
                ", totalSeconds='" + totalSeconds + '\'' +
                '}';
    }

    public long getTotalSeconds() {
        return totalSeconds;
    }
}

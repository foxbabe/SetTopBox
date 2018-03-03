package com.savor.ads.bean;

/**
 * Created by zhang.haiqiang on 2017/7/17.
 */

public class FaceLogBean {
    private String uuid;
    private long newestFrameIndex;
    private long startTime;
    private long endTime;
    private float totalSeconds;
    private int trackId;
    private String mediaIds;

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
        this.totalSeconds = (this.endTime - this.startTime) / 1000.0f;
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

    public float getTotalSeconds() {
        return totalSeconds;
    }

    public String getMediaIds() {
        return mediaIds;
    }

    public void setMediaIds(String mediaId) {
        this.mediaIds = mediaId;
    }
}

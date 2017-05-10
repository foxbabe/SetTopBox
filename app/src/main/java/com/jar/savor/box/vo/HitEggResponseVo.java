package com.jar.savor.box.vo;

/**
 * Created by zhanghq on 2017/5/8.
 */

public class HitEggResponseVo extends BaseResponse {
    /** 砸蛋进度*/
    private int progress;
    /** 是否已砸开 0否1是*/
    private int done;
    /** 是否中奖 0否1是*/
    private int win;

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getDone() {
        return done;
    }

    public void setDone(int done) {
        this.done = done;
    }

    public int getWin() {
        return win;
    }

    public void setWin(int win) {
        this.win = win;
    }
}

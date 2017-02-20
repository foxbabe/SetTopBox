package com.jar.savor.box.util;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class Constants {
    public static final int PORT = 8080;
    public static final int WHAT_INIT_SHOWIP = 4129;
    public static final int WHAT_VIDEO_RECEPT＿URL = 4130;
    public static final int WHAT_VIDEO_SEEK = 17;
    public static final int WHAT_VIDEO_PAUSE = 18;
    public static final int WHAT_VIDEO_STOP = 19;
    public static final int WHAT_VIDEO_RESTART = 20;
    public static final int WHAT_VIDEO_SPEED = 21;
    public static final int WHAT_VIDEO_START = 22;
    public static final int WHAT_VIDEO_ROTATION = 23;
    public static final int WHAT_IMAGE_RECEPT＿URL = 24;
    public static final int WHAT_IMAGE_SCALE = 25;
    public static final int WHAT_IMAGE_ROTATION = 32;
    public static final int WHAT_VIDEO_COVER = 33;
    public static final String KEY_VIDEO_START = "start";
    public static final String KEY_VIDEO_STOP = "stop";
    public static final String KEY_VIDEO_RESTART = "restart";
    public static final String KEY_VIDEO_SEEK = "seek";
    public static final String KEY_VIDEO_SPEED = "speed";
    public static final String KEY_VIDEO_PAUSE = "pause";
    public static final String KEY_VIDEO_URL = "url";
    public static final String KEY_VIDEO_ROTATION = "rotationvideo";
    public static final String KEY_VIDEO_COVER = "cover";
    public static final String KEY_IMAGE_SCALE = "scale";
    public static final String KEY_IMAGE＿ROTATION = "rotationimage";
    public static final String KEY_IMAGE_URL = "path";

    public Constants() {
    }

    public class Functions {
        public static final String PREPARE = "prepare";
        public static final String SEEK_TO = "seek_to";
        public static final String STOP = "stop";
        public static final String SET_VOLUME = "set_volume";
        public static final String ZOOM = "zoom";
        public static final String ROTATE = "rotate";
        public static final String QUERY = "query";
        public static final String SHOW_VOD_COVER = "show_vod_cover";
        public static final String PLAY = "play";
        public static final String HEART_BEAT = "heart_beat";

        public Functions() {
        }
    }

    public class ResultCode {
        public static final int SUCCESS = 0;
        public static final int FAILED = -1;
        public static final int UNAUTHORIZED = -2;
        public static final int SESSION_NOT_FOUND = -3;
        public static final int CONNECTION_UPPER_LIMIT = -4;
        public static final int SESSION_UPPER_LIMIT = -5;
        public static final int IP_MISTAKE = -6;
        public static final int HOMING_SYSTEM = -7;
        public static final int SESSION_SUCCESS = -8;
        public static final int SESSION_FAILED = -9;

        public ResultCode() {
        }
    }
}

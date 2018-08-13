package com.jar.savor.box.interfaces;

import com.jar.savor.box.vo.CodeVerifyBean;
import com.jar.savor.box.vo.HitEggResponseVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PptRequestVo;
import com.jar.savor.box.vo.PptVideoRequestVo;
import com.jar.savor.box.vo.PrepareResponseVoNew;
import com.jar.savor.box.vo.ResponseT;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.StopResponseVo;
import com.jar.savor.box.vo.VolumeResponseVo;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhanghq on 2016/12/22.
 */

public interface OnRemoteOperationListener {

    PrepareResponseVoNew showVod(String mediaName, String vodType, int position, boolean isFromWeb, boolean isNewDevice);

    PrepareResponseVoNew showImage(int imageType, int rotation, boolean isThumbnail, String seriesId, boolean isNewDevice);

    PrepareResponseVoNew showImage(int imageType, String imageUrl,boolean isThumbnail);
    PrepareResponseVoNew showImage(int imageType, String imageUrl,boolean isThumbnail,String words);

    PrepareResponseVoNew showVideo(String videoPath, int position, boolean isNewDevice);

    PrepareResponseVoNew showEgg(String date, int hunger);

    HitEggResponseVo hitEgg(String projectId);


    SeekResponseVo seek(int position, String projectId);

    /**
     * 控制播放、暂停
     * @param action 0：暂停；
     *               1：播放
     * @return
     */
    PlayResponseVo play(int action, String projectId);

    StopResponseVo stop(String projectId);

    void rstrStop();

    RotateResponseVo rotate(int rotateDegree, String projectId);

    /**
     * 控制音量
     * @param action 音量操作类型
     * 1：静音
     * 2：取消静音
     * 3：音量减
     * 4：音量加
     * @return
     */
    VolumeResponseVo volume(int action, String projectId);

    Object query(String projectId);

    void showCode();

    ResponseT<CodeVerifyBean> verify(String code);

    void showPpt(PptRequestVo currentPptRequest, boolean isNewDevice, String deviceId);

    void showVideoPpt(PptVideoRequestVo currentPptRequest, boolean isNewDevice, String deviceId);

    void showSpecialty(ArrayList<String> mediaPath, int interval, boolean isNewDevice);

    void showGreeting(String word, int template, int duration, boolean isNewDevice);

    void showAdv(ArrayList<String> mediaPath, boolean isNewDevice);

    void showGreetingThenSpecialty(String word, int template, int duration, ArrayList<String> specialtyPaths, int interval, boolean isNewDevice);
}

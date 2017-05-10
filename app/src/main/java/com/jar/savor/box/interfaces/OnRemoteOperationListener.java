package com.jar.savor.box.interfaces;

import com.jar.savor.box.vo.CodeVerifyBean;
import com.jar.savor.box.vo.HitEggResponseVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PrepareRequestVo;
import com.jar.savor.box.vo.PrepareResponseVo;
import com.jar.savor.box.vo.PrepareResponseVoNew;
import com.jar.savor.box.vo.ResponseT;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.StopResponseVo;
import com.jar.savor.box.vo.VolumeResponseVo;

/**
 * Created by zhanghq on 2016/12/22.
 */

public interface OnRemoteOperationListener {
    PrepareResponseVo prepare(PrepareRequestVo var1);

    PrepareResponseVoNew showVod(String mediaName, String vodType, int position, boolean isFromWeb);

    PrepareResponseVoNew showImage(int imageType, int rotation, boolean isThumbnail, String seriesId);

    PrepareResponseVoNew showVideo(String videoPath, int position);

    PrepareResponseVoNew showEgg();

    HitEggResponseVo hitEgg(String projectId);

    SeekResponseVo seek(int position);

    SeekResponseVo seek(int position, String projectId);

    /**
     * 控制播放、暂停
     * @param action 0：暂停；
     *               1：播放
     * @return
     */
    PlayResponseVo play(int action);
    PlayResponseVo play(int action, String projectId);

    StopResponseVo stop();
    StopResponseVo stop(String projectId);

    RotateResponseVo rotate(int rotateDegree);
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
    VolumeResponseVo volume(int action);
    VolumeResponseVo volume(int action, String projectId);

    Object query();
    Object query(String projectId);

    void showCode();

    ResponseT<CodeVerifyBean> verify(String code);
}

package com.jar.savor.box.interfaces;

import com.jar.savor.box.vo.BaseRequestVo;
import com.jar.savor.box.vo.BaseResponse;
import com.jar.savor.box.vo.CheckResponseVo;
import com.jar.savor.box.vo.CoverRequestVo;
import com.jar.savor.box.vo.CoverResponseVo;
import com.jar.savor.box.vo.PlayRequstVo;
import com.jar.savor.box.vo.PlayResponseVo;
import com.jar.savor.box.vo.PrepareRequestVo;
import com.jar.savor.box.vo.PrepareResponseVo;
import com.jar.savor.box.vo.QueryRequestVo;
import com.jar.savor.box.vo.RotateRequestVo;
import com.jar.savor.box.vo.RotateResponseVo;
import com.jar.savor.box.vo.SeekRequestVo;
import com.jar.savor.box.vo.SeekResponseVo;
import com.jar.savor.box.vo.StopRequestVo;
import com.jar.savor.box.vo.StopResponseVo;
import com.jar.savor.box.vo.VolumeRequestVo;
import com.jar.savor.box.vo.VolumeResponseVo;
import com.jar.savor.box.vo.ZoomRequestVo;
import com.jar.savor.box.vo.ZoomResponseVo;

/**
 * Created by zhanghq on 2016/12/22.
 */

public interface OnRemoteOperationListener {
    PrepareResponseVo prepare(PrepareRequestVo var1);

    SeekResponseVo seekTo(SeekRequestVo var1);

    PlayResponseVo play(PlayRequstVo var1);

    StopResponseVo stop(StopRequestVo var1);

    RotateResponseVo rotate(RotateRequestVo var1);

    ZoomResponseVo zoom(ZoomRequestVo var1);

    CoverResponseVo cover(CoverRequestVo var1);

    VolumeResponseVo volume(VolumeRequestVo var1);

    Object query(QueryRequestVo var1);

    CheckResponseVo check();

    void showQrcode(BaseRequestVo req);

    void showProjectionTip();
}

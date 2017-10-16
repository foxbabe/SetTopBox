package com.savor.ads.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 节目单对象
 * Created by bichao on 2017/10/12.
 */

public class ProgramBean implements Serializable{

    private VersionInfo version;
    private List<MediaLibBean> media_lib;

    public VersionInfo getVersion() {
        return version;
    }

    public void setVersion(VersionInfo version) {
        this.version = version;
    }

    public List<MediaLibBean> getMedia_lib() {
        return media_lib;
    }

    public void setMedia_lib(List<MediaLibBean> media_lib) {
        this.media_lib = media_lib;
    }
}

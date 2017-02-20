package com.savor.ads.bean;

import java.io.Serializable;
import java.util.List;

/**
 * Created by bichao on 2016/12/13.
 */

public class OnDemandListBean implements Serializable{

    private String areaid;
    private String period;
    private String type;
    private List<OnDemandBean> media_lib;

    public String getAreaid() {
        return areaid;
    }

    public void setAreaid(String areaid) {
        this.areaid = areaid;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<OnDemandBean> getMedia_lib() {
        return media_lib;
    }

    public void setMedia_lib(List<OnDemandBean> media_lib) {
        this.media_lib = media_lib;
    }
}

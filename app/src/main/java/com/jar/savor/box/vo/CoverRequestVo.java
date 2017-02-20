package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class CoverRequestVo implements Serializable {
    private static final long serialVersionUID = 182333784846454871L;
    private String function;
    private int turnon;
    private String jsonurl;
    private String catagory;
    private int scrollto;

    public CoverRequestVo() {
    }

    public int getTurnon() {
        return this.turnon;
    }

    public void setTurnon(int turnon) {
        this.turnon = turnon;
    }

    public String getJsonurl() {
        return this.jsonurl;
    }

    public void setJsonurl(String jsonurl) {
        this.jsonurl = jsonurl;
    }

    public String getCatagory() {
        return this.catagory;
    }

    public void setCatagory(String catagory) {
        this.catagory = catagory;
    }

    public int getScrollto() {
        return this.scrollto;
    }

    public void setScrollto(int scrollto) {
        this.scrollto = scrollto;
    }

    public String getFunction() {
        return this.function;
    }

    public void setFunction(String function) {
        this.function = function;
    }
}

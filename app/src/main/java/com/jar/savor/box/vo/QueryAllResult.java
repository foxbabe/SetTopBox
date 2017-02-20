package com.jar.savor.box.vo;

import java.io.Serializable;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class QueryAllResult implements Serializable {
    private static final long serialVersionUID = -3448339500345541552L;
    private int sessionid;
    private String status;
    private String assettype;
    private String asseturl;
    private String assetid;
    private String assetname;
    private String sessionstatus;
    private int pos;
    private int zoom;
    private int vol;
    private int rotation;
    private int bufferstatus;

    public QueryAllResult() {
    }

    public int getSessionid() {
        return this.sessionid;
    }

    public void setSessionid(int sessionid) {
        this.sessionid = sessionid;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssettype() {
        return this.assettype;
    }

    public void setAssettype(String assettype) {
        this.assettype = assettype;
    }

    public String getAsseturl() {
        return this.asseturl;
    }

    public void setAsseturl(String asseturl) {
        this.asseturl = asseturl;
    }

    public String getAssetid() {
        return this.assetid;
    }

    public void setAssetid(String assetid) {
        this.assetid = assetid;
    }

    public String getAssetname() {
        return this.assetname;
    }

    public void setAssetname(String assetname) {
        this.assetname = assetname;
    }

    public String getSessionstatus() {
        return this.sessionstatus;
    }

    public void setSessionstatus(String sessionstatus) {
        this.sessionstatus = sessionstatus;
    }

    public int getPos() {
        return this.pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public int getZoom() {
        return this.zoom;
    }

    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    public int getVol() {
        return this.vol;
    }

    public void setVol(int vol) {
        this.vol = vol;
    }

    public int getRotation() {
        return this.rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    public int getBufferstatus() {
        return this.bufferstatus;
    }

    public void setBufferstatus(int bufferstatus) {
        this.bufferstatus = bufferstatus;
    }
}

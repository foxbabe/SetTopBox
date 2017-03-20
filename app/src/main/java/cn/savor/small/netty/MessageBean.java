package cn.savor.small.netty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by DuoDuo on 2016/12/10.
 */
public class MessageBean implements Serializable {

    public static enum Action {
        /**客户端心跳包请求*/
        CLIENT_HEART_REQ,
        /**服务端心跳包回应*/
        SERVER_HEART_RESP,
        /**服务端数据包请求*/
        SERVER_ORDER_REQ,
        /**客户端数据包回应*/
        CLIENT_ORDER_RESP,
        /**客户端数据包请求*/
        CLIENT_ORDER_REQ,
        /**服务端数据包回应*/
        SERVER_ORDER_RESP
    }

    private Action cmd = Action.CLIENT_HEART_REQ;
    private List<String> content = null;
    private String serialnumber = null;
    private String ip = null;
    private String mac = null;
    /**
     * 酒楼ID
     */
    private String hotelId;
    /**
     * 包间ID
     */
    private String roomId;
    /**
     * 机顶盒ID
     */
    private String boxId;
    /**
     * SSID
     */
    private String ssid;
    /**
     * 手机连接码（展示二维码时使用）
     */
    private String connectCode;

    public List<String> getContent() {
        if (this.content == null)
            this.content = new ArrayList<String>();
        return this.content;
    }

    public   String getSerialnumber() {
        return serialnumber;
    }

    public  void setSerialnumber(String serialnumber) {
        this.serialnumber = serialnumber;
    }

    public Action getCmd() {
        return cmd;
    }

    public void setCmd(Action cmd) {
        this.cmd = cmd;
    }

    public void setContent(List<String> content) {
        this.content = content;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getHotelId() {
        return hotelId;
    }

    public void setHotelId(String hotelId) {
        this.hotelId = hotelId;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }
    public String getConnectCode() {
        return connectCode;
    }

    public void setConnectCode(String connectCode) {
        this.connectCode = connectCode;
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }
}

package com.jar.savor.box.session;

import com.jar.savor.box.session.SessionManager;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class SessionBean {
    private long stamp;
    private String ip;
    private boolean alive = true;
    private int SessionID = 1;
    private Thread t = new Thread() {
        public void run() {
            while(com.jar.savor.box.session.SessionBean.this.alive) {
                if(com.jar.savor.box.session.SessionBean.this.isTimeOut()) {
                    SessionManager.getInstance().Remove(com.jar.savor.box.session.SessionBean.this.ip);
                    com.jar.savor.box.session.SessionBean.this.alive = false;
                }
            }

        }
    };

    private SessionBean() {
    }

    public SessionBean(String ip) {
        this.stamp = System.currentTimeMillis();
        this.ip = ip;
        this.t.start();
    }

    public boolean isAlive() {
        return this.alive;
    }

    public void updateStamp() {
        this.stamp = System.currentTimeMillis();
    }

    private boolean isTimeOut() {
        return System.currentTimeMillis() - this.stamp >= 35000L;
    }

    public int getSessionID() {
        return this.SessionID;
    }

    public void setSessionID(int sessionID) {
        this.SessionID = sessionID;
    }
}

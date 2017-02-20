package com.jar.savor.box.session;

import com.jar.savor.box.services.RemoteService;
import com.savor.ads.utils.LogUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class SessionManager {
    private int max;
    private static Map<String, SessionBean> sessions = new HashMap();
    private int cycletime;
    static final int DEADLINE = 35000;
    private int sessionId;

    private SessionManager() {
        this.max = 1;
        this.cycletime = 30;
        this.sessionId = 1;
    }

    public static final SessionManager getInstance() {
        return SessionManager.InstanceHolder.instance.setMax(1);
    }

    public static final SessionManager getInstance(int max) {
        return SessionManager.InstanceHolder.instance.setMax(max <= 1?1:max);
    }

    private SessionManager setMax(int max) {
        this.max = max;
        return this;
    }

    public SessionResult createSession(String ip, String function) {
        if("prepare".equals(function)) {
            return this.create(ip);
        } else if("stop".equals(function)) {
            return this.destroy(ip);
        } else {
            SessionResult sessionResult = new SessionResult();
            ((SessionBean)sessions.get(ip)).updateStamp();
            sessionResult.setStatus(0);
            return sessionResult;
        }
    }

    private SessionResult destroy(String ip) {
        SessionResult sessionResult = new SessionResult();
        if(sessions.containsKey(ip)) {
            sessions.remove(ip);
            sessionResult.setStatus(0);
            return sessionResult;
        } else {
            sessionResult.setStatus(-1);
            return sessionResult;
        }
    }

    private SessionResult create(String ip) {
        this.printSessions();
        SessionResult sessionResult = new SessionResult();
        if(!sessions.isEmpty() && sessions.size() >= this.max) {
            sessionResult.setStatus(-1);
            sessionResult.setMsg("机顶盒连接达到上限,请稍后连接.");
            return sessionResult;
        } else if(sessions.containsKey(ip)) {
            LogUtils.i("要创建的Session" + ip + "已存在，无法创建");
            ((SessionBean)sessions.get(ip)).updateStamp();
            sessionResult.setStatus(0);
            return sessionResult;
        } else {
            SessionBean session = new SessionBean(ip);
            session.setSessionID(this.sessionId++);
            sessions.put(ip, session);
            sessionResult.setStatus(0);
            return sessionResult;
        }
    }

    private void printSessions() {
        if(!sessions.isEmpty()) {
            LogUtils.i("存活的Session个数：" + sessions.size());
            Set entrySet = sessions.entrySet();
            Iterator iterator = entrySet.iterator();
            LogUtils.i("******Session详情*********");

            while(iterator.hasNext()) {
                Map.Entry next = (Map.Entry)iterator.next();
                SessionBean bean = (SessionBean)next.getValue();
                LogUtils.i((String)next.getKey() + ":" + bean.getSessionID());
            }

            LogUtils.i("************************");
        }

    }

    public int getSessionId(String ip) {
        return sessions.containsKey(ip)?((SessionBean)sessions.get(ip)).getSessionID():-1;
    }

    public boolean contains(String ip) {
        return sessions.containsKey(ip);
    }

    public void Remove(String ip) {
        if(sessions.containsKey(ip)) {
            System.err.println("删除Session ： " + ip);
            RemoteService.stop(((SessionBean)sessions.get(ip)).getSessionID());
            sessions.remove(ip);
        }

    }

    private static class InstanceHolder {
        private static final SessionManager instance = new SessionManager();

        private InstanceHolder() {
        }
    }

    public static enum Result {
        DEAL,
        IGNORE;

        private Result() {
        }
    }
}

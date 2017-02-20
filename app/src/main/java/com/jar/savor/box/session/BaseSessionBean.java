package com.jar.savor.box.session;

import java.util.concurrent.CountDownLatch;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class BaseSessionBean extends CountDownLatch {
    protected int SessionID = 1;

    public BaseSessionBean() {
        super(1);
        ++this.SessionID;
    }

    public int getSessionID() {
        return this.SessionID;
    }
}

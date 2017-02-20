package com.jar.savor.box.vo;

import java.io.Serializable;
import java.util.List;

/**
 * Created by zhanghq on 2016/12/22.
 */

public class QueryAllResponseVo implements Serializable {
    private static final long serialVersionUID = 6922151567069094539L;
    private int result;
    private List<QueryAllResult> resultList;

    public QueryAllResponseVo() {
    }

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public List<QueryAllResult> getResultList() {
        return this.resultList;
    }

    public void setResultList(List<QueryAllResult> resultList) {
        this.resultList = resultList;
    }
}

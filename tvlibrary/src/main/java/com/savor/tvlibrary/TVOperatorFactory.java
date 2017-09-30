package com.savor.tvlibrary;

/**
 * Created by zhang.haiqiang on 2017/7/20.
 */

public class TVOperatorFactory {

    public static ITVOperator getTVOperator(TVType tvType) {
        ITVOperator itvOperator = null;
        switch (tvType) {
            case V600:
//                itvOperator = new V600TVOperator();
                break;
            case T966:
                // TODO: add T966 operator
                break;
            case JACK:
                itvOperator = new JackTVOperator();
                break;
        }
        return itvOperator;
    }

    public enum TVType {
        V600,
        T966,
        JACK,
    }
}

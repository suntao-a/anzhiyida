package com.azyd.face.dispatcher.base;


import com.azyd.face.base.RespBase;

import java.util.concurrent.Callable;

/**
 * @author suntao
 * @creat-time 2018/12/4 on 16:59
 * $describe$
 */
public abstract class BaseRequest implements Callable<RespBase>,Comparable<BaseRequest>{
    private int mPriority;

    public BaseRequest(int priority){
        mPriority = priority;
    }
    public int getPriority(){
        return mPriority;
    }

    @Override
    public int compareTo(BaseRequest b) {
        return mPriority-b.mPriority;
    }

}

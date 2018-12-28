package com.azyd.face.base.rxjava;

import com.azyd.face.base.RespBase;
import com.azyd.face.base.exception.ExceptionHandle;
import com.azyd.face.base.exception.RespThrowable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * @author suntao
 * @creat-time 2018/12/26 on 11:05
 * $describe$
 */
public abstract  class SimpleObserver<T extends RespBase> implements Observer<T> {
    private Disposable disposable;
    @Override
    public void onError(Throwable e) {

        if(e instanceof Exception){
            //访问获得对应的Exception
            onError(ExceptionHandle.handleException(e));
        }else {
            //将Throwable 和 未知错误的status code返回
            onError(new RespThrowable(e,ExceptionHandle.ERROR.UNKNOWN));
        }
    }
    public abstract void onError(RespThrowable responeThrowable);
    public abstract void onSuccess(T t);
    public abstract void onFail(T t);
    @Override
    public void onComplete() {

    }
    @Override
    public void onSubscribe(Disposable d) {
        disposable = d;
    }
    @Override
    public void onNext(T responseBase) {
        if(responseBase.isSuccess()){
            onSuccess(responseBase);
        } else {
            onFail(responseBase);
        }
    }
}

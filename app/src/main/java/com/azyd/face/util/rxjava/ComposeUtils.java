package com.azyd.face.util.rxjava;

import com.azyd.face.base.ResponseBase;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 17:36
 * $describe$
 */
public class ComposeUtils {
    public static ObservableTransformer asynSchedule() {
        return new ObservableTransformer() {
            @Override
            public ObservableSource apply(Observable upstream) {
                return upstream.subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .onErrorReturn(new Function<Throwable,ResponseBase>() {

                            @Override
                            public ResponseBase apply(Throwable throwable){
                                ResponseBase responseBase = new  ResponseBase();
                                responseBase.setTraceElements(throwable.getStackTrace());

                                return null;
                            }
                        });
            }

        };
    }
}

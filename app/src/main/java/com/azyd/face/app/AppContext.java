package com.azyd.face.app;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;
import android.widget.Toast;

import com.alibaba.android.arouter.launcher.ARouter;
import com.azyd.face.BuildConfig;
import com.azyd.face.util.MacUtils;
import com.azyd.face.util.Utils;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.tencent.bugly.crashreport.CrashReport;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

/**
 * @author suntao
 * @creat-time 2018/12/12 on 16:19
 * $describe$
 */
public class AppContext extends MultiDexApplication {
    private static AppContext mInstance;
    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        if (BuildConfig.DEBUG) {           // These two lines must be written before init, otherwise these configurations will be invalid in the init process
            ARouter.openLog();     // Print log
            ARouter.openDebug();   // Turn on debugging mode (If you are running in InstantRun mode, you must turn on debug mode! Online version needs to be closed, otherwise there is a security risk)
        }
        String packageName = getPackageName();
        String processName = Utils.getCurProcessName(this);
//        refWatcher = setupLeakCanary();
        ARouter.init(this);
        String strMa = MacUtils.getMobileMAC(getApplicationContext());
        if (MacUtils.ERROR_MAC_STR.equals(strMa)) {
            Toast.makeText(this, "请授予开启wifi权限 以保证正常获取mac", Toast.LENGTH_SHORT).show();
            MacUtils.getStartWifiEnabled();
        }
        //判断进程，进行初始化工作
        if (TextUtils.equals(processName, packageName)) {
            //需要在这里调用
            init();
        }
    }

    private void init() {
        Observable.just(this)
                .map(new Function<Application, Object>() {
                    @Override
                    public Object apply(Application application) throws Exception {
                        CrashReport.initCrashReport(getApplicationContext(), "af671157f5", true);
                        SpeechUtility. createUtility( getApplicationContext(), SpeechConstant.APPID + "=5c3de4c6" );


                        return application;
                    }
                })
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Object>() {
                    @Override
                    public void accept(Object o) throws Exception {
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                    }
                });
    }

    public static AppContext getInstance() {
        return mInstance;
    }

    public void exit(){
        System.exit(0);
    }

//    private RefWatcher setupLeakCanary() {
//        if (LeakCanary.isInAnalyzerProcess(this)) {
//            return RefWatcher.DISABLED;
//        }
//        return LeakCanary.install(this);
//    }
//
//    public static RefWatcher getRefWatcher(Context context) {
//        AppContext leakApplication = (AppContext) context.getApplicationContext();
//        return leakApplication.refWatcher;
//    }

}

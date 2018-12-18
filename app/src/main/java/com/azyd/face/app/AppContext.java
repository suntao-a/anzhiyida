package com.azyd.face.app;

import android.support.multidex.MultiDexApplication;
import android.text.TextUtils;

import com.alibaba.android.arouter.launcher.ARouter;
import com.azyd.face.BuildConfig;

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
        ARouter.init(this);
    }

    public static AppContext getInstance() {
        return mInstance;
    }
}

package com.azyd.face.app;

import android.support.multidex.MultiDexApplication;
import android.widget.Toast;

import com.alibaba.android.arouter.launcher.ARouter;
import com.azyd.face.BuildConfig;
import com.azyd.face.util.MacUtils;

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
//        refWatcher = setupLeakCanary();
        ARouter.init(this);
        String strMa = MacUtils.getMobileMAC(getApplicationContext());
        if (MacUtils.ERROR_MAC_STR.equals(strMa)) {
            Toast.makeText(this, "请授予开启wifi权限 以保证正常获取mac", Toast.LENGTH_SHORT).show();
            MacUtils.getStartWifiEnabled();
        }

    }

    public static AppContext getInstance() {
        return mInstance;
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

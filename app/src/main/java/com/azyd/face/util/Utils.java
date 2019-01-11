package com.azyd.face.util;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * @author suntao
 * @creat-time 2019/1/10 on 14:27
 * $describe$
 */
public class Utils {
    /**
     * 获取当前进程名称
     *
     * @param context
     * @return
     * @author bruce.zhang
     */
    public static String getCurProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager mActivityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfos = mActivityManager.getRunningAppProcesses();
        if (runningAppProcessInfos != null) {
            for (ActivityManager.RunningAppProcessInfo appProcess : runningAppProcessInfos) {
                if (appProcess.pid == pid) {
                    return appProcess.processName;
                }
            }
        }
        return null;
    }
}

package com.azyd.face.app;

import android.iandos.IandosManager;

/**
 * @author suntao
 * @creat-time 2018/12/25 on 17:47
 * $describe$
 */
public class AppInternal {
    Integer StrangerDetectCount=6;
    String IMEI;
    IandosManager iandosManager;
    private static class AppInternalHolder {
        static AppInternal instance = new AppInternal();
    }

    public static AppInternal getInstance() {
        return AppInternalHolder.instance;
    }

    public String getIMEI() {
        return IMEI;
    }

    public void setIMEI(String IMEI) {
        this.IMEI = IMEI;
    }

    public IandosManager getIandosManager() {
        return iandosManager;
    }

    public void setIandosManager(IandosManager iandosManager) {
        this.iandosManager = iandosManager;
    }

    public Integer getStrangerDetectCount() {
        return StrangerDetectCount;
    }

    public void setStrangerDetectCount(Integer strangerDetectCount) {
        StrangerDetectCount = strangerDetectCount;
    }
}

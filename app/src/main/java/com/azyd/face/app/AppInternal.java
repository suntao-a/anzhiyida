package com.azyd.face.app;

/**
 * @author suntao
 * @creat-time 2018/12/25 on 17:47
 * $describe$
 */
public class AppInternal {
    String IMEI;

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
}

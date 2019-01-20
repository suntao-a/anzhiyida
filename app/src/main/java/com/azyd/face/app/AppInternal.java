package com.azyd.face.app;

import android.iandos.IandosManager;

/**
 * @author suntao
 * @creat-time 2018/12/25 on 17:47
 * $describe$
 */
public class AppInternal {


    String IMEI;
    IandosManager iandosManager;

    String sdkIP;
    String serviceIP;
    int previewThreshold;
    int idcardThreshold;
    int inOut;
    int StrangerKeepTimes;//陌生人保存缓存时长
    int StrangerCompareTimes;//陌生人检测次数上报

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

    public String getSdkIP() {
        return sdkIP;
    }

    public void setSdkIP(String sdkIP) {
        this.sdkIP = sdkIP;
    }

    public String getServiceIP() {
        return serviceIP;
    }

    public void setServiceIP(String serviceIP) {
        this.serviceIP = serviceIP;
    }

    public int getPreviewThreshold() {
        return previewThreshold;
    }

    public void setPreviewThreshold(int previewThreshold) {
        this.previewThreshold = previewThreshold;
    }

    public int getIdcardThreshold() {
        return idcardThreshold;
    }

    public void setIdcardThreshold(int idcardThreshold) {
        this.idcardThreshold = idcardThreshold;
    }

    public int getInOut() {
        return inOut;
    }

    public void setInOut(int inOut) {
        this.inOut = inOut;
    }

    public int getStrangerKeepTimes() {
        return StrangerKeepTimes;
    }

    public void setStrangerKeepTimes(int strangerKeepTimes) {
        StrangerKeepTimes = strangerKeepTimes;
    }

    public int getStrangerCompareTimes() {
        return StrangerCompareTimes;
    }

    public void setStrangerCompareTimes(int strangerCompareTimes) {
        StrangerCompareTimes = strangerCompareTimes;
    }
}

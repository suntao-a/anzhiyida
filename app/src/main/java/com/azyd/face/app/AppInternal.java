package com.azyd.face.app;

import android.iandos.IandosManager;

/**
 * @author suntao
 * @creat-time 2018/12/25 on 17:47
 * $describe$
 */
public class AppInternal {

    Integer strangerDetectCount = 6;
    int strangerFaceKeepTimes = 5;
    String IMEI;
    IandosManager iandosManager;

    String sdkIP;
    String serviceIP;
    int previewThreshold;
    int idcardThreshold;
    int inOut;
    int StrangerFaceKeepTimes;//陌生人保存缓存时长
    int StrangerDetectCount;//陌生人检测次数上报

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
        return strangerDetectCount;
    }

    public void setStrangerDetectCount(Integer strangerDetectCount) {
        strangerDetectCount = strangerDetectCount;
    }

    public int getStrangerFaceKeepTimes() {
        return strangerFaceKeepTimes;
    }

    public void setStrangerFaceKeepTimes(int strangerFaceKeepTimes) {
        this.strangerFaceKeepTimes = strangerFaceKeepTimes;
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

    public void setStrangerDetectCount(int strangerDetectCount) {
        StrangerDetectCount = strangerDetectCount;
    }
}

package com.idcard;

import android.graphics.Bitmap;

import com.huashi.otg.sdk.HSIDCardInfo;

/**
 * @author suntao
 * @creat-time 2019/1/3 on 16:15
 * $describe$
 */
public class MyHSIDCardInfo extends HSIDCardInfo {
    byte[] faceBmp;
    int width;
    int height;
    public byte[] getFaceBmp() {
        return faceBmp;
    }

    public MyHSIDCardInfo setFaceBmp(byte[] faceBmp) {
        this.faceBmp = faceBmp;
        return this;
    }

    public int getWidth() {
        return width;
    }

    public MyHSIDCardInfo setWidth(int with) {
        this.width = with;
        return this;
    }

    public int getHeight() {
        return height;
    }

    public MyHSIDCardInfo setHeight(int height) {
        this.height = height;
        return this;
    }
}

package com.idcard;

import android.graphics.Bitmap;

import com.huashi.otg.sdk.HSIDCardInfo;

/**
 * @author suntao
 * @creat-time 2019/1/3 on 16:15
 * $describe$
 */
public class MyHSIDCardInfo extends HSIDCardInfo {
    Bitmap faceBmp;

    public Bitmap getFaceBmp() {
        return faceBmp;
    }

    public void setFaceBmp(Bitmap faceBmp) {
        this.faceBmp = faceBmp;
    }
}

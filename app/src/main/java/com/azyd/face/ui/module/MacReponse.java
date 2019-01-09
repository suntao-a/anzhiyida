package com.azyd.face.ui.module;

import com.azyd.face.base.IProguardKeeper;

/**
 * @author suntao
 * @creat-time 2019/1/9 on 16:11
 * $describe$
 */
public class MacReponse implements IProguardKeeper {
    String mac;
    String msg;
    boolean success;

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}

package com.azyd.face.base.exception;

/**
 * @author suntao
 * @creat-time 2018/12/26 on 14:43
 * $describe$
 */
public class RespThrowable extends Exception {
    int code;
    String message;

    public RespThrowable(Throwable throwable, int code) {
        super(throwable);
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

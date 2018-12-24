package com.azyd.face.base;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:50
 * $describe$
 */
public class ResponseBase {
    StackTraceElement[] mTraceElements;
    int code;
    String message;
    public ResponseBase(){

    }
    public ResponseBase(int code,String message){
        this.code = code;
        this.message = message;
    }
    public StackTraceElement[] getTraceElements() {
        return mTraceElements;
    }

    public void setTraceElements(StackTraceElement[] traceElements) {
        mTraceElements = traceElements;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

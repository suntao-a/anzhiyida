package com.azyd.face.base;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:50
 * $describe$
 */
public class RespBase implements IProguardKeeper{
    StackTraceElement[] mTraceElements;
    int code;
    String message;
    String voice;
    public RespBase(){

    }
    public RespBase(int code, String message){
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

    public String getVoice() {
        return voice;
    }

    public void setVoice(String voice) {
        this.voice = voice;
    }

    public boolean isSuccess(){
        if(code==200){
            return true;
        }
        return false;
    }
}

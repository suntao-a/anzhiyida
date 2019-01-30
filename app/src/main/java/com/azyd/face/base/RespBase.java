package com.azyd.face.base;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:50
 * $describe$
 */
public class RespBase implements IProguardKeeper{
    String rightTopMsg;
    int code;
    String message;
    String voice;
    public RespBase(){

    }
    public RespBase(int code, String message){
        this.code = code;
        this.message = message;
    }
    public RespBase(int code, String message,String rightTopMsg){
        this.code = code;
        this.message = message;
        this.rightTopMsg = rightTopMsg;
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

    public String getRightTopMsg() {
        return rightTopMsg;
    }

    public void setRightTopMsg(String rightTopMsg) {
        this.rightTopMsg = rightTopMsg;
    }

    public boolean isSuccess(){
        if(code==200){
            return true;
        }
        return false;
    }
}

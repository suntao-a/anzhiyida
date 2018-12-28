package com.azyd.face.base.exception;

/**
 * @author suntao
 * @creat-time 2018/12/27 on 14:18
 * $describe$
 */
public class ServerException extends Exception{
    int code;
    String message;
    public ServerException(int code,String message){
        this.code = code;
        this.message = message;
    }
}

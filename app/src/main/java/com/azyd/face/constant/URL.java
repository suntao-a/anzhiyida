package com.azyd.face.constant;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:58
 * $describe$
 */
public class URL {
    public static final String BASE = "http://192.168.1.52:8081/api/";

    public static final String CHECK_REGIST = BASE + "cmRegister/findFacTerminal";
    public static final String FACE_COMPARE_1_N = BASE + "cmAuthentication/faceRecognition";
    public static final String PASS_RECORD_NOCARD = BASE + "cmAuthentication/nocardPassRecord";
    public static final String PASS_RECORD_PREVIEW = BASE + "cmAuthentication/pushPassRecord";
    public static final String PASS_RECORD_IDCARD = BASE + "cmAuthentication/pushCardPassRecord";


}

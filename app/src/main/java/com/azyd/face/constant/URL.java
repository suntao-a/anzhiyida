package com.azyd.face.constant;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:58
 * $describe$
 */
public class URL {
    public static final String BASE = "http://4fc292561c854f50.natapp.cc/";
//    public static final String BASE = "http://8wr7rx.natappfree.cc";
//    public static final String BASE = "http://192.168.1.74:8081/";

    public static final String CHECK_REGIST = BASE + "api/cmRegister/findFacTerminal";
    public static final String FACE_COMPARE_1_N = BASE + "api/cmAuthentication/faceRecognition";
    public static final String PASS_RECORD_NOCARD = BASE + "api/cmAuthentication/nocardPassRecord";
    public static final String PASS_RECORD_PREVIEW = BASE + "api/cmAuthentication/pushPassRecord";
    public static final String PASS_RECORD_IDCARD = BASE + "api/cmAuthentication/pushCardPassRecord";


}

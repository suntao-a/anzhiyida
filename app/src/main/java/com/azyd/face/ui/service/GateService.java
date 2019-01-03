package com.azyd.face.ui.service;

import com.azyd.face.base.RespBase;
import com.azyd.face.constant.URL;
import com.azyd.face.ui.module.Compare1nReponse;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:51
 * $describe$
 */
public interface GateService {
    @POST(URL.CHECK_REGIST)
    Call<RespBase> checkRegist(@Body Map<String, Object> params);
    @POST(URL.FACE_COMPARE_1_N)
    Call<Compare1nReponse> compare1N(@Body Map<String, Object> params);

    @POST(URL.PASS_RECORD_PREVIEW)
    Call<RespBase> passRecordPreview(@Body Map<String, Object> params);

    @POST(URL.PASS_RECORD_NOCARD)
    Call<RespBase> passRecordNoCard(@Body Map<String, Object> params);

    @POST(URL.PASS_RECORD_IDCARD)
    Call<RespBase> passRecordIDCard(@Body Map<String, Object> params);

    @FormUrlEncoded
    @POST("http://127.0.0.1:8080/device/opendoor")
    Call<Map<String,Object>> openDoor(@FieldMap Map<String,Object> params);


}

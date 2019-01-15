package com.azyd.face.ui.service;

import com.azyd.face.base.RespBase;
import com.azyd.face.constant.URL;
import com.azyd.face.ui.module.Compare1nReponse;
import com.azyd.face.ui.module.MacReponse;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Url;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:51
 * $describe$
 */
public interface GateService {
    @POST
    Call<RespBase> checkRegist(@Url String url,@Body Map<String, Object> params);
    @POST
    Call<Compare1nReponse> compare1N(@Url String url,@Body Map<String, Object> params);

    @POST
    Call<RespBase> passRecordPreview(@Url String url,@Body Map<String, Object> params);

    @POST
    Call<RespBase> passRecordNoCard(@Url String url,@Body Map<String, Object> params);
//    @Headers("Cache-Control: max-age=560000")
    @POST
    Call<RespBase> passRecordIDCard(@Url String url,@Body Map<String, Object> params);

    @FormUrlEncoded
    @POST("http://127.0.0.1:8080/device/opendoor")
    Call<Map<String,Object>> openDoor(@FieldMap Map<String,Object> params);

    @GET("http://127.0.0.1:8080/device/info/mac")
    Call<MacReponse> getMac();
}

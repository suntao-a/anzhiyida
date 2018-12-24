package com.azyd.face.ui.service;

import com.azyd.face.base.ResponseBase;
import com.azyd.face.constant.URL;

import java.util.Map;

import io.reactivex.Observable;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * @author suntao
 * @creat-time 2018/12/24 on 14:51
 * $describe$
 */
public interface GateService {
    @FormUrlEncoded
    @POST(URL.CHECK_REGIST)
    Observable<ResponseBase> checkRegist(@Part Map<String, String> params);
}

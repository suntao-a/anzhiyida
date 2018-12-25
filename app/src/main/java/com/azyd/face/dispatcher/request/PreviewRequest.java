package com.azyd.face.dispatcher.request;


import android.util.Base64;

import com.azyd.face.base.ResponseBase;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.dispatcher.core.FaceListManager;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.module.Compare1nReponse;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.RequestParam;
import com.idfacesdk.FACE_DETECT_RESULT;

import java.io.IOException;

import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

/**
 * @author suntao
 * @creat-time 2018/12/4 on 17:13
 * $describe$
 */
public class PreviewRequest extends BaseRequest {
    private byte[] mFeatureData;
    private byte[] mFaceData;
    private int width;
    private int height;

    private FACE_DETECT_RESULT mFaceDetectResult;
    public PreviewRequest(){
        super(0);

    }
    public PreviewRequest setFeatureData(byte[] featureData){
        mFeatureData = featureData;
        return this;
    }
    public PreviewRequest setFaceData(byte[] faceData){
        mFaceData = faceData;
        return this;
    }
    public PreviewRequest setFaceDetectData(FACE_DETECT_RESULT detectResult){
        mFaceDetectResult = detectResult;
        return this;
    }
    public PreviewRequest setSize(int w,int h){
        width = w;
        height = h;
        return this;
    }
    @Override
    public String call() {
        //对比本地列表
        boolean have = FaceListManager.getInstance().contains(mFeatureData);
        if(have){
            //本地列表已有,放弃此次请求
            return "0";
        }
        //没有,就和服务端通信比对


        try {

            final GateService gateService = ServiceGenerator.createService(GateService.class);
            gateService.compare1N(RequestParam.build().with("feature",Base64.encode(mFeatureData,Base64.DEFAULT))
                    .with("library",new String[]{"1900000000","2000000000"})
                    .with("threshold",CameraConstant.getDefaultCameraParam().getFeatureQualityPass())
                    .with("resultNum",1).create())
                     .flatMap(new Function<Compare1nReponse, ObservableSource<ResponseBase>>() {
                         @Override
                         public ObservableSource<ResponseBase> apply(Compare1nReponse compare1nReponse) throws Exception {
                             if(compare1nReponse.isSuccess()){
                                 return gateService.passRecordPreview();
                             }
                             return null;
                         }
                     });


        } catch (IOException e) {
            e.printStackTrace();
        }
        have = true;//服务端结果
        if(!have){
            return "0";
        }
        //服务端有
        //加入队列
        FaceListManager.getInstance().put(mFeatureData);
        //开门

        //上报通行记录


        return "0";
    }
}

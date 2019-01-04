package com.azyd.face.ui.request;


import android.util.Base64;
import android.util.Log;

import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.base.exception.ExceptionHandle;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.constant.ErrorCode;
import com.azyd.face.constant.PassType;
import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.dispatcher.core.FaceListManager;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.module.Compare1nReponse;
import com.azyd.face.ui.module.PersonInfo;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.RequestParam;
import com.idfacesdk.FACE_DETECT_RESULT;

import java.util.Map;

/**
 * @author suntao
 * @creat-time 2018/12/4 on 17:13
 * $describe$
 */
public class PreviewRequest extends BaseRequest {
    private final String TAG="PreviewRequest";
    private byte[] mFeatureData;
    private byte[] mFaceData;
    private int width;
    private int height;

    private FACE_DETECT_RESULT mFaceDetectResult;

    public PreviewRequest() {
        super(0);

    }

    public PreviewRequest setFeatureData(byte[] featureData) {
        mFeatureData = featureData;
        return this;
    }

    public PreviewRequest setFaceData(byte[] faceData) {
        mFaceData = faceData;
        return this;
    }

    public PreviewRequest setFaceDetectResult(FACE_DETECT_RESULT detectResult) {
        mFaceDetectResult = detectResult;
        return this;
    }

    public PreviewRequest setImageSize(int w, int h) {
        width = w;
        height = h;
        return this;
    }

    @Override
    public RespBase call() {
        //对比本地列表
        boolean have = FaceListManager.getInstance().contains(mFeatureData);
        if (have) {
            //本地列表已有,放弃此次请求
            RespBase respBase = new RespBase(200,null);
            return respBase;
        }
        //没有,就和服务端通信比对
        try {
            final GateService gateService = ServiceGenerator.createService(GateService.class);
            Compare1nReponse compare1nReponse = gateService.compare1N(RequestParam.build().with("feature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                    .with("library", new String[]{"1900000000", "2000000000"})
                    .with("mac", AppInternal.getInstance().getIMEI())
                    .with("threshold", CameraConstant.getCameraParam().getFeatureQualityPass())
                    .with("resultNum", 1).create()).execute().body();
            if (compare1nReponse.isSuccess() && compare1nReponse.getContent() != null && compare1nReponse.getContent().getPersonInfo().size() > 0) {
                PersonInfo personInfo = compare1nReponse.getContent().getPersonInfo().get(0);
                //服务端有
                //加入队列
                FaceListManager.getInstance().put(mFeatureData);

                //上报通行记录
                RespBase resp = gateService.passRecordPreview(RequestParam.build().with("mac", AppInternal.getInstance().getIMEI())
                        .with("passType", PassType.FACE)
                        .with("passPersonId", personInfo.getId())
                        .with("verifyPhoto", Base64.encodeToString(mFaceData, Base64.DEFAULT))
                        .with("verifyFeature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                        .with("verifyThreshold", CameraConstant.getCameraParam().getFeatureQualityPass())
                        .with("verifyScore", personInfo.getScore())
                        .with("passPicFaceX", mFaceDetectResult.nFaceLeft / (float) width)
                        .with("passPicFaceY", mFaceDetectResult.nFaceTop / (float) height)
                        .with("passPicFaceWidth", (mFaceDetectResult.nFaceRight - mFaceDetectResult.nFaceLeft) / (float) width)
                        .with("passPicFaceHeight", (mFaceDetectResult.nFaceBottom - mFaceDetectResult.nFaceTop) / (float) height)
                        .create()).execute().body();
                if(resp.isSuccess()){
                    //开门
                    AppInternal.getInstance().getIandosManager().ICE_DoorSwitch(true,true);
//                    Map<String,Object> rf= gateService.openDoor(RequestParam.build().with("open",true).with("reverse",true).create()).execute().body();
                }
                return resp;

            } else {
                //服务端没有，结束

                FaceListManager.getInstance().put(mFeatureData);
                RespBase respBase = new RespBase(ErrorCode.WARING,"服务端没有此人信息");
                return respBase;
            }



        } catch (Exception e) {
            Log.e(TAG, "call: ", e);
            return new RespBase(ErrorCode.SYSTEM_ERROR,"核验主机故障");
        }



    }
}

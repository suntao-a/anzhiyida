package com.azyd.face.ui.request;

import android.util.Base64;

import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.base.exception.ExceptionHandle;
import com.azyd.face.base.exception.RespThrowable;
import com.azyd.face.constant.PassType;
import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.dispatcher.core.FaceListManager;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.RequestParam;
import com.idfacesdk.FACE_DETECT_RESULT;

/**
 * @author suntao
 * @creat-time 2018/12/26 on 15:33
 * $describe$
 */
public class CapturePhotoRequest extends BaseRequest {
    private byte[] mFeatureData;
    private byte[] mFaceData;
    private int width;
    private int height;

    private FACE_DETECT_RESULT mFaceDetectResult;

    public CapturePhotoRequest() {
        super(1);

    }

    public CapturePhotoRequest setFeatureData(byte[] featureData) {
        mFeatureData = featureData;
        return this;
    }

    public CapturePhotoRequest setFaceData(byte[] faceData) {
        mFaceData = faceData;
        return this;
    }

    public CapturePhotoRequest setFaceDetectResult(FACE_DETECT_RESULT detectResult) {
        mFaceDetectResult = detectResult;
        return this;
    }

    public CapturePhotoRequest setImageSize(int w, int h) {
        width = w;
        height = h;
        return this;
    }

    @Override
    public RespBase call() {
        //对比本地列表
//        boolean have = FaceListManager.getInstance().contains(mFeatureData);
//        if (have) {
//            //本地列表已有,放弃此次请求
//            return null;
//        }
        //没有,就和服务端通信比对
        try {
            final GateService gateService = ServiceGenerator.createService(GateService.class);
            RespBase response = gateService.passRecordNoCard(RequestParam.build().with("mac", AppInternal.getInstance().getIMEI())
                    .with("passType", PassType.DYNAMIC_NORMAL)
                    .with("verifyPhoto", Base64.encode(mFaceData, Base64.DEFAULT))
                    .with("verifyFeature", Base64.encode(mFeatureData, Base64.DEFAULT))
                    .with("passPicFaceX", mFaceDetectResult.nFaceLeft / (float) width)
                    .with("passPicFaceY", mFaceDetectResult.nFaceTop / (float) height)
                    .with("passPicFaceWidth", (mFaceDetectResult.nFaceRight - mFaceDetectResult.nFaceLeft) / (float) width)
                    .with("passPicFaceHeight", (mFaceDetectResult.nFaceBottom - mFaceDetectResult.nFaceTop) / (float) height)
                    .create()).execute().body();
            return response;


        } catch (Exception e) {
            RespThrowable throwable = ExceptionHandle.handleException(e);
            return new RespBase(200,throwable.getMessage());
        }

    }
}

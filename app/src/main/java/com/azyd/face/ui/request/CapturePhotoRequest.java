package com.azyd.face.ui.request;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.constant.ErrorCode;
import com.azyd.face.constant.PassType;
import com.azyd.face.constant.URL;
import com.azyd.face.dispatcher.SingleDispatcher;
import com.azyd.face.dispatcher.base.BaseRequest;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.ImageUtils;
import com.azyd.face.util.RequestParam;
import com.idfacesdk.FACE_DETECT_RESULT;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author suntao
 * @creat-time 2018/12/26 on 15:33
 * $describe$
 */
public class CapturePhotoRequest extends BaseRequest {
    private final String TAG="CapturePhotoRequest";
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
            Bitmap detectface = ImageUtils.rgb2Bitmap(mFaceData,width,height);
            String detectfacebase64 = ImageUtils.Bitmap2StrByBase64(detectface);
            detectface.recycle();
            detectface = null;
            final GateService gateService = ServiceGenerator.createService(GateService.class);
            RespBase response = gateService.passRecordNoCard(AppInternal.getInstance().getServiceIP() + URL.PASS_RECORD_NOCARD,RequestParam.build().with("mac", AppInternal.getInstance().getIMEI())
                    .with("inOut", AppInternal.getInstance().getInOut())
                    .with("passType", PassType.DYNAMIC_NORMAL)
                    .with("verifyPhoto", detectfacebase64)
                    .with("verifyFeature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                    .with("passPicFaceX", mFaceDetectResult.nFaceLeft / (float) width)
                    .with("passPicFaceY", mFaceDetectResult.nFaceTop / (float) height)
                    .with("passPicFaceWidth", (mFaceDetectResult.nFaceRight - mFaceDetectResult.nFaceLeft) / (float) width)
                    .with("passPicFaceHeight", (mFaceDetectResult.nFaceBottom - mFaceDetectResult.nFaceTop) / (float) height)
                    .create()).execute().body();
            if (response.isSuccess()) {
                //开门
                AppInternal.getInstance().getIandosManager().ICE_DoorSwitch(true, true);
                RespBase resp = new RespBase(ErrorCode.PLEASE_PASS, "请通行");
                return resp;
            } else {
                if(response.getCode()==500){
                    return new RespBase(ErrorCode.SERVER_ERROR, "核验主机故障");
                } else {
                    return response;
                }
            }


        } catch (Exception e) {
            Log.e(TAG, "call: ", e);
            return new RespBase(ErrorCode.SERVER_ERROR,"核验主机故障");
        } finally {
            System.gc();

            Observable.timer(2, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) {
                            SingleDispatcher.getInstance().getObservable().onNext(new RespBase(ErrorCode.EVENT_CAPTURE_REQUEST_COMPLETED,null));
                        }
                    });
        }

    }
}

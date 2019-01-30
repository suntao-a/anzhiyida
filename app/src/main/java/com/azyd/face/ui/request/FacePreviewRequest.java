package com.azyd.face.ui.request;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.azyd.face.R;
import com.azyd.face.app.AppContext;
import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.constant.ErrorCode;
import com.azyd.face.constant.PassType;
import com.azyd.face.constant.URL;
import com.azyd.face.dispatcher.base.BaseRequest;
import com.azyd.face.dispatcher.base.FaceListManager;
import com.azyd.face.dispatcher.base.StrangerListManager;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.module.Compare1nReponse;
import com.azyd.face.ui.module.PersonInfo;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.ImageUtils;
import com.azyd.face.util.RequestParam;
import com.idfacesdk.FACE_DETECT_RESULT;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

public class FacePreviewRequest extends BaseRequest {
    private final String TAG = "PreviewRequest";
    private byte[] mFeatureData;
    private byte[] mFaceData;
    private int width;
    private int height;
    private FACE_DETECT_RESULT mFaceDetectResult;

    public FacePreviewRequest() {
        super(0);

    }

    public FacePreviewRequest setFeatureData(byte[] featureData) {
        mFeatureData = featureData;
        return this;
    }

    public FacePreviewRequest setFaceData(byte[] faceData) {
        mFaceData = faceData;
        return this;
    }

    public FacePreviewRequest setFaceDetectResult(FACE_DETECT_RESULT detectResult) {
        mFaceDetectResult = detectResult;
        return this;
    }

    public FacePreviewRequest setImageSize(int w, int h) {
        width = w;
        height = h;
        return this;
    }

    @Override
    public RespBase call() {
        try {
            System.gc();
            //对比本地列表,同一个人1秒请求一次
            boolean have = FaceListManager.getInstance().contains(mFeatureData);
            if (have) {
                //本地同人列表已有,放弃此次请求
                return new RespBase(ErrorCode.NONE_THING_TODO, null);
            }
            FaceListManager.getInstance().put(mFeatureData,1);
            //和服务端通信比对
            Bitmap detectface = ImageUtils.rgb2Bitmap(mFaceData, width, height);
            String detectfacebase64 = ImageUtils.Bitmap2StrByBase64(detectface);
            detectface.recycle();
            detectface = null;
            final GateService gateService = ServiceGenerator.createService(GateService.class);
            Compare1nReponse compare1nReponse = gateService.compare1N(AppInternal.getInstance().getServiceIP() + URL.FACE_COMPARE_1_N,
                    RequestParam.build()
                            .with("feature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                            .with("library", new String[]{})
                            .with("mac", AppInternal.getInstance().getIMEI())
                            .with("threshold", 0)
                            .with("resultNum", 1).create()).execute().body();
            if (compare1nReponse.isSuccess() && compare1nReponse.getContent() != null && compare1nReponse.getContent().getPersonInfo().size() > 0) {
                //服务端有数据
                PersonInfo personInfo = compare1nReponse.getContent().getPersonInfo().get(0);

                if(personInfo.getScore()<AppInternal.getInstance().getPreviewThreshold()){
                    //比分小于阈值
                    //陌生人逻辑
                    Integer count = StrangerListManager.getInstance().loopReduceOnce(mFeatureData);
                    if (count == null) {
                        //本地没有缓存就放入陌生人缓存
                        StrangerListManager.getInstance().put(mFeatureData);
                    } else {
                        if (count <= 0) {
                            //上报陌生人
                            RespBase response = gateService.passRecordNoCard(AppInternal.getInstance().getServiceIP() + URL.PASS_RECORD_NOCARD, RequestParam.build().with("mac", AppInternal.getInstance().getIMEI())
                                    .with("inOut", AppInternal.getInstance().getInOut())
                                    .with("passType", PassType.STRANGER)
                                    .with("verifyPhoto", detectfacebase64)
                                    .with("verifyFeature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                                    .with("passPicFaceX", mFaceDetectResult.nFaceLeft / (float) width)
                                    .with("passPicFaceY", mFaceDetectResult.nFaceTop / (float) height)
                                    .with("passPicFaceWidth", (mFaceDetectResult.nFaceRight - mFaceDetectResult.nFaceLeft) / (float) width)
                                    .with("passPicFaceHeight", (mFaceDetectResult.nFaceBottom - mFaceDetectResult.nFaceTop) / (float) height)
                                    .create()).execute().body();
                            RespBase respBase = new RespBase(ErrorCode.STRANGER_WARN, "注意陌生人");
                            return respBase;

                        }
                    }
                    return new RespBase(ErrorCode.WARING,AppContext.getInstance().getString(R.string.please_see_camera),personInfo.getScore()+"");
                }
                //比分大于等于阈值 上报通行记录
                RespBase resp = gateService.passRecordPreview(AppInternal.getInstance().getServiceIP() + URL.PASS_RECORD_PREVIEW,
                        RequestParam.build()
                                .with("mac", AppInternal.getInstance().getIMEI())
                                .with("inOut", AppInternal.getInstance().getInOut())
                                .with("passType", PassType.FACE)
                                .with("passPersonId", personInfo.getId())
                                .with("verifyPhoto", detectfacebase64)
                                .with("verifyFeature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                                .with("verifyThreshold", AppInternal.getInstance().getPreviewThreshold())
                                .with("verifyScore", personInfo.getScore())
                                .with("passPicFaceX", mFaceDetectResult.nFaceLeft / (float) width)
                                .with("passPicFaceY", mFaceDetectResult.nFaceTop / (float) height)
                                .with("passPicFaceWidth", (mFaceDetectResult.nFaceRight - mFaceDetectResult.nFaceLeft) / (float) width)
                                .with("passPicFaceHeight", (mFaceDetectResult.nFaceBottom - mFaceDetectResult.nFaceTop) / (float) height)
                                .create()).execute().body();
                if (resp.isSuccess()) {
                    Observable.timer(500, TimeUnit.MILLISECONDS)
                            .subscribe(new Consumer<Long>() {
                                @Override
                                public void accept(Long aLong) {
                                    AppInternal.getInstance().getIandosManager().ICE_DoorSwitch(true, false);
                                    try {
                                        Thread.sleep(500);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    AppInternal.getInstance().getIandosManager().ICE_DoorSwitch(false, false);
                                }
                            });

                    return new RespBase(ErrorCode.PLEASE_PASS, "请通行",personInfo.getScore()+"");
                } else {
                    if (resp.getCode() == 500) {
                        return new RespBase(ErrorCode.SERVER_ERROR, "核验主机故障");
                    } else {
                        //不给通行
                        return resp;
                    }
                }
            } else {
                //服务端没有数据
                RespBase respBase = new RespBase(ErrorCode.WARING, "审核未通过\n请等待");
                return respBase;
            }

        } catch (Exception e) {
            Log.e(TAG, "call: ", e);
            return new RespBase(ErrorCode.SERVER_ERROR, "核验主机故障");
        } finally {
            System.gc();
        }


    }
}

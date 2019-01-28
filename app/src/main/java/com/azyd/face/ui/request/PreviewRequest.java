package com.azyd.face.ui.request;


import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.constant.CameraConstant;
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

/**
 * @author suntao
 * @creat-time 2018/12/4 on 17:13
 * $describe$
 */
public class PreviewRequest extends BaseRequest {
    private final String TAG = "PreviewRequest";
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

        try {
            //对比本地列表

            boolean have = FaceListManager.getInstance().contains(mFeatureData);
            if (have) {
                //本地列表已有,放弃此次请求
                RespBase respBase = new RespBase(ErrorCode.NORMAL,null);
                return respBase;
            }


            //和服务端通信比对
            Bitmap detectface = ImageUtils.rgb2Bitmap(mFaceData, width, height);
            String detectfacebase64 = ImageUtils.Bitmap2StrByBase64(detectface);
            detectface.recycle();
            detectface = null;
            final GateService gateService = ServiceGenerator.createService(GateService.class);
            Compare1nReponse compare1nReponse = gateService.compare1N(AppInternal.getInstance().getServiceIP() + URL.FACE_COMPARE_1_N, RequestParam.build().with("feature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                    .with("library", new String[]{})
                    .with("mac", AppInternal.getInstance().getIMEI())
                    .with("threshold", AppInternal.getInstance().getPreviewThreshold())
                    .with("resultNum", 1).create()).execute().body();
            if (compare1nReponse.isSuccess() && compare1nReponse.getContent() != null && compare1nReponse.getContent().getPersonInfo().size() > 0) {
                PersonInfo personInfo = compare1nReponse.getContent().getPersonInfo().get(0);
                //服务端有
                //加入队列
//                FaceListManager.getInstance().put(mFeatureData);

                //上报通行记录
                RespBase resp = gateService.passRecordPreview(AppInternal.getInstance().getServiceIP() + URL.PASS_RECORD_PREVIEW, RequestParam.build().with("mac", AppInternal.getInstance().getIMEI())
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
                    //开门
                    AppInternal.getInstance().getIandosManager().ICE_DoorSwitch(true, true);
                    RespBase respBase = new RespBase(ErrorCode.PLEASE_PASS, "请通行");
                    return respBase;
//                    Map<String,Object> rf= gateService.openDoor(RequestParam.build().with("open",true).with("reverse",true).create()).execute().body();
                } else {
                    if(resp.getCode()==500){
                        return new RespBase(ErrorCode.SERVER_ERROR, "核验主机故障");
                    } else {
                        return resp;
                    }

                }

            } else {
                //服务端没有此人
                Integer count = StrangerListManager.getInstance().loopReduceOnce(mFeatureData);
                if (count == null) {
                    //本地没有缓存就放入陌生人缓存
                    StrangerListManager.getInstance().put(mFeatureData);
                } else {
                    if (count <= 0) {
                        //上报陌生人
                        FaceListManager.getInstance().put(mFeatureData);
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
                        FaceListManager.getInstance().put(mFeatureData);
                        RespBase respBase = new RespBase(ErrorCode.STRANGER_WARN, "注意陌生人");
                        return respBase;

                    }
                }
                RespBase respBase = new RespBase(ErrorCode.WARING, "审核未通过\n请等待");
                return respBase;
            }


        } catch (Exception e) {
            Log.e(TAG, "call: ", e);
            return new RespBase(ErrorCode.SERVER_ERROR, "核验主机故障");
        } finally {
            mFeatureData = null;
            mFaceData = null;
            mFaceDetectResult = null;
            System.gc();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


    }
}

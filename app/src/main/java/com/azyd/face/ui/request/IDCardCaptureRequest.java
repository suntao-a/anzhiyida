package com.azyd.face.ui.request;

import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import com.alibaba.android.arouter.launcher.ARouter;
import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.constant.Dictionaries;
import com.azyd.face.constant.ErrorCode;
import com.azyd.face.constant.PassType;
import com.azyd.face.constant.RoutePath;
import com.azyd.face.constant.URL;
import com.azyd.face.dispatcher.SingleDispatcher;
import com.azyd.face.dispatcher.base.BaseRequest;
import com.azyd.face.dispatcher.base.FaceListManager;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.activity.SplashActivity;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.DateFormatUtils;
import com.azyd.face.util.RequestParam;
import com.azyd.face.util.ImageUtils;
import com.idcard.MyHSIDCardInfo;
import com.idfacesdk.FACE_DETECT_RESULT;
import com.idfacesdk.IdFaceSdk;

import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

/**
 * @author suntao
 * @creat-time 2019/1/3 on 17:52
 * $describe$
 */
public class IDCardCaptureRequest extends BaseRequest {
    private final String TAG = "IDCardCaptureRequest";
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");// 设置日期格式
    private byte[] mFeatureData;
    private byte[] mFaceData;
    private int width;
    private int height;
    private MyHSIDCardInfo mMyHSIDCardInfo;
    private FACE_DETECT_RESULT mFaceDetectResult;

    public IDCardCaptureRequest() {
        super(1);

    }

    public IDCardCaptureRequest setHSIDCardInfo(MyHSIDCardInfo cardInfo) {
        mMyHSIDCardInfo = cardInfo;
        return this;
    }

    public IDCardCaptureRequest setFeatureData(byte[] featureData) {
        mFeatureData = featureData;
        return this;
    }

    public IDCardCaptureRequest setFaceData(byte[] faceData) {
        mFaceData = faceData;
        return this;
    }

    public IDCardCaptureRequest setFaceDetectResult(FACE_DETECT_RESULT detectResult) {
        mFaceDetectResult = detectResult;
        return this;
    }

    public IDCardCaptureRequest setImageSize(int w, int h) {
        width = w;
        height = h;
        return this;
    }

    @Override
    public RespBase call() {
        try {
            RespBase respBase = new RespBase();
            //识别
            int ret = 0;
            FACE_DETECT_RESULT faceDetectResult = new FACE_DETECT_RESULT();
            int nFeatureSize = IdFaceSdk.IdFaceSdkFeatureSize();
            byte[] featureData = new byte[nFeatureSize];
            ret = IdFaceSdk.IdFaceSdkDetectFace(mMyHSIDCardInfo.getFaceBmp(), mMyHSIDCardInfo.getWidth(), mMyHSIDCardInfo.getHeight(), faceDetectResult);
            if (ret <= 0) {
                //检测人脸失败
                respBase.setCode(ErrorCode.WARING);
                respBase.setMessage("身份证照检测人脸失败");
                return respBase;
            }

            ret = IdFaceSdk.IdFaceSdkFeatureGet(mMyHSIDCardInfo.getFaceBmp(), mMyHSIDCardInfo.getWidth(), mMyHSIDCardInfo.getHeight(), faceDetectResult, featureData);
            if (ret != 0) {
                //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
                //检测人脸失败
                respBase.setCode(ErrorCode.WARING);
                respBase.setMessage("证件照检测失败");
                return respBase;
            }
            if (faceDetectResult.nFaceLeft == 0 && faceDetectResult.nFaceRight == 0) {
                respBase.setCode(ErrorCode.WARING);
                respBase.setMessage("证件照检测失败");
                return respBase;
            }
            ret = IdFaceSdk.IdFaceSdkFeatureCompare(mFeatureData, featureData);

            if (ret < AppInternal.getInstance().getIdcardThreshold()) {
                respBase.setCode(ErrorCode.MATCH_CASE_FAILED);
                respBase.setMessage("您的证件和本人不符");
                return respBase;
            }


            Bitmap cardface = ImageUtils.rgb2Bitmap(mMyHSIDCardInfo.getFaceBmp(), mMyHSIDCardInfo.getWidth(), mMyHSIDCardInfo.getHeight());
            String cardfacebase64 = ImageUtils.Bitmap2StrByBase64(cardface);
            cardface.recycle();
            cardface = null;
            Bitmap detectface = ImageUtils.rgb2Bitmap(mFaceData, width, height);
            String detectfacebase64 = ImageUtils.Bitmap2StrByBase64(detectface);
            detectface.recycle();
            detectface = null;
            final GateService gateService = ServiceGenerator.createService(GateService.class);
            RespBase response = gateService.passRecordIDCard(AppInternal.getInstance().getServiceIP() + URL.PASS_RECORD_IDCARD, RequestParam.build().with("mac", AppInternal.getInstance().getIMEI())
                    .with("inOut", AppInternal.getInstance().getInOut())
                    .with("personName", mMyHSIDCardInfo.getPeopleName())
                    .with("personSex", Dictionaries.getSexKey(mMyHSIDCardInfo.getSex()))
                    .with("personRace", Dictionaries.getPeopleKey(mMyHSIDCardInfo.getPeople()))
                    .with("personBirthday", DATE_FORMAT.format(mMyHSIDCardInfo.getBirthDay()))
                    .with("personAddress", mMyHSIDCardInfo.getAddr())
                    .with("cardNum", mMyHSIDCardInfo.getIDCard())
                    .with("cardDepart", mMyHSIDCardInfo.getDepartment())
//                    .with("cardDayFrom",DateFormatUtils.StringToDate(mMyHSIDCardInfo.getStrartDate(),"yyyy.MM.dd","yyyyMMdd"))
//                    .with("cardDayTo",DateFormatUtils.StringToDate(mMyHSIDCardInfo.getEndDate(),"yyyy.MM.dd","yyyyMMdd"))
                    .with("cardDayFrom", mMyHSIDCardInfo.getStrartDate())
                    .with("cardDayTo", mMyHSIDCardInfo.getEndDate())

                    .with("cardPhoto", cardfacebase64)
                    .with("cardPhotoFeature", Base64.encodeToString(featureData, Base64.DEFAULT))
                    .with("cardPhotoFeature", Base64.encodeToString(featureData, Base64.DEFAULT))

                    .with("passType", PassType.ID_CARD)

                    .with("verifyPhoto", detectfacebase64)
                    .with("verifyFeature", Base64.encodeToString(mFeatureData, Base64.DEFAULT))
                    .with("verifyThreshold", AppInternal.getInstance().getIdcardThreshold())
                    .with("verifyScore", ret)

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
                if (response.getCode() == 500) {
                    return new RespBase(ErrorCode.SERVER_ERROR, "核验主机故障");
                } else {
                    return response;
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "call: ", e);
            return new RespBase(ErrorCode.SERVER_ERROR, "核验主机故障");
        } finally {
            System.gc();
            Observable.timer(2, TimeUnit.SECONDS)
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) {
                            SingleDispatcher.getInstance().getObservable().onNext(new RespBase(ErrorCode.EVENT_IDCARD_REQUEST_COMPLETED, null));
                        }
                    });

        }


    }
}

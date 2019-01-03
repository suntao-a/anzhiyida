package com.azyd.face.ui.request;

import android.util.Base64;

import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.base.exception.ExceptionHandle;
import com.azyd.face.base.exception.RespThrowable;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.constant.PassType;
import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.dispatcher.core.FaceListManager;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.RequestParam;
import com.azyd.face.util.Utils;
import com.idcard.MyHSIDCardInfo;
import com.idfacesdk.FACE_DETECT_RESULT;
import com.idfacesdk.IdFaceSdk;

import java.text.SimpleDateFormat;

/**
 * @author suntao
 * @creat-time 2019/1/3 on 17:52
 * $describe$
 */
public class IDCardCaptureRequest extends BaseRequest {
    static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy年MM月dd日");// 设置日期格式
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

        RespBase respBase  = new RespBase();
//识别
        int ret = 0;
        FACE_DETECT_RESULT faceDetectResult = new FACE_DETECT_RESULT();
        int nFeatureSize = IdFaceSdk.IdFaceSdkFeatureSize();
        byte[] featureData = new byte[nFeatureSize];
        ret = IdFaceSdk.IdFaceSdkDetectFace(mMyHSIDCardInfo.getFaceBmp(), mMyHSIDCardInfo.getWidth(), mMyHSIDCardInfo.getHeight(), faceDetectResult);
        if (ret <= 0) {
            //检测人脸失败
            respBase.setCode(200);
            respBase.setMessage("身份证照检测人脸失败");
            return respBase;
        }

        ret = IdFaceSdk.IdFaceSdkFeatureGet(mMyHSIDCardInfo.getFaceBmp(), mMyHSIDCardInfo.getWidth(), mMyHSIDCardInfo.getHeight(), faceDetectResult, featureData);
        if (ret != 0) {
            //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
            //检测人脸失败
            respBase.setCode(200);
            respBase.setMessage("身份证照检测人脸失败");
            return respBase;
        }
        if (faceDetectResult.nFaceLeft == 0 && faceDetectResult.nFaceRight == 0) {
            respBase.setCode(200);
            respBase.setMessage("身份证照检测人脸失败");
            return respBase;
        }
        ret = IdFaceSdk.IdFaceSdkFeatureCompare(mFeatureData,featureData);
        if(ret< CameraConstant.getCameraParam().getFeatureQualityPass()){
            respBase.setCode(200);
            respBase.setMessage("身份证照和摄像头获取的人脸比对得分‘"+ret+"’较低，请换个姿势再来一次");
            return respBase;
        }

        try {
            final GateService gateService = ServiceGenerator.createService(GateService.class);
            RespBase response = gateService.passRecordIDCard(RequestParam.build().with("mac", AppInternal.getInstance().getIMEI())
                    .with("personName",mMyHSIDCardInfo.getPeopleName())
                    .with("personSex",mMyHSIDCardInfo.getSex())
                    .with("personRace",mMyHSIDCardInfo.getPeople())
                    .with("personBirthday",DATE_FORMAT.format(mMyHSIDCardInfo.getBirthDay()))
                    .with("personAddress",mMyHSIDCardInfo.getAddr())
                    .with("cardNum",mMyHSIDCardInfo.getIDCard())
                    .with("cardDepart",mMyHSIDCardInfo.getDepartment())
                    .with("cardDayFrom",mMyHSIDCardInfo.getStrartDate())
                    .with("cardDayTo",mMyHSIDCardInfo.getEndDate())
                    .with("cardPhoto",Base64.encode(mMyHSIDCardInfo.getFaceBmp(), Base64.DEFAULT))
                    .with("cardPhotoFeature",Base64.encode(featureData, Base64.DEFAULT))
                    .with("cardPhotoFeature",Base64.encode(featureData, Base64.DEFAULT))

                    .with("passType", PassType.ID_CARD)
                    .with("passStatus", "0")//0：允许通行，1：禁止通行

                    .with("verifyPhoto", Base64.encode(mFaceData, Base64.DEFAULT))
                    .with("verifyFeature", Base64.encode(mFeatureData, Base64.DEFAULT))
                    .with("verifyThreshold", CameraConstant.getCameraParam().getFeatureQualityPass())
                    .with("verifyScore", ret)

                    .with("passPicFaceX", mFaceDetectResult.nFaceLeft / (float) width)
                    .with("passPicFaceY", mFaceDetectResult.nFaceTop / (float) height)
                    .with("passPicFaceWidth", (mFaceDetectResult.nFaceRight - mFaceDetectResult.nFaceLeft) / (float) width)
                    .with("passPicFaceHeight", (mFaceDetectResult.nFaceBottom - mFaceDetectResult.nFaceTop) / (float) height)
                    .create()).execute().body();
            return response;


        } catch (Exception e) {
            RespThrowable throwable = ExceptionHandle.handleException(e);
            return new RespBase(200,throwable.getMessage());
        }finally {
            FaceListManager.getInstance().put(mFeatureData);
        }

    }
}

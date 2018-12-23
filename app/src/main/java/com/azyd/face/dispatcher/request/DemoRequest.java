package com.azyd.face.dispatcher.request;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import com.azyd.face.app.AppContext;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.util.Utils;
import com.idfacesdk.FACE_DETECT_RESULT;
import com.idfacesdk.IdFaceSdk;

import java.io.File;

public class DemoRequest extends BaseRequest {
    private byte[] mFeatureData;
    public DemoRequest(byte[] featureData){
        this(0);
        mFeatureData = featureData;
    }

    private DemoRequest(int priority) {
        super(priority);
    }

    @Override
    public String call() throws Exception {
        File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()+File.separator+ AppContext.getInstance().getPackageName());
        if(!file.exists()){
            file.mkdir();
        }
        File[] files = file.listFiles();
        if(files.length==0){
            return file.getCanonicalPath()+"没有图片";
        }
        for(File item:files){

            Bitmap bitmap = BitmapFactory.decodeFile(item.getCanonicalPath());
            byte[] faceRGB = Utils.bitmap2RGB(bitmap);
            int width=bitmap.getWidth();
            int height =bitmap.getHeight();
            bitmap.recycle();
            int ret=0;
            FACE_DETECT_RESULT faceDetectResult = new FACE_DETECT_RESULT();
            ret = IdFaceSdk.IdFaceSdkDetectFace(faceRGB, width, height, faceDetectResult);
            if (ret <= 0) {
                //检测人脸失败
                continue;
//                return -1;
            }
            int nFeatureSize = IdFaceSdk.IdFaceSdkFeatureSize();
            byte[] featureData = new byte[nFeatureSize];
            ret = IdFaceSdk.IdFaceSdkFeatureGet(faceRGB, width, height, faceDetectResult, featureData);
            if (ret != 0) {
                //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
//                return -1;
                continue;
            }

            ret = IdFaceSdk.IdFaceSdkFeatureCompare(mFeatureData,featureData);
            if(ret>= CameraConstant.getDefaultCameraParam().getFeatureQualityPass()){
                return "你是"+item.getName();
            }
        }
        return "未比对成功";

    }
}

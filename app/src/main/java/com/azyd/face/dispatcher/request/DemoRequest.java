package com.azyd.face.dispatcher.request;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.Base64;

import com.azyd.face.app.AppContext;
import com.azyd.face.app.AppInternal;
import com.azyd.face.base.RespBase;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.dispatcher.base.BaseRequest;
import com.azyd.face.util.ImageUtils;
import com.idfacesdk.FACE_DETECT_RESULT;
import com.idfacesdk.IdFaceSdk;

import java.io.File;

public class DemoRequest extends BaseRequest {
    private byte[] mFeatureData;

    public DemoRequest(byte[] featureData){
        this(2);
        mFeatureData = featureData;
    }

    private DemoRequest(int priority) {
        super(priority);
    }

    @Override
    public RespBase call() throws Exception {
        File file=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()+File.separator+ AppContext.getInstance().getPackageName());
        if(!file.exists()){
            file.mkdir();
        }
        File[] files = file.listFiles();
        if(files.length==0){
            return new RespBase(200,file.getCanonicalPath()+"没有图片");

        }
        for(File item:files){

            Bitmap bitmap = BitmapFactory.decodeFile(item.getCanonicalPath());
            byte[] faceRGB = ImageUtils.bitmap2RGB(bitmap);
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
            String detectfacebase64 = Base64.encodeToString(featureData, Base64.DEFAULT);
            if (ret != 0) {
                //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
//                return -1;
                continue;
            }

            ret = IdFaceSdk.IdFaceSdkFeatureCompare(mFeatureData,featureData);
            if(ret>= AppInternal.getInstance().getPreviewThreshold()){
                return new RespBase(200,"你是"+item.getName());

            }
        }
        return new RespBase(200,"未比对成功");
    }
}

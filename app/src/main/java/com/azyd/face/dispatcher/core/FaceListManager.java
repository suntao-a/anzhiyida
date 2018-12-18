package com.azyd.face.dispatcher.core;

import com.azyd.face.app.AppContext;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.util.ACache;
import com.idfacesdk.IdFaceSdk;
import java.util.concurrent.CopyOnWriteArrayList;

public class FaceListManager {
    private static FaceListManager faceListManager;
    private CopyOnWriteArrayList<String> saveTimesMap = new CopyOnWriteArrayList<>();
    ACache aCache;
    int mSaveTimes=6;
    public static FaceListManager getInstance(){
        if(faceListManager==null){
            synchronized (FaceListManager.class){
                if(faceListManager==null){
                    faceListManager = new FaceListManager();
                    faceListManager.mSaveTimes=CameraConstant.FACE_SAVE_TIMES;
                }
            }
        }
        return faceListManager;
    }
    private FaceListManager(){
        aCache = ACache.get(AppContext.getInstance());
    }
    public void put(byte[] data){
        String hashcode = String.valueOf(data.hashCode());
        saveTimesMap.add(hashcode);
        aCache.put(hashcode,data,mSaveTimes);
    }

    public boolean contains(byte[] featureData) {

        String key=null;
        byte[] value = null;
        for(String item : saveTimesMap){
            value = aCache.getAsBinary(key);
            if(value==null){
                saveTimesMap.remove(item);
                continue;
            }
            if(IdFaceSdk.IdFaceSdkFeatureCompare(featureData,value)>=CameraConstant.FEATURE_QUALITY_PASS){
                return true;
            }
        }
        return false;
    }
    public void onDestory(){
        aCache.clear();
        for(String item : saveTimesMap){
            saveTimesMap.remove(item);
        }
    }
}

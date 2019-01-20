package com.azyd.face.dispatcher.base;

import com.azyd.face.app.AppContext;
import com.azyd.face.app.AppInternal;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.util.ACache;
import com.idfacesdk.IdFaceSdk;

import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

public class FaceListManager {
    private static FaceListManager faceListManager;
    private CopyOnWriteArrayList<String> saveTimesMap = new CopyOnWriteArrayList<>();
    private ACache aCache;
    private int mSaveTimes=4;
    public static FaceListManager getInstance(){
        if(faceListManager==null){
            synchronized (FaceListManager.class){
                if(faceListManager==null){
                    faceListManager = new FaceListManager();
                    faceListManager.mSaveTimes=CameraConstant.getCameraParam().getFaceSaveTimes();
                }
            }
        }
        return faceListManager;
    }
    private FaceListManager(){
        aCache = ACache.get(AppContext.getInstance());
    }
    public void put(byte[] data){
        String hashcode = String.valueOf(Arrays.hashCode(data));
        saveTimesMap.add(hashcode);
        aCache.put(hashcode,data,mSaveTimes);
    }
    public void put(byte[] data,int outTimes){
        String hashcode = String.valueOf(Arrays.hashCode(data));
        saveTimesMap.add(hashcode);
        aCache.put(hashcode,data,outTimes);
    }
    public boolean contains(byte[] featureData) {
        byte[] value;
        for(String item : saveTimesMap){
            value = aCache.getAsBinary(item);
            if(value==null){
                saveTimesMap.remove(item);
                continue;
            }
            if(IdFaceSdk.IdFaceSdkFeatureCompare(featureData,value)>= AppInternal.getInstance().getPreviewThreshold()){
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

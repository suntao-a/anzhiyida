package com.azyd.face.dispatcher.base;

import com.azyd.face.app.AppContext;
import com.azyd.face.app.AppInternal;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.util.ACache;
import com.idfacesdk.IdFaceSdk;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;


public class StrangerListManager {
    private static StrangerListManager faceListManager;
    private Hashtable<String,Integer> mKeyCount = new Hashtable<>();
//    private CopyOnWriteArrayList<String> saveTimesMap = new CopyOnWriteArrayList<>();
    private ACache aCache;
    private int mSaveTimes=4;
    private int mDetectCount=5;
    public static StrangerListManager getInstance(){
        if(faceListManager==null){
            synchronized (FaceListManager.class){
                if(faceListManager==null){
                    faceListManager = new StrangerListManager();
                    faceListManager.mSaveTimes=CameraConstant.getCameraParam().getFaceSaveTimes();
                    faceListManager.mDetectCount = AppInternal.getInstance().getStrangerDetectCount();
                }
            }
        }
        return faceListManager;
    }
    private StrangerListManager(){
        aCache = ACache.get(AppContext.getInstance());
    }
    public void put(byte[] data){
        String hashcode = String.valueOf(Arrays.hashCode(data));
        mKeyCount.put(hashcode,mDetectCount);
        aCache.put(hashcode,data,mSaveTimes);
    }

    public boolean contains(byte[] featureData) {
        byte[] value;
        Iterator<String> i = mKeyCount.keySet().iterator();
        while (i.hasNext()){
            String key = i.next();
            value = aCache.getAsBinary(key);
            if(value==null){
                i.remove();
                continue;
            }
            if(IdFaceSdk.IdFaceSdkFeatureCompare(featureData,value)>=CameraConstant.getCameraParam().getFeatureQualityPass()){
                return true;
            }
        }
        return false;
    }
    public void onDestory(){
        aCache.clear();
        mKeyCount.clear();
    }
}

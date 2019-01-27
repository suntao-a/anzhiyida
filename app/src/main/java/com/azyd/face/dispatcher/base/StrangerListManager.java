package com.azyd.face.dispatcher.base;

import com.azyd.face.app.AppContext;
import com.azyd.face.app.AppInternal;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.util.ACache;
import com.idfacesdk.IdFaceSdk;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;


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
                    faceListManager.mSaveTimes=AppInternal.getInstance().getStrangerKeepTimes();
                    faceListManager.mDetectCount = AppInternal.getInstance().getStrangerCompareTimes();
                }
            }
        }
        return faceListManager;
    }
    private StrangerListManager(){
        aCache = ACache.get(AppContext.getInstance());
    }
    public void put(byte[] data){
        clean();
        String hashcode = String.valueOf(Arrays.hashCode(data));
        mKeyCount.put(hashcode,mDetectCount);
        aCache.put(hashcode,data,mSaveTimes);
    }

    /**
     * 判断缓存中是否同一人，不是返回null，是 返回m-1次，直到次数为0时上报陌生人
     * @param featureData
     * @return
     */
    public Integer loopReduceOnce(byte[] featureData) {
        Integer count=null;
        byte[] value;
        Iterator<String> i = mKeyCount.keySet().iterator();
        while (i.hasNext()){
            String key = i.next();
            value = aCache.getAsBinary(key);
            if(value==null){
                //清空过期缓存
                i.remove();
                continue;
            }
            if(IdFaceSdk.IdFaceSdkFeatureCompare(featureData,value)>=AppInternal.getInstance().getPreviewThreshold()){
                //同一人 次数减1，并更新缓存时间
                count = mKeyCount.get(key);
                mKeyCount.put(key,--count);
                aCache.put(key,value,mSaveTimes);
            }
        }
        if(count==null||count<=0){
            clean();
        }
        return count;
    }
    public void clean(){
        Iterator<String> i = mKeyCount.keySet().iterator();
        while (i.hasNext()){
            String key = i.next();
            aCache.remove(key);
            i.remove();
        }
    }
    public void onDestory(){
        aCache.clear();
        mKeyCount.clear();
    }
}

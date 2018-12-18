package com.azyd.face.dispatcher.request;


import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.dispatcher.core.FaceListManager;

/**
 * @author suntao
 * @creat-time 2018/12/4 on 17:13
 * $describe$
 */
public class PreviewRequest extends BaseRequest {
    private byte[] mFeatureData;
    public PreviewRequest(byte[] featureData){
        this();
        mFeatureData = featureData;
    }
    private PreviewRequest(){
        super(0);
    }


    @Override
    public String call() {
        //对比本地列表
        boolean have = FaceListManager.getInstance().contains(mFeatureData);
        if(have){
            //本地列表已有,放弃此次请求
            return "0";
        }
        //没有,就和服务端通信比对
        have = true;//服务端结果
        if(!have){
            return "0";
        }
        //服务端有
        //加入队列
        FaceListManager.getInstance().put(mFeatureData);
        //开门

        //上报通行记录


        return "0";
    }
}

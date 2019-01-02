package com.azyd.face.dispatcher.request;

import com.azyd.face.base.RespBase;
import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.dispatcher.core.FaceListManager;

/**
 * @author suntao
 * @creat-time 2018/12/27 on 17:17
 * $describe$
 */
public class SameFaceTestRequest extends BaseRequest {
    private byte[] mFeatureData;
    public  SameFaceTestRequest(byte[] data){
        super(0);
        mFeatureData = data;
    }
    @Override
    public RespBase call() throws Exception {
        if(!FaceListManager.getInstance().contains(mFeatureData)){
            FaceListManager.getInstance().put(mFeatureData);
        } else {

        }
        return null;
    }
}

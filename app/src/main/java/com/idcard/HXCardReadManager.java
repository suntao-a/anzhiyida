package com.idcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.azyd.face.base.rxjava.AsynTransformer;
import com.huashi.otg.sdk.HSIDCardInfo;
import com.huashi.otg.sdk.HandlerMsg;
import com.huashi.otg.sdk.HsOtgApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author suntao
 * @creat-time 2019/1/3 on 14:08
 * $describe$
 */
public class HXCardReadManager {

    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wltlib";// 授权目录
    HsOtgApi mHsOtgApi;
    Message msg;
    MyHSIDCardInfo ici;
    Handler mHandler;
    Context mContext;


    public HXCardReadManager(Handler handler, Context context) {
        mHandler = handler;
        mContext = context;
        mHsOtgApi = new HsOtgApi(mHandler, mContext);
    }

    public void start() {
        copy(mContext, "base.dat", "base.dat", filepath);
        copy(mContext, "license.lic", "license.lic", filepath);
        int ret = mHsOtgApi.init();// 因为第一次需要点击授权，所以第一次点击时候的返回是-1所以我利用了广播接受到授权后用handler发送消息
        if (ret == 1) {
//            statu.setText("连接成功");
//            sam.setText(api.GetSAMID());
            Observable.interval(300, TimeUnit.MILLISECONDS).subscribeOn(Schedulers.io())
                    .observeOn(Schedulers.io())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) throws Exception {
                            if (mHsOtgApi.Authenticate(200, 200) != 1) {
                                msg = Message.obtain();
                                msg.what = HandlerMsg.READ_ERROR;
                                mHandler.sendMessage(msg);
                            } else {
                                ici = new MyHSIDCardInfo();
                                if (mHsOtgApi.ReadCard(ici, 200, 1300) == 1) {
                                    try {
                                        int ret = mHsOtgApi.Unpack(filepath, ici.getwltdata());// 照片解码
                                        if (ret != 0) {// 读卡失败
                                            msg = Message.obtain();
                                            msg.what = HandlerMsg.READ_ERROR;
                                            mHandler.sendMessage(msg);
                                        } else {
                                            FileInputStream fis = new FileInputStream(filepath + "/zp.bmp");
                                            Bitmap bmp = BitmapFactory.decodeStream(fis);
                                            ici.setFaceBmp(bmp);
                                            fis.close();
                                            msg = Message.obtain();
                                            msg.obj = ici;
                                            msg.what = HandlerMsg.READ_SUCCESS;
                                            mHandler.sendMessage(msg);
                                        }
                                    } catch (Exception e){
                                        msg = Message.obtain();
                                        msg.what = HandlerMsg.READ_ERROR;
                                        mHandler.sendMessage(msg);
                                    }

                                }
                            }
                        }
                    });

        } else {
            //statu.setText("连接失败");
        }
    }
    public void close(){
        if(mHsOtgApi!=null){
            mHsOtgApi.unInit();
        }

    }

    private void copy(Context context, String fileName, String saveName, String savePath) {
        File path = new File(savePath);
        if (!path.exists()) {
            path.mkdir();
        }

        try {
            File e = new File(savePath + "/" + saveName);
            if (e.exists() && e.length() > 0L) {
                return;
            }

            FileOutputStream fos = new FileOutputStream(e);
            InputStream inputStream = context.getResources().getAssets()
                    .open(fileName);
            byte[] buf = new byte[1024];
            boolean len = false;

            int len1;
            while ((len1 = inputStream.read(buf)) != -1) {
                fos.write(buf, 0, len1);
            }

            fos.close();
            inputStream.close();
        } catch (Exception var11) {
            Log.i("LU", "IO异常");
        }

    }
}

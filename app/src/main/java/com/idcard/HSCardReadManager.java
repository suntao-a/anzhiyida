package com.idcard;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.azyd.face.base.rxjava.AsynTransformer;
import com.azyd.face.util.ImageUtils;
import com.huashi.otg.sdk.HandlerMsg;
import com.huashi.otg.sdk.HsOtgApi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * @author suntao
 * @creat-time 2019/1/3 on 14:08
 * $describe$
 */
public class HSCardReadManager {

    String filepath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/wltlib";// 授权目录
    HsOtgApi mHsOtgApi;
    Message msg;
    MyHSIDCardInfo ici;
    Handler mHandler;
    Context mContext;


    public HSCardReadManager(Handler handler, Context context) {
        mHandler = handler;
        mContext = context;
        mHsOtgApi = new HsOtgApi(mHandler, mContext);
        File file = new File(filepath);
        if(!file.exists()){
            file.mkdir();
        }
    }
    private Integer isConnected=-1;
    public void start() {
        copy(mContext, "base.dat", "base.dat", filepath);
        copy(mContext, "license.lic", "license.lic", filepath);
        Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {

                isConnected = mHsOtgApi.init();
                if(isConnected!=1){
                    return null;
                }
                return isConnected;
            }
        }).retry(3, new Predicate<Throwable>() {
            @Override
            public boolean test(Throwable throwable) throws Exception {
                Thread.sleep(1000);
                return true;
            }
        }).flatMap(new Function<Integer, ObservableSource<Long>>() {
            @Override
            public ObservableSource<Long> apply(Integer integer) throws Exception {
                if(integer!=1){
                    msg = Message.obtain();
                    msg.what = HandlerMsg.CONNECT_ERROR;
                    mHandler.sendMessage(msg);
                }
                return Observable.interval(500, TimeUnit.MILLISECONDS);
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (mHsOtgApi.Authenticate(200, 200) != 1) {
                            msg = Message.obtain();
                            msg.what = HandlerMsg.Authenticate_ERROR;
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
                                        byte[] faceRGB = ImageUtils.bitmap2RGB(bmp);
                                        ici.setFaceBmp(faceRGB)
                                                .setWidth(bmp.getWidth())
                                                .setHeight(bmp.getHeight());
                                        fis.close();
                                        bmp.recycle();
                                        msg = Message.obtain();
                                        msg.obj = ici;
                                        msg.what = HandlerMsg.READ_SUCCESS;
                                        mHandler.sendMessage(msg);
                                    }
                                } catch (Exception e) {
                                    msg = Message.obtain();
                                    msg.what = HandlerMsg.READ_ERROR;
                                    mHandler.sendMessage(msg);
                                }

                            }
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        msg = Message.obtain();
                        msg.what = HandlerMsg.CONNECT_ERROR;
                        mHandler.sendMessage(msg);
                    }
                });

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
            byte[] buf = new byte[512];
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

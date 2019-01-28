package com.idcard.huaxu;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;

import com.azyd.face.util.DateFormatUtils;
import com.azyd.face.util.ImageUtils;
import com.huashi.otg.sdk.HandlerMsg;
import com.huashi.otg.sdk.HsOtgApi;
import com.hxgc.hxj10readerid.HxJ10ReaderID;
import com.idcard.MyHSIDCardInfo;

import java.io.File;
import java.io.FileInputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class HXCardReadManager {
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private boolean mbIsOpened = false;

    // 读卡
    private HxJ10ReaderID mReaderID = null;
    private boolean canLoop=true;
    // 连续读卡计数
    private int ReadIDCount = 0;
    Integer isConnected = -1;
    private UsbManager mUsbManager = null;
    Message msg;
    Handler mHandler;
    MyHSIDCardInfo ici;
    Context mContext;
    private BroadcastReceiver mUsbPermissionActionReceiver;
    public HXCardReadManager(Handler handler, Context context) {
        mHandler = handler;
        mContext = context;
        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    }
    public void start(){
        tryGetUsbPermission();
    }
    private void _start() {
        if (mbIsOpened) {
            return;
        }
        startLoop();
        Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {

                mReaderID = new HxJ10ReaderID();
                isConnected = mReaderID.OpenDevice(mUsbManager);
                if (isConnected != 0) {
                    return null;
                    // 设备已打开

                }
                mbIsOpened = true;
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
                if (integer != 0) {
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
                        if(!canLoop()){
                            return;
                        }

                        String ErrMsg = null;
                        String ResultMsg = null;
                        Bitmap ResultPicture = null;
                        // 读取卡数据
                        int iRet = mReaderID.ReadCard();
                        if (iRet != 0) {
                            msg = Message.obtain();
                            msg.what = HandlerMsg.Authenticate_ERROR;
                            mHandler.sendMessage(msg);
                            return;
                        }
                        try {
                            stopLoop();
                            ////////////////////////////////////////////////////////
                            // 显示读取信息到界面

                            // 居民身份证
                            if (mReaderID.GetIDCardType() == false) {
                                // 姓名
                                String ReadIDName = mReaderID.GetName();
                                // 性别
                                String ReadIDSex = mReaderID.GetSex();
                                // 民族
                                String ReadIDNation = mReaderID.GetNation();
                                // 出生日期
                                String ReadIDBirth = mReaderID.GetBirth();
                                // 住址
                                String ReadIDAddress = mReaderID.GetAddr();
                                // 身份证号
                                String ReadIDCode = mReaderID.GetIDCode();
                                // 签发机关
                                String ReadIDIssue = mReaderID.GetIssue();
                                // 有效日期起始
                                String ReadIDBeginDate = mReaderID.GetBeginDate();
                                // 有效日期截止
                                String ReadIDEndDate = mReaderID.GetEndDate();
                                // 照片
                                Bitmap ReadIDPicture = mReaderID.GetPicture();

                                ici = new MyHSIDCardInfo();
                                byte[] faceRGB = ImageUtils.bitmap2RGB(ReadIDPicture);
                                ici.setFaceBmp(faceRGB)
                                        .setHeight(ReadIDPicture.getHeight())
                                        .setWidth(ReadIDPicture.getWidth());
                                ici.setPeopleName(ReadIDName);
                                ici.setSex(ReadIDSex);
                                ici.setPeopleName(ReadIDNation);
                                ici.setBirthDay(DateFormatUtils.StringToDate(ReadIDBirth, "yyyyMMdd"));
                                ici.setAddr(ReadIDAddress);
                                ici.setIDCard(ReadIDCode);
                                ici.setDepartment(ReadIDIssue);
                                ici.setStrartDate(ReadIDBeginDate);
                                ici.setEndDate(ReadIDEndDate);
                            } else if (mReaderID.GetIDCardType() == true) {// 外国人身份证
                                // 英文姓名
                                String ReadIDEnName = mReaderID.GetEnName();
                                // 性别
                                String ReadIDSex = mReaderID.GetSex();
                                // 永久居留证号码
                                String ReadIDCode = mReaderID.GetIDCode();
                                // 国籍
                                String ReadIDCountry = mReaderID.GetCountry();
                                // 中文姓名
                                String ReadIDName = mReaderID.GetName();
                                // 有效日期起始
                                String ReadIDBeginDate = mReaderID.GetBeginDate();
                                // 有效日期截止
                                String ReadIDEndDate = mReaderID.GetEndDate();
                                // 出生日期
                                String ReadIDBirth = mReaderID.GetBirth();
                                // 证件版本
                                @SuppressWarnings("unused")
                                String ReadIDCardVersion = mReaderID.GetCardVersion();
                                // 授权机关代码
                                String ReadIDAuthorCode = mReaderID.GetIssAuthCode();
                                // 证件类型标识
                                @SuppressWarnings("unused")
                                String ReadIDCardSign = mReaderID.GetCardSign();
                                // 照片
                                Bitmap ReadIDPicture = mReaderID.GetPicture();
                                ici = new MyHSIDCardInfo();
                                byte[] faceRGB = ImageUtils.bitmap2RGB(ReadIDPicture);
                                ici.setFaceBmp(faceRGB)
                                        .setHeight(ReadIDPicture.getHeight())
                                        .setWidth(ReadIDPicture.getWidth());
                                ici.setPeopleName(ReadIDName);
                                ici.setSex(ReadIDSex);
                                ici.setPeople(ReadIDEnName);
                                ici.setBirthDay(DateFormatUtils.StringToDate(ReadIDBirth, ""));
                                ici.setstrNationCode(ReadIDCountry);

                                ici.setStrartDate(ReadIDBeginDate);
                                ici.setEndDate(ReadIDEndDate);
                            }
                            msg = Message.obtain();
                            msg.obj = ici;
                            msg.what = HandlerMsg.READ_SUCCESS;
                            mHandler.sendMessage(msg);
                        } catch (Exception e) {
                            msg = Message.obtain();
                            msg.what = HandlerMsg.READ_ERROR;
                            mHandler.sendMessage(msg);
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

    public void close() {
        mReaderID.CloseDevice();
        mbIsOpened = false;
    }


    //获取USB权限
    private void tryGetUsbPermission(){

        mUsbPermissionActionReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (ACTION_USB_PERMISSION.equals(action)) {
                    context.unregisterReceiver(this);//解注册
                    synchronized (this) {
                        UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if(null != usbDevice){
                                //"已获取USB权限");
                                _start();
                            }
                        }
                        else {
                            //user choose NO for your previously popup window asking for grant perssion for this usb device
                            //Log.e(TAG,String.valueOf("USB权限已被拒绝，Permission denied for device" + usbDevice));
                        }
                    }

                }
            }
        };

        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);

        if(mUsbPermissionActionReceiver != null) {
            mContext.registerReceiver(mUsbPermissionActionReceiver, filter);
        }

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);

        boolean has_idcard_usb = false;
        for (final UsbDevice usbDevice : mUsbManager.getDeviceList().values()) {

            if(usbDevice.getVendorId() == 8301 && usbDevice.getProductId() == 1)//身份证设备USB
            {
                has_idcard_usb = true;
                //Log.e(TAG,usbDevice.getDeviceName()+"已找到身份证USB");
                if(mUsbManager.hasPermission(usbDevice)){
                    //Log.e(TAG,usbDevice.getDeviceName()+"已获取过USB权限");
                    _start();
                }else{
                    //Log.e(TAG,usbDevice.getDeviceName()+"请求获取USB权限");
                    mUsbManager.requestPermission(usbDevice, mPermissionIntent);
                }
            }

        }

        if(!has_idcard_usb) {
            msg = Message.obtain();
            msg.what = HandlerMsg.CONNECT_ERROR;
            mHandler.sendMessage(msg);
            //Log.e(TAG,"未找到身份证USB");
        }

    }
    public void stopLoop(){
        canLoop=false;
    }
    public void startLoop(){
        canLoop=true;
    }
    public boolean canLoop(){
        return canLoop;
    }
}

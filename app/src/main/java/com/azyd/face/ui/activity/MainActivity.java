package com.azyd.face.ui.activity;

import android.annotation.SuppressLint;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.azyd.face.R;
import com.azyd.face.app.AppContext;
import com.azyd.face.app.AppInternal;
import com.azyd.face.base.ButterBaseActivity;
import com.azyd.face.base.RespBase;
import com.azyd.face.base.rxjava.AsynTransformer;
import com.azyd.face.constant.ErrorCode;
import com.azyd.face.constant.RoutePath;
import com.azyd.face.dispatcher.SingleDispatcher;
import com.azyd.face.dispatcher.base.FaceListManager;
import com.azyd.face.ui.request.CapturePhotoRequest;
import com.azyd.face.ui.request.IDCardCaptureRequest;
import com.azyd.face.ui.request.PreviewRequest;
import com.azyd.face.util.AppCompat;
import com.azyd.face.util.ChineseToSpeech;
import com.azyd.face.util.KdxfSpeechUtils;
import com.azyd.face.view.CameraPreview;
import com.huashi.otg.sdk.HandlerMsg;
import com.idcard.HXCardReadManager;
import com.idcard.MyHSIDCardInfo;
import com.idfacesdk.IdFaceSdk;

import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

@Route(path = RoutePath.MAIN)
public class MainActivity extends ButterBaseActivity {
    final String TAG = "MainActivity";
    final String EMPTY = "";
    @BindView(R.id.iv_service)
    ImageView ivService;
    @BindView(R.id.cl_frame)
    ConstraintLayout clFrame;
    @BindView(R.id.cameraView)
    CameraPreview cameraView;
    @BindView(R.id.surfaceview)
    SurfaceView surfaceview;
    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.tv_result)
    TextView tvResult;
    @BindView(R.id.fl_dialog)
    FrameLayout flDialog;
    @BindView(R.id.btn_custom)
    TextView btnCustom;
    Disposable msgDisposable, cameraDisposable;
    private PublishSubject<MyHSIDCardInfo> mCardInfoPublishSubject;
    private PublishSubject<CameraPreview.CameraFaceData> mIDCardCapturePublishSubject;
    private HXCardReadManager mHxCardReadManager;
    private final String willcome = "欢迎使用";
    private final RespBase normalResp;

    {
        normalResp = new RespBase(ErrorCode.NORMAL, willcome);
    }

    @Override
    protected void beforeSetContent() {
        AppCompat.setFullWindow(getWindow());
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initView(Bundle savedInstanceState) {

        //处于顶层
        surfaceview.setZOrderOnTop(true);
        //设置surface为透明
        surfaceview.getHolder().setFormat(PixelFormat.TRANSPARENT);
        cameraView.setSurfaceView(surfaceview);

        if (AppInternal.getInstance().getIandosManager() != null) {
            AppInternal.getInstance().getIandosManager().ICE_LEDSetBrightness(6);
            openLed();
        }


    }

    @SuppressLint("CheckResult")
    @Override
    protected void initData(Bundle savedInstanceState) {

        mCardInfoPublishSubject = PublishSubject.create();
        mIDCardCapturePublishSubject = PublishSubject.create();
        //启动身份证读卡器
        mHxCardReadManager = new HXCardReadManager(h, this);
        mHxCardReadManager.start();
        //启动单任务执行中心
        try {
            SingleDispatcher.getInstance().start();
        } catch (Exception e) {

        }
        SingleDispatcher.getInstance().getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .onErrorReturn(new Function() {
                    @Override
                    public RespBase apply(Object o) {
                        return normalResp;
                    }
                })
                .subscribe(msgObserver);
        //捕获相机拍照、预览、身份证拍照事件
        cameraView.getObservable().subscribe(cameraObserver);

        Observable.combineLatest(mCardInfoPublishSubject, mIDCardCapturePublishSubject, new BiFunction<MyHSIDCardInfo, CameraPreview.CameraFaceData, Boolean>() {
            @Override
            public Boolean apply(MyHSIDCardInfo myHSIDCardInfo, CameraPreview.CameraFaceData cameraFaceData) throws Exception {
                if (myHSIDCardInfo != null && cameraFaceData != null && cameraFaceData.getFaceData() != null) {
                    IDCardCaptureRequest request = new IDCardCaptureRequest();
                    request.setImageSize(cameraFaceData.getImageWidth(), cameraFaceData.getImageHeight())
                            .setFeatureData(cameraFaceData.getFeatureData())
                            .setFaceDetectResult(cameraFaceData.getFaceDetectResult())
                            .setFaceData(cameraFaceData.getFaceData())
                            .setHSIDCardInfo(myHSIDCardInfo);
                    SingleDispatcher.getInstance().add(request);
                }
                return true;
            }
        }).subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean aBoolean) throws Exception {

            }
        });
    }

    //处理结果
    private Observer msgObserver = new Observer<RespBase>() {

        @Override
        public void onSubscribe(Disposable d) {
            msgDisposable = d;
        }

        @Override
        public void onNext(RespBase respBase) {
            int delayTimes = 3000;
            switch (respBase.getCode()) {
                case ErrorCode.NORMAL:
                    tvResult.setText(respBase.getMessage());
                    tvResult.setTextColor(Color.WHITE);
                    tvResult.setBackgroundResource(R.drawable.main_dialog_bg);
                    tvName.setBackgroundResource(R.drawable.main_name_bg);
                    flDialog.setBackgroundResource(R.drawable.main_dialog);
                    clFrame.setBackgroundResource(R.drawable.main_frame);
                    ivService.setImageResource(R.drawable.icon_service);
                    delayTimes = 3000;
                    break;
                case ErrorCode.SUCCESS:
                    tvResult.setText(respBase.getMessage());
                    tvResult.setTextColor(Color.WHITE);
                    tvResult.setBackgroundResource(R.drawable.main_dialog_bg);
                    tvName.setBackgroundResource(R.drawable.main_name_bg);
                    flDialog.setBackgroundResource(R.drawable.main_dialog);
                    clFrame.setBackgroundResource(R.drawable.main_frame);
                    ivService.setImageResource(R.drawable.icon_service);
                    KdxfSpeechUtils.speekText(MainActivity.this, respBase.getVoice());
                    delayTimes = 3000;
                    break;
                case ErrorCode.WARING:
                    tvResult.setText(respBase.getMessage());
                    tvResult.setTextColor(Color.YELLOW);
                    tvResult.setBackgroundResource(R.drawable.main_dialog_bg);
                    tvName.setBackgroundResource(R.drawable.main_name_bg);
                    flDialog.setBackgroundResource(R.drawable.main_dialog);
                    clFrame.setBackgroundResource(R.drawable.main_frame);
                    ivService.setImageResource(R.drawable.icon_service);
                    KdxfSpeechUtils.speekText(MainActivity.this, respBase.getVoice());
                    delayTimes = 5000;
                    break;
                case ErrorCode.SYSTEM_ERROR:
                    tvResult.setText(respBase.getMessage());
                    tvResult.setTextColor(Color.YELLOW);
                    tvResult.setBackgroundResource(R.drawable.main_dialog_bg_error);
                    tvName.setBackgroundResource(R.drawable.main_name_bg_error);
                    flDialog.setBackgroundResource(R.drawable.main_dialog_error);
                    clFrame.setBackgroundResource(R.drawable.main_frame_error);
                    ivService.setImageResource(R.drawable.icon_service_error);
                    KdxfSpeechUtils.speekText(MainActivity.this, respBase.getVoice());
                    delayTimes = 5000;
                    break;
                default:
                    break;

            }
            h.removeCallbacks(resetRun);
            h.postDelayed(resetRun, delayTimes);
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };

    private Runnable resetRun = new Runnable() {
        @Override
        public void run() {
            SingleDispatcher.getInstance().getObservable().onNext(normalResp);
        }
    };
    private Observer cameraObserver = new Observer<WeakReference<CameraPreview.CameraFaceData>>() {

        @Override
        public void onSubscribe(Disposable d) {
            cameraDisposable = d;
        }

        @Override
        public void onNext(WeakReference<CameraPreview.CameraFaceData> weakcameraFaceData) {
            CameraPreview.CameraFaceData cameraFaceData = weakcameraFaceData.get();
            if (cameraFaceData == null) {
                return;
            }
            switch (cameraFaceData.getType()) {
                case CameraPreview.CameraFaceData.PREVIEW:
                    PreviewRequest request = new PreviewRequest();
                    request.setImageSize(cameraFaceData.getImageWidth(), cameraFaceData.getImageHeight())
                            .setFeatureData(cameraFaceData.getFeatureData())
                            .setFaceDetectResult(cameraFaceData.getFaceDetectResult())
                            .setFaceData(cameraFaceData.getFaceData());
                    SingleDispatcher.getInstance().add(request);
                    break;
                case CameraPreview.CameraFaceData.CAPTURE:
                    CapturePhotoRequest captureRequest = new CapturePhotoRequest();
                    captureRequest.setImageSize(cameraFaceData.getImageWidth(), cameraFaceData.getImageHeight())
                            .setFeatureData(cameraFaceData.getFeatureData())
                            .setFaceDetectResult(cameraFaceData.getFaceDetectResult())
                            .setFaceData(cameraFaceData.getFaceData());
                    SingleDispatcher.getInstance().add(captureRequest);
                    break;
                case CameraPreview.CameraFaceData.IDCARD_CAPTURE:
                    mIDCardCapturePublishSubject.onNext(cameraFaceData);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onError(Throwable e) {

        }

        @Override
        public void onComplete() {

        }
    };
    Handler h = new Handler() {
        @Override
        public void handleMessage(android.os.Message msg) {
            if (msg.what == 99 || msg.what == 100) {
                //statu.setText((String)msg.obj);
            }
            //第一次授权时候的判断是利用handler判断，授权过后就不用这个判断了
            if (msg.what == HandlerMsg.CONNECT_SUCCESS) {
                //"连接成功");
            }
            if (msg.what == HandlerMsg.CONNECT_ERROR) {
                //"连接失败";
                SingleDispatcher.getInstance().getObservable().onNext(new RespBase(ErrorCode.SYSTEM_ERROR, "身份证读卡器故障"));
            }
            if (msg.what == HandlerMsg.READ_ERROR) {
                //cz();
                //"卡认证失败"
                SingleDispatcher.getInstance().getObservable().onNext(new RespBase(ErrorCode.WARING, "卡认证失败"));
            }
            if (msg.what == HandlerMsg.READ_SUCCESS) {
                //"读卡成功"
                MyHSIDCardInfo ic = (MyHSIDCardInfo) msg.obj;
                SingleDispatcher.getInstance().getObservable().onNext(new RespBase(ErrorCode.WARING, getResources().getString(R.string.please_see_camera)));
                //清空历史人脸数据
                mIDCardCapturePublishSubject.onNext(new CameraPreview.CameraFaceData());
                //新拍人脸
                cameraView.takeIDCardPicture();

                mCardInfoPublishSubject.onNext(ic);
//                byte[] fp = new byte[1024];
//                fp = ic.getFpDate();
//                String m_FristPFInfo = "";
//                String m_SecondPFInfo = "";

//                if (ic.getcertType() == " ") {
//                    tv_info.setText("证件类型：身份证\n" + "姓名："
//                            + ic.getPeopleName() + "\n" + "性别：" + ic.getSex()
//                            + "\n" + "民族：" + ic.getPeople() + "\n" + "出生日期："
//                            + df.format(ic.getBirthDay()) + "\n" + "地址："
//                            + ic.getAddr() + "\n" + "身份号码：" + ic.getIDCard()
//                            + "\n" + "签发机关：" + ic.getDepartment() + "\n"
//                            + "有效期限：" + ic.getStrartDate() + "-"
//                            + ic.getEndDate() + "\n" + m_FristPFInfo + "\n"
//                            + m_SecondPFInfo);
//                } else {
//                    if(ic.getcertType() == "J")
//                    {
//                        tv_info.setText("证件类型：港澳台居住证（J）\n"
//                                + "姓名：" + ic.getPeopleName() + "\n" + "性别："
//                                + ic.getSex() + "\n"
//                                + "签发次数：" + ic.getissuesNum() + "\n"
//                                + "通行证号码：" + ic.getPassCheckID() + "\n"
//                                + "出生日期：" + df.format(ic.getBirthDay())
//                                + "\n" + "地址：" + ic.getAddr() + "\n" + "身份号码："
//                                + ic.getIDCard() + "\n" + "签发机关："
//                                + ic.getDepartment() + "\n" + "有效期限："
//                                + ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
//                                + m_FristPFInfo + "\n" + m_SecondPFInfo);
//                    }
//                    else{
//                        if(ic.getcertType() == "I")
//                        {
//                            tv_info.setText("证件类型：外国人永久居留证（I）\n"
//                                    + "英文名称：" + ic.getPeopleName() + "\n"
//                                    + "中文名称：" + ic.getstrChineseName() + "\n"
//                                    + "性别：" + ic.getSex() + "\n"
//                                    + "永久居留证号：" + ic.getIDCard() + "\n"
//                                    + "国籍：" + ic.getstrNationCode() + "\n"
//                                    + "出生日期：" + df.format(ic.getBirthDay())
//                                    + "\n" + "证件版本号：" + ic.getstrCertVer() + "\n"
//                                    + "申请受理机关：" + ic.getDepartment() + "\n"
//                                    + "有效期限："+ ic.getStrartDate() + "-" + ic.getEndDate() + "\n"
//                                    + m_FristPFInfo + "\n" + m_SecondPFInfo);
//                        }
//                    }
//
//                }


            }
        }


    };

    @Override
    protected void onStore(Bundle outState) {

    }

    @Override
    protected void onReStore(Bundle outState) {

    }

    @Override
    protected void onBeforeDestroy() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.onResume(this);
    }

    @Override
    protected void onPause() {
        cameraView.onPause();
        super.onPause();
    }

    public void takePic() {
        cameraView.takePicture();
    }


    @Override
    public void onDestroy() {
        closeLed();
        h.removeCallbacks(resetRun);
        if (msgDisposable != null && !msgDisposable.isDisposed()) {
            msgDisposable.dispose();
        }
        if (cameraDisposable != null && !cameraDisposable.isDisposed()) {
            cameraDisposable.dispose();
        }
        cameraView.onDestroy();
        mHxCardReadManager.close();
        FaceListManager.getInstance().onDestory();
        SingleDispatcher.getInstance().quit();
        IdFaceSdk.IdFaceSdkUninit();
        super.onDestroy();

    }

    @Override
    public void onBackPressed() {
        closeLed();
        if (msgDisposable != null && !msgDisposable.isDisposed()) {
            msgDisposable.dispose();
        }
        if (cameraDisposable != null && !cameraDisposable.isDisposed()) {
            cameraDisposable.dispose();
        }
        super.onBackPressed();
    }


    @OnClick(R.id.btn_custom)
    public void onViewClicked(View view) {
        takePic();
    }

    private void openLed(){
        AppInternal.getInstance().getIandosManager().ICE_LEDSwitch(true,false);
    }
    private void closeLed(){
        AppInternal.getInstance().getIandosManager().ICE_LEDSwitch(false,false);
    }
//    { 0, "" },
//    { 1, "汉" }, { 2, "蒙古" } , { 3, "回" }, { 4, "藏" }, { 5, "维吾尔" }, { 6, "苗" }, { 7, "彝" }, { 8, "壮" },
//    { 9, "布依" }, { 10, "朝鲜" }, { 11, "满" }, { 12, "侗" }, { 13, "瑶" }, { 14, "白" }, { 15, "土家" }, { 16, "哈尼" },
//    { 17, "哈萨克" }, { 18, "傣" }, { 19, "黎" }, { 20, "傈僳" }, { 21, "佤" }, { 22, "畲" }, {23, "高山" }, { 24, "拉祜" },
//    { 25, "水" }, { 26, "东乡" }, { 27, "纳西" }, { 28, "景颇" }, { 29, "柯尔克孜" }, { 30, "土" }, { 31, "达斡尔" }, { 32, "仫佬" },
//    { 33, "羌" }, { 34, "布朗" }, { 35, "撒拉" }, { 36, "毛南" }, { 37, "仡佬" }, { 38, "锡伯" }, { 39, "阿昌" }, { 40, "普米" },
//    { 41, "塔吉克" }, { 42, "怒" }, { 43, "乌孜别克" }, { 44, "俄罗斯" }, { 45, "鄂温克" }, { 46, "德昂" }, { 47, "保安" }, { 48, "裕固" },
//    { 49, "京" }, { 50, "塔塔尔" }, { 51, "独龙" }, { 52, "鄂伦春" }, { 53, "赫哲" }, { 54, "门巴" }, { 55, "珞巴" }, { 56, "基诺" },
//    { 97, "其它" }, { 98, "外国血统中国籍" }

    //{0,"未知"}，{1，"男"},{2,"女"}，{9，"其他"}
}

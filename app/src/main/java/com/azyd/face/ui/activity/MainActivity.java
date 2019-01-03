package com.azyd.face.ui.activity;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.azyd.face.R;
import com.azyd.face.base.ButterBaseActivity;
import com.azyd.face.base.RespBase;
import com.azyd.face.base.rxjava.AsynTransformer;
import com.azyd.face.constant.RoutePath;
import com.azyd.face.dispatcher.SingleDispatcher;
import com.azyd.face.dispatcher.core.FaceListManager;
import com.azyd.face.ui.request.CapturePhotoRequest;
import com.azyd.face.ui.request.IDCardCaptureRequest;
import com.azyd.face.ui.request.PreviewRequest;
import com.azyd.face.util.AppCompat;
import com.azyd.face.view.CameraPreview;
import com.huashi.otg.sdk.HandlerMsg;
import com.idcard.HXCardReadManager;
import com.idcard.MyHSIDCardInfo;
import com.idfacesdk.IdFaceSdk;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

@Route(path = RoutePath.MAIN)
public class MainActivity extends ButterBaseActivity {


    @BindView(R.id.cameraView)
    CameraPreview cameraView;
    @BindView(R.id.surfaceview)
    SurfaceView surfaceview;
    @BindView(R.id.tv_name)
    TextView tvName;
    @BindView(R.id.tv_result)
    TextView tvResult;
    @BindView(R.id.btn_custom)
    TextView btnCustom;
    Disposable mDisposable;
    private PublishSubject<MyHSIDCardInfo> mCardInfoPublishSubject;
    private PublishSubject<CameraPreview.CameraFaceData> mCapturePublishSubject;
    private HXCardReadManager mHxCardReadManager;
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
    }

    @Override
    protected void initData(Bundle savedInstanceState) {
        mCardInfoPublishSubject = PublishSubject.create();
        mCapturePublishSubject = PublishSubject.create();
        //启动身份证读卡器
        mHxCardReadManager = new HXCardReadManager(h,this);
        mHxCardReadManager.start();
        //启动单任务执行中心
        SingleDispatcher.getInstance().start();
        SingleDispatcher.getInstance().getObservable()
                .compose(new AsynTransformer())
                .subscribe(new Consumer<RespBase>() {
                               @Override
                               public void accept(RespBase result) {
                                   if(!TextUtils.isEmpty(result.getMessage())){
                                       tvResult.setText(result.getMessage());
                                   }

                                   if (mDisposable != null && !mDisposable.isDisposed()) {
                                       mDisposable.dispose();
                                   }
                                   mDisposable = Observable.just("欢迎使用")
                                           .delay(3, TimeUnit.SECONDS)
                                           .compose(new AsynTransformer())
                                           .subscribe(new Consumer<String>() {
                                               @Override
                                               public void accept(String s) throws Exception {
                                                   tvResult.setText(s);
                                               }
                                           });
                               }
                           }, new Consumer<Throwable>() {
                               @Override
                               public void accept(Throwable throwable) throws Exception {

                               }
                           }
                );
        //捕获相机拍照、预览、身份证拍照事件
        cameraView.getObservable().subscribe(new Consumer<CameraPreview.CameraFaceData>() {
                                                 @Override
                                                 public void accept(CameraPreview.CameraFaceData cameraFaceData) throws Exception {
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
                                                             mCapturePublishSubject.onNext(cameraFaceData);
                                                             break;
                                                         default:
                                                             break;
                                                     }
                                                 }
                                             }, new Consumer<Throwable>() {
                                                 @Override
                                                 public void accept(Throwable throwable) throws Exception {

                                                 }
                                             }
        );
        Observable.combineLatest(mCardInfoPublishSubject, mCapturePublishSubject, new BiFunction<MyHSIDCardInfo, CameraPreview.CameraFaceData, Boolean>() {
            @Override
            public Boolean apply(MyHSIDCardInfo myHSIDCardInfo, CameraPreview.CameraFaceData cameraFaceData) throws Exception {
                if(myHSIDCardInfo!=null&&cameraFaceData!=null){
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
        }).compose(new AsynTransformer())
                .subscribe(new Consumer() {
                    @Override
                    public void accept(Object o) throws Exception {

                    }
                });
    }

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
            }
            if (msg.what == HandlerMsg.READ_ERROR) {
                //cz();
                //"卡认证失败"

            }
            if (msg.what == HandlerMsg.READ_SUCCESS) {
                //"读卡成功"
                MyHSIDCardInfo ic = (MyHSIDCardInfo) msg.obj;
                cameraView.idCardCapturePicture();
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

        ;
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
        cameraView.onDestroy();
        mHxCardReadManager.close();
        FaceListManager.getInstance().onDestory();
        SingleDispatcher.getInstance().quit();
        IdFaceSdk.IdFaceSdkUninit();
        super.onDestroy();

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: add setContentView(...) invocation
        ButterKnife.bind(this);
    }

    @OnClick(R.id.btn_custom)
    public void onViewClicked(View view) {
        takePic();
    }
}

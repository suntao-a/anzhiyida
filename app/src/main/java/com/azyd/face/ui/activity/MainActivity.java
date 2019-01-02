package com.azyd.face.ui.activity;

import android.graphics.PixelFormat;
import android.os.Bundle;
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
import com.azyd.face.ui.request.PreviewRequest;
import com.azyd.face.util.AppCompat;
import com.azyd.face.view.CameraPreview;
import com.idfacesdk.IdFaceSdk;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

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
        SingleDispatcher.getInstance().start();
        SingleDispatcher.getInstance().getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
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
                                                     }
                                                     if (cameraFaceData.getType() == CameraPreview.CameraFaceData.PREVIEW) {


                                                     } else if (cameraFaceData.getType() == CameraPreview.CameraFaceData.CAPTURE) {

                                                     }
                                                 }
                                             }, new Consumer<Throwable>() {
                                                 @Override
                                                 public void accept(Throwable throwable) throws Exception {

                                                 }
                                             }
        );
    }

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

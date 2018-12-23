package com.azyd.face.ui.activity;

import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.azyd.face.R;
import com.azyd.face.base.ButterBaseActivity;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.constant.RoutePath;
import com.azyd.face.util.AppCompat;
import com.azyd.face.view.CameraPreview;
import com.idfacesdk.IdFaceSdk;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
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
        cameraView.getSingleDispatcher()
                .getObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                               @Override
                               public void accept(String s) {
                                   tvResult.setText(s);
                               }
                           }, new Consumer<Throwable>() {
                               @Override
                               public void accept(Throwable throwable) {
                                   tvResult.setText(throwable.getMessage());
                               }
                           }
                );
    }

    @Override
    protected void onStore(Bundle outState) {
        cameraView.onDestroy();
        IdFaceSdk.IdFaceSdkUninit();
    }

    @Override
    protected void onReStore(Bundle outState) {

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

    public void takePic(View view) {
        cameraView.takePicture();
    }


    @Override
    public void onDestroy() {
        cameraView.onDestroy();
        IdFaceSdk.IdFaceSdkUninit();
        super.onDestroy();

    }

}

package com.azyd.face.ui.activity;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.widget.TextView;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.azyd.face.R;
import com.azyd.face.app.AppInternal;
import com.azyd.face.base.ButterBaseActivity;
import com.azyd.face.base.ResponseBase;
import com.azyd.face.constant.RoutePath;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.AppCompat;
import com.azyd.face.util.PhoneInfoUtil;
import com.azyd.face.util.RequestParam;
import com.azyd.face.util.permission.PermissionReq;
import com.azyd.face.util.permission.PermissionResult;
import com.azyd.face.util.permission.Permissions;
import com.azyd.face.util.rxjava.ComposeUtils;
import com.idfacesdk.IdFaceSdk;

import java.lang.ref.WeakReference;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * @author suntao
 * @creat-time 2018/12/5 on 18:29
 * $describe$
 */
@Route(path = RoutePath.SPLASH)
public class SplashActivity extends ButterBaseActivity {
    Disposable disposable1,disposable2;
    @BindView(R.id.tv_process)
    TextView tvProcess;

    private String strCacheDir = "";
    private boolean bSdkInit = false;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void beforeSetContent() {
        AppCompat.setFullWindow(getWindow());
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        // 获取本APP的cache目录
        Context cont = this.getApplicationContext();
        strCacheDir = cont.getCacheDir().getAbsolutePath();
        PermissionReq.with(this).permissions(Permissions.CAMERA[0], Permissions.STORAGE[1], Permissions.PHONE[0])
                .result(new PermissionResult() {
                    @Override
                    public void onGranted() {
                        tvProcess.setText("申请权限成功..");
                        StartSdk();
                    }

                    @Override
                    public void onDenied() {
                        finish();
                    }
                }
                        .setShowGoSetting(false))
                .request();
    }

    public static class MyHandler extends Handler {
        WeakReference<Activity> mWeakReference;

        public MyHandler(Activity activity) {
            mWeakReference = new WeakReference<Activity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final Activity activity = mWeakReference.get();
            if (activity != null) {
                if (msg.what == 1) {

                }
            }
        }
    }

    @Override
    protected void initData(Bundle savedInstanceState) {

    }

    @Override
    protected void onStore(Bundle outState) {

    }

    @Override
    protected void onReStore(Bundle outState) {

    }

    @Override
    protected void onBeforeDestroy() {
        if (bSdkInit) {
            bSdkInit = false;
            IdFaceSdk.IdFaceSdkUninit();
        }
        disposable1.dispose();
        if(disposable2!=null){
            disposable2.dispose();
        }
    }

    protected void StartSdk() {
        Observable<ResponseBase> startsdk = Observable.create(new ObservableOnSubscribe<ResponseBase>() {
            @Override
            public void subscribe(ObservableEmitter<ResponseBase> e) {
                String str;
                ResponseBase response = new ResponseBase();
                if (bSdkInit == false) {
                    // 设置云授权信息,服务器IP地址需指定实际运行的云授权服务器地址
                    // 用户名及部门信息非必须，但可由终端设置或编辑后就可在服务器上按这些信息查询以方便管理
                    // 密码信息暂时无用，但用户名密码等信息将来或可用于扩展鉴权
//                            IdFaceSdk.IdFaceSdkSetServer(MainActivity.this, "172.21.12.76", 6389, "张三san", "8888888", "研发部e");
                    IdFaceSdk.IdFaceSdkSetServer(SplashActivity.this, "192.168.0.110", 6389, "张三san", "8888888", "研发部e");

                    int version = IdFaceSdk.IdFaceSdkVer();
                    // 初始化人脸算法
                    long tStart = System.nanoTime() / 1000000;
                    int ret = IdFaceSdk.IdFaceSdkInit(strCacheDir);
                    long tEnd = System.nanoTime() / 1000000;
                    if (ret == 0) {
                        bSdkInit = true;
                        str = "NET-SDK启动成功, 用时 " + (tEnd - tStart) + " 毫秒";
                        response.setCode(200);
                        response.setMessage(str);
                    } else {
                        str = "NET-SDK启动失败";
                        response.setCode(0);
                        response.setMessage(str);
                    }

                } else {
                    str = "NET-SDK已启动";
                    response.setCode(200);
                    response.setMessage(str);
                }


                e.onNext(response);
                e.onComplete();
            }
        }).subscribeOn(Schedulers.io());
        Observable<ResponseBase> startCheck = ServiceGenerator.createService(GateService.class).checkRegist(RequestParam.build(1).with("mac", PhoneInfoUtil.getIMEI(this)).create());
        disposable1 = Observable.concat( startCheck,startsdk)
                .compose(ComposeUtils.asynSchedule())
                .subscribe(new Consumer<ResponseBase>() {

                    @Override
                    public void accept(ResponseBase responseBase) throws Exception {
                        tvProcess.setText(responseBase.getMessage());
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {

                    }
                }, new Action() {
                    @Override
                    public void run() throws Exception {
                        disposable2 = Observable.timer(2, TimeUnit.SECONDS)
                                .subscribe(new Consumer<Long>() {
                                    @Override
                                    public void accept(Long aLong) {
                                        finish();
                                        ARouter.getInstance().build(RoutePath.MAIN).navigation();
                                    }
                                });
                    }
                });


    }

    private Observable<ResponseBase> initAppInternal(){
        return Observable.create(new ObservableOnSubscribe<ResponseBase>() {
            @Override
            public void subscribe(ObservableEmitter<ResponseBase> e) {
                AppInternal.getInstance().setIMEI(PhoneInfoUtil.getIMEI(getApplication()));
                ResponseBase response = new ResponseBase();
                response.setCode(200);
                response.setMessage("参数初始化完成...");
            }
        });
    }

}

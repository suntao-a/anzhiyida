package com.azyd.face.ui.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.iandos.IIandosService;
import android.iandos.IandosManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.android.arouter.facade.annotation.Route;
import com.alibaba.android.arouter.launcher.ARouter;
import com.azyd.face.R;
import com.azyd.face.app.AppInternal;
import com.azyd.face.base.ButterBaseActivity;
import com.azyd.face.base.RespBase;
import com.azyd.face.base.exception.RespThrowable;
import com.azyd.face.base.exception.ServerException;
import com.azyd.face.base.rxjava.AsynTransformer;
import com.azyd.face.base.rxjava.SimpleObserver;
import com.azyd.face.constant.ExtraName;
import com.azyd.face.constant.RoutePath;
import com.azyd.face.constant.URL;
import com.azyd.face.net.ServiceGenerator;
import com.azyd.face.ui.module.MacReponse;
import com.azyd.face.ui.service.GateService;
import com.azyd.face.util.AppCompat;
import com.azyd.face.util.MacUtils;
import com.azyd.face.util.PhoneInfoUtil;
import com.azyd.face.util.RequestParam;
import com.azyd.face.util.SharedPreferencesHelper;
import com.azyd.face.util.ShowMyToast;
import com.azyd.face.util.permission.PermissionReq;
import com.azyd.face.util.permission.PermissionResult;
import com.azyd.face.util.permission.Permissions;
import com.idfacesdk.IdFaceSdk;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import retrofit2.http.Url;

/**
 * @author suntao
 * @creat-time 2018/12/5 on 18:29
 * $describe$
 */
@Route(path = RoutePath.SPLASH)
public class SplashActivity extends ButterBaseActivity {
    Disposable disposable2;
    Disposable disposable1;
    Disposable disposable3;
    @BindView(R.id.tv_process)
    TextView tvProcess;
    @BindView(R.id.image_back)
    ImageView imageBack;
    AlertDialog dialog;
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
                        initBackground();
                    }

                    @Override
                    public void onDenied() {
                        finish();
                    }
                }
                        .setShowGoSetting(false))
                .request();
    }


    @Override
    protected void initData(Bundle savedInstanceState) {
        mTimer = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                isRun = true;
            }

            @Override
            public void onFinish() {
                isRun = false;
                count = 0;
            }
        };
    }

    @Override
    protected void onStore(Bundle outState) {

    }

    @Override
    protected void onReStore(Bundle outState) {

    }

    @Override
    protected void onBeforeDestroy() {
        if (disposable2 != null) {
            disposable2.dispose();
        }
        if (disposable3 != null) {
            disposable3.dispose();
        }
    }

    @Override
    public void onBackPressed() {
        if (disposable1 != null && !disposable1.isDisposed()) {
            disposable1.dispose();
        }
        if (disposable2 != null && !disposable2.isDisposed()) {
            disposable2.dispose();
        }
        if (disposable3 != null && !disposable3.isDisposed()) {
            disposable3.dispose();
        }
        if (bSdkInit) {
            bSdkInit = false;
            IdFaceSdk.IdFaceSdkUninit();
        }
        super.onBackPressed();
    }

    protected void initBackground() {

        Observable.concat(createInitMac(), createInitIandosManager(), createCheckMac(), createStartSDK())
                .compose(new AsynTransformer())
                .subscribe(new SimpleObserver() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable1 = d;
                    }

                    @Override
                    public void onError(RespThrowable responeThrowable) {
                        tvProcess.setText(responeThrowable.getMessage());
                    }

                    @Override
                    public void onSuccess(RespBase respBase) {
                        tvProcess.setText(respBase.getMessage());
                    }

                    @Override
                    public void onFail(RespBase respBase) {
                        tvProcess.setText(respBase.getMessage());
                    }

                    @Override
                    public void onComplete() {
                        if (dialog != null && dialog.isShowing()) {
                            return;
                        }
                        disposable2 = Observable.timer(2, TimeUnit.SECONDS)
                                .subscribe(new Consumer<Long>() {
                                    @Override
                                    public void accept(Long aLong) {
                                        finish();
                                        ARouter.getInstance().build(RoutePath.MAIN).navigation(SplashActivity.this);
                                    }
                                });

                    }
                });

    }

    private Observable createInitMac() {
        return Observable.create(new ObservableOnSubscribe<RespBase>() {
            @Override
            public void subscribe(ObservableEmitter<RespBase> e) throws Exception {
                RespBase response = new RespBase();
                response.setCode(200);
                response.setMessage("mac获取...");
                e.onNext(response);
                try {
                    MacReponse macReponse = ServiceGenerator.createService(GateService.class).getMac().execute().body();
                    AppInternal.getInstance().setIMEI(macReponse.getMac().toUpperCase().replace(":", "-"));
                    response.setCode(200);
                    response.setMessage("mac获取成功");
                    e.onNext(response);
                    e.onComplete();
                } catch (Exception e1) {

                    String mac = MacUtils.getMobileMAC(getApplication()).toUpperCase().replace(":", "-");
                    AppInternal.getInstance().setIMEI(mac);

                    if (!TextUtils.isEmpty(AppInternal.getInstance().getIMEI())) {
                        response.setCode(200);
                        response.setMessage("mac获取成功");
                        e.onNext(response);
                        e.onComplete();
                    } else {
                        throw new ServerException(404, "mac地址获取失败");
                    }
                }
                AppInternal.getInstance().setIandosManager((IandosManager) getSystemService("iandos"));
//                AppInternal.getInstance().setStrangerDetectCount(SharedPreferencesHelper);
                AppInternal.getInstance().setSdkIP((String) SharedPreferencesHelper.getInstance().get(ExtraName.SERVICE_IP, URL.BASE));
                AppInternal.getInstance().setIdcardThreshold(Integer.parseInt((String)SharedPreferencesHelper.getInstance().get(ExtraName.IDCARD_THRESHOLD, "65")));
                AppInternal.getInstance().setServiceIP((String) SharedPreferencesHelper.getInstance().get(ExtraName.SERVICE_IP, ""));
                AppInternal.getInstance().setInOut((Integer) SharedPreferencesHelper.getInstance().get(ExtraName.IN_OUT, 0));
                AppInternal.getInstance().setPreviewThreshold(Integer.parseInt((String)SharedPreferencesHelper.getInstance().get(ExtraName.PREVIEW_THRESHOLD, "70")));

            }
        });
    }
//
//    private Observable createInitIandosManager() {
//        return Observable.create(new ObservableOnSubscribe<RespBase>() {
//            @Override
//            public void subscribe(ObservableEmitter<RespBase> e) throws Exception {
//                RespBase response = new RespBase();
//                response.setCode(200);
//                response.setMessage("IandosManager初始化...");
//                e.onNext(response);
//                AppInternal.getInstance().setIandosManager((IandosManager) getSystemService("iandos"));
//                if (AppInternal.getInstance().getIandosManager() != null) {
//                    response.setCode(200);
//                    response.setMessage("IandosManager初始化成功");
//                    e.onNext(response);
//                    e.onComplete();
//                } else {
////                    throw new ServerException(404, "IandosManager初始化失败...");
//                    e.onComplete();
//                }
//
//            }
//        });
//    }

    private Observable createCheckMac() {
        return Observable.create(new ObservableOnSubscribe<RespBase>() {
            @Override
            public void subscribe(ObservableEmitter<RespBase> e) throws ServerException {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                String str;
                RespBase response = new RespBase();
                response.setCode(200);
                response.setMessage("设备在线检测...");
                e.onNext(response);
                try {
                    response = ServiceGenerator.createService(GateService.class).checkRegist(AppInternal.getInstance().getBaseUrl()+URL.CHECK_REGIST,RequestParam.build(1).with("mac", AppInternal.getInstance().getIMEI()).create())
                            .execute().body();
                    if (response != null) {
                        if (response.isSuccess()) {
                            response.setMessage("设备在线检测成功");
                            e.onNext(response);
                            e.onComplete();
                        } else {
                            throw new ServerException(response.getCode(), "设备mac:" + AppInternal.getInstance().getIMEI() + "\n" + response.getMessage());
                        }

                    } else {
                        response = new RespBase();
                        response.setCode(200);
                        response.setMessage("设备在线检测...");
                        e.onNext(response);
                        e.onComplete();
//                        throw new ServerException(404, "设备mac:" + AppInternal.getInstance().getIMEI());
                    }
                } catch (IOException e1) {
                    throw new ServerException(404, "核验主机故障");
                }

            }
        });
    }

    private Observable createStartSDK() {
        return Observable.create(new ObservableOnSubscribe<RespBase>() {
            @Override
            public void subscribe(ObservableEmitter<RespBase> e) throws ServerException {
                RespBase response = new RespBase();
                response.setCode(200);
                response.setMessage("NET-SDK启动...");
                e.onNext(response);
                String str;
                if (bSdkInit == false) {
                    // 设置云授权信息,服务器IP地址需指定实际运行的云授权服务器地址
                    // 用户名及部门信息非必须，但可由终端设置或编辑后就可在服务器上按这些信息查询以方便管理
                    // 密码信息暂时无用，但用户名密码等信息将来或可用于扩展鉴权
//                            IdFaceSdk.IdFaceSdkSetServer(MainActivity.this, "192.168.0.107", 6389, "张三san", "8888888", "研发部e");
//                    String ip = (String) SharedPreferencesHelper.getInstance().get(ExtraName.SDK_IP, "192.168.0.106");
                    String ip = (String) SharedPreferencesHelper.getInstance().get(ExtraName.SDK_IP, "http://8wr7rx.natappfree.cc");
                    IdFaceSdk.IdFaceSdkSetServer(SplashActivity.this, ip, 6389, "张三san", "8888888", "研发部e");

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
                        throw new ServerException(404, str);
                    }

                } else {
                    str = "NET-SDK已启动";
                    response.setCode(200);
                    response.setMessage(str);
                }


                e.onNext(response);
                e.onComplete();
            }
        });
    }

    CountDownTimer mTimer;
    int count = 0;
    final int sum = 4;
    boolean isRun = false;

    @OnClick(R.id.image_back)
    public void onViewClicked() {
        if (!isRun) {
            mTimer.start();
        }
        count++;

        if (count < sum) {
            if (sum - count < 2) {
                Toast toast = Toast.makeText(this, "再点击" + (sum - count) + "次", Toast.LENGTH_LONG);
                ShowMyToast.show(toast, 700);
            }


        } else {
            if (disposable1 != null && !disposable1.isDisposed()) {
                disposable1.dispose();
            }
            if (disposable2 != null && !disposable2.isDisposed()) {
                disposable2.dispose();
            }
            count = 0;
            View view = getLayoutInflater().inflate(R.layout.dialog_view, null);
            final EditText editSdkIP = (EditText) view.findViewById(R.id.et_ip);
            final EditText editServiceIP = (EditText) view.findViewById(R.id.et_server_ip);
            final EditText editPreviewTh = (EditText) view.findViewById(R.id.et_preview_threshold);
            final EditText editIDCardTh = (EditText) view.findViewById(R.id.et_idcard_threshold);
            final Spinner spInOut = (Spinner) view.findViewById(R.id.sp_inout);
            String sdkIP = (String) SharedPreferencesHelper.getInstance().get(ExtraName.SDK_IP, "");
            String serviceIP = (String) SharedPreferencesHelper.getInstance().get(ExtraName.SERVICE_IP, "");
            String previewTh = (String) SharedPreferencesHelper.getInstance().get(ExtraName.PREVIEW_THRESHOLD, "");
            String idCardTh = (String) SharedPreferencesHelper.getInstance().get(ExtraName.IDCARD_THRESHOLD, "");
            int inOut = (Integer) SharedPreferencesHelper.getInstance().get(ExtraName.IN_OUT, 0);
            editSdkIP.setText(sdkIP);
            editServiceIP.setText(serviceIP);
            editPreviewTh.setText(previewTh);
            editIDCardTh.setText(idCardTh);
            spInOut.setSelection(inOut);
            dialog = new AlertDialog.Builder(this)
                    .setIcon(R.mipmap.ic_launcher)//设置标题的图片
                    .setTitle("系统设置")//设置对话框的标题
                    .setView(view)
                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            SharedPreferencesHelper.getInstance().put(ExtraName.SDK_IP, editSdkIP.getText().toString().trim());
                            SharedPreferencesHelper.getInstance().put(ExtraName.SERVICE_IP, editServiceIP.getText().toString().trim());
                            SharedPreferencesHelper.getInstance().put(ExtraName.PREVIEW_THRESHOLD, editPreviewTh.getText().toString().trim());
                            SharedPreferencesHelper.getInstance().put(ExtraName.IDCARD_THRESHOLD, editIDCardTh.getText().toString().trim());
                            SharedPreferencesHelper.getInstance().put(ExtraName.IN_OUT, spInOut.getSelectedItemPosition());
                            Toast.makeText(SplashActivity.this, "保存成功,即将重新启动", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            disposable3 = Observable.timer(2, TimeUnit.SECONDS)
                                    .subscribe(new Consumer<Long>() {
                                        @Override
                                        public void accept(Long aLong) {
                                            finish();
                                            ARouter.getInstance().build(RoutePath.SPLASH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).navigation(SplashActivity.this);
                                        }
                                    });
                        }
                    }).create();
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }
}

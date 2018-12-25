package com.azyd.face.base;

import android.os.Bundle;
import android.support.annotation.NonNull;

import com.azyd.face.util.permission.PermissionReq;

import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * @author suntao
 * @creat-time 2018/11/21 on 15:14
 * $describe$
 */
public abstract class ButterBaseActivity extends BaseActivity{
    private final String STORE_DATA = "store_data";
    Unbinder unbinder;

    /**
     * 返回布局ID
     * @return
     */
    protected abstract int getLayoutId();

    /**
     * 初始化view
     * @param savedInstanceState
     */
    protected abstract void initView(Bundle savedInstanceState);

    /**
     * 初始化数据
     * @param savedInstanceState
     */
    protected abstract void initData(Bundle savedInstanceState);

    /**
     * 保存数据
     * @param outState
     */
    protected abstract void onStore(Bundle outState);

    /**
     * 恢复数据
     * @param outState
     */
    protected abstract void onReStore(Bundle outState);
    protected abstract void onBeforeDestroy();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            Bundle store;
            if((store = savedInstanceState.getBundle("STORE_DATA"))!=null){
                onReStore(store);
            }
        }
        beforeSetContent();
        setContentView(getLayoutId());
        unbinder = ButterKnife.bind(this);
        initView(savedInstanceState);
        initData(savedInstanceState);
    }
    protected void beforeSetContent(){

    }
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Bundle storeData = new Bundle();
        onStore(storeData);
        outState.putBundle(STORE_DATA,storeData);

    }
    @Override
    public void onDestroy() {
        onBeforeDestroy();
        super.onDestroy();
        unbinder.unbind();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionReq.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

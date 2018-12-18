package com.azyd.face.util.permission;

public abstract class PermissionResult {
    Object mContext;
    boolean showGoSetting = true;
    public Object getContext() {
        return mContext;
    }
    public PermissionResult setShowGoSetting(boolean show){
        showGoSetting = show;
        return this;
    }

    public boolean isShowGoSetting() {
        return showGoSetting;
    }

    public void setContext(Object context) {
        mContext = context;
    }

    public abstract void onGranted();

    public abstract void onDenied();
}

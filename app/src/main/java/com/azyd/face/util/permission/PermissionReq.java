package com.azyd.face.util.permission;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.SparseArray;


import com.azyd.face.R;

import java.util.ArrayList;
import java.util.List;

public class PermissionReq {
    private static int sRequestCode = 0;
    private static SparseArray<PermissionResult> sResultArray = new SparseArray<>();

    private Object mObject;
    private String[] mPermissions;
    private PermissionResult mResult;

    private PermissionReq(Object object) {
        mObject = object;
    }

    public static PermissionReq with(@NonNull Activity activity) {
        return new PermissionReq(activity);
    }

    public static PermissionReq with(@NonNull Fragment fragment) {
        return new PermissionReq(fragment);
    }

    public PermissionReq permissions(@NonNull String... permissions) {
        mPermissions = permissions;
        return this;
    }

    public PermissionReq result(@Nullable PermissionResult result) {
        mResult = result;
        mResult.setContext(mObject);
        return this;
    }

    public void request() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            if (mResult != null) {
                mResult.onGranted();
            }
            return;
        }

        Activity activity = getActivity(mObject);
        if (activity == null) {
            throw new IllegalArgumentException(mObject.getClass().getName() + " is not supported");
        }

        List<String> deniedPermissionList = getDeniedPermissions(activity, mPermissions);
        if (deniedPermissionList.isEmpty()) {
            if (mResult != null) {
                mResult.onGranted();
            }
            return;
        }

        int requestCode = genRequestCode();
        String[] deniedPermissions = deniedPermissionList.toArray(new String[deniedPermissionList.size()]);
        requestPermissions(mObject, deniedPermissions, requestCode);
        sResultArray.put(requestCode, mResult);
    }

    public static void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        int index = (requestCode >> 16) & 0xffff;//去掉高于16位的(index)
        if(index!=0){
            requestCode=requestCode& 0xffff;//去掉高于16位的(index)
        }
        PermissionResult result = sResultArray.get(requestCode);

        if (result == null) {
            return;
        }
        sResultArray.remove(requestCode);
        verifyPermissions(result, permissions, grantResults);
//        for (int grantResult : grantResults) {
//            if (grantResult != PackageManager.PERMISSION_GRANTED) {
//                result.onDenied();
//                return;
//            }
//        }
//        result.onGranted();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void requestPermissions(Object object, String[] permissions, int requestCode) {
        if (object instanceof Activity) {
            ((Activity) object).requestPermissions(permissions, requestCode);
        } else if (object instanceof Fragment) {
            ((Fragment) object).requestPermissions(permissions, requestCode);
        }
    }

    private static List<String> getDeniedPermissions(Context context, String[] permissions) {
        List<String> deniedPermissionList = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissionList.add(permission);
            }
        }
        return deniedPermissionList;
    }

    private static Activity getActivity(Object object) {
        if (object != null) {
            if (object instanceof Activity) {
                return (Activity) object;
            } else if (object instanceof Fragment) {
                return ((Fragment) object).getActivity();
            }
        }
        return null;
    }

    /**
     * 检测是否说有的权限都已经授权
     *
     * @param grantResults 授权结果
     * @return 是否授权
     */
    private static boolean verifyPermissions(PermissionResult result, @NonNull String[] permissions, @NonNull int[] grantResults) {
        List<String> deniedPermissions = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            int grantResult = grantResults[i];
            String permission = permissions[i];
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(permission);
            }
        }
        boolean isGranted = deniedPermissions.isEmpty();
        if (result != null) {
            if (isGranted) {
                result.onGranted();
            } else {
                if(result.isShowGoSetting()){
                    showMissingPermissionDialog(getActivity(result.getContext()), deniedPermissions);
                }
                result.onDenied();
            }
        }
        return isGranted;
    }

    private static int genRequestCode() {
        return ++sRequestCode;
    }

    public static void showMissingPermissionDialog(final Context activity, List<String> permissions) {
        if (activity == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for(String permiss:permissions){
            if(PermissionTable.get(permiss)!=null){
                sb.append(PermissionTable.get(permiss));
                sb.append("、");
            }
        }




        String content = activity.getString(R.string.permissions_content, sb.length()>0?sb.toString().substring(0,sb.length()-1):"相关");
        AlertDialog mAlertDialog = new AlertDialog.Builder(activity)
                .setTitle("提示")
                .setMessage(content)
                .setPositiveButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setNegativeButton("设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent intent = new Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + activity.getPackageName()));
                        activity.startActivity(intent);
                    }
                }).show();
    }
}

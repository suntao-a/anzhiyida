package com.azyd.face.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.alibaba.android.arouter.launcher.ARouter;
import com.azyd.face.constant.RoutePath;
import com.azyd.face.ui.activity.SplashActivity;

/**
 * @author suntao
 * @creat-time 2019/1/15 on 11:30
 * $describe$
 */
public class BootCompletedReceiver extends BroadcastReceiver
{
    public BootCompletedReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
//        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent i = new Intent(context, SplashActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
//            ARouter.getInstance().build(RoutePath.SPLASH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).navigation();
//        }
    }
}
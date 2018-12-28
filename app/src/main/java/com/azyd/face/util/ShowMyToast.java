package com.azyd.face.util;

import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author suntao
 * @creat-time 2018/12/27 on 18:57
 * $describe$
 */
public class ShowMyToast {
    public static void show(final Toast toast, final int cnt) {
        final Timer timer =new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                toast.show();
            }
        },0,3000);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                toast.cancel();
                timer.cancel();
            }
        }, cnt );
    }

}

package com.azyd.face.dispatcher;

import android.os.Process;
import android.util.Log;

import com.azyd.face.base.RespBase;
import com.azyd.face.constant.CameraConstant;
import com.azyd.face.dispatcher.core.BaseRequest;
import com.azyd.face.dispatcher.core.FaceListManager;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * @author suntao
 * @creat-time 2018/12/5 on 13:45
 * $describe$
 */
public class SingleDispatcher extends Thread {
    private static SingleDispatcher mInstance;
    private PublishSubject<RespBase> mSubject = PublishSubject.create();
    private final String TAG = "SingleDispatcher";
    private final BlockingQueue<BaseRequest> mPriorityQueue;
    private volatile boolean mQuit = false;
    private volatile boolean isBusy = false;
    private volatile int mCurrentPriority = -1;
    private final int REQUEST_TIMEOUT = 5000;
    private BaseRequest mCurrentRequest;
    private Future<RespBase> mCurrentfuture;
    private ThreadPoolExecutor mExecutor;
    public Observable<RespBase> getObservable(){
        return mSubject;
    }
    public static SingleDispatcher getInstance(){
        if(mInstance==null){
            synchronized (FaceListManager.class){
                if(mInstance==null){
                    mInstance = new SingleDispatcher();

                }
            }
        }
        return mInstance;
    }
    private SingleDispatcher() {
        isBusy = false;
        mPriorityQueue = new PriorityBlockingQueue<>();
        ThreadFactory threadFactory = new ThreadFactory() {
            int counter = 1;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "SingleDispatcher-" + "-Threadpool_" + counter);
                counter++;
                return t;
            }
        };
        mExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    public void add(BaseRequest request) {
        if (!isBusy) {
            //没有任务再执行，就加入新任务
            mPriorityQueue.offer(request);
            Log.e(TAG, "add成功");
        } else {
            if (mCurrentRequest != null) {
                try {
                    if (mCurrentRequest.getPriority() >= request.getPriority()) {
                        //新加的优先级底，不处理
                        Log.e(TAG, "add拒绝");
                        return;
                    } else {
                        //新加的优先级高于正在执行的，则取消正在执行的，并加入
                        mCurrentfuture.cancel(true);
                        mPriorityQueue.clear();
                        mPriorityQueue.offer(request);
                        Log.e(TAG, "add替换成功");
                    }
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
    }

    public void quit() {
        mQuit = true;
        mExecutor.shutdownNow();
        interrupt();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        Log.e("SingleDispatcher", "SingleDispatcher-主线程-" + Thread.currentThread().getName());
        BaseRequest request;
        while (true) {
            try {
                request = mPriorityQueue.take();
                isBusy = true;
                mCurrentRequest = request;
            } catch (InterruptedException e) {
                if (mQuit) {
                    return;
                }
                continue;
            }
            try {
                mCurrentfuture = mExecutor.submit(mCurrentRequest);
                RespBase result = mCurrentfuture.get();
                mSubject.onNext(result);
            } catch (CancellationException e) {
                Log.e(TAG, "被取消");
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                isBusy = false;
            }
        }
    }
}

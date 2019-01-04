package com.azyd.face.view;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.azyd.face.app.AppContext;
import com.azyd.face.base.RespBase;
import com.azyd.face.constant.CameraConstant;

import com.azyd.face.constant.ErrorCode;
import com.azyd.face.dispatcher.request.DemoRequest;
import com.azyd.face.dispatcher.core.FaceListManager;
import com.azyd.face.dispatcher.SingleDispatcher;

import com.azyd.face.dispatcher.request.SameFaceTestRequest;
import com.azyd.face.util.Utils;
import com.idfacesdk.FACE_DETECT_RESULT;
import com.idfacesdk.IdFaceSdk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

/**
 * @author suntao
 * @creat-time 2018/11/22 on 10:47
 * $describe$
 */
public class CameraPreview extends TextureView {
    private PublishSubject<CameraFaceData> mSubject = PublishSubject.create();
    private CameraConstant.ICameraParam mCameraParam;
    private static final String TAG = "CameraPreview";
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();//从屏幕旋转转换为JPEG方向
    private static final int MAX_PREVIEW_WIDTH = 1920;//Camera2 API 保证的最大预览宽高
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final int STATE_PREVIEW = 0;//显示相机预览
    private static final int STATE_WAITING_LOCK = 1;//焦点锁定中
    private static final int STATE_WAITING_PRE_CAPTURE = 2;//拍照中
    private static final int STATE_WAITING_NON_PRE_CAPTURE = 3;//其它状态
    private static final int STATE_PICTURE_TAKEN = 4;//拍照完毕

    private boolean mFlashSupported;
    private int mPhotoAngle = -90;
    private int mInterval = 5000;
    private boolean mSwitchAspect = true;
    private String mCameraId = "0";
    private int mState = STATE_PREVIEW;
    private int mRatioWidth = 0, mRatioHeight = 0;
    private int mSensorOrientation;

    private boolean mMirror = true;
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);//使用信号量 Semaphore 进行多线程任务调度
    private Activity activity;
    private File mFile;
    private String mPath;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private Size mPreviewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;
    private CameraCaptureSession mCaptureSession;
    private ImageReader mImageReader;
    private ImageReader mImagePreviewReader;
    private ImageReader mImageIdCardCaptureReader;
    private Paint mPaint;
    private RectF mRectF = new RectF();
    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;
    private boolean mFaceDetectSupported;
    private Integer mFaceDetectMode;
    Size cPixelSize;//相机成像尺寸
    boolean isFront = true;


    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public CameraPreview(Context context) {
        this(context, null);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mFile = new File(getContext().getExternalFilesDir(null), "pic.jpg");
        mPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath() + File.separator + AppContext.getInstance().getPackageName() + File.separator;
        mPaint = new Paint();
        mPaint.setColor(Color.parseColor("#FFD700"));
        mPaint.setStyle(Paint.Style.STROKE);//空心

        // 设置paint的外框宽度
        mPaint.setStrokeWidth(5f);
        if (CameraConstant.getCameraParam() != null) {
            mInterval = CameraConstant.getCameraParam().getInterval();
            mPhotoAngle = CameraConstant.getCameraParam().getPhotoRotate();
            mMirror = CameraConstant.getCameraParam().isMirror();
            mSwitchAspect = CameraConstant.getCameraParam().isViewNeedSwitchAspect();
            mCameraId = CameraConstant.getCameraParam().getCameraId();
        }
    }

    public Observable<CameraFaceData> getObservable() {
        return mSubject;
    }

    public void setSurfaceView(SurfaceView surface) {
        mSurfaceView = surface;
        mSurfaceHolder = mSurfaceView.getHolder();
    }

    /**
     * 根据mCameraId打开相机
     */
    private void openCamera(int width, int height) {
        setUpCameraOutputs(width, height);
        configureTransform(width, height);
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                SingleDispatcher.getInstance().getObservable().onNext(new RespBase(ErrorCode.SYSTEM,"摄像机被占用"));
                return;
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
        }
    }

    /**
     * 相机状态改变回调
     */
    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            Log.d(TAG, "相机已打开");
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
            if (null != activity) {
                activity.finish();
            }
        }
    };

    /**
     * 为相机预览创建新的CameraCaptureSession
     */
    private void createCameraPreviewSession() {
        try {
            SurfaceTexture texture = this.getSurfaceTexture();
            assert texture != null;
            // 将默认缓冲区的大小配置为想要的相机预览的大小
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImagePreviewReader.getSurface());
            mPreviewRequestBuilder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE,
                    CameraMetadata.STATISTICS_FACE_DETECT_MODE_FULL);
            // 我们创建一个 CameraCaptureSession 来进行相机预览
            mCameraDevice.createCaptureSession(Arrays.asList(surface,mImageIdCardCaptureReader.getSurface(), mImageReader.getSurface(), mImagePreviewReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                            if (null == mCameraDevice) {
                                return;
                            }
                            // 会话准备好后，我们开始显示预览
                            mCaptureSession = cameraCaptureSession;
                            try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                setAutoFlash(mPreviewRequestBuilder);
                                mPreviewRequest = mPreviewRequestBuilder.build();
                                //设置人脸检测
                                setFaceDetect(mPreviewRequestBuilder, mFaceDetectMode);
                                mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mBackgroundHandler);

                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        }
                    }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setFaceDetect(CaptureRequest.Builder builder, Integer faceDetectMode) {
        builder.set(CaptureRequest.STATISTICS_FACE_DETECT_MODE, faceDetectMode);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (0 == mRatioWidth || 0 == mRatioHeight) {
            setMeasuredDimension(width, height);
        } else {
            if (width < height * mRatioWidth / mRatioHeight) {
                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
            } else {
                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
            }
        }
//        if(mSurfaceView!=null){
//            mSurfaceView.setLayoutParams(new FrameLayout.LayoutParams(getMeasuredWidth(),getMeasuredHeight()));
//            mSurfaceView.setX(getX());
//            mSurfaceView.setY(getY());
//        }
    }


    public void onResume(Activity activity) {
        this.activity = activity;
        startBackgroundThread();
        //当Activity或Fragment OnResume()时,可以冲洗打开一个相机并开始预览,否则,这个Surface已经准备就绪
        if (this.isAvailable()) {
            openCamera(this.getWidth(), this.getHeight());
        } else {
            this.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    public void onPause() {
        closeCamera();
        stopBackgroundThread();
    }

    public void setOutPutDir(File file) {
        this.mFile = file;
    }

    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size can't be negative");
        }
        mRatioWidth = width;
        mRatioHeight = height;
        requestLayout();
    }

    public void setAutoFlash(CaptureRequest.Builder requestBuilder) {
        if (mFlashSupported) {
            requestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
    }

    public void takePicture() {
        lockFocus();
//        captureStillPicture();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 处理生命周期内的回调事件
     */
    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    };


    /**
     * 处理与照片捕获相关的事件
     */
    private CameraCaptureSession.CaptureCallback mCaptureCallback = new CameraCaptureSession.CaptureCallback() {

        private void process(CaptureResult result) {

//            detectFaces(result);

            switch (mState) {
                case STATE_PREVIEW: {
                    break;
                }
                case STATE_WAITING_LOCK: {
                    Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState == null) {
                        captureStillPicture();
                    } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                        Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                        if (aeState == null || aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                            mState = STATE_PICTURE_TAKEN;
                            captureStillPicture();
                        } else {
                            runPreCaptureSequence();
                        }
                    }
                    break;
                }
                case STATE_WAITING_PRE_CAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                            aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                        mState = STATE_WAITING_NON_PRE_CAPTURE;
                    }
                    break;
                }
                case STATE_WAITING_NON_PRE_CAPTURE: {
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    }
                    break;
                }
                default:
                    break;
            }
        }

        @Override
        public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                        @NonNull CaptureRequest request,
                                        @NonNull CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }

    };

    protected void detectFaces(CaptureResult captureResult) {
        //获得Face类
        Face faces[] = captureResult.get(CaptureResult.STATISTICS_FACES);
        Canvas canvas = mSurfaceHolder.lockCanvas();
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);//旧画面清理覆盖

        if (faces.length > 0) {
            for (int i = 0; i < faces.length; i++) {
                Rect fRect = faces[i].getBounds();

                //人脸检测坐标基于相机成像画面尺寸以及坐标原点。此处进行比例换算
                //成像画面与方框绘制画布长宽比比例（同画面角度情况下的长宽比例（此处前后摄像头成像画面相对预览画面倒置（±90°），计算比例时长宽互换））
                float scaleWidth = canvas.getHeight() * 1.0f / cPixelSize.getWidth();
                float scaleHeight = canvas.getWidth() * 1.0f / cPixelSize.getHeight();

                //坐标缩放
                int l = (int) (fRect.left * scaleWidth);
                int t = (int) (fRect.top * scaleHeight);
                int r = (int) (fRect.right * scaleWidth);
                int b = (int) (fRect.bottom * scaleHeight);

                //人脸检测坐标基于相机成像画面尺寸以及坐标原点。此处进行坐标转换以及原点(0,0)换算
                //人脸检测：坐标原点为相机成像画面的左上角，left、top、bottom、right以成像画面左上下右为基准
                //画面旋转后：原点位置不一样，根据相机成像画面的旋转角度需要换算到画布的左上角，left、top、bottom、right基准也与原先不一样，
                //如相对预览画面相机成像画面角度为90°那么成像画面坐标的top，在预览画面就为left。如果再翻转，那成像画面的top就为预览画面的right，且坐标起点为右，需要换算到左边
                if (isFront) {
                    //此处前置摄像头成像画面相对于预览画面顺时针90°+翻转。left、top、bottom、right变为bottom、right、top、left，并且由于坐标原点由左上角变为右下角，X,Y方向都要进行坐标换算
                    canvas.drawRect(canvas.getWidth() - b, canvas.getHeight() - r, canvas.getWidth() - t, canvas.getHeight() - l, mPaint);
                } else {
                    //此处后置摄像头成像画面相对于预览画面顺时针270°，left、top、bottom、right变为bottom、left、top、right，并且由于坐标原点由左上角变为左下角，Y方向需要进行坐标换算
                    canvas.drawRect(canvas.getWidth() - b, l, canvas.getWidth() - t, r, mPaint);
                }
            }
        }
        mSurfaceHolder.unlockCanvasAndPost(canvas);
    }

    /**
     * 在确定相机预览大小后应调用此方法
     *
     * @param viewWidth  宽
     * @param viewHeight 高
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mPreviewSize || null == activity) {
            return;
        }
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        this.setTransform(matrix);
    }


    /**
     * 关闭相机
     */
    private void closeCamera() {
        try {
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReader) {
                mImageReader.close();
                mImageReader = null;
            }
            if (null != mImageIdCardCaptureReader) {
                mImageIdCardCaptureReader.close();
                mImageIdCardCaptureReader = null;
            }
            if (null != mImagePreviewReader) {
                mImagePreviewReader.close();
                mImagePreviewReader = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    /**
     * 设置相机相关的属性或变量
     *
     * @param width  相机预览的可用尺寸的宽度
     * @param height 相机预览的可用尺寸的高度
     */
    @SuppressWarnings("SuspiciousNameCombination")
    private void setUpCameraOutputs(int width, int height) {
        CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                if (!cameraId.equals(mCameraId)) {
                    continue;
                }
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);


                // 在这个例子中不使用前置摄像头
//                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
//                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
//                    continue;
//                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    continue;
                }

                Size largest = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        new CompareSizesByArea());


                int displayRotation = activity.getWindowManager().getDefaultDisplay().getRotation();
                // noinspection ConstantConditions
                mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                boolean swappedDimensions = false;
                switch (displayRotation) {
                    case Surface.ROTATION_0:
                    case Surface.ROTATION_180:
                        if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                            swappedDimensions = true;
                        }
                        break;
                    case Surface.ROTATION_90:
                    case Surface.ROTATION_270:
                        if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                            swappedDimensions = true;
                        }
                        break;
                    default:
                        Log.e(TAG, "Display rotation is invalid: " + displayRotation);
                }

                Point displaySize = new Point();
                activity.getWindowManager().getDefaultDisplay().getSize(displaySize);
                int rotatedPreviewWidth = width;
                int rotatedPreviewHeight = height;
//                int maxPreviewWidth = displaySize.x;
//                int maxPreviewHeight = displaySize.y;
                int maxPreviewWidth = width;
                int maxPreviewHeight = height;

//                if (swappedDimensions) {
//                    rotatedPreviewWidth = height;
//                    rotatedPreviewHeight = width;
//                    maxPreviewWidth = displaySize.y;
//                    maxPreviewHeight = displaySize.x;
//                }

                if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                    maxPreviewWidth = MAX_PREVIEW_WIDTH;
                }

                if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                    maxPreviewHeight = MAX_PREVIEW_HEIGHT;
                }
                largest = new Size(width, height);
                mPreviewSize = chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                        rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                        maxPreviewHeight, largest);
                cPixelSize = characteristics.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE);//获取成像尺寸，同上
                mImageReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                        ImageFormat.JPEG, 1);

                mImageReader.setOnImageAvailableListener(
                        mOnImageAvailableListener, mBackgroundHandler);

                mImagePreviewReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                        ImageFormat.JPEG, 1);
                mImagePreviewReader.setOnImageAvailableListener(
                        mOnImagePreViewAvailableListener, mBackgroundHandler);
                mImageIdCardCaptureReader = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(),
                        ImageFormat.JPEG, 1);
                mImageIdCardCaptureReader.setOnImageAvailableListener(
                        mOnIdCardCaptureAvailableListener, mBackgroundHandler);
                int orientation = getResources().getConfiguration().orientation;
//                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
//                    setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
//                } else {
//                    setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
//                }
                if (!mSwitchAspect) {
                    setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } else {
                    setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                }
                Boolean available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                mFlashSupported = available == null ? false : available;
                //获取人脸检测参数
                int[] FD = characteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
                int maxFD = characteristics.get(CameraCharacteristics.STATISTICS_INFO_MAX_FACE_COUNT);
                if (FD.length > 0) {
                    List<Integer> fdList = new ArrayList<>();
                    for (int FaceD : FD
                            ) {
                        fdList.add(FaceD);
                        Log.e(TAG, "setUpCameraOutputs: FD type:" + Integer.toString(FaceD));
                    }
                    Log.e(TAG, "setUpCameraOutputs: FD count" + Integer.toString(maxFD));

                    if (maxFD > 0) {
                        mFaceDetectSupported = true;
                        mFaceDetectMode = Collections.max(fdList);
                    }
                }

                mCameraId = cameraId;
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
            SingleDispatcher.getInstance().getObservable().onNext(new RespBase(ErrorCode.SYSTEM,"摄像机故障"));
        }
    }

    /**
     * 获取一个合适的相机预览尺寸
     *
     * @param choices           支持的预览尺寸列表
     * @param textureViewWidth  相对宽度
     * @param textureViewHeight 相对高度
     * @param maxWidth          可以选择的最大宽度
     * @param maxHeight         可以选择的最大高度
     * @param aspectRatio       宽高比
     * @return 最佳预览尺寸
     */
    private static Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight,
                                          int maxWidth, int maxHeight, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();

        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() <= option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[choices.length - 1];
        }
    }


    /**
     * 从指定的屏幕旋转中检索照片方向
     *
     * @param rotation 屏幕方向
     * @return 照片方向（0,90,270,360）
     */
    private int getOrientation(int rotation) {
        return (ORIENTATIONS.get(rotation) + mSensorOrientation + 270) % 360;
    }

    /**
     * 锁定焦点
     */
    private void lockFocus() {

        try {
            // 如何通知相机锁定焦点
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraMetadata.CONTROL_AF_TRIGGER_START);
            // 通知mCaptureCallback等待锁定
            mState = STATE_WAITING_LOCK;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 解锁焦点
     */
    private void unlockFocus() {
        try {
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                    CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
            setAutoFlash(mPreviewRequestBuilder);
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback,
                    mBackgroundHandler);
            mState = STATE_PREVIEW;
            mCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback,
                    mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍摄静态图片
     */
    private void captureStillPicture() {
        try {
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // 方向
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            captureBuilder.addTarget(mImageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);


            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    Toast.makeText(getContext(), "Saved: " + mFile, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, mFile.toString());
                    unlockFocus();
                }
            };
            mCaptureSession.stopRepeating();
//            mCaptureSession.abortCaptures();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 为idcard拍摄静态图片
     */
    public void takeIDCardPicture() {
        try {
            if (null == activity || null == mCameraDevice) {
                return;
            }
            // 方向
            int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation));
            captureBuilder.addTarget(mImageIdCardCaptureReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                    CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            setAutoFlash(captureBuilder);


            CameraCaptureSession.CaptureCallback captureCallback
                    = new CameraCaptureSession.CaptureCallback() {

                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                               @NonNull CaptureRequest request,
                                               @NonNull TotalCaptureResult result) {
                    unlockFocus();
                }
            };
            mCaptureSession.stopRepeating();
            mCaptureSession.capture(captureBuilder.build(), captureCallback, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }
    /**
     * 运行preCapture序列来捕获静止图像
     */
    private void runPreCaptureSequence() {
        try {
            // 设置拍照参数请求
            mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                    CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
            mState = STATE_WAITING_PRE_CAPTURE;
            mCaptureSession.capture(mPreviewRequestBuilder.build(), mCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 比较两者大小
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }
    }

    /**
     * ImageReader的回调对象
     */
    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            try {

//                File parent = new File(mPath);
//                if(!parent.exists()){
//                    parent.mkdir();
//                }
//                mFile = new File(mPath,UUID.randomUUID().toString().replace("-","")+".jpg");
//                mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
                Image image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                image.close();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                //旋转、镜像
                Bitmap faceImg = null;
                if (mPhotoAngle == 0 && !mMirror) {
                    faceImg = bitmap;
                } else {
                    faceImg = Utils.rotaingImageView(bitmap, mPhotoAngle, mMirror);
                }

                int width = faceImg.getWidth();
                int height = faceImg.getHeight();
                byte[] faceRGB = Utils.bitmap2RGB(faceImg);
                if(!bitmap.isRecycled()){
                    bitmap.recycle();
                }
                if(!faceImg.isRecycled()){
                    faceImg.recycle();
                }
                //识别
                int ret = 0;
                FACE_DETECT_RESULT faceDetectResult = new FACE_DETECT_RESULT();
                int nFeatureSize = IdFaceSdk.IdFaceSdkFeatureSize();
                byte[] featureData = new byte[nFeatureSize];
                ret = IdFaceSdk.IdFaceSdkDetectFace(faceRGB, width, height, faceDetectResult);
                if (ret <= 0) {
                    //检测人脸失败

                    return;
                }

                ret = IdFaceSdk.IdFaceSdkFeatureGet(faceRGB, width, height, faceDetectResult, featureData);
                if (ret != 0) {
                    //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
                    return;
                }
                if (faceDetectResult.nFaceLeft == 0 && faceDetectResult.nFaceRight == 0) {
                    return;
                }
                mSubject.onNext(new CameraFaceData()
                        .setType(CameraFaceData.CAPTURE)
                        .setFaceData(faceRGB)
                        .setFeatureData(featureData)
                        .setFaceDetectResult(faceDetectResult)
                        .setImageHeight(height)
                        .setImageWidth(width));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }

        }
    };
    /**
     * ImageIdCardCaptureReader的回调对象
     */
    private final ImageReader.OnImageAvailableListener mOnIdCardCaptureAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @Override
        public void onImageAvailable(ImageReader reader) {
            try {

                Image image = reader.acquireLatestImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                image.close();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                //旋转、镜像
                Bitmap faceImg = null;
                if (mPhotoAngle == 0 && !mMirror) {
                    faceImg = bitmap;
                } else {
                    faceImg = Utils.rotaingImageView(bitmap, mPhotoAngle, mMirror);
                }
                int width = faceImg.getWidth();
                int height = faceImg.getHeight();
                byte[] faceRGB = Utils.bitmap2RGB(faceImg);

                if(!bitmap.isRecycled()){
                    bitmap.recycle();
                }
                if(!faceImg.isRecycled()){
                    faceImg.recycle();
                }
                //识别
                int ret = 0;
                FACE_DETECT_RESULT faceDetectResult = new FACE_DETECT_RESULT();
                int nFeatureSize = IdFaceSdk.IdFaceSdkFeatureSize();
                byte[] featureData = new byte[nFeatureSize];
                ret = IdFaceSdk.IdFaceSdkDetectFace(faceRGB, width, height, faceDetectResult);
                if (ret <= 0) {
                    //检测人脸失败

                    return;
                }

                ret = IdFaceSdk.IdFaceSdkFeatureGet(faceRGB, width, height, faceDetectResult, featureData);
                if (ret != 0) {
                    //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
                    return;
                }
                if (faceDetectResult.nFaceLeft == 0 && faceDetectResult.nFaceRight == 0) {
                    return;
                }
                mSubject.onNext(new CameraFaceData()
                        .setType(CameraFaceData.IDCARD_CAPTURE)
                        .setFaceData(faceRGB)
                        .setFeatureData(featureData)
                        .setFaceDetectResult(faceDetectResult)
                        .setImageHeight(height)
                        .setImageWidth(width));
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }

        }
    };
    /**
     * ImageReader的回调对象
     */
    long currentTime = System.currentTimeMillis();
    private final ImageReader.OnImageAvailableListener mOnImagePreViewAvailableListener
            = new ImageReader.OnImageAvailableListener() {

        @SuppressLint("CheckResult")
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            if (image == null) {
                return;
            }
            try {
                long tempcurrtime = System.currentTimeMillis();
                if (tempcurrtime - currentTime > mInterval) {
                    //处理
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    final byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    image.close();
                    image=null;
//                    Observable.create(new ObservableOnSubscribe<FACE_DETECT_RESULT>() {
//
//                        @Override
//                        public void subscribe(ObservableEmitter<FACE_DETECT_RESULT> result) throws Exception {
//                            Bitmap faceImg = BitmapFactory.decodeByteArray(data, 0, data.length);
//                            //旋转、镜像
//                            if (mPhotoAngle == 0 && !mMirror) {
//
//                            } else {
//                                faceImg = Utils.rotaingImageView(faceImg, mPhotoAngle, mMirror);
//                            }
//                            int width = faceImg.getWidth();
//                            int height = faceImg.getHeight();
//                            byte[] faceRGB = Utils.bitmap2RGB(faceImg);
//                            faceImg.recycle();
//                            //识别
//                            int ret = 0;
//                            FACE_DETECT_RESULT faceDetectResult = new FACE_DETECT_RESULT();
//                            int nFeatureSize = IdFaceSdk.IdFaceSdkFeatureSize();
//                            byte[] featureData = new byte[nFeatureSize];
//                            ret = IdFaceSdk.IdFaceSdkDetectFace(faceRGB, width, height, faceDetectResult);
//                            if (ret <= 0) {
//                                //检测人脸失败
//                                result.onNext(null);
//                                result.onComplete();
//                                return;
//                            }
//
//                            ret = IdFaceSdk.IdFaceSdkFeatureGet(faceRGB, width, height, faceDetectResult, featureData);
//                            if (ret != 0) {
//                                //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
//                                result.onNext(null);
//                                result.onComplete();
//                                return;
//                            }
//                            if (faceDetectResult.nFaceLeft == 0 && faceDetectResult.nFaceRight == 0) {
//                                result.onNext(null);
//                                result.onComplete();
//                                return;
//                            }
//                            mSubject.onNext(new CameraFaceData()
//                                    .setType(CameraFaceData.PREVIEW)
//                                    .setFaceData(faceRGB)
//                                    .setFeatureData(featureData)
//                                    .setFaceDetectResult(faceDetectResult)
//                                    .setImageHeight(height)
//                                    .setImageWidth(width));
//                            Log.e(TAG, "Left:" + faceDetectResult.nFaceLeft + " _Right:" + faceDetectResult.nFaceRight + "_Top:" + faceDetectResult.nFaceTop + "_Bottom:" + faceDetectResult.nFaceBottom);
//                            float widthRate = getWidth() / (float) width;
//                            float heightRate = getHeight() / (float) height;
//                            faceDetectResult.nFaceLeft = (int) (faceDetectResult.nFaceLeft * widthRate);
//                            faceDetectResult.nFaceRight = (int) (faceDetectResult.nFaceRight * widthRate);
//                            faceDetectResult.nFaceTop = (int) (faceDetectResult.nFaceTop * heightRate);
//                            faceDetectResult.nFaceBottom = (int) (faceDetectResult.nFaceBottom * heightRate);
//                            result.onNext(faceDetectResult);
//                            result.onComplete();
//                        }
//                    }).subscribeOn(Schedulers.io())
//                            .observeOn(AndroidSchedulers.mainThread())
//                            .subscribe(new Consumer<FACE_DETECT_RESULT>() {
//                                @Override
//                                public void accept(FACE_DETECT_RESULT face_detect_result) {
//                                    Canvas canvas = mSurfaceHolder.lockCanvas();
//                                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //清楚掉上一次的画框。
//                                    if (face_detect_result != null) {
//                                        if (face_detect_result.nFaceLeft != 0
//                                                || face_detect_result.nFaceRight != 0
//                                                ) {
//
//                                            mRectF.left = face_detect_result.nFaceLeft;
//                                            mRectF.right = face_detect_result.nFaceRight;
//                                            mRectF.top = face_detect_result.nFaceTop;
//                                            mRectF.bottom = face_detect_result.nFaceBottom;
//                                            canvas.drawRect(mRectF, mPaint);
//
//                                        }
//                                    }
//                                    mSurfaceHolder.unlockCanvasAndPost(canvas);
//                                }
//                            }, new Consumer<Throwable>() {
//                                @Override
//                                public void accept(Throwable throwable) {
//                                    Canvas canvas = mSurfaceHolder.lockCanvas();
//                                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //清楚掉上一次的画框。
//                                    mSurfaceHolder.unlockCanvasAndPost(canvas);
//                                }
//                            });

                    Observable.just(data)
                            .map(new Function<byte[], FACE_DETECT_RESULT>() {
                                @Override
                                public FACE_DETECT_RESULT apply(byte[] bytes) {
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                                    //旋转、镜像
                                    Bitmap faceImg = null;
                                    if (mPhotoAngle == 0 && !mMirror) {
                                        faceImg = bitmap;
                                    } else {
                                        faceImg = Utils.rotaingImageView(bitmap, mPhotoAngle, mMirror);
                                    }

                                    int width = faceImg.getWidth();
                                    int height = faceImg.getHeight();
                                    byte[] faceRGB = Utils.bitmap2RGB(faceImg);
                                    if(!bitmap.isRecycled()){
                                        bitmap.recycle();
                                    }
                                    if(!faceImg.isRecycled()){
                                        faceImg.recycle();
                                    }
                                    //识别
                                    int ret = 0;
                                    FACE_DETECT_RESULT faceDetectResult = new FACE_DETECT_RESULT();
                                    int nFeatureSize = IdFaceSdk.IdFaceSdkFeatureSize();
                                    byte[] featureData = new byte[nFeatureSize];
                                    ret = IdFaceSdk.IdFaceSdkDetectFace(faceRGB, width, height, faceDetectResult);
                                    if (ret <= 0) {
                                        //检测人脸失败
                                        return null;
                                    }

                                    ret = IdFaceSdk.IdFaceSdkFeatureGet(faceRGB, width, height, faceDetectResult, featureData);
                                    if (ret != 0) {
                                        //strResult = "JPEG文件提取特征失败，返回 " + ret + ", 文件路径: " + fileNames[i];
                                        return null;
                                    }
                                    if (faceDetectResult.nFaceLeft == 0 && faceDetectResult.nFaceRight == 0) {
                                        return null;
                                    }
                                    mSubject.onNext(new CameraFaceData()
                                            .setType(CameraFaceData.PREVIEW)
                                            .setFaceData(faceRGB)
                                            .setFeatureData(featureData)
                                            .setFaceDetectResult(faceDetectResult)
                                            .setImageHeight(height)
                                            .setImageWidth(width));
                                    Log.e(TAG, "Left:" + faceDetectResult.nFaceLeft + " _Right:" + faceDetectResult.nFaceRight + "_Top:" + faceDetectResult.nFaceTop + "_Bottom:" + faceDetectResult.nFaceBottom);
                                    float widthRate = getWidth() / (float) width;
                                    float heightRate = getHeight() / (float) height;
                                    faceDetectResult.nFaceLeft = (int) (faceDetectResult.nFaceLeft * widthRate);
                                    faceDetectResult.nFaceRight = (int) (faceDetectResult.nFaceRight * widthRate);
                                    faceDetectResult.nFaceTop = (int) (faceDetectResult.nFaceTop * heightRate);
                                    faceDetectResult.nFaceBottom = (int) (faceDetectResult.nFaceBottom * heightRate);
                                    return faceDetectResult;

                                }
                            })
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(new Consumer<FACE_DETECT_RESULT>() {
                                @Override
                                public void accept(FACE_DETECT_RESULT face_detect_result) {
                                    Canvas canvas = mSurfaceHolder.lockCanvas();
                                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //清楚掉上一次的画框。
                                    if (face_detect_result != null) {
                                        if (face_detect_result.nFaceLeft != 0
                                                || face_detect_result.nFaceRight != 0
                                                ) {

                                            mRectF.left = face_detect_result.nFaceLeft;
                                            mRectF.right = face_detect_result.nFaceRight;
                                            mRectF.top = face_detect_result.nFaceTop;
                                            mRectF.bottom = face_detect_result.nFaceBottom;
                                            canvas.drawRect(mRectF, mPaint);

                                        }
                                    }
                                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                                }
                            }, new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) {
                                    Canvas canvas = mSurfaceHolder.lockCanvas();
                                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR); //清楚掉上一次的画框。
                                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                                }
                            });
                    currentTime = tempcurrtime;
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            } finally {
                if (image != null) {
                    image.close();

                }

            }


        }
    };

    /**
     * 将捕获到的图像保存到指定的文件中
     */
    private static class ImageSaver implements Runnable {

        private final Image mImage;
        private final File mFile;

        ImageSaver(Image image, File file) {
            mImage = image;
            mFile = file;
        }

        @Override
        public void run() {

        }
    }

    public void onDestroy() {

    }

    public static class CameraFaceData {
        public final static int PREVIEW = 0;
        public final static int CAPTURE = 1;
        public final static int IDCARD_CAPTURE = 2;
        private int type = PREVIEW;
        private byte[] mFeatureData;
        private byte[] mFaceData;
        private FACE_DETECT_RESULT mFaceDetectResult;
        private int width;
        private int height;

        public byte[] getFeatureData() {
            return mFeatureData;
        }

        public CameraFaceData setFeatureData(byte[] featureData) {
            mFeatureData = featureData;
            return this;
        }

        public byte[] getFaceData() {
            return mFaceData;
        }

        public CameraFaceData setFaceData(byte[] faceData) {
            mFaceData = faceData;
            return this;
        }

        public int getImageWidth() {
            return width;
        }

        public CameraFaceData setImageWidth(int width) {
            this.width = width;
            return this;
        }

        public int getImageHeight() {
            return height;
        }

        public CameraFaceData setImageHeight(int height) {
            this.height = height;
            return this;
        }

        public int getType() {
            return type;
        }

        public CameraFaceData setType(int type) {
            this.type = type;
            return this;
        }

        public FACE_DETECT_RESULT getFaceDetectResult() {
            return mFaceDetectResult;
        }

        public CameraFaceData setFaceDetectResult(FACE_DETECT_RESULT faceDetectResult) {
            mFaceDetectResult = faceDetectResult;
            return this;
        }
    }
}

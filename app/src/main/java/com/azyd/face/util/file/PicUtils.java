package com.azyd.face.util.file;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * @author suntao
 * @creat-time 2018/12/17 on 14:35
 * $describe$
 */
public class PicUtils {
    public interface PicSaveCallBack{
        void kwPicSaveSuccess(String filePath);
        void kwPicSaveOnFail(Throwable throwable);
    }

    static class PicSaveResult{
        private boolean success;
        private String filePath;


        public PicSaveResult(boolean success, String filePath) {
            this.success = success;
            this.filePath = filePath;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getFilePath() {
            return filePath;
        }

        public void setFilePath(String filePath) {
            this.filePath = filePath;
        }
    }

    /**
     * 通过rxjava保存图片到相册
     * @param context Activity或者Context 都可以
     * @param bitmap 优先使用此bitmap
     * @param picSaveCallBack 保存图片成功或者失败后的回调
     */
    public static void  rxSaveBitmapToAlbum(final Context context, final Bitmap bitmap, final PicSaveCallBack picSaveCallBack){
        kwRxSaveBitmapToAlbum(context,bitmap,null,null,picSaveCallBack);
    }


    /**
     * 通过rxjava保存图片到相册
     * @param context Activity或者Context 都可以
     * @param imageView 优先从此参数获取bitMap
     * @param imageUrl 远程下载BitMap
     * @param picSaveCallBack 保存图片成功或者失败后的回调
     */
    public static void  rxSaveBitmapToAlbum(final Context context, final ImageView imageView, final String imageUrl, final PicSaveCallBack picSaveCallBack){
        kwRxSaveBitmapToAlbum(context,null,imageView,imageUrl,picSaveCallBack);
    }

    /**
     * 通过rxjava保存图片到相册
     * @param context Activity或者Context 都可以
     * @param bitmap 优先使用此bitmap
     * @param imageView 优先从此参数获取bitMap
     * @param imageUrl 远程下载BitMap
     * @param picSaveCallBack 保存图片成功或者失败后的回调
     */
    public static void  kwRxSaveBitmapToAlbum(final Context context,final Bitmap bitmap, final ImageView imageView, final String imageUrl,final PicSaveCallBack picSaveCallBack){
        Observable.just(context)
                .map(new Function<Context, PicSaveResult>() {
                    @Override
                    public PicSaveResult apply(Context param) throws Exception {
                        return kwSaveBitmapToAlbum(context,bitmap,imageView,imageUrl);
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<PicSaveResult>() {
                    @Override
                    public void accept(PicSaveResult result) throws Exception {
                        if(picSaveCallBack != null){
                            if(result != null && result.isSuccess()){
                                picSaveCallBack.kwPicSaveSuccess(result.getFilePath());
                                return;
                            }
                            picSaveCallBack.kwPicSaveOnFail(null);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) throws Exception {
                        if(picSaveCallBack != null){
                            picSaveCallBack.kwPicSaveOnFail(throwable);
                        }
                    }
                });
    }


    /**
     * 保存图片到相册
     * @param context Activity或者Context 都可以
     * @param imageView 优先从此参数获取bitMap
     * @param imageUrl  远程下载BitMap
     * @return
     */
    public static PicSaveResult kwSaveBitmapToAlbum(Context context,Bitmap bitmap, ImageView imageView, String imageUrl) {
        return kwSaveBitmap(context,bitmap,imageView,imageUrl,true);
    }
    /**
     * 保存图片到手机
     * @param context Activity或者Context 都可以
     * @param imageView 优先从此参数获取bitMap
     * @param imageUrl 远程下载BitMap
     *  @param album true 表示优先保存到相册，false 表示不保存到相册
     * @return 保存成功返回true,否则返回false
     */
    public static PicSaveResult kwSaveBitmap(Context context,Bitmap bitmap, ImageView imageView, String imageUrl,boolean album){
        if(context == null){
            return null;
        }
        String suffix=".jpg";
        /***如果bitmap==null 尝试获取gif*/
        if(bitmap == null){
            /***如果bitmap==null 判断imageUrl不为空,尝试获取gif的Bitmap*/
            if(!TextUtils.isEmpty(imageUrl)){
//                bitmap = kwObtainBitmapWithGif(context,imageUrl);
                if(bitmap != null){
                    /***只有这种情况下才使用.gif*/
                    suffix=".gif";
                }
            }
        }
        /***如果bitmap==null,尝试从ImageView中获取Bitmap*/
        if(bitmap == null){
            if(imageView != null){
                Drawable drawable = imageView.getDrawable();
                if(drawable != null && drawable instanceof BitmapDrawable){
                    BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
                    bitmap = bitmapDrawable.getBitmap();
                }
            }
        }

        /***如果bitmap==null,尝试从imageUrl中获取Bitmap*/
        if(bitmap == null){
//            bitmap=kwObtainBitmap(imageUrl);
        }
        if(bitmap == null){
            return null;
        }
        if(album){
            return kwSaveBitmapToAlbum(context,bitmap,suffix);
        }else {
            return kwSaveBitmapToSdCard(context,bitmap,suffix);
        }
    }

    /**
     * 保存图片到SD卡
     * 预计在App File目录
     * @param context conext即可
     * @param bitmap 图片的BitMap
     * @return 保存成功返回true,否则返回false
     */
    public static PicSaveResult kwSaveBitmapToSdCard(Context context, Bitmap bitmap,String suffix){
        String picFilePath = AppPathManager.getAppPictureFilePath(context,null,suffix);
        if (kwSaveBitmapToSD(picFilePath, bitmap)) {
            kwScanFile(context, picFilePath);
            return new PicSaveResult(true,picFilePath);
        }
        return null;
    }

    /**
     * 优先保存图片到相册,如果没有权限，则保存在APP内部
     * 预计应该是/storage/emulated/0/Pictures/包名/图片名
     * @param context conext即可
     * @param bitmap 图片的BitMap
     * @return
     */
    public static PicSaveResult kwSaveBitmapToAlbum(Context context, Bitmap bitmap,String suffix){
        String filePath = AppPathManager.getPictureFilePath(context,suffix);
        if(TextUtils.isEmpty(filePath)){
            return kwSaveBitmapToSdCard(context,bitmap,suffix);
        }
        if (kwSaveBitmapToSD(filePath, bitmap)) {
            kwScanFile(context, filePath);
            return new PicSaveResult(true,filePath);
        }
        return null;
    }



    /**
     * 通过imageUrl地址下载获取Bitmap
     * @param imageUrl 图片远程地址
     * @return 返回远程地址的bitmap
     */
//    public static Bitmap kwObtainBitmap(String imageUrl){
//        return ImageLoader.getInstance().loadImageSync(imageUrl);
//    }


    /**
     * 根据gifUrl地址下载获取BitMap
     * @param context conext即可
     * @return 获取gif的Bitmap
     */
//    public static Bitmap kwObtainBitmapWithGif(Context context, String gifUrl){
//        if (gifUrl.endsWith("#gif")) {
//            Bitmap bitmap = null;
//            try {
//                bitmap = Glide.with(context)
//                        .load(gifUrl.substring(0, gifUrl.length() - 4))
//                        .asBitmap()
//                        .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
//                        .get();
//            } catch (Throwable e) {
//                e.printStackTrace();
//            }
//            return bitmap;
//        }
//        return null;
//    }


    /**
     * 保存图片到sd卡中
     * @param filePath 保存的路径
     * @param bitmap   源文件
     * @return 保存成功返回true,否则返回false
     */
    private static boolean kwSaveBitmapToSD(String filePath, Bitmap bitmap) {
        if (bitmap == null)
            return false;

        FileOutputStream fos = null;
        boolean isSuccess = false;
        try {
            fos = new FileOutputStream(filePath);
            isSuccess = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!bitmap.isRecycled()) {
            bitmap.recycle();
        }
        return isSuccess;
    }

    /**
     * 扫描指定文件
     * @param context conext即可
     * @param localPath 文件本地地址
     */
    private static void kwScanFile(final Context context, final String localPath) {
        MediaScannerConnection.scanFile(context,
                new String[]{localPath}, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                    }
                });
    }
}

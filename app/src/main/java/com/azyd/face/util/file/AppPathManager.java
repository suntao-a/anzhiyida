package com.azyd.face.util.file;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;

/**
 * @author suntao
 * @creat-time 2018/12/17 on 14:50
 * $describe$
 */
public class AppPathManager {
    //常用目录名
    public static final String BUSINESS_PICTURE_FOLDER = "picture";
    public static final String BUSINESS_VIDEO_FOLDER = "video";
    public static final String BUSINESS_MUSIC_FOLDER = "music";

    /**
     * 获取app独立的图片路径，app卸载了，图片依然存在 sdcard/Pictures
     * @param suffix 文件后缀名，如.jpg  .gif; 默认值.jpg
     * @return
     */
    public static String getPictureFilePath(Context context, String suffix)
    {
        if(TextUtils.isEmpty(suffix))
            suffix = ".jpg";

        return AppPathManager.getFilePath(context, Environment.DIRECTORY_PICTURES, suffix);
    }

    /**
     * 获取app独立的视频路径，app卸载了，视频依然存在 sdcard/Movies
     *  @param suffix 文件后缀名，如.mp4  .flv; 默认值.mp4
     * @return
     */
    public static String getVideoFilePath(Context context, String suffix)
    {
        if(TextUtils.isEmpty(suffix))
            suffix = ".mp4";

        return AppPathManager.getFilePath(context, Environment.DIRECTORY_MOVIES, suffix);
    }

    /**
     * 获取app独立的音频路径，app卸载了，音乐依然存在 sdcard/Music
     * @param suffix 文件后缀名，如.mp3 ; 默认值.mp3
     * @return
     */
    public static String getMusicFilePath(Context context, String suffix)
    {
        if(TextUtils.isEmpty(suffix))
            suffix = ".mp3";

        return AppPathManager.getFilePath(context, Environment.DIRECTORY_MUSIC, suffix);
    }

    /**
     * 获取app独立的路径 sdcard/Pictures, sdcard/Movies, sdcard/Music
     * @param context
     * @param type      Environment类中的常量，如Environment.DIRECTORY_PICTURES等，不能自己随便写
     * @param suffix    文件后缀名如.mp3
     * @return
     */
    private static String getFilePath(Context context, String type, String suffix)
    {
        if(!FileUtils.hasSDCard())
            return null;

        if(!FileUtils.hasExternalStoragePermission(context))
            return null;

        //TODO 包名简化策略，可以考虑用配置的方式，从外面传进来
        File dir = FileUtils.getExternalPublicDir(type);
        String subFolderName = context.getPackageName();
        dir = new File(dir,  subFolderName);
        if (!dir.exists())
            dir.mkdirs();

        String path = dir.getAbsolutePath() + File.separator + FileUtils.createUniqueFileName(suffix);
        return path;
    }


    /**
     * 获取app专属的图片路径，直到app卸载才删除
     * @param context
     * @return
     */
    public static String getAppPictureFilePath(Context context, String subFolderName, String suffix)
    {
        return AppPathManager.getAppFilePath(context, BUSINESS_PICTURE_FOLDER, subFolderName, suffix);
    }

    /**
     * 获取app专属的视频路径，直到app卸载才删除
     * @param context
     * @return
     */
    public static String getAppVideoFilePath(Context context, String subFolderName, String suffix)
    {
        return AppPathManager.getAppFilePath(context, BUSINESS_VIDEO_FOLDER, subFolderName, suffix);
    }

    /**
     * 获取app专属的音频路径，直到app卸载才删除
     * @param context
     * @return
     */
    public static String getAppMusicFilePath(Context context, String subFolderName, String suffix)
    {
        return AppPathManager.getAppFilePath(context, BUSINESS_MUSIC_FOLDER, subFolderName, suffix);
    }

    /**
     * 获取app专属的文件路径，直到app卸载才删除；存放在SDCard/Android/data/{packageName}/files或者/data/data/{packageName}/files目录下
     * 策略：先从sdcard获取，失败后从内部空间获取
     * @param context
     * @param businessFolderName 业务目录，如picture，video，music以及其他业务
     * @param subFolderName 某个任务的子目录，建议随机生成KWFileUtils.createUniqueDirName();，避免和其他任务重复，方便任务结束时删除该目录中的文件
     * @param suffix 文件后缀名
     * @return
     */
    public static String getAppFilePath(Context context, String businessFolderName, String subFolderName, String suffix)
    {
        File dir = AppPathManager.getAppFileDir(context, businessFolderName, subFolderName);

        String path = dir.getAbsolutePath() + File.separator + FileUtils.createUniqueFileName(suffix);
        return path;
    }

    /**
     * 获取app专属的文件路径，直到app卸载才删除；存放在SDCard/Android/data/{packageName}/files或者/data/data/{packageName}/files目录下
     * 策略：先从sdcard获取，失败后从内部空间获取
     * @param context
     * @param businessFolderName 业务目录，如picture，video，music以及其他业务
     * @param subFolderName 某个任务的子目录，建议随机生成KWFileUtils.createUniqueDirName();，避免和其他任务重复，方便任务结束时删除该目录中的文件
     * @return
     */
    public static File getAppFileDir(Context context, String businessFolderName, String subFolderName)
    {
        File dir = FileUtils.getExternalFilesDir(context);
        if(dir == null)
            dir = FileUtils.getInternalFilesDir(context);

        if(!TextUtils.isEmpty(businessFolderName))
            dir = new File(dir, businessFolderName);
        if(!TextUtils.isEmpty(subFolderName))
            dir = new File(dir, subFolderName);
        if (!dir.exists())
            dir.mkdirs();

        return dir;
    }

    /**
     * 获取app专属的缓存路径，存放在SDCard/Android/data/{packageName}/cache或者/data/data/{packageName}/cache目录下
     * 策略：先从sdcard获取，失败后从内部空间获取
     * @param context
     * @param businessFolderName 业务目录，如picture，video，music以及其他业务
     * @param subFolderName 某个任务的子目录，建议随机生成KWFileUtils.createUniqueDirName()，避免和其他任务重复，方便任务结束时删除该目录中的文件
     * @param suffix 文件后缀名
     * @return
     */
    public static String getAppCacheFilePath(Context context, String businessFolderName, String subFolderName, String suffix)
    {
        File dir = AppPathManager.getAppCacheFileDir(context, businessFolderName, subFolderName);

        String path = dir.getAbsolutePath() + File.separator + FileUtils.createUniqueFileName(suffix);
        return path;
    }

    /**
     * 获取app专属的缓存路径，存放在SDCard/Android/data/{packageName}/cache或者/data/data/{packageName}/cache目录下
     * 策略：先从sdcard获取，失败后从内部空间获取
     * @param context
     * @param businessFolderName 业务目录，如picture，video，music以及其他业务
     * @param subFolderName 某个任务的子目录，建议随机生成KWFileUtils.createUniqueDirName()，避免和其他任务重复，方便任务结束时删除该目录中的文件
     * @return
     */
    public static File getAppCacheFileDir(Context context, String businessFolderName, String subFolderName)
    {
        File dir = FileUtils.getExternalCacheDir(context);
        if(dir == null)
            dir = FileUtils.getInternalCacheDir(context);

        if(!TextUtils.isEmpty(businessFolderName))
            dir = new File(dir, businessFolderName);
        if(!TextUtils.isEmpty(subFolderName))
            dir = new File(dir, subFolderName);
        if (!dir.exists())
            dir.mkdirs();

        return dir;
    }

    /**
     * 随机生成文件路径
     * @param parent 调用KWAppPathManager.getAppCacheFileDir或者KWAppPathManager.getAppFileDir生成的目录
     * @param suffix 文件后缀名 可以是null
     * @return
     */
    public static String getFilePathInParent(File parent, String suffix)
    {
        if(parent == null) {
            throw new IllegalArgumentException("KWAppPathManager getFilePath parent is null");
        }
        String path = parent.getAbsolutePath() + File.separator + FileUtils.createUniqueFileName(suffix);
        return path;
    }
}

package com.azyd.face.util.file;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.UUID;

/**
 * @author suntao
 * @creat-time 2018/12/17 on 14:32
 * $describe$
 */
public class FileUtils {
    /**
     * 是否有文件写入权限
     * @param context
     * */
    public static boolean hasExternalStoragePermission(Context context) {
        return context.checkCallingOrSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean hasSDCard() {
        // 个别机型上 Environment.getExternalStorageState()方法内部报空指针
        try {
            return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 生成成唯一文件夹名
     * @return
     */
    public static String createUniqueDirName()
    {
        return createUniqueFileName(null);
    }

    /**
     * 生成唯一的文件名
     *
     * @param suffix
     * @return
     */
    public static String createUniqueFileName(String suffix)
    {
        UUID uuid = UUID.randomUUID();
        StringBuilder newName = new StringBuilder();
        newName.append(uuid.toString());
        if(suffix != null){
            newName.append(suffix);
        }


        return newName.toString().replaceAll("-", "");
    }

    public static String getFilePath(File file)
    {
        if(file != null){
            return file.getAbsolutePath() + File.separator;
        } else {
            return null;
        }

    }

    /**
     * @return /data/data/<package name>/files
     */
    public static String getInternalFilesPath(Context context) {
        return context.getFilesDir().getAbsolutePath();
    }

    /**
     * @param context
     * @return /data/data/<package name>/files
     */
    public static File getInternalFilesDir(Context context) {
        return context.getFilesDir();
    }

    /**
     * @return /data/data/<package name>/cache
     */
    public static String getInternalCachePath(Context context) {
        return context.getCacheDir().getAbsolutePath();
    }

    /**
     * @return /data/data/<package name>/cache
     */
    public static File getInternalCacheDir(Context context) {
        return context.getCacheDir();
    }

    /**
     * @return /sdcard/Android/<package name>/cache
     */
    public static String getExternalCachePath(Context context) {
        File file = getExternalCacheDir(context);
        return getFilePath(file);
    }

    /**
     * @return /sdcard/Android/<package name>/cache
     */
    public static File getExternalCacheDir(Context context) {
        return hasSDCard()
                ?
                context.getExternalCacheDir()
                : null;
    }

    /**
     * sdcard/Android/data/你的应用的包名/files/
     * @return
     */
    public static String getExternalFilesPath(Context context) {
        File file = getExternalFilesDir(context);
        return getFilePath(file);
    }

    /**
     * sdcard/Android/data/你的应用的包名/files/
     * @return
     */
    public static File getExternalFilesDir(Context context) {
        return hasSDCard()
                ?
                context.getExternalFilesDir(null)
                : null;
    }

    /**
     * @param type Environment.DIRECTORY_PICTURES等
     *
     * sdcard/Pictures, sdcard/Movies, sdcard/Music等
     * @return
     */
    public static String getExternalPublicPath(String type) {
        File file = getExternalPublicDir(type);
        return getFilePath(file);
    }


    /**
     * @param type Environment.DIRECTORY_PICTURES等
     *
     * sdcard/Pictures, sdcard/Movies, sdcard/Music等
     * @return
     */
    public static File getExternalPublicDir(String type) {
        return hasSDCard()
                ?
                Environment.getExternalStoragePublicDirectory(type)
                : null;
    }

    /**
     * 获取sdcard根目录
     * @return
     */
    public static String getSDCardRootPath() {
        return Environment.getExternalStorageDirectory().getAbsolutePath();
    }

    /**
     * 将字节数组保存为文件
     * @param fileData
     * @param filePath
     */
    public static void saveFileCache(byte[] fileData, String filePath) {
        File file = new File(filePath);
        ByteArrayInputStream is = new ByteArrayInputStream(fileData);
        FileOutputStream os = null;
        if(!file.exists()) {
            try {
                file.createNewFile();
                os = new FileOutputStream(file);
                byte[] e = new byte[1024];
                boolean len = false;

                int len1;
                while(-1 != (len1 = is.read(e))) {
                    os.write(e, 0, len1);
                }

                os.flush();
            } catch (Exception var12) {
                throw new RuntimeException(FileUtils.class.getClass().getName(), var12);
            } finally {
                closeIO(new Closeable[]{is, os});
            }
        }

    }

    public static File getSaveFile(String filePath) {
        File file = new File(filePath);

        try {
            file.createNewFile();
        } catch (IOException var4) {
            var4.printStackTrace();
            file = null;
        }

        return file;
    }

    public static String getMimeType(File file) {
        MimeTypeMap map = MimeTypeMap.getSingleton();
        String mimeType = map.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(file.getAbsolutePath()));
        return TextUtils.isEmpty(mimeType) ? "application/octet-stream" : mimeType;
    }

    public static void copyStream(InputStream input, OutputStream output)
            throws IOException {

        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buffer)) != -1) {
            output.write(buffer, 0, bytesRead);
        }
    }

    public static final byte[] input2byte(InputStream inStream) {
        if(inStream == null) {
            return null;
        } else {
            byte[] in2b = null;
            BufferedInputStream in = new BufferedInputStream(inStream);
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            boolean rc = false;

            try {
                int rc1;
                while((rc1 = in.read()) != -1) {
                    swapStream.write(rc1);
                }

                in2b = swapStream.toByteArray();
            } catch (IOException var9) {
                var9.printStackTrace();
            } finally {
                closeIO(new Closeable[]{inStream, in, swapStream});
            }

            return in2b;
        }
    }

    public static void copyFile(File from, File to) {
        if(from != null && from.exists()) {
            if(to != null) {
                FileInputStream is = null;
                FileOutputStream os = null;

                try {
                    is = new FileInputStream(from);
                    if(!to.exists()) {
                        to.createNewFile();
                    }

                    os = new FileOutputStream(to);
                    copyFileFast(is, os);
                } catch (Exception var8) {
                    throw new RuntimeException(FileUtils.class.getClass().getName(), var8);
                } finally {
                    closeIO(new Closeable[]{is, os});
                }
            }
        }
    }

    public static void copyFileFast(FileInputStream is, FileOutputStream os) throws IOException {
        FileChannel in = is.getChannel();
        FileChannel out = os.getChannel();
        in.transferTo(0L, in.size(), out);
    }

    public static void closeIO(Closeable... closeables) {
        if(closeables != null && closeables.length > 0) {
            Closeable[] var4 = closeables;
            int var3 = closeables.length;

            for(int var2 = 0; var2 < var3; ++var2) {
                Closeable cb = var4[var2];

                try {
                    if(cb != null) {
                        cb.close();
                    }
                } catch (IOException var6) {
                    throw new RuntimeException(FileUtils.class.getClass().getName(), var6);
                }
            }

        }
    }

    public static String readFile(String filePath) {
        FileInputStream is = null;

        try {
            is = new FileInputStream(filePath);
        } catch (Exception var3) {
            throw new RuntimeException(FileUtils.class.getName() + "readFile---->" + filePath + " not found");
        }

        return inputStream2String(is);
    }

    public static String readFileFromAssets(Context context, String name) {
        InputStream is = null;

        try {
            is = context.getResources().getAssets().open(name);
        } catch (Exception var4) {
            throw new RuntimeException(FileUtils.class.getName() + ".readFileFromAssets---->" + name + " not found");
        }

        return inputStream2String(is);
    }

    public static String inputStream2String(InputStream is) {
        if(is == null) {
            return null;
        } else {
            StringBuilder resultSb = null;

            try {
                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                resultSb = new StringBuilder();

                String len;
                while((len = br.readLine()) != null) {
                    resultSb.append(len);
                }
            } catch (Exception var7) {
                ;
            } finally {
                closeIO(new Closeable[]{is});
            }

            return resultSb == null?null:resultSb.toString();
        }
    }

    /**
     *  删除目录中文件
     *
     * @param dirPath
     */
    public static void deleteFiles(String dirPath){
        if (dirPath != null) {
            File file = new File(dirPath);
            if(file.exists()){
                deleteFiles(file);
            }
        }
    }

    public static void deleteFiles(File file){
        if(file != null) {
            if(file.isDirectory()){
                File[] files = file.listFiles();
                if(files != null) {
                    for(int i=0; i<files.length; i++){
                        deleteFiles(files[i]);
                    }
                }
            }
            try {
                file.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 将文本写入File文件
     * @author wangfei
     * @param data    文本
     * @param desPath 目标文件
     * @param append  是否在原有文本上追加
     */
    public static void writeTxtToFile(String data, String desPath, boolean append) {
        if (TextUtils.isEmpty(data) || TextUtils.isEmpty(desPath)) {
            return;
        }

        File file = new File(desPath);
        if(!file.getParentFile().exists()){
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(desPath, append);
            fw.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(fw != null){
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

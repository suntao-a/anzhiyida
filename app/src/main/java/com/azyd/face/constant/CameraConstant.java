package com.azyd.face.constant;

/**
 * @author suntao
 * @creat-time 2018/12/18 on 17:52
 * $describe$
 */
public class CameraConstant {
    public static interface ICameraParam{
        String getCameraId();
        /**
         * 根据预览成像的宽高比设置View的宽带比
         * @return
         */
        boolean isViewNeedSwitchAspect();
        int getPhotoRotate();
        boolean isMirror();
        int getInterval();
        int getFaceSaveTimes();
    }


    public static ICameraParam getCameraParam(){
        return new HuaXiaDeviceCamera();
    }


    public static class HuaXiaDeviceCamera implements ICameraParam{

        @Override
        public String getCameraId() {
            return "0";
        }

        @Override
        public boolean isViewNeedSwitchAspect() {
            return false;
        }

        @Override
        public int getPhotoRotate() {
            return 0;
        }

        @Override
        public boolean isMirror() {
            return false;
        }

        @Override
        public int getInterval() {
            return 200;
        }



        @Override
        public int getFaceSaveTimes() {
            return 6;
        }

    }
    public static class DefaultDeviceCamera implements ICameraParam{

        @Override
        public String getCameraId() {
            return "1";
        }

        @Override
        public boolean isViewNeedSwitchAspect() {
            return true;
        }

        @Override
        public int getPhotoRotate() {
            return -90;
        }

        @Override
        public boolean isMirror() {
            return true;
        }

        @Override
        public int getInterval() {
            return 200;
        }


        @Override
        public int getFaceSaveTimes() {
            return 6;
        }


    }
}

package com.azyd.face.constant;

/**
 * @author suntao
 * @creat-time 2018/12/18 on 17:52
 * $describe$
 */
public class CameraConstant {
    public static interface ICameraParam{
        /**
         * 根据预览成像的宽高比设置View的宽带比
         * @return
         */
        boolean isViewNeedSwitchAspect();
        int getPhotoRotate();
        boolean isMirror();
        int getInterval();
        int getFeatureQualityPass();
        int getFaceSaveTimes();
    }


    public static ICameraParam getDefaultCameraParam(){
        return new DefaultDeviceCamera();
    }


    public static class HuaXiaDeviceCamera implements ICameraParam{

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
        public int getFeatureQualityPass() {
            return 70;
        }

        @Override
        public int getFaceSaveTimes() {
            return 6;
        }
    }
    public static class DefaultDeviceCamera implements ICameraParam{

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
            return 10000;
        }

        @Override
        public int getFeatureQualityPass() {
            return 70;
        }

        @Override
        public int getFaceSaveTimes() {
            return 6;
        }
    }
}

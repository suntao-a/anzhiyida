package com.azyd.face.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.idcard.huaxu.HXCardReadManager;

import java.util.ArrayList;

public class USBDiskReceiver extends BroadcastReceiver {
    static final int CARD_VID = 8301;
    static HXCardReadManager hxCardReadManager;
    public static void setCardReadManager(HXCardReadManager manager){
        hxCardReadManager = manager;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action.equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
                if (usbDevice.getVendorId() == CARD_VID && usbDevice.getProductId() == 1) {
                    //身份证设备USB
                    hxCardReadManager.start();
                }

            }
        } else if(action.equals(UsbManager.ACTION_USB_DEVICE_DETACHED)){
            ArrayList<Integer> list = new ArrayList<>();
            UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
            for(UsbDevice usbDevice : usbManager.getDeviceList().values()){
                list.add(usbDevice.getVendorId());
            }
            if(!list.contains(CARD_VID)){
                hxCardReadManager.close();
            }
        }
    }
}

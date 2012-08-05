package com.handbagdevices.handbag;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

public class Activity_SetupUsb extends Activity {

    // TODO: Leave all this for the comms service instead?
    private UsbAccessoryHandlerInterface usbHandler = null;


    // TODO: Ideally this would be a static method in the interface...
    private void setUsbHandler() {
        try {
            Class.forName("android.hardware.usb.UsbManager");
            Log.d(this.getClass().getSimpleName(), "Using: 'android.hardware.usb.UsbManager'");
            usbHandler = new UsbAccessoryHandlerHoneycomb();
        } catch (ClassNotFoundException ex) {
            try {
                Class.forName("com.android.future.usb.UsbManager");
                Log.d(this.getClass().getSimpleName(), "Using: 'com.android.future.usb.UsbManager'");
                usbHandler = new UsbAccessoryHandlerGingerbread();
            } catch (ClassNotFoundException ex1) {
                Log.w(this.getClass().getSimpleName(), "Neither 'UsbManager' class found--no USB accessory support available.");
            }
        }
    }


    public Activity_SetupUsb() {
        setUsbHandler();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (usbHandler == null) {
            // TODO: Display error message. (Shouldn't happen when only using intents.)
            finish();
            return;
        }

        usbHandler.setManager(this);


    }
}

package com.handbagdevices.handbag;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Messenger;
import android.util.Log;

public class Activity_SetupUsb extends Activity {

    Messenger commsService = null;
    boolean commsServiceIsBound = false;

    private ServiceConnection connCommsService = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            // Store the object we will use to communicate with the service.
            commsService = new Messenger(service);
            commsServiceIsBound = true;
            Log.d(this.getClass().getSimpleName(), "USB Comms Service bound");
        }


        public void onServiceDisconnected(ComponentName className) {
            // Called when the service crashes/unexpectedly disconnects.
            commsService = null;
            commsServiceIsBound = false;
        }

    };


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


    private void startDisplayActivity() {
        // open display activity
        Intent startDisplayActivityIntent = new Intent(Activity_SetupUsb.this, Activity_MainDisplay.class);

        startDisplayActivityIntent.putExtra("COMMS_SERVICE", commsService); // TODO: Use a constant

        startActivity(startDisplayActivityIntent);
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


    // TODO: Refactor duplication with other Activity_Setup* classes?

    @Override
    protected void onStart() {
        Log.d(this.getClass().getSimpleName(), "Entered onStart()");
        super.onStart();

        startService(new Intent(Activity_SetupUsb.this, CommsService_Usb.class));
        boolean bindSuccessful = bindService(new Intent(Activity_SetupUsb.this, CommsService_Usb.class), connCommsService, Context.BIND_AUTO_CREATE);
        Log.d(this.getClass().getSimpleName(), "Comms Service bound: " + bindSuccessful);
        Log.d(this.getClass().getSimpleName(), "Comms Service: " + commsService);

        if (bindSuccessful) {
            startDisplayActivity();
        } else {
            Log.e(this.getClass().getSimpleName(), "Comms service not bound--not starting display activity.");
        }

        Log.d(this.getClass().getSimpleName(), "Exited onStart()");
    }


    @Override
    protected void onStop() {
        Log.d(this.getClass().getSimpleName(), "Entered onStop()");

        super.onStop();

        // Note: MainDisplay activity is responsible for closing connections

        unbindService(connCommsService);
        // connCommsService = null;

        Log.d(this.getClass().getSimpleName(), "Exited onStop()");
    }

}

package com.handbagdevices.handbag;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
            // TODO: Check if we should have the same location of this in the network activity?
            startDisplayActivity();
            // startUsbConnection();
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

        // "ForResult" is used so we can make back button go to "main" config screen.
        startDisplayActivityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivityForResult(startDisplayActivityIntent, 0);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // TODO: Change to use "choose comms method screen" rather than network.
        // This is the second part of our "back button" functionality.
        Log.d(this.getClass().getSimpleName(), "Entered onActivityResult.");
        Intent startChooseActivityIntent = new Intent(Activity_SetupUsb.this, Activity_SetupNetwork.class);
        startChooseActivityIntent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(startChooseActivityIntent);

        // TODO: Fix this? (Maybe with something in 'onResume'?)
        finish(); // Without this we have to press back twice--but with it we get a "duplicate finish" log message.

        Log.d(this.getClass().getSimpleName(), "Exited onActivityResult.");
    }


    private void startUsbConnection() {
        // TODO: Just make this automatic within the USB comms service?

        if (commsService != null) {
            try {
                commsService.send(Message.obtain(null, CommsService_Usb.MSG_USB_START_CONNECTION));
            } catch (RemoteException e) {
                Log.d(this.getClass().getSimpleName(), "RemoteException on start USB connection.");
            }
        }

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

        if (!bindSuccessful) {
            // TODO: Do something else here?
            Log.e(this.getClass().getSimpleName(), "Comms service not bound--display activity will not be started.");
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

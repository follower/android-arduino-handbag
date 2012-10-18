package com.handbagdevices.handbag;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
            // TODO: Check if we should have the same location of this in the network activity?
            startDisplayActivity();
            // startUsbConnection();
            finish();
        }


        public void onServiceDisconnected(ComponentName className) {
            // Called when the service crashes/unexpectedly disconnects.
            commsService = null;
            commsServiceIsBound = false;
        }

    };


    private void startDisplayActivity() {
        // open display activity
        Intent startDisplayActivityIntent = new Intent(Activity_SetupUsb.this, Activity_MainDisplay.class);

        startDisplayActivityIntent.putExtra("COMMS_SERVICE", commsService); // TODO: Use a constant

        startDisplayActivityIntent.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);

        startActivity(startDisplayActivityIntent);
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

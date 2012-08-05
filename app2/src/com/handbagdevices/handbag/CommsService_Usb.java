package com.handbagdevices.handbag;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

public class CommsService_Usb extends Service {

    // Used to receive messages from client(s)
    public class IncomingUsbCommsServiceHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(this.getClass().getSimpleName(), "USB Comms Service received message:");

            switch (msg.what) {

                default:
                    Log.d(this.getClass().getSimpleName(), "    (unknown): " + msg.what);
                    super.handleMessage(msg);
            }

        }
    }


    // Clients will use this to communicate with us.
    final Messenger ourMessenger = new Messenger(new IncomingUsbCommsServiceHandler());


    @Override
    public IBinder onBind(Intent intent) {
        // Return our `messenger` instance to enable clients to send us messages.
        Log.d(this.getClass().getSimpleName(), "onBind entered");

        return ourMessenger.getBinder();
    }

}

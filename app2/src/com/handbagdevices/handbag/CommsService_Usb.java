package com.handbagdevices.handbag;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class CommsService_Usb extends Service {

    // Used to communicate with the UI Activity & Communication Service
    Messenger uiActivity = null; // Orders us around
    Messenger parseService = null; // Receives our data, sends us its data.

    // Flag that should be checked
    private boolean shutdownRequested = false;


    // Used to receive messages from client(s)
    public class IncomingUsbCommsServiceHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            Log.d(this.getClass().getSimpleName(), "USB Comms Service received message:");

            switch (msg.what) {

            // TODO: Don't actually get registered by UI activity because we don't use it (also in WiFi)?
                case CommsService_WiFi.MSG_SETUP_ACTIVITY_REGISTER: // TODO: Move this constant into UI class?
                    // TODO: Handle receiving this more than once?
                    Log.d(this.getClass().getSimpleName(), "    MSG_SETUP_ACTIVITY_REGISTER");
                    uiActivity = msg.replyTo;

                    shutdownRequested = false;

                    try {
                        uiActivity.send(Message.obtain(null, Activity_MainDisplay.MSG_DISPLAY_ACTIVITY_REGISTERED)); // TODO: Change to specify who was registered with. ***
                    } catch (RemoteException e) {
                        // UI Activity client is dead so no longer try to access it.
                        uiActivity = null;
                    }

                    break;


                case HandbagParseService.MSG_PARSE_SERVICE_REGISTER: // TODO: Move this constant into UI class?
                    // TODO: Handle receiving this more than once?
                    Log.d(this.getClass().getSimpleName(), "    MSG_PARSE_SERVICE_REGISTER");
                    parseService = msg.replyTo;

                    try {
                        parseService.send(Message.obtain(null, CommsService_WiFi.MSG_PARSE_SERVICE_REGISTERED));
                    } catch (RemoteException e) {
                        // Parse service client is dead so no longer try to access it.
                        parseService = null;
                    }

                    break;


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

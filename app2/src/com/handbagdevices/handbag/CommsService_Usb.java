package com.handbagdevices.handbag;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class CommsService_Usb extends Service {

    public static final int MSG_USB_START_CONNECTION = 701;

    // Used to communicate with the UI Activity & Communication Service
    Messenger uiActivity = null; // Orders us around
    Messenger parseService = null; // Receives our data, sends us its data.

    // Flag that should be checked
    private boolean shutdownRequested = false;

    UsbConnection usbConnection = null;

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

                    // TODO: Handle somewhere else?
                    if ((parseService != null) && (usbConnection == null)) {
                        startUsbConnection();
                    }

                    break;


                case MSG_USB_START_CONNECTION:
                    Log.d(this.getClass().getSimpleName(), "    MSG_USB_START_CONNECTION");
                    startUsbConnection();
                    // TODO: Send some sort of response?
                    break;


                case HandbagParseService.MSG_UI_SHUTDOWN_REQUEST: // TODO: Move this constant into UI class?
                    Log.d(this.getClass().getSimpleName(), "    MSG_UI_SHUTDOWN_REQUEST");

                    // Set a flag so repeating sub-tasks will stop themselves.
                    shutdownRequested = true;
                    // TODO: Be more proactive and stop them here instead?

                    if (usbConnection != null) {
                        Log.d(this.getClass().getSimpleName(), "    Cancelling USB connection.");
                        usbConnection.cancel(true);
                        usbConnection = null;
                    }

                    parseService = null;
                    break;


                default:
                    Log.d(this.getClass().getSimpleName(), "    (unknown): " + msg.what);
                    super.handleMessage(msg);
            }

        }
    }


    private void startUsbConnection() {
        usbConnection = new UsbConnection();
        usbConnection.start();
    }

    // TODO: Extract interface/base-class from this and NetworkConnection class?

    public class UsbConnection extends AsyncTask<Void, String[], Integer> {

        private void start() {
            this.execute();
        }


        // TODO: This is a duplicate of the same in NetworkConnection so extract it.
        private void deliverPacket(String[] packet) {

            Log.d(this.getClass().getSimpleName(), "Enter 'deliverPacket'.");

            Log.d(this.getClass().getSimpleName(), "    'shutdownRequested`:" + shutdownRequested);

            // TODO: Do this properly...
            if (!shutdownRequested) {
                // Only continue if we haven't been told to shutdown

                if (parseService != null) {

                    try {
                        Message msg = Message.obtain(null, CommsService_WiFi.MSG_COMMS_PACKET_RECEIVED);
                        Bundle bundle = new Bundle();
                        bundle.putStringArray(null, packet);
                        msg.setData(bundle);

                        parseService.send(msg);
                    } catch (RemoteException e) {
                        Log.d(this.getClass().getSimpleName(), "Remote exception occurred deliverying packet.");
                        // Parse service client is dead so no longer try to access it.
                        parseService = null;
                    }

                } else {
                    Log.d(this.getClass().getSimpleName(), "Dropping packet as 'parseService' is null.");
                }
            }


            Log.d(this.getClass().getSimpleName(), "Exit 'deliverPacket'.");

        }


        @Override
        protected void onProgressUpdate(String[]... values) {
            deliverPacket(values[0]);
        }


        @Override
        protected Integer doInBackground(Void... params) {
            String[] newPacket;

            newPacket = new String[] { "widget", "label", "1", "35", "1", "hello!", };

            publishProgress(newPacket);

            return 0;
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

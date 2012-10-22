package com.handbagdevices.handbag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

public class CommsService_Usb extends Service {

    // Used to communicate with the UI Activity & Communication Service
    Messenger uiActivity = null; // Orders us around
    Messenger parseService = null; // Receives our data, sends us its data.

    // Flag that should be checked
    private boolean shutdownRequested = false;

    UsbConnection usbConnection = null;

    private UsbAccessoryHandlerInterface usbHandler = null;


    // TODO: Make this part of the UsbHandler class instead?
    private final BroadcastReceiver usbActionReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(this.getClass().getSimpleName(), "Entered usbActionReceiver onReceive.");
            if (usbHandler.get_ACTION_USB_ACCESSORY_DETACHED().equals(action)) {
                Log.d(this.getClass().getSimpleName(), "Detach event occurred.");
                if (usbHandler.matchesThisAccessory(intent)) {
                    Log.d(this.getClass().getSimpleName(), "Detached accessory matches us.");

                    try {
                        uiActivity.send(Message.obtain(null, Activity_MainDisplay.MSG_DISPLAY_TARGET_DISCONNECTED_NORMAL));
                    } catch (RemoteException e) {
                        // UI Activity client is dead so no longer try to access it.
                        uiActivity = null;
                    }

                }
            }
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(this.getClass().getSimpleName(), "onCreate() called");

        if (usbHandler == null) {
            // TODO: Display error message. (Shouldn't happen when only using intents.)
            return;
        }

        usbHandler.setManager(this.getApplicationContext());

        // TODO: Put in UsbConnection class instead?
        IntentFilter filter = new IntentFilter(usbHandler.get_ACTION_USB_ACCESSORY_DETACHED());
        registerReceiver(usbActionReceiver, filter);

    }


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


    public CommsService_Usb() {
        setUsbHandler();
    }


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
                    Log.d(this.getClass().getSimpleName(), "parseService: " + parseService);
                    Log.d(this.getClass().getSimpleName(), "usbConnection: " + usbConnection);
                    if ((parseService != null) && (usbConnection == null)) {
                        startUsbConnection();
                    }

                    break;


                case CommsService_WiFi.MSG_UI_DISCONNECT_FROM_TARGET:
                    Log.d(this.getClass().getSimpleName(), "    MSG_UI_DISCONNECT_FROM_TARGET (treating as next case...)");
                    // Falling through to:
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

                    stopSelf();

                    break;


                case CommsService_WiFi.MSG_COMMS_SEND_PACKET:
                    Log.d(this.getClass().getSimpleName(), "    MSG_COMMS_SEND_PACKET");
                    sendPacketToTarget(msg.getData().getStringArray(null));
                    // TODO: Return some sort of response?
                    break;


                default:
                    Log.d(this.getClass().getSimpleName(), "    (unknown): " + msg.what);
                    super.handleMessage(msg);
            }

        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(this.getClass().getSimpleName(), "onDestroy() called");
        unregisterReceiver(usbActionReceiver);
        usbHandler = null;

        // This attempts to work around "Device not found" error on
        // Gingerbread (& less often Honeycomb) for a second connection.
        // TODO: Solve this error properly.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

    }


    private void startUsbConnection() {
        usbConnection = new UsbConnection();
        usbConnection.start();
    }


    public void sendPacketToTarget(String[] packet) {
        usbConnection.packetsToSendQueue.add(packet);
    }


    // TODO: Extract interface/base-class from this and NetworkConnection class?

    public class UsbConnection extends AsyncTask<Void, String[], Integer> {

        private DataOutputStream dataOutStream;
        private DataInputStream dataInStream;

        private PacketParser parser;

        public BlockingQueue<String[]> packetsToSendQueue = new LinkedBlockingQueue<String[]>();

        public BlockingQueue<String[]> packetsReceivedQueue = new LinkedBlockingQueue<String[]>();


        ParcelFileDescriptor fileDescriptorParcel;


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


        private boolean setUp() {
            boolean result = false;

            Object accessory = usbHandler.getConnectedAccessory();

            // It's possible we end up here with no accessory connected because
            // Android's intents/activities/lifecycle approach is annoying.
            if (accessory == null) {
                Log.d(this.getClass().getSimpleName(), "Early exit of 'setup()' as 'accessory' is null.");
                return false;
            }

            fileDescriptorParcel = usbHandler.openAccessory(accessory);

            usbHandler.setAccessory(accessory);

            if (fileDescriptorParcel != null) {
                FileDescriptor fd = fileDescriptorParcel.getFileDescriptor();

                dataOutStream = new DataOutputStream(new FileOutputStream(fd));
                dataInStream = new DataInputStream(new FileInputStream(fd));

                parser = new PacketParser(packetsReceivedQueue, dataInStream);

                // TODO: Do handshake/version check when first connected?

                result = true;

            }

            return result;
        }


        private void cleanUp() {
            Log.d(this.getClass().getSimpleName(), "cleanUp() called.");
            try {
                dataOutStream.close();
                dataInStream.close();
                fileDescriptorParcel.close();
            } catch (IOException e) {
                Log.d(this.getClass().getSimpleName(), "Ignoring IOException when closing USB device.");
            }
            usbHandler.setAccessory(null);
        }


        @Override
        protected void onProgressUpdate(String[]... values) {
            deliverPacket(values[0]);
        }


        @Override
        protected Integer doInBackground(Void... params) {
            String[] newPacket;

            if (setUp()) {

                // TODO: Do proper handshake.
                // Note: We have to send bytes first for an Arduino-based network device to detect us.
                try {
                    dataOutStream.writeBytes(PacketGenerator.fromArray(new String[] { "HB2" }));
                    dataOutStream.flush();
                } catch (IOException e1) {
                    Log.d(this.getClass().getSimpleName(), "IOException sending initial handshake packet.");
                    e1.printStackTrace();
                    // TODO: Bail properly here.
                    return -1;
                }


                // TODO: Extract most of this common copy-pasta from WiFi comms service.
                parser.start();

                Log.d(this.getClass().getSimpleName(), "Entering main data transfer handling loop.");

                while (true) {
                    if (this.isCancelled()) {
                        parser.interrupt();
                        Log.d(this.getClass().getSimpleName(), "Connection canceled.");
                        break;
                    }

                    try {
                        newPacket = this.packetsReceivedQueue.poll(1, TimeUnit.MILLISECONDS);

                        if (newPacket != null) {
                            Log.d(this.getClass().getSimpleName(), "Got result: " + Arrays.toString(newPacket));

                            publishProgress(newPacket);
                        }
                    } catch (InterruptedException e) {
                        // TODO: Handle properly?
                        Log.d(this.getClass().getSimpleName(), "InterruptedException handling received packet.");
                    }


                    try {
                        String[] packetToSend = this.packetsToSendQueue.poll(1, TimeUnit.MILLISECONDS);

                        if (packetToSend != null) {
                            Log.d(this.getClass().getSimpleName(), "Sending packet. ");
                            dataOutStream.writeBytes(PacketGenerator.fromArray(packetToSend));
                            dataOutStream.flush();
                        }

                    } catch (IOException e) {
                        // TODO: Handle properly?
                        Log.d(this.getClass().getSimpleName(), "IOException sending packet.");
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        // TODO: Handle properly?
                        Log.d(this.getClass().getSimpleName(), "InterruptedException sending packet.");
                    }
                }

                cleanUp();
            }

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

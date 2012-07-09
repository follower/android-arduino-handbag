package com.handbagdevices.handbag;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

// TODO: Rename this because its purpose has changed a bit?

public class HandbagParseService extends Service {

	// Messages from UI activity
	static final int MSG_UI_ACTIVITY_REGISTER = 101;
	static final int MSG_UI_SHUTDOWN_REQUEST = 102;

	static final int MSG_UI_EVENT_OCCURRED = 100;

	// Messages to communications service
	static final int MSG_PARSE_SERVICE_REGISTER = 200;

	// Used to communicate with the UI Activity & Communication Service
	// We passively receive this connection.
	Messenger uiActivity = null;

	// We pro-actively make this connection.
	Messenger commsService = null;
	boolean commsServiceIsBound = false;


	// Flag that should be checked
	private boolean shutdownRequested = false;


	private void bindCommsService() {

		// TODO: Use the chosen Comms Service (WiFi, USB ADK, BT?)
		boolean bindSuccessful = bindService(new Intent(HandbagParseService.this, HandbagWiFiCommsService.class), connCommsService, Context.BIND_AUTO_CREATE);
		Log.d(this.getClass().getSimpleName(), "Comms Service bound:" + bindSuccessful);

		if (commsService != null) {
			// We do this here to handle the situation that we're "resuming" after being
			// hidden. This ensures the Comms Server is "woken up".
			registerWithWiFiCommsService(); // TODO: Make generic.
		}

	}

	// TODO: Just receive the required comms communication objects directly from the UI activity?
	private ServiceConnection connCommsService = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			// Store the object we will use to communicate with the service.
			commsService = new Messenger(service);
			commsServiceIsBound = true;
			Log.d(this.getClass().getSimpleName(), "Comms Service bound");

			registerWithWiFiCommsService();
		}

		public void onServiceDisconnected(ComponentName className) {
			// Called when the service crashes/unexpectedly disconnects.
			commsService = null;
			commsServiceIsBound = false;
		}

	};



    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(connCommsService);
        connCommsService = null;
    }


    private void registerWithWiFiCommsService() {

    	Log.d(this.getClass().getSimpleName(), "Registering with WiFi Comms Service.");

		Message msg = Message.obtain(null, MSG_PARSE_SERVICE_REGISTER); // TODO: Use Comms-specific constant or move here.
		msg.replyTo = ourMessenger;

		try {
			commsService.send(msg);
		} catch (RemoteException ex1) {
			// Service crashed so just ignore it
			// TODO: Do something else?
		}

    }


	// Used to receive messages from client(s)
	public class IncomingParseServiceHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Log.d(this.getClass().getSimpleName(), "Tid (parse):" + android.os.Process.myTid());

			Log.d(this.getClass().getSimpleName(), "Parse Service received message:");

			switch (msg.what) {
				case MSG_UI_ACTIVITY_REGISTER:
					// TODO: Handle receiving this more than once?
					Log.d(this.getClass().getSimpleName(), "    MSG_UI_ACTIVITY_REGISTER");
					uiActivity = msg.replyTo;

					shutdownRequested = false;

					try {
                        uiActivity.send(Message.obtain(null, Activity_MainDisplay.MSG_DISPLAY_ACTIVITY_REGISTERED));
					} catch (RemoteException e) {
						// UI Activity client is dead so no longer try to access it.
						uiActivity = null;
					}

					if (uiActivity != null) {

						bindCommsService();

						if (commsService != null) {
							registerWithWiFiCommsService();
						}

						//startTestMessages();
					}

					break;

				case HandbagWiFiCommsService.MSG_PARSE_SERVICE_REGISTERED:
					Log.d(this.getClass().getSimpleName(), "received: MSG_PARSE_SERVICE_REGISTERED");
					break;


                case HandbagWiFiCommsService.MSG_COMMS_PACKET_RECEIVED:
                    Log.d(this.getClass().getSimpleName(), "received: MSG_COMMS_PACKET_RECEIVED");
                    processPacketContent(msg.getData().getStringArray(null));
                    break;


//				case MSG_COMMS_SERVICE_REGISTER:
//					// TODO: Handle receiving this more than once?
//					Log.d(this.getClass().getSimpleName(), "    MSG_COMM_SERVICE_REGISTER");
//					commsService = msg.replyTo;
//
//					try {
//						commsService.send(Message.obtain(null, HandbagUI.MSG_UI_ACTIVITY_REGISTERED));
//					} catch (RemoteException e) {
//						// UI Activity client is dead so no longer try to access it.
//						commsService = null;
//					}
//
//					if (commsService != null) {
//						//startTestNetwork();
//					}
//
//					break;


				case MSG_UI_SHUTDOWN_REQUEST:
					Log.d(this.getClass().getSimpleName(), "    MSG_UI_SHUTDOWN_REQUEST");

					// Set a flag so repeating sub-tasks will stop themselves.
					shutdownRequested = true;
					// TODO: Be more proactive and stop them here instead?
					break;

				default:
					Log.d(this.getClass().getSimpleName(), "    (unknown)");
					super.handleMessage(msg);
			}
		}

	}

	// Clients will use this to communicate with us.
    final Messenger ourMessenger = new Messenger(new IncomingParseServiceHandler());

	@Override
	public IBinder onBind(Intent intent) {
		// Return our `messenger` instance to enable clients to send us messages.
		Log.d(this.getClass().getSimpleName(), "onBind entered");
		return ourMessenger.getBinder();
	}


    private static final int PACKET_OFFSET_PACKET_TYPE = 0;

    private static final String PACKET_TYPE_WIDGET = "widget";
    private static final String PACKET_TYPE_FEATURE = "feature";


    // TODO: Move the feature type dispatch into separate class?
    private static final String FEATURE_TYPE_SPEECH = "speech";
    private static final String FEATURE_TYPE_SMS = "sms";

    private static final int PACKET_OFFSET_FEATURE_TYPE = 1;

    private static final Map<String, String> MAP_FEATURE_TO_CLASS = new HashMap<String, String>();

    static {
        MAP_FEATURE_TO_CLASS.put(FEATURE_TYPE_SPEECH, "SpeechFeature");
        MAP_FEATURE_TO_CLASS.put(FEATURE_TYPE_SMS, "SmsFeature");
    };


    public void processPacketContent(String[] packet) {

        // TODO: Add "config" as a field to "widget"s?

        // TODO: Check packet length.

        if (packet[PACKET_OFFSET_PACKET_TYPE].equals(PACKET_TYPE_WIDGET)) {

            // TODO: Pass around the Message object to save re-doing this?
            Message msg = Message.obtain(null, Activity_MainDisplay.MSG_DISPLAY_RECEIVED_WIDGET_PACKET);
            Bundle bundle = new Bundle();
            bundle.putStringArray(null, packet);
            msg.setData(bundle);

            try {
                uiActivity.send(msg);
            } catch (RemoteException e) {
                // UI Activity client is probably dead so no longer try to access it.
                Log.d(this.getClass().getSimpleName(), "Failed to send packet to UI.");
            }

        } else if (packet[PACKET_OFFSET_PACKET_TYPE].equals(PACKET_TYPE_FEATURE)) {
            processFeaturePacket(packet);
        } else {
            Log.d(this.getClass().getSimpleName(), "Unknown packet type: " + packet[PACKET_OFFSET_PACKET_TYPE]);
        }


    }


    private void processFeaturePacket(String[] packet) {

        // TODO: Create the feature object once and then use it for all subsequent actions?

        FeatureConfig feature = null;

        String featureClassName = MAP_FEATURE_TO_CLASS.get(packet[PACKET_OFFSET_FEATURE_TYPE]);

        if (featureClassName != null) {

            // TODO: Find a better way to add the package name to make the fully qualified name?
            featureClassName = this.getPackageName() + "." + featureClassName;

            try {
                feature = (FeatureConfig) Class.forName(featureClassName).getMethod("fromArray", Context.class, String[].class).invoke(null, getApplicationContext(), packet);
            } catch (NoSuchMethodException e) {
                Log.e(this.getClass().getSimpleName(), "fromArray method not found in feature class: " + featureClassName);
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                Log.e(this.getClass().getSimpleName(), "no class found of name: " + featureClassName);
            } catch (IllegalArgumentException e) {
                Log.e(this.getClass().getSimpleName(), "IllegalArgumentException occurred instantiating: " + featureClassName);
            } catch (IllegalAccessException e) {
                Log.e(this.getClass().getSimpleName(), "IllegalAccessException occurred instantiating: " + featureClassName);
            } catch (InvocationTargetException e) {
                Log.e(this.getClass().getSimpleName(), "InvocationTargetException occurred instantiating: " + featureClassName);
            }

            if (feature != null) {
                feature.doAction();
            }

        } else {
            Log.d(this.getClass().getSimpleName(), "Unknown feature type: " + packet[PACKET_OFFSET_FEATURE_TYPE]);
        }
    }

}

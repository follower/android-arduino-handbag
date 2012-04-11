package com.handbagdevices.handbag;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
	
	private Handler testMessageHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {

			if (!shutdownRequested) {
				// Only continue if we haven't been told to shutdown.

				try {
					uiActivity.send(Message.obtain(null, HandbagUI.MSG_UI_TEST_MESSAGE));
				} catch (RemoteException e) {
					// UI Activity client is dead so no longer try to access it.
					uiActivity = null;
				}

				
				if (commsService != null) {
					try {
						commsService.send(Message.obtain(null, HandbagWiFiCommsService.MSG_UI_TEST_NETWORK));
					} catch (RemoteException e) {
						Log.d(this.getClass().getSimpleName(), "RemoteException on network test.");
					}
				}
				
				
				// Only doing this once now...
				//testMessageHandler.sendEmptyMessageDelayed(0, 10000);
				
				
				
			}
	
		}
	
	};
	
	private void startTestMessages() {
		testMessageHandler.removeCallbacksAndMessages(null);
		testMessageHandler.sendEmptyMessageDelayed(0, 5000);
	}
	
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
						uiActivity.send(Message.obtain(null, HandbagUI.MSG_UI_ACTIVITY_REGISTERED));
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

}

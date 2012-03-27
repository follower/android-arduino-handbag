package com.handbagdevices.handbag;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class HandbagWiFiCommsService extends Service {

	// Messages from UI activity
	static final int MSG_UI_TEST_NETWORK = 501;


	static final int MSG_PARSE_SERVICE_REGISTERED = 502;
	
	
	// Flag that should be checked
	private boolean shutdownRequested = false;

	// Used to communicate with the UI Activity & Communication Service 
	Messenger uiActivity = null; // Orders us around
	Messenger parseService = null; // Receives our data, sends us its data.
	
	// Used to receive messages from client(s)
	public class IncomingWiFiCommsServiceHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			Log.d(this.getClass().getSimpleName(), "Tid (wifi):" + android.os.Process.myTid());

			Log.d(this.getClass().getSimpleName(), "WiFi Comms Service received message:");
			
			switch (msg.what) {

				case HandbagParseService.MSG_UI_ACTIVITY_REGISTER: // TODO: Move this constant into UI class?
					// TODO: Handle receiving this more than once?
					Log.d(this.getClass().getSimpleName(), "    MSG_UI_ACTIVITY_REGISTER");
					uiActivity = msg.replyTo;
					
					shutdownRequested = false;
					
					try {
						uiActivity.send(Message.obtain(null, HandbagUI.MSG_UI_ACTIVITY_REGISTERED)); // TODO: Change to specify who was registered with. ***
					} catch (RemoteException e) {
						// UI Activity client is dead so no longer try to access it.
						uiActivity = null;
					}
					
					if (uiActivity != null) {
						//startTestMessages();
					}
					
					break;
			
			
				case HandbagParseService.MSG_UI_SHUTDOWN_REQUEST: // TODO: Move this constant into UI class?
					Log.d(this.getClass().getSimpleName(), "    MSG_UI_SHUTDOWN_REQUEST");
					
					// Set a flag so repeating sub-tasks will stop themselves.
					shutdownRequested  = true;
					// TODO: Be more proactive and stop them here instead?
					break;
				
				default:
					Log.d(this.getClass().getSimpleName(), "    (unknown)");					
					super.handleMessage(msg);					
			}
		}
		
	}
	
	// Clients will use this to communicate with us.
    final Messenger ourMessenger = new Messenger(new IncomingWiFiCommsServiceHandler());
	
	@Override
	public IBinder onBind(Intent intent) {
		// Return our `messenger` instance to enable clients to send us messages.
		Log.d(this.getClass().getSimpleName(), "onBind entered");
		return ourMessenger.getBinder();
	}

}

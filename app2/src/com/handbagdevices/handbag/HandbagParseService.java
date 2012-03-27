package com.handbagdevices.handbag;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class HandbagParseService extends Service {
	
	// Messages from UI activity
	static final int MSG_UI_ACTIVITY_REGISTER = 101;
	static final int MSG_UI_SHUTDOWN_REQUEST = 102;
	
	static final int MSG_UI_EVENT_OCCURRED = 100;
	
	// Messages from communications service
	static final int MSG_COMM_SERVICE_REGISTER = 200;	
	
	// Used to communicate with the UI Activity & Communication Service 
	// We passively receive this connection.
	Messenger uiActivity = null;
	Messenger commService = null; // TODO: Implement service.	
	
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

				testMessageHandler.sendEmptyMessageDelayed(0, 10000);				
			}
	
		}
	
	};
	
	private void startTestMessages() {
		testMessageHandler.removeCallbacksAndMessages(null);
		testMessageHandler.sendEmptyMessageDelayed(0, 5000);
	}
	
	// Used to receive messages from client(s)
	public class IncomingParseServiceHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
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
						startTestMessages();
					}
					
					break;
					
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

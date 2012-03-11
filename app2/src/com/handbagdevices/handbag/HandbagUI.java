package com.handbagdevices.handbag;

import android.app.Activity;
import android.app.AlertDialog;
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
import android.os.Handler;

public class HandbagUI extends Activity {
	
	// Messages to UI activity (i.e. us)
	static final int MSG_UI_ACTIVITY_REGISTERED = 1;
	static final int MSG_UI_TEST_MESSAGE = 2;
	
	
	Messenger parseService = null;
	boolean parseServiceIsBound = false;
	
	// Used to receive messages from service(s)
	class IncomingUiHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_UI_ACTIVITY_REGISTERED:
					Log.d(this.getClass().getSimpleName(), "received: MSG_UI_ACTIVITY_REGISTERED");
					break;
					
				case MSG_UI_TEST_MESSAGE:
					new AlertDialog.Builder(HandbagUI.this).setMessage("Message received!").show();
					break;
				
				default:
					super.handleMessage(msg);
			}
		}
	}
	
	// Services we connect to will use this to communicate with us.	
    final Messenger ourMessenger = new Messenger(new IncomingUiHandler());
    
	
	private ServiceConnection connParseService = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			// Store the object we will use to communicate with the service.
			parseService = new Messenger(service);
			parseServiceIsBound = true;
			Log.d(this.getClass().getSimpleName(), "Parse Service bound");
			
			registerWithParseServer();			
		}

		public void onServiceDisconnected(ComponentName className) {
			// Called when the service crashes/unexpectedly disconnects.
			parseService = null;
			parseServiceIsBound = false;
		}
		
	};
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    
    private void registerWithParseServer() {
    	
    	Log.d(this.getClass().getSimpleName(), "Registering with Parse Server.");
    	
		Message msg = Message.obtain(null, HandbagParseService.MSG_UI_ACTIVITY_REGISTER);
		msg.replyTo = ourMessenger;					
		
		try {
			parseService.send(msg);
		} catch (RemoteException ex1) {
			// Service crashed so just ignore it
			// TODO: Do something else?
		}
    	
    }
    

	@Override
	protected void onStart() {
		Log.d(this.getClass().getSimpleName(), "Entered onStart()");		
		super.onStart();
		
		// Bind to Parse Service which receives configuration information from the data
		// source, and to which we send event information. TODO: Improve class name?
		boolean bindSuccessful = bindService(new Intent(HandbagUI.this, HandbagParseService.class), connParseService, Context.BIND_AUTO_CREATE);
		Log.d(this.getClass().getSimpleName(), "bound:" + bindSuccessful);

		
		if (parseService != null) {
			// We do this here to handle the situation that we're "resuming" after being
			// hidden. This ensures the Parse Server is "woken up".
			registerWithParseServer();
		}
		
		Log.d(this.getClass().getSimpleName(), "Exited onStart()");		
	}


	@Override
	protected void onStop() {
		Log.d(this.getClass().getSimpleName(), "Entered onStop()");		
		super.onStop();

		try {
			parseService.send(Message.obtain(null, HandbagParseService.MSG_UI_SHUTDOWN_REQUEST));
		} catch (RemoteException e) {
			// Service crashed so just ignore it
		}
		
/*
        // For some reason this causes a "Service not registered" error:
         * 
		// Unbind from Parse Service if we're still connected.
		if (parseServiceIsBound) {
			unbindService(connParseService);
			parseServiceIsBound = false;
		}
*/    

		Log.d(this.getClass().getSimpleName(), "Exited onStop()");
	}
    
}
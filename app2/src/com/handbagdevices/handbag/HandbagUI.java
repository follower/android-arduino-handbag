package com.handbagdevices.handbag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.EditText;
import android.os.Handler;

public class HandbagUI extends Activity {
	
	// Messages to UI activity (i.e. us)
	static final int MSG_UI_ACTIVITY_REGISTERED = 1;
	static final int MSG_UI_TEST_MESSAGE = 2;
	
	
	Messenger parseService = null;
	boolean parseServiceIsBound = false;
	
	Messenger commsService = null;
	boolean commsServiceIsBound = false;
	
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
					
					Log.d(this.getClass().getSimpleName(), "Tid (ui):" + android.os.Process.myTid());
					
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
	
	
	private SharedPreferences appPrefs;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        appPrefs = getSharedPreferences("handbag", MODE_PRIVATE); // Add MODE_MULTI_PROCESS ?
        
        // TODO: Find a simpler approach for all these prefs...
        String pref_network_host_name = appPrefs.getString("network_host_name", "");
        
        if (!pref_network_host_name.isEmpty()) {
        	EditText widgetHostName = (EditText) findViewById(R.id.hostName);
        	widgetHostName.setText(pref_network_host_name);
        }
        
        
        String pref_network_host_port = appPrefs.getString("network_host_port", "");
        
        if (!pref_network_host_port.isEmpty()) {
        	EditText widgetHostPort = (EditText) findViewById(R.id.hostPort);
        	widgetHostPort.setText(pref_network_host_port);        	
        }
        
        
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

    
    private void registerWithWiFiCommsService() {
    	
    	Log.d(this.getClass().getSimpleName(), "Registering with WiFi Comms Service.");
    	
		Message msg = Message.obtain(null, HandbagParseService.MSG_UI_ACTIVITY_REGISTER); // TODO: Use Comms-specific constant or move here.
		msg.replyTo = ourMessenger;
		
		try {
			commsService.send(msg);
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
		Log.d(this.getClass().getSimpleName(), "Parse Service bound:" + bindSuccessful);

		// TODO: Use the chosen Comms Service (WiFi, USB ADK, BT?) 
		bindSuccessful = bindService(new Intent(HandbagUI.this, HandbagWiFiCommsService.class), connCommsService, Context.BIND_AUTO_CREATE);
		Log.d(this.getClass().getSimpleName(), "Comms Service bound:" + bindSuccessful);
		
		if (parseService != null) {
			// We do this here to handle the situation that we're "resuming" after being
			// hidden. This ensures the Parse Server is "woken up".
			registerWithParseServer();
		}
		
		if (commsService != null) {
			// We do this here to handle the situation that we're "resuming" after being
			// hidden. This ensures the Comms Server is "woken up".
			registerWithWiFiCommsService(); // TODO: Make generic.
		}
		
		Log.d(this.getClass().getSimpleName(), "Exited onStart()");		
	}


	@Override
	protected void onPause() {
		super.onPause();
		
		SharedPreferences.Editor prefs = appPrefs.edit();
		
		
		EditText widgetHostName = (EditText) findViewById(R.id.hostName);
		
		if (widgetHostName.length() > 0) {
			// TODO: Save this when edited instead?
			prefs.putString("network_host_name", widgetHostName.getText().toString());
		}
		
		
		EditText widgetHostPort = (EditText) findViewById(R.id.hostPort);
		if (widgetHostPort.length() > 0) {
			// TODO: Set default if empty?
			// TODO: Save this when edited instead?
			prefs.putString("network_host_port", widgetHostPort.getText().toString());
		}
		
		
		prefs.commit();
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
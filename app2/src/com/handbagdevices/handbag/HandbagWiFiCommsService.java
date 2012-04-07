package com.handbagdevices.handbag;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
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
	
	private class TestSocketTask extends AsyncTask<Void, Void, String[]> {

		@Override
		protected String[] doInBackground(Void... params) {
			return doSocketTest();
		}

		@Override
		protected void onPostExecute(String[] result) {
			super.onPostExecute(result);

			if ((!shutdownRequested) && (result != null)) {
				// Only continue if we haven't been told to shutdown
				// and request was successful.

				// TODO: Notify caller of (reason for) unsuccessful request
				//       and/or leave message handler to check for null?

				// TODO: Do this properly send to parse service...
				try {
					Message msg = Message.obtain(null, HandbagUI.MSG_UI_TEST_ARRAY_MESSAGE);
					Bundle bundle = new Bundle();
					bundle.putStringArray(null, result);
					msg.setData(bundle);
					
					uiActivity.send(msg);
				} catch (RemoteException e) {
					// UI Activity client is dead so no longer try to access it.
					uiActivity = null;
				}
			}
			
		}

	};
	
	String[] doSocketTest() {
		Socket socket = null;
		
		DataOutputStream dataOutStream = null;
		DataInputStream dataInStream = null;
		
		String[] newPacket = null;
		
		// ---------
		// TODO: Should really handle this differently--like when first registered?
		//       (Also, these won't have been updated until the app paused...)
		SharedPreferences appPrefs = getSharedPreferences("handbag", MODE_PRIVATE); // Add MODE_MULTI_PROCESS ?
		
		String hostName = appPrefs.getString("network_host_name", "");
		
		if (hostName.isEmpty()) {
			Log.e(this.getClass().getSimpleName(), "No hostname provided.");
			return null; // TODO: Do something else
		}
		
		String hostPortAsString = appPrefs.getString("network_host_port", "");
		
		Log.d(this.getClass().getSimpleName(), "Port as string: " + hostPortAsString);
		
		if (hostPortAsString.isEmpty()) {
			hostPortAsString = "0";
		}
		
		Integer hostPort = Integer.valueOf(hostPortAsString);
		
		if (hostPort == 0) {
			hostPort = 0xba9;
		}
		
		Log.d(this.getClass().getSimpleName(), "Host: " + hostName +" Port: " + hostPort);
		
		// ---------
		
		// TODO: Check network available
		
		try {
			//socket = new Socket("www.google.com", 80);
			//socket = new Socket("74.125.237.100", 80);
			//socket = new Socket("10.1.1.3", 0xBA9);
			socket = new Socket(hostName, hostPort);
		} catch (UnknownHostException e) {
			Log.d(this.getClass().getSimpleName(), "Unknown Host."); // Note: Requires internet permission ***
		} catch (IOException e) {
			Log.d(this.getClass().getSimpleName(), "IOException when creating Socket."); // Note: Requires internet permission ***
		}
		
		Log.d(this.getClass().getSimpleName(), "socket: " + socket);
		
		if (socket != null) {
			try {
				dataOutStream = new DataOutputStream(socket.getOutputStream());
				dataInStream = new DataInputStream(socket.getInputStream());
			} catch (IOException e) {
				Log.d(this.getClass().getSimpleName(), "IOException when creating data streams.");
			}

			Log.d(this.getClass().getSimpleName(), "dataOutStream: " + dataOutStream);
			Log.d(this.getClass().getSimpleName(), "dataInStream: " + dataInStream);

			
			// TODO: Redo all this with one catch for IOException & use finally to close socket?
			if ((dataOutStream != null) && (dataInStream != null)) {
				try {
					dataOutStream.writeBytes("HELLO SERVER. I CAN HAZ PAGE?\n\n");

					Log.d("Got", "available: " + dataInStream.available());

					PacketParser parser = new PacketParser(new InputStreamReader(dataInStream));

					newPacket = parser.getNextPacket();

					Log.d(this.getClass().getSimpleName(), "Got result: " + Arrays.toString(newPacket));
					
				} catch (IOException e) {
					Log.d(this.getClass().getSimpleName(), "IOException when sending/reading data.");
					e.printStackTrace();
				} 
			}
			
			if (socket!= null) { // TODO: Can this happen here?
				try {
					socket.close();
				} catch (IOException e) {
					Log.d(this.getClass().getSimpleName(), "IOException when closing Socket.");
				}
			}
			
			try {
				dataOutStream.close();
				dataInStream.close();
			} catch (NullPointerException ex1) {
				Log.d(this.getClass().getSimpleName(), "NullPointerException when closing data streams.");
			} catch (IOException e) {
				Log.d(this.getClass().getSimpleName(), "IOException when closing data streams.");
			}
		}
		
		return newPacket;
		
	}
	


	
	
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
						
						// Can't do this here because a callback counts as a main thread?
						//doSocketTest();
						
					}
					
					break;

					
				case HandbagParseService.MSG_PARSE_SERVICE_REGISTER: // TODO: Move this constant into UI class?
					// TODO: Handle receiving this more than once?
					Log.d(this.getClass().getSimpleName(), "    MSG_PARSE_SERVICE_REGISTER");
					parseService = msg.replyTo;
					
					try {
						parseService.send(Message.obtain(null, MSG_PARSE_SERVICE_REGISTERED));
					} catch (RemoteException e) {
						// UI Activity client is dead so no longer try to access it.
						parseService = null;
					}
					
					break;
					
					
				case MSG_UI_TEST_NETWORK:
					Log.d(this.getClass().getSimpleName(), "    MSG_UI_TEST_NETWORK");					
					// TODO: Retrieve host name/port from message rather than prefs?
					if (parseService != null) {
						//doSocketTest();
						new TestSocketTask().execute();
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

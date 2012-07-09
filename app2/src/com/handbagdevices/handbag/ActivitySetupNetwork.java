package com.handbagdevices.handbag;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.os.Handler;

public class ActivitySetupNetwork extends Activity {

    // Messages to setup activity (i.e. us)
    static final int MSG_SETUP_ACTIVITY_REGISTERED = 1;

	Messenger commsService = null;
	boolean commsServiceIsBound = false;

    // Used to receive messages from comms service
    class CommsMessageHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {

			// TODO: Only continue if a shutdown hasn't been ordered? (Can we stop this handler directly?)

			switch (msg.what) {
                case MSG_SETUP_ACTIVITY_REGISTERED:
                    Log.d(this.getClass().getSimpleName(), "received: MSG_SETUP_ACTIVITY_REGISTERED");
					break;

				default:
					super.handleMessage(msg);
			}
		}
	}

	// Services we connect to will use this to communicate with us.
    final Messenger ourMessenger = new Messenger(new CommsMessageHandler());


    private ServiceConnection connCommsService = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			// Store the object we will use to communicate with the service.
			commsService = new Messenger(service);
			commsServiceIsBound = true;
			Log.d(this.getClass().getSimpleName(), "Comms Service bound");
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

        populateFromPrefs();

    }


    private void populateFromPrefs() {

        // TODO: Find a simpler approach for all these prefs...
        String pref_network_host_name = appPrefs.getString("network_host_name", "");

        if (!pref_network_host_name.isEmpty()) {
        	EditText widgetHostName = (EditText) findViewById(R.id.hostName);
        	widgetHostName.setText(pref_network_host_name);
        }

		attachPrefsSaver("network_host_name", R.id.hostName);


        String pref_network_host_port = appPrefs.getString("network_host_port", "");

        if (!pref_network_host_port.isEmpty()) {
        	EditText widgetHostPort = (EditText) findViewById(R.id.hostPort);
        	widgetHostPort.setText(pref_network_host_port);
        }

		// TODO: Set default if empty?
		attachPrefsSaver("network_host_port", R.id.hostPort);

    }




	@Override
	protected void onStart() {
		Log.d(this.getClass().getSimpleName(), "Entered onStart()");
		super.onStart();


		// TODO: Use the chosen Comms Service (WiFi, USB ADK, BT?)
        startService(new Intent(ActivitySetupNetwork.this, HandbagWiFiCommsService.class));
		boolean bindSuccessful = bindService(new Intent(ActivitySetupNetwork.this, HandbagWiFiCommsService.class), connCommsService, Context.BIND_AUTO_CREATE);
		Log.d(this.getClass().getSimpleName(), "Comms Service bound:" + bindSuccessful);

		Log.d(this.getClass().getSimpleName(), "Exited onStart()");
	}


	@Override
	protected void onStop() {
		Log.d(this.getClass().getSimpleName(), "Entered onStop()");

		super.onStop();

		// Note: MainDisplay activity is responsible for closing connections

        unbindService(connCommsService);
        // connCommsService = null;

		Log.d(this.getClass().getSimpleName(), "Exited onStop()");
	}

	private void attachPrefsSaver(final String prefsName, int id) {

		EditText widget = (EditText) findViewById(id);
		if (widget != null) {
			widget.addTextChangedListener(new TextWatcher() {

				public void onTextChanged(CharSequence s, int start, int before, int count) {
					// TODO: Only save valid values? (e.g. support a supplied validation function?)
					//       And/or only save on "Done"/lose focus instead?
					appPrefs.edit().putString(prefsName, s.toString()).commit();
				}

				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				public void afterTextChanged(Editable s) {}
			});
		}

	}


	private boolean isOnline() {
		NetworkInfo netInfo = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

		return (netInfo != null) ? netInfo.isConnected() : false;
	}


	public void onClick_buttonConnect(View theView) {

		// TODO: Make this check proactive and disable the button and/or
		//       do the check in the WiFi comms service?
		if (!isOnline()) {
			new AlertDialog.Builder(this).setMessage("No wireless connection available.").show();
			return;
		}

        // TODO: Move this into separate routine
        // TODO: Provide some sort of status feedback if any of this fails.
        Message msg = getMessageForConnect();
        if ((commsService != null) && (msg != null)) {
            try {
                commsService.send(msg);
            } catch (RemoteException e) {
                Log.d(this.getClass().getSimpleName(), "RemoteException on network connection.");
            }
        }

        // open display activity
        Intent startDisplayActivityIntent = new Intent(ActivitySetupNetwork.this, Activity_MainDisplay.class);

        startDisplayActivityIntent.putExtra("COMMS_SERVICE", commsService); // TODO: Use a constant

        startActivity(startDisplayActivityIntent);
	}


    public void onClick_buttonStartTestServer(View theView) {

        // Note: This code uses Fully Qualified Names to avoid having
        //       to have imports--since this code is only intended to be
        //       temporary.

        android.os.AsyncTask<String, Void, Void> serverThread = new android.os.AsyncTask<String, Void, Void>() {

            @Override
            protected Void doInBackground(String... params) {
                java.net.ServerSocket server = null;
                java.net.Socket client = null;

                try {
                    server = new java.net.ServerSocket(0xba9); // Note: localhost only
                    server.setReuseAddress(true);
                    client = server.accept();

                    client.getOutputStream().write(params[0].getBytes());

                    client.close();

                    server.close();
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }

                return null;
            }

        };

        serverThread.execute(((EditText) findViewById(R.id.textToSend)).getText().toString());

    }

    private Message getMessageForConnect() {
        Message msg;

        String hostName = appPrefs.getString("network_host_name", "");

        if (hostName.isEmpty()) {
            Log.e(this.getClass().getSimpleName(), "No hostname provided.");
            return null; // TODO: Throw exception instead?
        }

        Integer hostPort = Integer.valueOf(appPrefs.getString("network_host_port", "0"));

        if (hostPort == 0) {
            hostPort = 0xba9; // Default port
        }

        Log.d(this.getClass().getSimpleName(), "Host: " + hostName + " Port: " + hostPort);

        Bundle bundle = new Bundle();
        bundle.putString("hostName", hostName);
        bundle.putInt("hostPort", hostPort);

        msg = Message.obtain(null, HandbagWiFiCommsService.MSG_UI_CONNECT_TO_TARGET);
        msg.setData(bundle);

        return msg;
    }


	@Override
	public void onBackPressed() {

		// TODO: Need to solve "leaked connection" errors before re-enabling this:
        super.onBackPressed();
	}


}
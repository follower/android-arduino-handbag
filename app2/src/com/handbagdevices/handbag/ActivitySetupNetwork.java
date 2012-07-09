package com.handbagdevices.handbag;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

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
import android.view.ViewGroup;
import android.widget.EditText;
import android.os.Handler;

public class ActivitySetupNetwork extends Activity implements ISetupActivity {

	// Messages to UI activity (i.e. us)
	static final int MSG_UI_ACTIVITY_REGISTERED = 1;
    static final int MSG_UI_RECEIVED_WIDGET_PACKET = 5;

	Messenger parseService = null;
	boolean parseServiceIsBound = false;

	Messenger commsService = null;
	boolean commsServiceIsBound = false;

	// Used to receive messages from service(s)
	class IncomingUiHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {

			// TODO: Only continue if a shutdown hasn't been ordered? (Can we stop this handler directly?)

			switch (msg.what) {
				case MSG_UI_ACTIVITY_REGISTERED:
					Log.d(this.getClass().getSimpleName(), "received: MSG_UI_ACTIVITY_REGISTERED");
					break;


                case MSG_UI_RECEIVED_WIDGET_PACKET:
                    handleWidgetPacket(msg.getData().getStringArray(null));
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
		boolean bindSuccessful = bindService(new Intent(ActivitySetupNetwork.this, HandbagParseService.class), connParseService, Context.BIND_AUTO_CREATE);
		Log.d(this.getClass().getSimpleName(), "Parse Service bound:" + bindSuccessful);

		// TODO: Use the chosen Comms Service (WiFi, USB ADK, BT?)
		bindSuccessful = bindService(new Intent(ActivitySetupNetwork.this, HandbagWiFiCommsService.class), connCommsService, Context.BIND_AUTO_CREATE);
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

		try {
			commsService.send(Message.obtain(null, HandbagParseService.MSG_UI_SHUTDOWN_REQUEST));
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

	private boolean showingMainStage = false;

	private void displayMainStage() {
		if (!showingMainStage) {
			setContentView(R.layout.mainstage);
			showingMainStage = true;
		}
	}

	private void hideMainStage() {
		if (showingMainStage) {
			setContentView(R.layout.main);
			showingMainStage = false;
			populateFromPrefs();

            // TODO: Move this functionality elsewhere?
            disconnectFromTarget();
		}
	}


    private void disconnectFromTarget() {
        if (commsService != null) {
            try {
                commsService.send(Message.obtain(null, HandbagWiFiCommsService.MSG_UI_DISCONNECT_FROM_TARGET));
            } catch (RemoteException e) {
                // Service crashed so just ignore it
            }
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

		displayMainStage();

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

		hideMainStage();

		// TODO: Need to solve "leaked connection" errors before re-enabling this:
		//super.onBackPressed();
	}


    private static final String WIDGET_TYPE_LABEL = "label";
    private static final String WIDGET_TYPE_BUTTON = "button";
    private static final String WIDGET_TYPE_DIALOG = "dialog";
    private static final String WIDGET_TYPE_PROGRESS_BAR = "progress";
    private static final String WIDGET_TYPE_TEXT_INPUT = "textinput";

    private static final int PACKET_OFFSET_WIDGET_TYPE = 1;

    private static final Map<String, String> MAP_WIDGET_TO_CLASS = new HashMap<String, String>();

    static {
        MAP_WIDGET_TO_CLASS.put(WIDGET_TYPE_LABEL, "LabelWidget");
        MAP_WIDGET_TO_CLASS.put(WIDGET_TYPE_DIALOG, "DialogWidget");
        MAP_WIDGET_TO_CLASS.put(WIDGET_TYPE_PROGRESS_BAR, "ProgressBarWidget");
        MAP_WIDGET_TO_CLASS.put(WIDGET_TYPE_BUTTON, "ButtonWidget");
        MAP_WIDGET_TO_CLASS.put(WIDGET_TYPE_TEXT_INPUT, "TextInputWidget");
    };


    private void handleWidgetPacket(String[] packet) {

        WidgetConfig widget = null;

        String widgetClassName = MAP_WIDGET_TO_CLASS.get(packet[PACKET_OFFSET_WIDGET_TYPE]);

        if (widgetClassName != null) {

            // TODO: Find a better way to add the package name to make the fully qualified name?
            widgetClassName = this.getPackageName() + "." + widgetClassName;

            try {
                widget = (WidgetConfig) Class.forName(widgetClassName).getMethod("fromArray", String[].class).invoke(null, (Object) packet);
            } catch (NoSuchMethodException e) {
                Log.e(this.getClass().getSimpleName(), "fromArray method not found in widget class: " + widgetClassName);
            } catch (ClassNotFoundException e) {
                Log.e(this.getClass().getSimpleName(), "no class found of name: " + widgetClassName);
            } catch (IllegalArgumentException e) {
                Log.e(this.getClass().getSimpleName(), "IllegalArgumentException occurred instantiating: " + widgetClassName);
            } catch (IllegalAccessException e) {
                Log.e(this.getClass().getSimpleName(), "IllegalAccessException occurred instantiating: " + widgetClassName);
            } catch (InvocationTargetException e) {
                Log.e(this.getClass().getSimpleName(), "InvocationTargetException occurred instantiating: " + widgetClassName);
            }

            if (widget != null) {

                widget.setParentActivity(this); // TODO: Do better/properly?

                ViewGroup mainstage = (ViewGroup) findViewById(R.id.mainstage);

                // If we can't find mainstage it's probably because the
                // active view has changed since the request was made.
                if (mainstage != null) {
                    widget.displaySelf(mainstage);
                }
            }

        } else {
            Log.d(this.getClass().getSimpleName(), "Unknown widget type: " + packet[PACKET_OFFSET_WIDGET_TYPE]);
        }

    }


    /* protected */ public void sendPacket(String[] packet) {
        try {
            Message msg = Message.obtain(null, HandbagWiFiCommsService.MSG_COMMS_SEND_PACKET);
            Bundle bundle = new Bundle();
            bundle.putStringArray(null, packet);
            msg.setData(bundle);

            commsService.send(msg);
        } catch (RemoteException e) {
            // Comms service client is dead so no longer try to access it.
            commsService = null;
        }
    }
}
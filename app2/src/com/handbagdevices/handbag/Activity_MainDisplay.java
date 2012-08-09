package com.handbagdevices.handbag;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
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
import android.view.ViewGroup;

public class Activity_MainDisplay extends Activity implements IDisplayActivity {

    // Messages to display activity (i.e. us)
    static final int MSG_DISPLAY_ACTIVITY_REGISTERED = 1;
    static final int MSG_DISPLAY_RECEIVED_WIDGET_PACKET = 5;

	Messenger parseService = null;

	Messenger commsService = null;


    // Used to receive messages from comms service
    class ParserMessageHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            // TODO: Only continue if a shutdown hasn't been ordered? (Can we stop this handler directly?)

            switch (msg.what) {
                case MSG_DISPLAY_ACTIVITY_REGISTERED:
                    Log.d(this.getClass().getSimpleName(), "received: MSG_UI_ACTIVITY_REGISTERED");
                    break;

                case MSG_DISPLAY_RECEIVED_WIDGET_PACKET:
                    Log.d(this.getClass().getSimpleName(), "received: MSG_DISPLAY_RECEIVED_WIDGET_PACKET");
                    handleWidgetPacket(msg.getData().getStringArray(null));
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }


    // Services we connect to will use this to communicate with us.
    final Messenger ourMessenger = new Messenger(new ParserMessageHandler());

    private ServiceConnection connParseService = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            // Store the object we will use to communicate with the service.
            parseService = new Messenger(service);
            Log.d(this.getClass().getSimpleName(), "Parse Service bound");

            registerWithParseServer();
        }


        public void onServiceDisconnected(ComponentName className) {
            // Called when the service crashes/unexpectedly disconnects.
            parseService = null;
        }

    };


    private void registerWithParseServer() {

        Log.d(this.getClass().getSimpleName(), "Registering with Parse Server.");

        Message msg = Message.obtain(null, HandbagParseService.MSG_UI_ACTIVITY_REGISTER); // TODO: Change "UI" to "DISPLAY" when we finish move to this new approach
        msg.replyTo = ourMessenger;

        try {
            parseService.send(msg);
        } catch (RemoteException ex1) {
            // Service crashed so just ignore it
            // TODO: Do something else?
        }

    }

    private void registerWithCommsService() {

        Log.d(this.getClass().getSimpleName(), "Registering with current comms service.");

        Message msg = Message.obtain(null, CommsService_WiFi.MSG_SETUP_ACTIVITY_REGISTER); // TODO: Use Comms-specific interface constant or move here.
        msg.replyTo = ourMessenger;

        try {
            commsService.send(msg);
        } catch (RemoteException ex1) {
            // Service crashed so just ignore it
            // TODO: Do something else?
            Log.e(this.getClass().getSimpleName(), "Failed to register with comms service");
        }

    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // TODO: Store connections from bundle
        commsService = getIntent().getExtras().getParcelable("COMMS_SERVICE");

        Log.d(this.getClass().getSimpleName(), "onCreate received COMMS_SERVICE: " + commsService);
    }


	@Override
	protected void onStart() {
		Log.d(this.getClass().getSimpleName(), "Entered onStart()");
		super.onStart();

        if (wasRestart) {
            wasRestart = false;
            finish();
            return;
        }

        // Bind to Parse Service which receives configuration information from the data
        // source, and to which we send event information. TODO: Improve class name?
        Intent startParseServiceIntent = new Intent(Activity_MainDisplay.this, HandbagParseService.class);
        startParseServiceIntent.putExtra("COMMS_SERVICE", commsService); // TODO: Use a constant

        boolean bindSuccessful = bindService(startParseServiceIntent, connParseService, Context.BIND_AUTO_CREATE);
        Log.d(this.getClass().getSimpleName(), "Parse Service bound:" + bindSuccessful);

        if (parseService != null) {
            // We do this here to handle the situation that we're "resuming" after being
            // hidden. This ensures the Parse Server is "woken up".
            registerWithParseServer();
        }

        if (commsService != null) {
            registerWithCommsService();
        }

        setContentView(R.layout.mainstage);

		Log.d(this.getClass().getSimpleName(), "Exited onStart()");
	}

    boolean wasRestart = false;

    @Override
    protected void onRestart() {
        Log.d(this.getClass().getSimpleName(), "onRestart() called.");
        super.onRestart();
        wasRestart = true;
    }


    @Override
    protected void onStop() {
        Log.d(this.getClass().getSimpleName(), "onStop() called.");
        super.onStop();

        // We currently make no promises to keep things going in the background, so
        // we handle this as if the user had pressed the back button first.
        // TODO: Keep some and/or all things running in the background? (e.g. SMS service?)
        // Note: If we remove this onDestroy & the wasRestart above then things will run
        // okay apart from the pre-existing widgets are saved. If we saved them we
        // could keep things going.
        onDestroy();
    }

	@Override
    protected void onDestroy() {
        Log.d(this.getClass().getSimpleName(), "Entered onDestroy()");

        super.onDestroy();

		try {
			parseService.send(Message.obtain(null, HandbagParseService.MSG_UI_SHUTDOWN_REQUEST));
		} catch (RemoteException e) {
			// Service crashed so just ignore it
		}

        if (commsService != null) {
            try {
                commsService.send(Message.obtain(null, HandbagParseService.MSG_UI_SHUTDOWN_REQUEST));
            } catch (RemoteException e) {
                // Service crashed so just ignore it
            }
        }

        if (connParseService != null) {
            unbindService(connParseService);
            connParseService = null;
        }

        Log.d(this.getClass().getSimpleName(), "Exited onDestroy()");
	}


    private void disconnectFromTarget() {
        if (commsService != null) {
            try {
                commsService.send(Message.obtain(null, CommsService_WiFi.MSG_UI_DISCONNECT_FROM_TARGET));
            } catch (RemoteException e) {
                // Service crashed so just ignore it
            }
        }
    }


    private void normalDisconnect() {

        // This is kinda redundant when called as a result of a "target disconnected" message
        // but enables us to be consistent in our handling of exits.
        disconnectFromTarget();

        setResult(RESULT_OK);
    }


	@Override
	public void onBackPressed() {

        normalDisconnect();

        // TODO: Need to solve "leaked connection" errors before re-enabling this:
        super.onBackPressed();
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


    protected void handleWidgetPacket(String[] packet) {

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
            Message msg = Message.obtain(null, CommsService_WiFi.MSG_COMMS_SEND_PACKET);
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
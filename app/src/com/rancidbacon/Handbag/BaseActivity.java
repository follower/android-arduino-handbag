package com.rancidbacon.Handbag;

import com.rancidbacon.Handbag.R;
import com.rancidbacon.Handbag.HandbagActivity.ConfigMsg;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BaseActivity extends HandbagActivity {

	private static final int UI_WIDGET_BUTTON = 0x00;
	private static final int UI_WIDGET_LABEL = 0x01;
	private static final int UI_WIDGET_TEXT_INPUT = 0x02;
	private static final int UI_WIDGET_DIALOG = 0x03;

	private static final byte COMMAND_GOT_EVENT = (byte) 0x80;
	private static final byte EVENT_BUTTON_CLICK = (byte) 0x01;
	private static final byte EVENT_TEXT_INPUT = (byte) 0x02;
	
	private static final int WIDGET_ID_OFFSET = 7200;
	
	private InputController mInputController;

	public BaseActivity() {
		super();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (mAccessory != null) {
			showControls();
		} else {
			hideControls();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add("Simulate");
		menu.add("Quit");
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getTitle() == "Simulate") {
			showControls();
		} else if (item.getTitle() == "Quit") {
			finish();
			System.exit(0);
		}
		return true;
	}

	protected void enableControls(boolean enable) {
		if (enable) {
			showControls();
		} else {
			hideControls();
		}
	}

	protected void hideControls() {
		setContentView(R.layout.no_device);
		mInputController = null;
	}

	
	void addButton(String labelText, final byte widgetId) {
				
		LinearLayout layout = (LinearLayout) findViewById(R.id.mainstage);
		
        Button button = new Button(this);
        button.setId(WIDGET_ID_OFFSET + widgetId);
        button.setText(labelText);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	sendCommand(COMMAND_GOT_EVENT, EVENT_BUTTON_CLICK, widgetId);
            }
        });        
        
        layout.addView(button);
	};
	
	
	void addTextInput(/* TODO: Add default text? */ final byte widgetId) {
		/*

		 */

		LinearLayout layout = (LinearLayout) findViewById(R.id.mainstage);
		
        // TODO: Do a find by ID in the listener rather than make this final?
        final EditText textInput = new EditText(this);
        layout.addView(textInput);
        
        textInput.setOnKeyListener(new OnKeyListener() {

			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) &&
			            (keyCode == KeyEvent.KEYCODE_ENTER)) {
					
					sendCommand(COMMAND_GOT_EVENT, EVENT_TEXT_INPUT, widgetId);
					sendString(textInput.getText().toString());
					
					return true;
				}
				return false;
			}
        	
        });
	}
	
	void addLabel(String labelText, int fontSize, byte widgetAlignment, final byte widgetId) {
		/*
		 
		   This (now slightly misnamed) method either creates a new label or modifies an existing
		   label.
		   
		   It works for both labels and other widgets that subclass TextView. (e.g. Buttons)
		   
		 */
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.mainstage);
		
		// TODO: Check we actually got what we were looking for.
		TextView label = (TextView) layout.findViewById(WIDGET_ID_OFFSET + widgetId);
		
		if (label == null) {
			label = new TextView(this);
			label.setId(WIDGET_ID_OFFSET + widgetId);

			layout.addView(label);
		}

		label.setText(labelText);		
        
        if (fontSize > 0) {
        	label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, fontSize);
        }
        
        if (widgetAlignment > 0) {
        	label.setGravity(widgetAlignment);
        }

	}
	
	void showDialog(String labelText) {
		/*
		 */
		new AlertDialog.Builder(this).setMessage(labelText).show();
	}
	
	protected void showControls() {
		setContentView(R.layout.main);

		mInputController = new InputController(this);
		mInputController.accessoryAttached();
	}

	protected void handleConfigMessage(ConfigMsg c) {
		switch (c.getWidgetType()) {
			case UI_WIDGET_BUTTON:
				addButton(c.getWidgetText(), c.getWidgetId());
				break;
				
			case UI_WIDGET_LABEL:
				addLabel(c.getWidgetText(), c.getFontSize(), c.getWidgetAlignment(), c.getWidgetId());
				break;

			case UI_WIDGET_TEXT_INPUT:
				addTextInput(c.getWidgetId());
				break;	
			
			case UI_WIDGET_DIALOG:
				showDialog(c.getWidgetText());
				break;	
		} 
	}	
	
	protected void handleHandshakeMessage(HandshakeMsg h) {
		// TODO: Do this properly
		new AlertDialog.Builder(this).setMessage("This accessory is not compatible with this version of the Handbag App.").show();
	}
	
	protected void handleLightMessage(LightMsg l) {
		if (mInputController != null) {
			mInputController.setLightValue(l.getLight());
		}
	}

	protected void handleSwitchMessage(SwitchMsg o) {
		if (mInputController != null) {
			byte sw = o.getSw();
			if (sw >= 0 && sw < 4) {
				mInputController.switchStateChanged(sw, o.getState() != 0);
			}
		}
	}

}
package com.rancidbacon.Handbag;

import com.rancidbacon.Handbag.R;
import com.rancidbacon.Handbag.HandbagActivity.ConfigMsg;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BaseActivity extends HandbagActivity {

	private static final int UI_WIDGET_BUTTON = 0x00;
	private static final int UI_WIDGET_LABEL = 0x01;

	private static final byte COMMAND_GOT_EVENT = (byte) 0x80;
	private static final byte EVENT_BUTTON_CLICK = (byte) 0x01;
	
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
        button.setText(labelText);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
            	sendCommand(COMMAND_GOT_EVENT, EVENT_BUTTON_CLICK, widgetId);
            }
        });        
        
        layout.addView(button);
	}
	
	void addLabel(String labelText, int fontSize, byte widgetAlignment) {
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.mainstage);
		
		TextView label = new TextView(this); 
        label.setText(labelText);
        
        if (fontSize > 0) {
        	label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, fontSize);
        }
        
        if (widgetAlignment > 0) {
        	label.setGravity(widgetAlignment);
        }

        layout.addView(label);
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
				addLabel(c.getWidgetText(), c.getFontSize(), c.getWidgetAlignment());
				break;
		} 
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
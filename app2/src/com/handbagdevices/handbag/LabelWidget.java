package com.handbagdevices.handbag;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

public class LabelWidget extends WidgetConfig {


	private LabelWidget() {
		// TODO Auto-generated method stub
	}
	
	
	public void displaySelf(ViewGroup parent) { // TODO: Accept view/layout but cast to viewgroup?
		
		Log.d(this.getClass().getSimpleName(), "parent: " + parent);
		
		// TODO: Add check for existing widget.

		TextView label = new TextView(parent.getContext());

		parent.addView(label);

		label.setText("Hello");
		
	}

	// @Hides
	public static WidgetConfig fromArray(String[] theArray) {
		return new LabelWidget();
	}

}

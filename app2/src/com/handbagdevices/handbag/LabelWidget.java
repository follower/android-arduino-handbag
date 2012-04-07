package com.handbagdevices.handbag;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;

public class LabelWidget extends WidgetConfig {

	final static int LABEL_ARRAY_OFFSET_ID = 2;
	final static int LABEL_ARRAY_OFFSET_FONT_SIZE = 3;
	final static int LABEL_ARRAY_OFFSET_ALIGNMENT = 4;
	final static int LABEL_ARRAY_OFFSET_TEXT = 5;

	int fontSize;
	int alignment;
	String text;

	private LabelWidget(int widgetId, int theFontSize, int widgetAlignment, String labelText) {
		remoteWidgetId = widgetId;
		fontSize = theFontSize;
		alignment = widgetAlignment;
		text = labelText;
	}
	
	
	public void displaySelf(ViewGroup parent) { // TODO: Accept view/layout but cast to viewgroup?
		
		Log.d(this.getClass().getSimpleName(), "parent: " + parent);
		
		// TODO: Add check for existing widget.

		TextView label = new TextView(parent.getContext());
		label.setId(WIDGET_ID_OFFSET + remoteWidgetId); // TODO: Make this use a function to get the ID?

		parent.addView(label);

		label.setText(text);

		if (fontSize > 0) {
			label.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, fontSize);
		}

		if (alignment > 0) {
			label.setGravity(alignment);
		}
		
	}

	// @Hides
	public static WidgetConfig fromArray(String[] theArray) {
		return new LabelWidget(Integer.valueOf(theArray[LABEL_ARRAY_OFFSET_ID]),
							   Integer.valueOf(theArray[LABEL_ARRAY_OFFSET_FONT_SIZE]),
							   Integer.valueOf(theArray[LABEL_ARRAY_OFFSET_ALIGNMENT]),
							   theArray[LABEL_ARRAY_OFFSET_TEXT]);
	}

}

package com.handbagdevices.handbag;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

// TODO: Subclass from LabelWidget more.

public class ButtonWidget extends LabelWidget {

	int fontSize;
	int alignment;
	String text;

	private ButtonWidget(int widgetId, int theFontSize, int widgetAlignment, String labelText) {
        super(widgetId, theFontSize, widgetAlignment, labelText);
	}


	@Override
    public void displaySelf(ViewGroup parent) { // TODO: Accept view/layout but cast to viewgroup?

		Log.d(this.getClass().getSimpleName(), "parent: " + parent);

        Button button = (Button) parent.findViewById(WIDGET_ID_OFFSET + remoteWidgetId);

        if (button == null) {
            button = new Button(parent.getContext());
            button.setId(WIDGET_ID_OFFSET + remoteWidgetId); // TODO: Make this use a function to get the ID?

            parent.addView(button);
        }

        configureAsTextView(button);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                Log.d(this.getClass().getSimpleName(), "button click: " + remoteWidgetId);
                // TODO: Send response
                // TODO: Send properly rather than accessing activity method directly (or accept callback tunnel from activity)?
                // TODO: Ensure not null?
                activity.sendPacket(new String[] { "widget", "event", Integer.toString(remoteWidgetId), "click" }); // TODO: Do properly.
            }
        });

	}


	// @Hides
	public static WidgetConfig fromArray(String[] theArray) {
		return new ButtonWidget(Integer.valueOf(theArray[LABEL_ARRAY_OFFSET_ID]),
							   Integer.valueOf(theArray[LABEL_ARRAY_OFFSET_FONT_SIZE]),
							   Integer.valueOf(theArray[LABEL_ARRAY_OFFSET_ALIGNMENT]),
							   theArray[LABEL_ARRAY_OFFSET_TEXT]);
	}

}

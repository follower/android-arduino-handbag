package com.handbagdevices.handbag;

import android.util.Log;
import android.view.ViewGroup;
import android.widget.EditText;

// TODO: Extend LabelWidget instead so we can get the same formatting options?
public class TextInputWidget extends WidgetConfig {

    final static int TEXT_INPUT_ARRAY_OFFSET_ID = 2;


    // TODO: Enable default text etc to be supplied?
    protected TextInputWidget(int widgetId) {
        remoteWidgetId = widgetId;
    }


    @Override
    void displaySelf(ViewGroup parent) {
        // TODO Auto-generated method stub

        Log.d(this.getClass().getSimpleName(), "parent: " + parent);

        EditText widget = new EditText(parent.getContext());

        widget.setId(WIDGET_ID_OFFSET + remoteWidgetId); // TODO: Make this use a function to get the ID?

        parent.addView(widget);

        // TODO: Handle changes ***
    }

    // @Hides
    public static WidgetConfig fromArray(String[] theArray) {
        return new TextInputWidget(Integer.valueOf(theArray[TEXT_INPUT_ARRAY_OFFSET_ID]));
    }

}

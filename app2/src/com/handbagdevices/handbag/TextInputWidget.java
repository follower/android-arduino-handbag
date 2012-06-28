package com.handbagdevices.handbag;

import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnKeyListener;
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

        widget.setOnKeyListener(new OnKeyListener() {

            // TODO: Ensure this works everywhere--the docs say apparently soft keyboards don't have to trigger this.
            public boolean onKey(View v, int keyCode, KeyEvent event) {

                Log.d(this.getClass().getSimpleName(), "text input enter pressed: " + remoteWidgetId);

                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {

                    // TODO: Send properly rather than accessing activity method directly (or accept callback tunnel from activity)?
                    activity.sendPacket(new String[] { "widget", "event",
                                                       Integer.toString(remoteWidgetId),
                                                       "input",
                                                       ((EditText) v).getText().toString()
                                                       }); // TODO: Do properly.

                    return true;
                }
                return false;
            }

        });

    }

    // @Hides
    public static WidgetConfig fromArray(String[] theArray) {
        return new TextInputWidget(Integer.valueOf(theArray[TEXT_INPUT_ARRAY_OFFSET_ID]));
    }

}

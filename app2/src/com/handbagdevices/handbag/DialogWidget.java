package com.handbagdevices.handbag;

import android.app.AlertDialog;
import android.view.ViewGroup;

// Note: This isn't *really* a widget as it's only temporarily displayed
//       but we'll use this approach anyway.
public class DialogWidget extends WidgetConfig {

    final static int DIALOG_ARRAY_OFFSET_TEXT = 2;

    String text;


    public DialogWidget(String text) {
        this.text = text;
    }


    @Override
    void displaySelf(ViewGroup parent) {
        new AlertDialog.Builder(parent.getContext()).setMessage(this.text).show();
    }


    // @Hides
    public static WidgetConfig fromArray(String[] theArray) {
        return new DialogWidget(theArray[DIALOG_ARRAY_OFFSET_TEXT]);
    }

}

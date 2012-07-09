package com.handbagdevices.handbag;

import android.view.ViewGroup;
import java.lang.UnsupportedOperationException;

abstract class WidgetConfig {

	int remoteWidgetId;

	// WIDGET_ID_OFFSET + remoteWidgetId = Android UI widget ID
	static final int WIDGET_ID_OFFSET = 7200;

	abstract void displaySelf(ViewGroup parent);

	public static WidgetConfig fromArray(String[] theArray) {
		// This is a work around for Java not having static methods in Interfaces
		// and not having overridable static methods in Abstract Classes.
		// All subclasses of this class should "hide" (because you can't override)
		// this static method.
		throw new UnsupportedOperationException();
	}

    // TODO: Do all this properly to provide "callback" method?.
    protected IDisplayActivity activity = null;


    protected void setParentActivity(IDisplayActivity activity) {
        this.activity = activity;
    }

}

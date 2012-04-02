package com.handbagdevices.handbag;

import android.view.ViewGroup;
import java.lang.UnsupportedOperationException;

abstract class WidgetConfig {

	abstract void displaySelf(ViewGroup parent);
	
	public static WidgetConfig fromArray(String[] theArray) {
		// This is a work around for Java not having static methods in Interfaces
		// and not having overridable static methods in Abstract Classes.
		// All subclasses of this class should "hide" (because you can't override)
		// this static method.
		throw new UnsupportedOperationException();
	}
	
}

package com.rancidbacon.Handbag;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

public interface UsbAccessoryHandlerInterface {
	
	public abstract String get_ACTION_USB_ACCESSORY_DETACHED();

	public abstract void setManager(Activity theActivity);

	public abstract void setAccessory(Object theAccessoryObj);

	public abstract ParcelFileDescriptor openAccessory(Object theAccessoryObj);

	public abstract void getPermission(Intent intent);

	public abstract boolean matchesThisAccessory(Intent intent);

	public abstract boolean hasPermission(Object theAccessoryObj);

	public abstract Object getConnectedAccessory();
	
	public abstract Object getAccessory();	

	public abstract void requestPermission(Object theAccessoryObj,
			PendingIntent permissionIntent);

}
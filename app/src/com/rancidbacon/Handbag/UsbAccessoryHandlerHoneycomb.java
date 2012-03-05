package com.rancidbacon.Handbag;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;

public class UsbAccessoryHandlerHoneycomb implements UsbAccessoryHandlerInterface {

	private UsbManager mUsbManager;

	public UsbAccessory mAccessory;

	public String get_ACTION_USB_ACCESSORY_DETACHED() {
		return UsbManager.ACTION_USB_ACCESSORY_DETACHED;
	}
	
	public void setManager(Activity theActivity) {
		mUsbManager = (UsbManager) theActivity.getSystemService(Context.USB_SERVICE);
	}
	
	public void setAccessory(Object theAccessoryObj) {
		mAccessory = (UsbAccessory) theAccessoryObj;
	}
	
	public ParcelFileDescriptor openAccessory(Object theAccessoryObj) {
		return mUsbManager.openAccessory((UsbAccessory) theAccessoryObj);
	}
	
	public void getPermission(Intent intent) {

		UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
		
		if (intent.getBooleanExtra(
				UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
			openAccessory(accessory);
		} else {
			// TODO: Handle this properly (to allow TAG logging)
			//Log.d(TAG, "permission denied for accessory "
			//		+ accessory);
		}		
		
	}
	
	public boolean matchesThisAccessory(Intent intent) { // TODO: Name this something better
		UsbAccessory accessory = (UsbAccessory) intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
		return accessory != null && accessory.equals(mAccessory);
	}
	
	
	public boolean hasPermission(Object theAccessoryObj) {
		return mUsbManager.hasPermission((UsbAccessory) theAccessoryObj);
	}
	
	public Object getConnectedAccessory() {
		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		return accessory;
	}

	public Object getAccessory() {
		return mAccessory;
	}
	
	
	public void requestPermission(Object theAccessoryObj, PendingIntent permissionIntent) {
		mUsbManager.requestPermission((UsbAccessory) theAccessoryObj, permissionIntent);
	}
}

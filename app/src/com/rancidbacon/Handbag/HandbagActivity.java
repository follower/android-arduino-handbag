/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rancidbacon.Handbag;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.SeekBar;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;
import com.rancidbacon.Handbag.R;

public class HandbagActivity extends Activity implements Runnable {
	private static final String TAG = "Handbag";

	private static final String ACTION_USB_PERMISSION = "com.rancidbacon.Handbag.action.USB_PERMISSION";

	private UsbManager mUsbManager;
	private PendingIntent mPermissionIntent;
	private boolean mPermissionRequestPending;

	UsbAccessory mAccessory;
	ParcelFileDescriptor mFileDescriptor;
	FileInputStream mInputStream;
	FileOutputStream mOutputStream;

	private static final int MESSAGE_CONFIGURE = 0x10;
	private static final int MESSAGE_HANDSHAKE = 0x48;

	// These need to change if they're changed in the sketch
	private static final int CONFIG_OFFSET_WIDGET_TYPE = 1;
	private static final int CONFIG_OFFSET_WIDGET_ID = 2;
	private static final int CONFIG_OFFSET_FONT_SIZE = 3;
	private static final int CONFIG_OFFSET_WIDGET_ALIGNMENT = 4;	
	private static final int CONFIG_OFFSET_TEXT_LENGTH = 5;
	private static final int CONFIG_OFFSET_TEXT_START = 6;
	
	private boolean handshakeAttempted = false;
	private boolean handshakeOk = false;

	protected class ConfigMsg {
		private int widgetType;
		private byte widgetId;
		private int fontSize;
		private byte widgetAlignment;
		private String widgetText;
		
		public ConfigMsg(int widgetType, byte widgetId, int fontSize, byte widgetAlignment, byte[] widgetTextAsBytes) {
			this.widgetType = widgetType;
			this.widgetId = widgetId;
			this.fontSize = fontSize;
			this.widgetAlignment = widgetAlignment;
			this.widgetText = new String(widgetTextAsBytes);
		}
		
		public int getWidgetType() {
			return widgetType;
		}
		
		public byte getWidgetId() {
			return widgetId;
		}

		public int getFontSize() {
			return fontSize;
		}

		public byte getWidgetAlignment() {
			return widgetAlignment;
		}

		public String getWidgetText() {
			return widgetText;
		}
	}
	
	protected class HandshakeMsg {
		public byte byte1;
		public byte byte2;
		public byte byte3;
		
		public HandshakeMsg(byte theByte1, byte theByte2, byte theByte3) {
			byte1 = theByte1;
			byte2 = theByte2;
			byte3 = theByte3;			
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = UsbManager.getAccessory(intent);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {
						Log.d(TAG, "permission denied for accessory "
								+ accessory);
					}
					mPermissionRequestPending = false;
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}
		}
	};

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mUsbManager = UsbManager.getInstance(this);
		mPermissionIntent = PendingIntent.getBroadcast(this, 0, new Intent(
				ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		registerReceiver(mUsbReceiver, filter);

		if (getLastNonConfigurationInstance() != null) {
			mAccessory = (UsbAccessory) getLastNonConfigurationInstance();
			openAccessory(mAccessory);
		}

		setContentView(R.layout.main);

		enableControls(false);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (mAccessory != null) {
			return mAccessory;
		} else {
			return super.onRetainNonConfigurationInstance();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		Intent intent = getIntent();
		if (mInputStream != null && mOutputStream != null) {
			return;
		}

		UsbAccessory[] accessories = mUsbManager.getAccessoryList();
		UsbAccessory accessory = (accessories == null ? null : accessories[0]);
		if (accessory != null) {
			if (mUsbManager.hasPermission(accessory)) {
				openAccessory(accessory);
			} else {
				synchronized (mUsbReceiver) {
					if (!mPermissionRequestPending) {
						mUsbManager.requestPermission(accessory,
								mPermissionIntent);
						mPermissionRequestPending = true;
					}
				}
			}
		} else {
			Log.d(TAG, "mAccessory is null");
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		closeAccessory();
	}

	@Override
	public void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void openAccessory(UsbAccessory accessory) {
		mFileDescriptor = mUsbManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			mAccessory = accessory;
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			mInputStream = new FileInputStream(fd);
			mOutputStream = new FileOutputStream(fd);
			Thread thread = new Thread(null, this, "Handbag");
			thread.start();
			Log.d(TAG, "accessory opened");
			enableControls(true);
		} else {
			Log.d(TAG, "accessory open fail");
		}
	}

	private void closeAccessory() {
		enableControls(false);

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}
	}

	protected void enableControls(boolean enable) {
	}

	private int composeInt(byte hi, byte lo) {
		int val = (int) hi & 0xff;
		val *= 256;
		val += (int) lo & 0xff;
		return val;
	}

	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		byte textLength = 0;
		
		// TODO: Reset this if we are disconnected?
		if (handshakeAttempted && !handshakeOk) {
			// Ignore all subsequent communication.
			return;
		}
		
		while (ret >= 0) {
			try {
				ret = mInputStream.read(buffer);
			} catch (IOException e) {
				break;
			}
			
			if (!handshakeAttempted) {
				handshakeAttempted = true;
				// TODO: Do all this properly (& handle "too old"/"too new"/ options.)				
				if ((ret >= 3) && (buffer[0] == MESSAGE_HANDSHAKE) && (buffer[1] == 'B') && buffer[2] == 0x01) {
					handshakeOk = true;
					// Send response.
					sendCommand((byte) 'H', (byte) 'B', (byte) 0x01); 
				} else {
					handshakeOk = false;
					Message m = Message.obtain(mHandler, MESSAGE_HANDSHAKE);
					m.obj = new HandshakeMsg((byte) 0, (byte) 0, (byte) 0);
					mHandler.sendMessage(m);
				}
			}
			
			if (!handshakeOk) {
				return;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;

				switch (buffer[i]) {

				case MESSAGE_CONFIGURE:
					if (len >= 6) {
						Message m = Message.obtain(mHandler, MESSAGE_CONFIGURE);
						textLength = buffer[i + CONFIG_OFFSET_TEXT_LENGTH];						
						m.obj = new ConfigMsg(buffer[i + CONFIG_OFFSET_WIDGET_TYPE],
								buffer[i + CONFIG_OFFSET_WIDGET_ID],
								composeInt((byte) 0x00, buffer[i + CONFIG_OFFSET_FONT_SIZE]), // To deal with fact bytes are signed in Java. :/
								buffer[i + CONFIG_OFFSET_WIDGET_ALIGNMENT],
								Arrays.copyOfRange(buffer,
										i + CONFIG_OFFSET_TEXT_START,
										i + CONFIG_OFFSET_TEXT_START + textLength));
						mHandler.sendMessage(m);
					}
					i += (6 + textLength); // NOTE: This needs to change when more items are added.
					break;					
					
				default:
					Log.d(TAG, "unknown msg: " + buffer[i]);
					i = len;
					break;
				}
			}

		}
	}

	Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MESSAGE_CONFIGURE:
				ConfigMsg c = (ConfigMsg) msg.obj;
				handleConfigMessage(c);
				break;
				
			case MESSAGE_HANDSHAKE:
				HandshakeMsg h = (HandshakeMsg) msg.obj;
				handleHandshakeMessage(h);
				break;
			}
		}
	};

	public void sendCommand(byte command, byte target, int value) {
		byte[] buffer = new byte[3];
		if (value > 255)
			value = 255;

		buffer[0] = command;
		buffer[1] = target;
		buffer[2] = (byte) value;
		if (mOutputStream != null && buffer[1] != -1) {
			try {
				mOutputStream.write(buffer);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	public void sendString(String theString) {
		// TODO: Implement all this better.
		byte[] buffer = theString.getBytes();
		
		final byte MAX_STRING_LENGTH = 20;

		byte numBytesToWrite = (byte) Math.min(buffer.length, MAX_STRING_LENGTH);
		
		// Hacky way of sending the length without altering anything else.
		sendCommand((byte) 0xff, (byte) 0x00, (int) numBytesToWrite);
		
		if (mOutputStream != null) {
			try {
				mOutputStream.write(buffer, 0, numBytesToWrite);
			} catch (IOException e) {
				Log.e(TAG, "write failed", e);
			}
		}
	}

	protected void handleConfigMessage(ConfigMsg c) {
	}

	protected void handleHandshakeMessage(HandshakeMsg c) {
	}
}

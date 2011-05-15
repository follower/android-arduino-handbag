package com.example.TinyAccessory;

import android.view.ViewGroup;

public class ServoController implements Slider.SliderPositionListener {
	private final byte mCommandTarget;
	private Slider mSlider;
	private TinyAccessoryActivity mActivity;

	public ServoController(TinyAccessoryActivity activity, int servoNumber) {
		mActivity = activity;
		mCommandTarget = (byte) (servoNumber - 1 + 0x10);
	}

	public void attachToView(ViewGroup targetView) {
		mSlider = (Slider) targetView.getChildAt(1);
		mSlider.setPositionListener(this);
	}

	public void onPositionChange(double value) {
		byte v = (byte) (value * 255);
		mActivity.sendCommand(TinyAccessoryActivity.LED_SERVO_COMMAND,
				mCommandTarget, v);
	}

}

package com.example.TinyAccessory;

import android.view.ViewGroup;

public class PwmOutController implements Slider.SliderPositionListener {
	private final byte mCommandTarget;
	private Slider mSlider;
	private TinyAccessoryActivity mActivity;

	public PwmOutController(TinyAccessoryActivity activity, int pwmoutNumber) {
		mActivity = activity;
		mCommandTarget = (byte) pwmoutNumber;
	}

	public void attachToView(ViewGroup targetView) {
		mSlider = (Slider) targetView.getChildAt(1);
		mSlider.setPositionListener(this);
	}

	public void onPositionChange(double value) {
		byte v = (byte) (value * 255);
		mActivity.sendCommand(TinyAccessoryActivity.PWM_OUT_COMMAND,
				mCommandTarget, v);
	}

}

package com.rancidbacon.Handbag;

import android.view.ViewGroup;

public class PwmOutController implements Slider.SliderPositionListener {
	private final byte mCommandTarget;
	private Slider mSlider;
	private HandbagActivity mActivity;

	public PwmOutController(HandbagActivity activity, int pwmoutNumber) {
		mActivity = activity;
		mCommandTarget = (byte) pwmoutNumber;
	}

	public void attachToView(ViewGroup targetView) {
		mSlider = (Slider) targetView.getChildAt(1);
		mSlider.setPositionListener(this);
	}

	public void onPositionChange(double value) {
		byte v = (byte) (value * 255);
		mActivity.sendCommand(HandbagActivity.PWM_OUT_COMMAND,
				mCommandTarget, v);
	}

}

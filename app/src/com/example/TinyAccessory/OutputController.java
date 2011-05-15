package com.example.TinyAccessory;

import com.example.TinyAccessory.R;

import android.view.ViewGroup;

public class OutputController extends AccessoryController {

	private boolean mVertical;

	OutputController(TinyAccessoryActivity hostActivity, boolean vertical) {
		super(hostActivity);
		mVertical = vertical;
	}

	protected void onAccesssoryAttached() {
		setupPwmOutController(0, R.id.pwmout1);

		setupDigitalOutController(1, R.id.digitalout1);
	}

	private void setupPwmOutController(int pwmoutIndex, int viewId) {
		PwmOutController sc = new PwmOutController(mHostActivity, pwmoutIndex);
		sc.attachToView((ViewGroup) findViewById(viewId));
	}

	private void setupDigitalOutController(int index, int viewId) {
		DigitalOutController r = new DigitalOutController(mHostActivity, index,
				getResources());
		r.attachToView((ViewGroup) findViewById(viewId));
	}

}

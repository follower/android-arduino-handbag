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

		setupRelayController(1, R.id.relay1);
	}

	private void setupPwmOutController(int pwmoutIndex, int viewId) {
		PwmOutController sc = new PwmOutController(mHostActivity, pwmoutIndex);
		sc.attachToView((ViewGroup) findViewById(viewId));
	}

	private void setupRelayController(int index, int viewId) {
		RelayController r = new RelayController(mHostActivity, index,
				getResources());
		r.attachToView((ViewGroup) findViewById(viewId));
	}

}

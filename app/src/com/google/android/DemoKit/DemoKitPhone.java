package com.google.android.DemoKit;

public class DemoKitPhone extends BaseActivity {
	static final String TAG = "DemoKitPhone";
	/** Called when the activity is first created. */
	OutputController mOutputController;

	@Override
	protected void hideControls() {
		super.hideControls();
		mOutputController = null;
	}

	protected void showControls() {
		super.showControls();

		mOutputController = new OutputController(this, false);
		mOutputController.accessoryAttached();
	}

}
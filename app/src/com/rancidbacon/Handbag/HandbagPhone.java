package com.rancidbacon.Handbag;

public class HandbagPhone extends BaseActivity {
	static final String TAG = "HandbagPhone";
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
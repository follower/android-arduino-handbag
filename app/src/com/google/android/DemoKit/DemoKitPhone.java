package com.google.android.DemoKit;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DemoKitPhone extends BaseActivity {
	static final String TAG = "DemoKitPhone";
	/** Called when the activity is first created. */
	LinearLayout mInputContainer;
	LinearLayout mOutputContainer;
	Drawable mFocusedTabImage;
	Drawable mNormalTabImage;
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
		mInputContainer = (LinearLayout) findViewById(R.id.inputContainer);

		mInputContainer.setVisibility(View.VISIBLE);		
	}

}
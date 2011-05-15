package com.example.TinyAccessory;

import com.example.TinyAccessory.R;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ToggleButton;

public class RelayController implements OnCheckedChangeListener {
	private final byte mCommandTarget;
	private TinyAccessoryActivity mActivity;
	private ToggleButton mButton;
	private Drawable mOffBackground;
	private Drawable mOnBackground;

	public RelayController(TinyAccessoryActivity activity, int relayNumber,
			Resources res) {
		mActivity = activity;
		mCommandTarget = (byte) (relayNumber - 1);
		mOffBackground = res
				.getDrawable(R.drawable.toggle_button_off_holo_dark);
		mOnBackground = res.getDrawable(R.drawable.toggle_button_on_holo_dark);
	}

	public void attachToView(ViewGroup targetView) {
		mButton = (ToggleButton) targetView.getChildAt(1);
		mButton.setOnCheckedChangeListener(this);
	}

	public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
		if (isChecked) {
			mButton.setBackgroundDrawable(mOnBackground);
		} else {
			mButton.setBackgroundDrawable(mOffBackground);
		}
		if (mActivity != null) {
			mActivity.sendCommand(TinyAccessoryActivity.RELAY_COMMAND,
					mCommandTarget, isChecked ? 1 : 0);
		}
	}

}

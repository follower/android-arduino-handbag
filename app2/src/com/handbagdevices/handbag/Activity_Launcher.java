/*
    This activity serves as a common starting screen for the different types
    of communication methods Handbag can use.

    It is also part of the code required for the USB launch-on-connect functionality
    to work properly.

 */

package com.handbagdevices.handbag;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


// TODO: Replace this with a tabbed interface?

public class Activity_Launcher extends Activity {

    private boolean justReturnedFromMainDisplay = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(this.getClass().getSimpleName(), "onCreate()");

        setContentView(R.layout.ui_launcher); // TODO: Should only do this when not being launched by USB connection?
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(this.getClass().getSimpleName(), "onActivityResult()");

        // Note: We do this so we don't immediately try to reconnect.
        // TODO: Do we still need to do this with the current approach?
        justReturnedFromMainDisplay = true;
    }


    @TargetApi(12)
    private boolean usbAccessoryIsConnected() {
        // Note: I don't really want this USB-specific function in this launcher
        //       but it seems to be the easiest way to make USB re-connections work.

        boolean result = false;

        try {
            // Note: In theory the check for `android.hardware.usb.UsbManager` should
            //       fail on Gingerbread but as of "recent" Android Eclipse builds
            //       it seems like somehow `android.hardware.usb.UsbManager` is now used
            //       and found on Gingerbread too.
            //       Note however that the IDE still thinks that Gingerbread is required
            //       thus the use of the `@TargetAPi(12)` above.
            Class.forName("android.hardware.usb.UsbManager");

            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            UsbAccessory[] accessoryList = manager.getAccessoryList();

            // Note: The permissions check here is because sometimes we end up
            //       with spurious accessories (e.g. ^B in the device description etc)
            //       and presumably it also stops problems if an accessory is connected
            //       when we're open.
            result = ((accessoryList != null) && (manager.hasPermission(accessoryList[0])));
        } catch (ClassNotFoundException e) {
            Log.d(this.getClass().getSimpleName(),
                    "No USB accessory support, 'android.hardware.usb.UsbManager' not found.");
        }

        return result;
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.d(this.getClass().getSimpleName(), "onResume()");

        if (justReturnedFromMainDisplay) {

            // This is to avoid re-launching USB screen when the back button is used.
            Log.d(this.getClass().getSimpleName(), "justReturnedFromMainDisplay");
            justReturnedFromMainDisplay = false;

        } else {

            if (usbAccessoryIsConnected()) {
                Log.d(this.getClass().getSimpleName(), "Detected attached accessory.");
                startActivityForResult(new Intent(Activity_Launcher.this, Activity_SetupUsb.class), 0);
            }

        }

    }


    public void onClick_buttonNetworkConnection(View theView) {

        startActivity(new Intent(Activity_Launcher.this, Activity_SetupNetwork.class));

    }

}

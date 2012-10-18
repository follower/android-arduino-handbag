package com.handbagdevices.handbag;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Activity_LaunchFromUsb extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        startActivity(new Intent(Activity_LaunchFromUsb.this, Activity_Launcher.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
    }

}

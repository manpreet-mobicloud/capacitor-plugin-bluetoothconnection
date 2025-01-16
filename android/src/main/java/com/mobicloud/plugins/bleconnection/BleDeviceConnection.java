package com.mobicloud.plugins.bleconnection;

import android.util.Log;

public class BleDeviceConnection {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}

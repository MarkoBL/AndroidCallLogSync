package com.markobl.callhistory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutostartReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
        String action = intent.getAction();
        if(action.contains("BOOT")) {
            SyncWorker.start(context.getApplicationContext());
        }
    }
}
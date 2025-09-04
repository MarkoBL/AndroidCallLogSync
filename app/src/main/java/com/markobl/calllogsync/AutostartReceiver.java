package com.markobl.calllogsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutostartReceiver extends BroadcastReceiver
{
    public void onReceive(Context context, Intent intent)
    {
        SyncWorker.registerWorker(context);
    }
}
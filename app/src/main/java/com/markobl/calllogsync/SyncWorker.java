package com.markobl.calllogsync;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    public static final String BROADCAST_SYNC_DONE = "com.markobl.calllogsync.sync-done";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void registerWorker(Context context) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest work =new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES).setConstraints(constraints).build();

        WorkManager.getInstance(context).enqueueUniquePeriodicWork("sync", ExistingPeriodicWorkPolicy.KEEP, work);
    }

    public  static  void syncNow(Context context)
    {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(
                SyncWorker.class).setConstraints(constraints).build());
    }

    public Worker.Result doWork() {

        final Context context = getApplicationContext();
        if(!Sync.isEnabled(context))
            return Worker.Result.success();

        final Config config = Config.load(context);
        final long lastId = Config.getLastCallLogId(context);

        Sync.syncCallHistory(context, config, lastId, (syncResult -> {

            final LogItem logItem = new LogItem(context, syncResult);
            LogItem.addLogItem(context, logItem);

            Intent intent = new Intent(BROADCAST_SYNC_DONE);
            LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
            bm.sendBroadcast(intent);

            if(syncResult.syncResultType == SyncResultType.SUCCESS)
            {
                Config.setLastCallLogId(context, syncResult.lastCallLogId);

                if(syncResult.more)
                    doWork();
            }
        }));

        return ListenableWorker.Result.success();
    }
}
package com.markobl.calllogsync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ListenableWorker;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void start(Context context) {

        WorkManager.getInstance(context).enqueueUniquePeriodicWork( "com.markobl.syncworker", ExistingPeriodicWorkPolicy.KEEP, new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES
        ).build());

        WorkManager.getInstance(context).enqueue(new OneTimeWorkRequest.Builder(
                SyncWorker.class).build());
    }

    public Worker.Result doWork() {

        final Context context = getApplicationContext();

        if(!Sync.isEnabled(context))
            return Worker.Result.success();

        final Config config = Config.load(context);
        final long lastId = Config.getLastCallLogId(context);

        Sync.syncCallHistory(context, config, lastId, (syncResult -> {

            if(syncResult.syncResultType == SyncResultType.NOTHING_TO_SYNC)
                return;

            final LogItem logItem = new LogItem(context, syncResult);
            LogItem.addLogItem(context, logItem);

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
package com.markobl.callhistory;

import android.content.Context;
import android.content.res.Resources;

public class SyncResult {

    public final SyncResultType syncResultType;

    public final Long lastCallLogId;
    public final Boolean more;
    public final int count;

    public final int responseCode;
    public final String responseMessage;

    public final Exception exception;

    public SyncResult(Exception ex)
    {
        syncResultType = SyncResultType.EXCEPTION;
        exception = ex;
        responseMessage = "" + ex;
        responseCode = 0;
        lastCallLogId = -1L;
        more = false;
        count = 0;
    }

    public SyncResult(String responseMessage, long lastId, Boolean more, int count)
    {
        syncResultType = SyncResultType.SUCCESS;
        this.responseMessage = responseMessage;
        responseCode = 200;
        this.lastCallLogId = lastId;
        this.more = more;
        this.count = count;
        exception = null;
    }

    public SyncResult(String responseMessage, int responseCode)
    {
        syncResultType = SyncResultType.FAILURE;
        this.responseMessage = responseMessage;
        this.responseCode = responseCode;
        exception = null;
        lastCallLogId = -1L;
        more = false;
        count = 0;
    }

    public SyncResult()
    {
        syncResultType = SyncResultType.NOTHING_TO_SYNC;
        responseMessage = null;
        responseCode = 0;
        exception = null;
        lastCallLogId = -1L;
        more = false;
        count = 0;
    }

    public String getLogMessage(Context context)
    {
        Resources resources = context.getResources();
        if(syncResultType == SyncResultType.SUCCESS)
            return resources.getString(R.string.sync_success, count);
        else if (syncResultType == SyncResultType.NOTHING_TO_SYNC)
            return resources.getString(R.string.sync_nothing);
        else if (syncResultType == SyncResultType.FAILURE)
            return resources.getString(R.string.sync_failure, responseMessage, responseCode);
        else if (syncResultType == SyncResultType.EXCEPTION)
            return resources.getString(R.string.sync_exception, responseMessage);
        return "";
    }
}

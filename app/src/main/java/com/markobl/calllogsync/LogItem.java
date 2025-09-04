package com.markobl.calllogsync;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Semaphore;

public class LogItem {
    public Date date;
    public SyncResultType type;
    public String message;

    private static Semaphore lock = new Semaphore(1);

    public LogItem()
    {

    }

    public LogItem(Context context, SyncResult syncResult)
    {
        date = Calendar.getInstance().getTime();
        type = syncResult.syncResultType;
        message = syncResult.getLogMessage(context);
    }

    public static  void reset(Context context)
    {
        try
        {
            lock.acquire();

            SharedPreferences settings = Config.getSharedPreferences(context);
            SharedPreferences.Editor edit = settings.edit();
            edit.putString("log", "[]");
            edit.apply();
        }
        catch (Exception ex)
        {
            Log.e("LOG", "" + ex);
        }
        finally {
            lock.release();
        }
    }

    public static LogItem getLastLogItem(Context context)
    {
        SharedPreferences settings = Config.getSharedPreferences(context);
        String json = settings.getString("lastlog", null);
        if(json == null)
            return  null;

        Gson gson = new Gson();
        return gson.fromJson(json, LogItem.class);
    }

    public static void addLogItem(Context context, LogItem logItem)
    {
        try
        {
            lock.acquire();

            ArrayList<LogItem> logItems = getLogItemsInternal(context);
            logItems.add(0, logItem);

            while (logItems.size() > 100)
                logItems.remove(logItems.size() - 1);

            Gson gson = new Gson();
            SharedPreferences settings = Config.getSharedPreferences(context);
            SharedPreferences.Editor edit = settings.edit();
            edit.putString("log", gson.toJson(logItems));
            edit.putString("lastlog", gson.toJson(logItem));
            edit.apply();
        }
        catch (Exception ex) {
            Log.e("LOG", "" + ex);
        }
        finally {
            lock.release();
        }
    }

    public static ArrayList<LogItem> getLogItems(Context context)
    {
        try
        {
            lock.acquire();
            return getLogItemsInternal(context);
        }
        catch (Exception ex) {
            Log.e("LOG", "" + ex);
        }
        finally {
            lock.release();
        }
        return new ArrayList<>();
    }

    private static ArrayList<LogItem> getLogItemsInternal(Context context)
    {
        try
        {
            Gson gson = new Gson();
            SharedPreferences settings = Config.getSharedPreferences(context);
            String json = settings.getString("log", "[]");
            return gson.fromJson(json, new TypeToken<ArrayList<LogItem>>() { }.getType());
        }
        catch (Exception ex) {
            Log.e("LOG", "" + ex);
        }
        return new ArrayList<>();
    }
}

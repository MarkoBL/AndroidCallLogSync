package com.markobl.calllogsync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class Config {

    public URL endpoint;

    public String deviceName;
    public String deviceToken;

    public Map<String, String> additionalHeaders = new HashMap<>();

    public static Config newConfig()
    {
        Config config = new Config();
        config.deviceName = Build.MODEL;
        config.deviceToken = RandomString.getRandomString(32);
        return  config;
    }

    public static SharedPreferences getSharedPreferences(Context context)
    {
        return context.getSharedPreferences("callhistory", Context.MODE_PRIVATE);
    }

    public static void reset(Context context)
    {
        Config config = Config.newConfig();
        config.save(context);
        Config.setLastCallLogId(context, 0);
    }

    public static Config load(@NonNull Context context)
    {
        try {
            SharedPreferences settings = getSharedPreferences(context);
            if (settings.contains("config")) {
                String configJson = settings.getString("config", "{}");
                Gson gson = new Gson();
                Config config = gson.fromJson(configJson, Config.class);

                if (config.deviceName == null || config.deviceName.trim().isEmpty())
                    config.deviceName = Build.MODEL;
                if(config.deviceToken == null || config.deviceToken.trim().isEmpty())
                    config.deviceToken = RandomString.getRandomString(32);

                if(config.additionalHeaders == null)
                    config.additionalHeaders = new HashMap<>();

                return config;
            }
        }
        catch (Exception ex) {
            Log.e("CONFIG", "" + ex);
        }

        return newConfig();
    }

    public boolean save(@NonNull Context context)
    {
        try {
            Gson gson = new Gson();
            String json = gson.toJson(this);

            SharedPreferences settings = getSharedPreferences(context);
            SharedPreferences.Editor editor = settings.edit();

            editor.putString("config", json);

            editor.apply();
            return  true;
        }
        catch (Exception ex)
        {
            Log.e("CONFIG", "" + ex);
        }
        return  false;
    }

    public static long getLastCallLogId(Context context)
    {
        SharedPreferences settings = getSharedPreferences(context);
        return  settings.getLong("lastcalllogid", 0);
    }

    public static void setLastCallLogId(Context context, long lastCallLogId)
    {
        SharedPreferences settings = getSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();

        editor.putLong("lastcalllogid", lastCallLogId);
        editor.apply();
    }
}

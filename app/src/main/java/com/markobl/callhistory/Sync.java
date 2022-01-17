package com.markobl.callhistory;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Handler;
import android.provider.CallLog;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class Sync {

    public static boolean isEnabled(@NonNull final Context context) {
        SharedPreferences settings = Config.getSharedPreferences(context);
        return settings.getBoolean("syncenabled", false);
    }

    public static void setEnabled(@NonNull final Context context, boolean enabled) {
        SharedPreferences settings = Config.getSharedPreferences(context);
        SharedPreferences.Editor edit = settings.edit();
        edit.putBoolean("syncenabled", enabled);
        edit.apply();
    }

    public static void syncCallHistory(@NonNull final Context context, @NonNull final Config config, final long lastCallLogId, @NonNull final SyncResultRunner syncResultRunner) {
        try {

            long lastId = lastCallLogId;

            final Cursor managedCursor = context.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, "_id > " + lastId, null, null);
            final int idIndex = managedCursor.getColumnIndex("_id");
            final int numberIndex = managedCursor.getColumnIndex(CallLog.Calls.NUMBER);
            final int typeIndex = managedCursor.getColumnIndex(CallLog.Calls.TYPE);
            final int dateIndex = managedCursor.getColumnIndex(CallLog.Calls.DATE);
            final int durationIndex = managedCursor.getColumnIndex(CallLog.Calls.DURATION);

            final JSONArray json = new JSONArray();

            int count = 0;
            while (managedCursor.moveToNext()) {


                final long id = managedCursor.getLong(idIndex);
                lastId = Math.max(lastId, id);

                final String number = managedCursor.getString(numberIndex);
                final StringBuilder numberBuilder = new StringBuilder();
                for(int i = 0; i < number.length(); i++)
                {
                    char c = number.charAt(i);
                    if(c == '+')
                    {
                        numberBuilder.append("00");
                    }
                    else if ((c >= '0' && c <= '9') || c == '#' || c == '*')
                    {
                        numberBuilder.append(c);
                    }
                }

                final int type = managedCursor.getInt(typeIndex);
                final long date = managedCursor.getLong(dateIndex);
                final long duration = managedCursor.getLong(durationIndex);
                final String finalNumber = numberBuilder.toString();
                if(finalNumber.length() == 0)
                    continue;

                count++;
                try {
                    JSONObject item = new JSONObject();
                    item.put("ID", id);
                    item.put("NUMBER", finalNumber);
                    item.put("TYPE", type);
                    item.put("DATE", date);
                    item.put("DURATION", duration);

                    json.put(item);
                } catch (JSONException ex) {
                    Log.e("SYNC", "" + ex);
                }

                if(count >= 100)
                    break;
            }

            final Boolean more = managedCursor.moveToNext();
            managedCursor.close();

            if (count == 0)
            {
                syncResultRunner.run(new SyncResult());
                return;
            }

            final long LastId = lastId;
            final int Count = count;

            new Thread(() -> {

                Handler mainHandler = new Handler(context.getMainLooper());

                try {
                    byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);
                    URL url = config.endpoint;

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    connection.setRequestProperty("Content-Length", Long.toString(data.length));
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setRequestProperty("Device-Name", config.deviceName);
                    connection.setRequestProperty("Device-Token", config.deviceToken);
                    for(Map.Entry<String, String> entry : config.additionalHeaders.entrySet())
                        connection.setRequestProperty(entry.getKey(), entry.getValue());

                    connection.setInstanceFollowRedirects(true);
                    connection.setDoOutput(true);
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                    connection.connect();

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(data);
                    outputStream.flush();

                    int responseCode = connection.getResponseCode();

                    InputStream responseStream;
                    if (responseCode >= 100 && responseCode <= 399)
                        responseStream = connection.getInputStream();
                    else
                        responseStream = connection.getErrorStream();

                    BufferedReader responseReader = new BufferedReader(new InputStreamReader(responseStream));

                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = responseReader.readLine()) != null) {
                        response.append(responseLine);
                    }

                    if (responseCode == 200) {
                        mainHandler.post(() -> syncResultRunner.run(new SyncResult(response.toString(), LastId, more, Count)));
                        //syncResultRunner.run(new SyncResult(response.toString(), LastId, more, Count));
                    } else {
                        mainHandler.post(() -> syncResultRunner.run(new SyncResult(response.toString(), responseCode)));
                        //syncResultRunner.run(new SyncResult(response.toString(), responseCode));
                    }
                } catch (Exception ex) {
                    Log.e("SYNC", "" + ex);
                    mainHandler.post(() -> syncResultRunner.run(new SyncResult(ex)));
                    //syncResultRunner.run(new SyncResult(ex));
                }
            }).start();
        }
        catch (Exception ex)
        {
            Log.e("SYNC", "" + ex);
            syncResultRunner.run(new SyncResult(ex));
        }
    }
}

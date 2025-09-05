package com.markobl.calllogsync;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.IntentCompat;
import androidx.core.content.PackageManagerCompat;
import androidx.core.content.UnusedAppRestrictionsConstants;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.common.util.concurrent.ListenableFuture;

import java.net.URL;

public class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        Config config = Config.load(getActivity());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (config.endpoint != null)
            editor.putString("endpoint", config.endpoint.toString());
        else
            editor.putString("endpoint", null);

        editor.putBoolean("sync", Sync.isEnabled(getActivity()));
        editor.putString("name", config.deviceName);
        editor.putString("token", config.deviceToken);
        editor.putString("number", config.deviceNumber);
        editor.apply();

        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        findPreference("token").setSummary(config.deviceToken);
        findPreference("last_id").setSummary(Long.toString(Config.getLastCallLogId(getActivity())));

        findPreference("endpoint").setOnPreferenceChangeListener(this);
        findPreference("sync").setOnPreferenceChangeListener(this);
        findPreference("name").setOnPreferenceChangeListener(this);
        findPreference("number").setOnPreferenceChangeListener(this);

        findPreference("test").setOnPreferenceClickListener(this);
        findPreference("token").setOnPreferenceClickListener(this);
        findPreference("reset").setOnPreferenceClickListener(this);
        findPreference("setup").setOnPreferenceClickListener(this);
        findPreference("lock").setOnPreferenceClickListener(this);


        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            String toast = getString(R.string.call_log_required);
            Toast.makeText(getActivity(), toast, Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_CALL_LOG}, 0);
        }

        Activity activity = getActivity();
        PowerManager powerManager = (PowerManager) activity.getSystemService(Activity.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(activity.getPackageName())) {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                alertDialog.setTitle(R.string.app_name);
                alertDialog.setMessage(R.string.batteryopt);
                alertDialog.setNegativeButton(R.string.no, null);
                alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    }
                });

                alertDialog.create().show();
            }
        }

        ListenableFuture<Integer> future = PackageManagerCompat.getUnusedAppRestrictionsStatus(activity);
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    int status = future.get();
                    if(status == UnusedAppRestrictionsConstants.API_30 || status == UnusedAppRestrictionsConstants.API_30_BACKPORT || status == UnusedAppRestrictionsConstants.API_31)
                    {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                        alertDialog.setTitle(R.string.app_name);
                        alertDialog.setMessage(R.string.restrictopt);
                        alertDialog.setNegativeButton(R.string.no, null);
                        alertDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent intent = IntentCompat.createManageUnusedAppRestrictionsIntent(getContext(), getActivity().getPackageName());
                                startActivityForResult(intent, 0);
                            }
                        });

                        alertDialog.create().show();
                    }
                }
                catch (Exception ex)
                {

                }
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();

        Config config = Config.load(getActivity());
        if(config.deviceLocked)
        {
            if(!key.equals("sync"))
            {
                Toast.makeText(getContext(), getResources().getString(R.string.device_lock_info), Toast.LENGTH_SHORT).show();
                return  false;
            }
        }

        if(key.equals("endpoint"))
        {
            try {
                final URL url = new URL((String)newValue);
                String protocol = url.getProtocol();

                if(protocol.equals("http") || protocol.equals("https")) {

                    Toast.makeText(getContext(), getResources().getString(R.string.testing_endpoint), Toast.LENGTH_SHORT).show();

                    config.endpoint = url;
                    config.additionalHeaders.put("Test-Run", "1");

                    Sync.syncCallHistory(getActivity(), config, 0, syncResult -> {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                        if(syncResult.syncResultType == SyncResultType.SUCCESS)
                        {
                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("endpoint", url.toString());
                            editor.apply();
                            ((EditTextPreference)findPreference("endpoint")).setText(url.toString());

                            alertDialog.setTitle(R.string.sync_alert_success);
                            Config newConfig = Config.load(getActivity());
                            newConfig.endpoint = url;
                            newConfig.save(getActivity());
                        }
                        else
                        {
                            alertDialog.setTitle(R.string.sync_alert_failed);
                        }
                        alertDialog.setMessage(syncResult.responseMessage);
                        alertDialog.setPositiveButton(R.string.ok, null);
                        alertDialog.create().show();
                    });
                    return  false;
                }
            }
            catch (Exception ex)
            {
                Log.e("SETTINGS", "" + ex);
            }

            Toast.makeText(getContext(), getResources().getString(R.string.invalid_endpoint), Toast.LENGTH_SHORT).show();
        }
        else if (key.equals("sync"))
        {
            if(config.endpoint == null)
            {
                Toast.makeText(getContext(), getResources().getString(R.string.no_endpoint), Toast.LENGTH_SHORT).show();
                return false;
            }

            boolean enabled = (boolean)newValue;
            Sync.setEnabled(getActivity(), enabled);

            if(enabled)
            {
                SyncWorker.syncNow(getActivity());
            }

            return true;
        }
        else if (key.equals("name"))
        {
            config.deviceName = (String)newValue;
            config.save(getActivity());
            return true;
        }
        else if (key.equals("number"))
        {
            config.deviceNumber = (String)newValue;
            config.save(getActivity());
            return  true;
        }

        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();

        Config config = Config.load(getActivity());
        if(config.deviceLocked)
        {
            if(!(key.equals("reset") || key.equals("test") || key.equals("sync") || key.equals("setup") || key.equals("token")))
            {
                Toast.makeText(getContext(), getResources().getString(R.string.device_lock_info), Toast.LENGTH_SHORT).show();
                return  false;
            }
        }


        if(key.equals("test"))
        {
            if(config.endpoint == null)
            {
                Toast.makeText(getContext(), getResources().getString(R.string.no_endpoint), Toast.LENGTH_SHORT).show();
                return false;
            }

            Toast.makeText(getContext(), getResources().getString(R.string.testing_endpoint), Toast.LENGTH_SHORT).show();

            config.additionalHeaders.put("Test-Run", "1");
            Sync.syncCallHistory(getActivity(), config, 0, syncResult -> {
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                if(syncResult.syncResultType == SyncResultType.SUCCESS)
                {
                    alertDialog.setTitle(R.string.sync_alert_success);
                }
                else
                {
                    alertDialog.setTitle(R.string.sync_alert_failed);
                }
                alertDialog.setMessage(syncResult.responseMessage);
                alertDialog.setPositiveButton(R.string.ok, null);
                alertDialog.create().show();
            });
        }
        else if (key.equals("token"))
        {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Device Token", config.deviceToken);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(getContext(), getResources().getString(R.string.token_copied), Toast.LENGTH_SHORT).show();
        }
        else if (key.equals("reset"))
        {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
            alertDialog.setTitle(R.string.app_name);
            alertDialog.setMessage(R.string.reset_info);
            alertDialog.setPositiveButton(R.string.ok, (dialogInterface, i) -> {
                AlertDialog.Builder resetDialog = new AlertDialog.Builder(getActivity());
                resetDialog.setTitle(R.string.app_name);
                resetDialog.setMessage(R.string.reset_info2);
                resetDialog.setNegativeButton(R.string.no, null);

                resetDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Sync.setEnabled(getActivity(), false);
                        Config.reset(getActivity());
                        LogItem.reset(getActivity());

                        getActivity().onBackPressed();
                    }
                });

                resetDialog.create().show();

            });
            alertDialog.create().show();
        }
        else if (key.equals("lock"))
        {
            AlertDialog.Builder resetDialog = new AlertDialog.Builder(getActivity());
            resetDialog.setTitle(R.string.app_name);
            resetDialog.setMessage(R.string.device_lock_info);
            resetDialog.setNegativeButton(R.string.no, null);

            resetDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    Config newConfig = Config.load(getActivity());
                    newConfig.deviceLocked = true;
                    newConfig.save(getActivity());
                }
            });

            resetDialog.create().show();
        }
        else if (key.equals("setup"))
        {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/MarkoBL/AndroidCallLogSync"));
            startActivity(browserIntent);
        }
        return true;
    }
}
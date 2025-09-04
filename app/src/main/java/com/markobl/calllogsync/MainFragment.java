package com.markobl.calllogsync;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.markobl.calllogsync.databinding.FragmentMainBinding;

import java.text.DateFormat;

public class MainFragment extends Fragment implements View.OnClickListener {

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(SyncWorker.BROADCAST_SYNC_DONE)) {
                //final String param = intent.getStringExtra(EXTRA_PARAM_B);
                // do something
                updateView();
            }
        }
    };

    private FragmentMainBinding binding;
    private DateFormat dateFormat;
    private DateFormat timeFormat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
        timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());

        binding = FragmentMainBinding.inflate(inflater, container, false);

        binding.syncNow.setOnClickListener(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(SyncWorker.BROADCAST_SYNC_DONE);
        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.registerReceiver(mBroadcastReceiver, filter);

        return binding.getRoot();
    }


    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.sync_now)
        {
            SyncWorker.syncNow(getContext());
        }
    }

    void updateView()
    {
        if(Sync.isEnabled(getActivity()))
        {
            binding.syncInfo.setVisibility(View.GONE);
            binding.syncNow.setVisibility(View.VISIBLE);
            LogItem lastItem = LogItem.getLastLogItem(getActivity());
            if(lastItem != null)
            {
                binding.syncLast.setText(getResources().getString(R.string.last_sync, dateFormat.format(lastItem.date), timeFormat.format(lastItem.date) ));
                binding.syncLast.setVisibility(View.VISIBLE);
            }
            else
            {
                binding.syncLast.setVisibility(View.GONE);
            }
        }
        else
        {
            binding.syncActive.setVisibility(View.GONE);
            binding.syncInfo.setVisibility(View.VISIBLE);
            binding.syncLast.setVisibility(View.GONE);
            binding.syncNow.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        updateView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getContext());
        bm.unregisterReceiver(mBroadcastReceiver);

        binding = null;
    }
}
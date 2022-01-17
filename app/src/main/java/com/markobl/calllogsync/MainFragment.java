package com.markobl.calllogsync;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.markobl.calllogsync.databinding.FragmentMainBinding;

import java.text.DateFormat;

public class MainFragment extends Fragment {

    private FragmentMainBinding binding;
    private DateFormat dateFormat;
    private DateFormat timeFormat;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        dateFormat = android.text.format.DateFormat.getDateFormat(getActivity());
        timeFormat = android.text.format.DateFormat.getTimeFormat(getActivity());

        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onStart() {
        super.onStart();

        if(Sync.isEnabled(getActivity()))
        {
            binding.syncInfo.setVisibility(View.GONE);
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
        }
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
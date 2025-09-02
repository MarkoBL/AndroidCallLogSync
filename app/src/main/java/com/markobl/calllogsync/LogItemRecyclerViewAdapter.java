package com.markobl.calllogsync;

import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.markobl.calllogsync.databinding.FragmentLogBinding;

import java.text.DateFormat;
import java.util.List;

public class LogItemRecyclerViewAdapter extends RecyclerView.Adapter<LogItemRecyclerViewAdapter.ViewHolder> {

    private final List<LogItem> mValues;
    private final DateFormat dateFormat;
    private final DateFormat timeFormat;

    public LogItemRecyclerViewAdapter(Context context, List<LogItem> items) {

        mValues = items;
        dateFormat = android.text.format.DateFormat.getDateFormat(context);
        timeFormat = android.text.format.DateFormat.getTimeFormat(context);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ViewHolder(FragmentLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.mItem = mValues.get(position);

        StringBuilder date = new StringBuilder();
        date.append(dateFormat.format(holder.mItem.date));
        date.append('\n');
        date.append(timeFormat.format(holder.mItem.date));

        holder.mIdView.setText(date);
        holder.mContentView.setText(mValues.get(position).message);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mIdView;
        public final TextView mContentView;
        public LogItem mItem;

        public ViewHolder(FragmentLogBinding binding) {
            super(binding.getRoot());
            mIdView = binding.itemNumber;
            mContentView = binding.content;
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
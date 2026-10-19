package com.hape.payrus.ui.adapter;

import android.content.Context;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hape.payrus.R;
import com.hape.payrus.data.NoteItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(NoteItem item);
        void onItemLongClick(NoteItem item, View anchorView);
    }

    private final Context context;
    private final List<NoteItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public NoteAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setItems(List<NoteItem> newItems) {
        this.items.clear();
        if (newItems != null) {
            this.items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoteItem item = items.get(position);
        if (item.isDirectory()) {
            holder.textTitle.setText(item.getName().endsWith("/") ? item.getName() : item.getName() + "/");
        } else {
            holder.textTitle.setText(item.getName());
        }

        holder.textDate.setText(formatDate(item.getLastModified()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onItemLongClick(item, v);
            }
            return true;
        });
    }

    private String formatDate(long timeMillis) {
        if (timeMillis <= 0) return "";
        String timeStr = DateFormat.getTimeFormat(context).format(timeMillis);
        if (DateUtils.isToday(timeMillis)) {
            return context.getString(R.string.pe_today) + " " + timeStr;
        }
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -1);
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timeMillis);
        if (cal.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)) {
            return context.getString(R.string.pe_yesterday) + " " + timeStr;
        }
        return new SimpleDateFormat("d MMMM yyyy HH:mm", Locale.getDefault()).format(new Date(timeMillis));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;
        TextView textDate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDate = itemView.findViewById(R.id.textDate);
        }
    }
}

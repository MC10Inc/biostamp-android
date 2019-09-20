package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecordingAdapter extends RecyclerView.Adapter<RecordingAdapter.RecordingViewHolder> {
    public static class RecordingItem {
        private RecordingInfo recordingInfo;

        public RecordingItem(RecordingInfo recordingInfo) {
            this.recordingInfo = recordingInfo;
        }

        public RecordingInfo getRecordingInfo() {
            return recordingInfo;
        }
    }

    private List<RecordingItem> items = Collections.emptyList();
    private int selection = RecyclerView.NO_POSITION;

    static class RecordingViewHolder extends RecyclerView.ViewHolder {
        interface SelectListener {
            void selected(int position);
        }

        View view;

        @BindView(R.id.textView)
        TextView textView;

        RecordingViewHolder(View view, SelectListener listener) {
            super(view);
            this.view = view;
            ButterKnife.bind(this, view);

            View.OnClickListener clickListener = v -> listener.selected(getAdapterPosition());
            view.setOnClickListener(clickListener);
        }
    }

    @NonNull
    @Override
    public RecordingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_recording, parent, false);
        return new RecordingViewHolder(v, this::setSelected);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull RecordingViewHolder holder, int position) {
        RecordingItem item = items.get(position);
        RecordingInfo rec = item.getRecordingInfo();
        StringBuilder s = new StringBuilder();
        s.append(String.format("<b>%s %s</b>",
                rec.getSerial(),
                rec.getStartTimestampString()));
        s.append(String.format("<br />%dsec %dpg %s",
                rec.getDurationSec(),
                rec.getNumPages(),
                rec.getSensorConfig().toString()));
        if (rec.isInProgress()) {
            s.append("<br />Recording in progress...");
        }
        holder.textView.setText(Html.fromHtml(s.toString(), 0));
        holder.view.setSelected(position == selection);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setRecordings(List<RecordingItem> items) {
        this.items = items;
        this.selection = RecyclerView.NO_POSITION;
        notifyDataSetChanged();
    }

    private void setSelected(int position) {
        selection = position;
        notifyDataSetChanged();
    }

    public RecordingItem getSelectedItem() {
        if (selection == RecyclerView.NO_POSITION) {
            return null;
        } else {
            return items.get(selection);
        }
    }
}

package com.mc10inc.biostamp3.sdkexample;

import android.annotation.SuppressLint;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mc10inc.biostamp3.sdk.recording.DownloadStatus;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdkexample.databinding.ListItemRecordingBinding;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

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

        ListItemRecordingBinding binding;
        View view;

        RecordingViewHolder(View view, SelectListener listener) {
            super(view);
            binding = ListItemRecordingBinding.bind(view);
            this.view = view;

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
        if (rec.getMetadata().length > 0) {
            String metadataStr = new String(rec.getMetadata(), StandardCharsets.UTF_8);
            s.append(String.format("<br />Metadata: %s", metadataStr));
        }
        if (rec.isInProgress()) {
            s.append("<br />Recording in progress...");
        } else {
            DownloadStatus ds = rec.getDownloadStatus();
            if (ds != null) {
                if (ds.isComplete()) {
                    s.append("<br />Download complete");
                } else {
                    s.append(String.format("<br />%d of %d pages downloaded",
                            ds.getDownloadedPages(),
                            ds.getNumPages()));
                }
            }
        }
        holder.binding.textView.setText(Html.fromHtml(s.toString(), 0));
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

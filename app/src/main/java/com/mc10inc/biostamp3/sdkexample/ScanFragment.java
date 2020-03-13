package com.mc10inc.biostamp3.sdkexample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.ScannedSensorStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ScanFragment extends BaseFragment {
    @BindView(R.id.sensorList)
    RecyclerView sensorList;

    private BioStampManager bs;
    private ScanSensorAdapter sensorAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bs = BioStampManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        unbinder = ButterKnife.bind(this, view);

        sensorList.setLayoutManager(new LinearLayoutManager(getContext()));
        sensorAdapter = new ScanSensorAdapter();
        sensorList.setAdapter(sensorAdapter);

        return view;
    }

    @OnClick(R.id.startButton) void startButton() {
        if (!bs.hasPermissions()) {
            if (getActivity() != null) {
                bs.requestPermissions(getActivity());
            }
            return;
        }

        bs.getSensorsInRangeLiveData().removeObservers(this);
        bs.getSensorsInRangeLiveData().observe(this, this::updateSensorList);
    }

    @OnClick(R.id.stopButton) void stopButton() {
        bs.getSensorsInRangeLiveData().removeObservers(this);
    }

    @OnClick(R.id.selectButton) void selectButton() {
        String serial = sensorAdapter.getSelectedItem();
        if (serial != null) {
            BioStamp bioStamp = bs.getBioStamp(serial);
        }
    }

    private void updateSensorList(Map<String, ScannedSensorStatus> sensorsInRange) {
        List<String> serials = new ArrayList<>(sensorsInRange.keySet());
        Collections.sort(serials);
        sensorAdapter.setSensorSerials(serials);
    }

    private static class ScanSensorAdapter extends RecyclerView.Adapter<ScanSensorViewHolder> {
        private int selection = RecyclerView.NO_POSITION;
        private List<String> sensorSerials = Collections.emptyList();

        @NonNull
        @Override
        public ScanSensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_sensor, parent, false);
            return new ScanSensorViewHolder(v, this::setSelected);
        }

        @Override
        public void onBindViewHolder(@NonNull ScanSensorViewHolder holder, int position) {
            holder.serialTextView.setText(sensorSerials.get(position));
            holder.view.setSelected(position == selection);
        }

        @Override
        public int getItemCount() {
            return sensorSerials.size();
        }

        private void setSelected(int position) {
            selection = position;
            notifyDataSetChanged();
        }

        private void setSensorSerials(List<String> sensorSerials) {
            if (!this.sensorSerials.equals(sensorSerials)) {
                this.sensorSerials = sensorSerials;
                notifyDataSetChanged();
            }
        }

        String getSelectedItem() {
            if (selection == RecyclerView.NO_POSITION) {
                return null;
            } else {
                return sensorSerials.get(selection);
            }
        }
    }

    static class ScanSensorViewHolder extends RecyclerView.ViewHolder {
        interface SelectListener {
            void selected(int position);
        }

        View view;

        @BindView(R.id.serialTextView)
        TextView serialTextView;

        ScanSensorViewHolder(View view, SelectListener listener) {
            super(view);
            this.view = view;
            ButterKnife.bind(this, view);

            View.OnClickListener clickListener = v -> listener.selected(getAdapterPosition());
            view.setOnClickListener(clickListener);
        }
    }
}

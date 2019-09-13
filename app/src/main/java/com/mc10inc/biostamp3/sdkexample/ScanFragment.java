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

import com.mc10inc.biostamp3.sdk.BioStampManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ScanFragment extends BaseFragment {
    @BindView(R.id.sensorList)
    RecyclerView sensorList;

    private BioStampManager bs;
    private SensorAdapter sensorAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bs = BioStampManager.getInstance(requireActivity());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scan, container, false);
        unbinder = ButterKnife.bind(this, view);

        sensorList.setLayoutManager(new LinearLayoutManager(getContext()));
        sensorAdapter = new SensorAdapter();
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

        bs.startScanning(this::updateSensorList);
    }

    @OnClick(R.id.stopButton) void stopButton() {
        bs.stopScanning();
    }

    @OnClick(R.id.provisionButton) void provisionButton() {

    }

    private void updateSensorList() {
        List<String> serials = new ArrayList<>(bs.getScanResults().keySet());
        Collections.sort(serials);
        sensorAdapter.setSensorSerials(serials);
    }

    private class SensorAdapter extends RecyclerView.Adapter<SensorViewHolder> {
        private int selection = RecyclerView.NO_POSITION;
        private List<String> sensorSerials = Collections.emptyList();

        @NonNull
        @Override
        public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_sensor, parent, false);
            return new SensorViewHolder(v, this::setSelected);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
            holder.textView.setText(sensorSerials.get(position));
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
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {
        interface SelectListener {
            void selected(int position);
        }

        View view;

        @BindView(R.id.textView)
        TextView textView;

        SensorViewHolder(View view, SelectListener listener) {
            super(view);
            this.view = view;
            ButterKnife.bind(this, view);

            View.OnClickListener clickListener = v -> listener.selected(getAdapterPosition());
            view.setOnClickListener(clickListener);
        }
    }
}

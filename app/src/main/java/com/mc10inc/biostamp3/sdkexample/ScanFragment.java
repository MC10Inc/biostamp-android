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
import timber.log.Timber;

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

    @OnClick(R.id.connectButton) void connectButton() {
        String serial = sensorAdapter.getSelectedItem();
        if (serial != null) {
            BioStamp sensor = bs.getBioStamp(serial);
            if (sensor.getState() != BioStamp.State.DISCONNECTED) {
                Timber.i("Cannot connect, state is %s", sensor.getState());
                return;
            }
            sensor.connect(new BioStamp.ConnectListener() {
                @Override
                public void connected() {
                    Timber.i("Connected to %s", sensor.getSerial());
                }

                @Override
                public void connectFailed() {
                    Timber.i("Failed to connect");
                }

                @Override
                public void disconnected() {
                    Timber.i("Disconnected");
                }
            });
        }
    }

    private void updateSensorList(Map<String, ScannedSensorStatus> sensorsInRange) {
        List<String> serials = new ArrayList<>(sensorsInRange.keySet());
        Collections.sort(serials);
        sensorAdapter.setSensorSerials(serials);
    }

    private static class ScanSensorAdapter extends RecyclerView.Adapter<SensorViewHolder> {
        private int selection = RecyclerView.NO_POSITION;
        private List<String> sensorSerials = Collections.emptyList();
        private Map<String, BioStamp> biostamps = Collections.emptyMap();

        @NonNull
        @Override
        public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_sensor, parent, false);
            return new SensorViewHolder(v, this::setSelected);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
            String serial = sensorSerials.get(position);
            holder.serialTextView.setText(serial);
            if (biostamps != null && biostamps.containsKey(serial)) {
                BioStamp sensor = biostamps.get(serial);
                String status = "";
                if (sensor != null) {
                    switch (sensor.getState()) {
                        case CONNECTED:
                            status = "Connected";
                            break;
                        case DISCONNECTED:
                            status = "";
                            break;
                        case CONNECTING:
                            status = "Connecting…";
                            break;
                    }
                }
                holder.statusTextView.setText(status);
            }
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
            this.sensorSerials = sensorSerials;
            this.biostamps = BioStampManager.getInstance().getBioStampsLiveData().getValue();
            notifyDataSetChanged();
        }

        String getSelectedItem() {
            if (selection == RecyclerView.NO_POSITION) {
                return null;
            } else {
                return sensorSerials.get(selection);
            }
        }
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {
        interface SelectListener {
            void selected(int position);
        }

        View view;

        @BindView(R.id.serialTextView)
        TextView serialTextView;

        @BindView(R.id.statusTextView)
        TextView statusTextView;

        SensorViewHolder(View view, SensorViewHolder.SelectListener listener) {
            super(view);
            this.view = view;
            ButterKnife.bind(this, view);

            View.OnClickListener clickListener = v -> listener.selected(getAdapterPosition());
            view.setOnClickListener(clickListener);
        }
    }
}

package com.mc10inc.biostamp3.sdkexample;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.ScannedSensorStatus;
import com.mc10inc.biostamp3.sdkexample.databinding.FragmentScanBinding;
import com.mc10inc.biostamp3.sdkexample.databinding.ListItemSensorBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class ScanFragment extends BaseFragment {
    private FragmentScanBinding binding;
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
        binding = FragmentScanBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.sensorList.setLayoutManager(new LinearLayoutManager(getContext()));
        sensorAdapter = new ScanSensorAdapter();
        binding.sensorList.setAdapter(sensorAdapter);
        binding.startButton.setOnClickListener(this::startButton);
        binding.stopButton.setOnClickListener(this::stopButton);
        binding.connectButton.setOnClickListener(this::connectButton);

        return view;
    }

    private void startButton(View v) {
        if (!bs.hasPermissions()) {
            if (getActivity() != null) {
                bs.requestPermissions(getActivity());
            }
            return;
        }

        bs.getSensorsInRangeLiveData().removeObservers(this);
        bs.getSensorsInRangeLiveData().observe(this, this::updateSensorList);
    }

    private void stopButton(View v) {
        bs.getSensorsInRangeLiveData().removeObservers(this);
    }

    private void connectButton(View v) {
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
        List<ScannedSensorStatus> sensors = new ArrayList<>(sensorsInRange.values());
        Collections.sort(sensors, (a, b) -> a.getSerial().compareTo(b.getSerial()));
        sensorAdapter.setSensors(sensors);
    }

    private static class ScanSensorAdapter extends RecyclerView.Adapter<SensorViewHolder> {
        private String selectedSerial = null;
        private List<ScannedSensorStatus> sensors = Collections.emptyList();
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
            ScannedSensorStatus sensor = sensors.get(position);
            holder.binding.serialTextView.setText(sensor.getSerial());
            if (biostamps != null && biostamps.containsKey(sensor.getSerial())) {
                BioStamp bioStamp = biostamps.get(sensor.getSerial());
                String status = "";
                if (bioStamp != null) {
                    switch (bioStamp.getState()) {
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
                holder.binding.statusTextView.setText(status);
            } else {
                holder.binding.statusTextView.setText("");
            }
            if (sensor.getStatusBroadcast() != null) {
                holder.binding.sensorStatusTextView.setText(sensor.getStatusBroadcast().toString());
            } else {
                holder.binding.sensorStatusTextView.setText("");
            }
            holder.view.setSelected(sensor.getSerial().equals(selectedSerial));
        }

        @Override
        public int getItemCount() {
            return sensors.size();
        }

        private void setSelected(int position) {
            if (position >= 0 && position < sensors.size()) {
                selectedSerial = sensors.get(position).getSerial();
            } else {
                selectedSerial = null;
            }
            notifyDataSetChanged();
        }

        private void setSensors(List<ScannedSensorStatus> sensors) {
            this.sensors = sensors;
            this.biostamps = BioStampManager.getInstance().getBioStampsLiveData().getValue();
            if (selectedSerial != null) {
                boolean selectedIsInList = false;
                for (ScannedSensorStatus sensor : sensors) {
                    if (sensor.getSerial().equals(selectedSerial)) {
                        selectedIsInList = true;
                        break;
                    }
                }
                if (!selectedIsInList) {
                    selectedSerial = null;
                }
            }
            notifyDataSetChanged();
        }

        String getSelectedItem() {
            return selectedSerial;
        }
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {
        interface SelectListener {
            void selected(int position);
        }

        ListItemSensorBinding binding;
        View view;

        SensorViewHolder(View view, SensorViewHolder.SelectListener listener) {
            super(view);
            binding = ListItemSensorBinding.bind(view);
            this.view = view;

            View.OnClickListener clickListener = v -> listener.selected(getAdapterPosition());
            view.setOnClickListener(clickListener);
        }
    }
}

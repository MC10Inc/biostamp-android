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
import com.mc10inc.biostamp3.sdkexample.databinding.FragmentSensorsBinding;
import com.mc10inc.biostamp3.sdkexample.databinding.ListItemSensorBinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

public class SensorsFragment extends BaseFragment {
    private FragmentSensorsBinding binding;
    private BioStampManager bs;
    private SensorAdapter sensorAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bs = BioStampManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSensorsBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        binding.sensorList.setLayoutManager(new LinearLayoutManager(getContext()));
        sensorAdapter = new SensorAdapter();
        binding.sensorList.setAdapter(sensorAdapter);
        binding.connectButton.setOnClickListener(this::connectButton);
        binding.disconnectButton.setOnClickListener(this::disconnectButton);

        bs.getBioStampsLiveData().observe(getViewLifecycleOwner(), sensors -> {
            List<BioStamp> ss = new ArrayList<>(sensors.values());
            Collections.sort(ss, (c1, c2) -> c1.getSerial().compareTo(c2.getSerial()));
            sensorAdapter.setSensors(ss);
        });
        return view;
    }

    private void connectButton(View v) {
        BioStamp sensor = sensorAdapter.getSelectedItem();
        if (sensor != null) {
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

    private void disconnectButton(View v) {
        BioStamp sensor = sensorAdapter.getSelectedItem();
        if (sensor != null) {
            sensor.disconnect();
        }
    }

    private static class SensorAdapter extends RecyclerView.Adapter<SensorViewHolder> {
        private int selection = RecyclerView.NO_POSITION;
        private List<BioStamp> sensors = Collections.emptyList();

        @NonNull
        @Override
        public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_sensor, parent, false);
            return new SensorViewHolder(v, this::setSelected);
        }

        @Override
        public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
            BioStamp sensor = sensors.get(position);
            holder.binding.serialTextView.setText(sensor.getSerial());
            String status = "";
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
            holder.binding.statusTextView.setText(status);
            holder.binding.sensorStatusTextView.setText("");
            holder.view.setSelected(position == selection);
        }

        @Override
        public int getItemCount() {
            return sensors.size();
        }

        private void setSelected(int position) {
            selection = position;
            notifyDataSetChanged();
        }

        private void setSensors(List<BioStamp> sensors) {
            selection = RecyclerView.NO_POSITION;
            this.sensors = sensors;
            notifyDataSetChanged();
        }

        BioStamp getSelectedItem() {
            if (selection == RecyclerView.NO_POSITION) {
                return null;
            } else {
                return sensors.get(selection);
            }
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

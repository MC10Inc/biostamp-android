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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class SensorsFragment extends BaseFragment {
    @BindView(R.id.sensorList)
    RecyclerView sensorList;

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
        View view = inflater.inflate(R.layout.fragment_sensors, container, false);
        unbinder = ButterKnife.bind(this, view);

        sensorList.setLayoutManager(new LinearLayoutManager(getContext()));
        sensorAdapter = new SensorAdapter();
        sensorList.setAdapter(sensorAdapter);

        bs.getBioStampsLiveData().observe(getViewLifecycleOwner(), sensors -> {
            List<BioStamp> ss = new ArrayList<>(sensors.values());
            Collections.sort(ss, (c1, c2) -> c1.getSerial().compareTo(c2.getSerial()));
            sensorAdapter.setSensors(ss);
        });
        return view;
    }

    @OnClick(R.id.connectButton) void connectButton() {
        BioStamp sensor = sensorAdapter.getSelectedItem();
        if (sensor != null) {
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

    @OnClick(R.id.disconnectButton) void disconnectButton() {
        BioStamp sensor = sensorAdapter.getSelectedItem();
        if (sensor != null) {
            sensor.disconnect();
        }
    }

    @OnClick(R.id.deprovisionButton) void deprovisionButton() {
        BioStamp sensor = sensorAdapter.getSelectedItem();
        if (sensor != null) {
            bs.deprovisionSensor(sensor.getSerial());
        }
    }

    private class SensorAdapter extends RecyclerView.Adapter<SensorViewHolder> {
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
            holder.serialTextView.setText(sensor.getSerial());
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
            holder.statusTextView.setText(status);
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

        public BioStamp getSelectedItem() {
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

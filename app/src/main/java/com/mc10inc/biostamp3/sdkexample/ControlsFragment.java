package com.mc10inc.biostamp3.sdkexample;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampManager;
import com.mc10inc.biostamp3.sdk.SensorStatus;

import java.util.Iterator;
import java.util.Map;

import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class ControlsFragment extends BaseFragment {
    private BioStampManager bs;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bs = BioStampManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_controls, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.scanButton) void scanForSensors() {
        if (!bs.hasPermissions()) {
            if (getActivity() != null) {
                bs.requestPermissions(getActivity());
            }
            return;
        }

        /*
        handler.postDelayed(() -> {
            bs.stopScanning();
            Map<String, SensorStatus> results = bs.getSensorsInRange();
            Timber.i(results.toString());
            if (!results.isEmpty()) {
                Iterator<SensorStatus> iter = results.values().iterator();
                SensorStatus s = iter.next();
                BioStamp b = bs.getBioStamp(s.getSerial());
                b.connect(new BioStamp.ConnectListener() {
                    @Override
                    public void connected() {
                        Timber.i("connected");
                        b.test();
                    }

                    @Override
                    public void connectFailed() {
                        Timber.i("connect failed");
                    }

                    @Override
                    public void disconnected() {
                        Timber.i("disconnected");
                    }
                });
           }
        }, 3000);
*/
        bs.startScanning();
    }
}

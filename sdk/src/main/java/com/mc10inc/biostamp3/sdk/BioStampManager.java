package com.mc10inc.biostamp3.sdk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.ble.SensorBleBitgatt;
import com.mc10inc.biostamp3.sdk.ble.StatusBroadcast;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import timber.log.Timber;

public class BioStampManager {
    private static BioStampManager INSTANCE;

    public static BioStampManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BioStampManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BioStampManager(context.getApplicationContext());
                    INSTANCE.start();
                }
            }
        }
        return INSTANCE;
    }

    private static final FitbitGatt gatt = FitbitGatt.getInstance();

    private final Context applicationContext;
    private Runnable discoveryListener;
    private Handler handler = new Handler(Looper.getMainLooper());

    private BioStampManager(Context context) {
        this.applicationContext = context;
    }

    private void start() {
        gatt.startWithScanFilters(applicationContext, StatusBroadcast.getScanFilters(), callback);
    }

    public boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                0);
    }

    public void startScanning(Runnable discoveryListener) {
        this.discoveryListener = discoveryListener;
        gatt.startHighPriorityScan(applicationContext);
    }

    public void stopScanning() {
        this.discoveryListener = null;
        gatt.cancelScan(applicationContext);
    }

    public Map<String, SensorStatus> getScanResults() {
        Map<String, SensorStatus> results = new HashMap<>();
        List<GattConnection> conns = gatt.getMatchingConnectionsForDeviceNames(null);
        for (GattConnection conn : conns) {
            String serial = conn.getDevice().getName();
            results.put(serial, new SensorStatus(conn));
        }
        return results;
    }

    public BioStamp getBioStamp(String serial) {
        List<GattConnection> conns = gatt.getMatchingConnectionsForDeviceNames(
                Collections.singletonList(serial));
        if (conns.isEmpty()) {
            return null;
        }
        if (conns.size() > 1) {
            Timber.e("Found %d connections matching name %s", conns.size(), serial);
        }

        SensorBle sensorBle = new SensorBleBitgatt(conns.get(0));
        return new BioStampImpl(this, sensorBle);
    }

    private final FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
        @Override
        public void onBluetoothPeripheralDiscovered(GattConnection connection) {
            Runnable dl = discoveryListener;
            if (dl != null) {
                handler.post(dl);
            }
        }

        @Override
        public void onBluetoothPeripheralDisconnected(GattConnection connection) {

        }

        @Override
        public void onFitbitGattReady() {

        }

        @Override
        public void onFitbitGattStartFailed() {

        }

        @Override
        public void onScanStarted() {

        }

        @Override
        public void onScanStopped() {

        }

        @Override
        public void onPendingIntentScanStopped() {

        }

        @Override
        public void onPendingIntentScanStarted() {

        }

        @Override
        public void onBluetoothOff() {

        }

        @Override
        public void onBluetoothOn() {

        }

        @Override
        public void onBluetoothTurningOn() {

        }

        @Override
        public void onBluetoothTurningOff() {

        }
    };
}

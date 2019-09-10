package com.mc10inc.biostamp3.sdk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.SystemClock;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.mc10inc.biostamp3.sdk.ble.StatusBroadcast;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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

    private class ScannedSensor {
        final GattConnection conn;
        final long scannedTime;

        ScannedSensor(GattConnection conn) {
            this.conn = conn;
            scannedTime = SystemClock.elapsedRealtime();
        }
    }

    private static final FitbitGatt gatt = FitbitGatt.getInstance();

    private final Context applicationContext;
    private final Map<String, ScannedSensor> scannedSensors = new HashMap<>();

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

    public void startScanning() {
        gatt.startHighPriorityScan(applicationContext);
    }

    public void stopScanning() {
        gatt.cancelScan(applicationContext);
    }

    public Map<String, SensorStatus> getScanResults() {
        Map<String, SensorStatus> results = new HashMap<>();
        for (ScannedSensor ss : scannedSensors.values()) {
            String name = ss.conn.getDevice().getName();
            results.put(name, new SensorStatus(ss.conn));
        }
        return results;
    }

    private final FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
        @Override
        public void onBluetoothPeripheralDiscovered(GattConnection connection) {
            synchronized (scannedSensors) {
                scannedSensors.put(connection.getDevice().getName(), new ScannedSensor(connection));
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

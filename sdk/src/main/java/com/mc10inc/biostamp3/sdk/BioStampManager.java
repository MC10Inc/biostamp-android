package com.mc10inc.biostamp3.sdk;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.ble.SensorBleBitgatt;
import com.mc10inc.biostamp3.sdk.ble.StatusBroadcast;
import com.mc10inc.biostamp3.sdk.db.BioStampDb;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import timber.log.Timber;

public class BioStampManager {
    private static BioStampManager INSTANCE;

    public static void initialize(Context context) {
        if (INSTANCE != null) {
            throw new IllegalStateException("BioStampManager is already initialized!");
        }
        INSTANCE = new BioStampManager(context.getApplicationContext());
        INSTANCE.start();
    }

    public static BioStampManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("BioStampManager is not initialized!");
        }
        return INSTANCE;
    }

    private static final int SENSOR_IN_RANGE_TTL = 10000;

    private static final FitbitGatt gatt = FitbitGatt.getInstance();

    private final Context applicationContext;
    private final Map<String, BioStampImpl> biostamps = new HashMap<>();
    private final MutableLiveData<Map<String, BioStamp>> biostampsLiveData = new MutableLiveData<>();
    private final BioStampDb db;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ScannedSensorStatus> sensorsInRange =
            new PassiveExpiringMap<>(SENSOR_IN_RANGE_TTL, new HashMap<>());
    private final SensorsInRangeLiveData sensorsInRangeLiveData = new SensorsInRangeLiveData();
    private final ThroughputStats throughputStats = new ThroughputStats();

    private BioStampManager(Context context) {
        this.applicationContext = context;
        db = new BioStampDb(context);
    }

    private final Runnable updateThroughput = new Runnable() {
        @Override
        public void run() {
            throughputStats.update(0);
            handler.postDelayed(updateThroughput, 1000);
        }
    };

    private void start() {
        gatt.startWithScanFilters(applicationContext, StatusBroadcast.getScanFilters(), callback);
        handler.post(updateThroughput);
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

    public LiveData<Map<String, ScannedSensorStatus>> getSensorsInRangeLiveData() {
        return sensorsInRangeLiveData;
    }

    public BioStamp getBioStamp(String serial) {
        synchronized (biostamps) {
            if (!biostamps.containsKey(serial)) {
                biostamps.put(serial, new BioStampImpl(this, serial));
                updateBioStampLiveData();
            }
            return biostamps.get(serial);
        }
    }

    public LiveData<Map<String, BioStamp>> getBioStampsLiveData() {
        return biostampsLiveData;
    }

    public BioStampDb getDb() {
        return db;
    }

    public void dbExecute(Runnable runnable) {
        dbExecutor.execute(runnable);
    }

    SensorBle getSensorBle(String serial) {
        List<GattConnection> conns = gatt.getMatchingConnectionsForDeviceNames(
                Collections.singletonList(serial));
        if (conns.isEmpty()) {
            return null;
        }
        if (conns.size() > 1) {
            Timber.e("Found %d connections matching name %s", conns.size(), serial);
        }

        return new SensorBleBitgatt(conns.get(0));
    }

    private void updateBioStampLiveData() {
        biostampsLiveData.postValue(new HashMap<>(biostamps));
    }

    void notifyConnStateChange() {
        updateBioStampLiveData();
    }

    void updateThroughput(int bytes) {
        throughputStats.update(bytes);
    }

    public LiveData<Integer> getThroughput() {
        return throughputStats.getThroughput();
    }

    private final FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
        @Override
        public void onBluetoothPeripheralDiscovered(GattConnection connection) {
            String serial = connection.getDevice().getName();
            sensorsInRange.put(serial, new ScannedSensorStatus(connection));
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

    private class SensorsInRangeLiveData extends MutableLiveData<Map<String, ScannedSensorStatus>> {
        private static final int SCAN_FAILED_RETRY_DELAY = 5000;
        private static final int STOP_SCAN_DELAY = 30000;
        private static final int UPDATE_RATE = 250;

        private boolean scanInProgress;

        SensorsInRangeLiveData() {
            super(Collections.emptyMap());
        }

        private final Runnable updateRunnable = new Runnable() {
            @Override
            public void run() {
                synchronized (sensorsInRange) {
                    postValue(new HashMap<>(sensorsInRange));
                }
                handler.postDelayed(updateRunnable, UPDATE_RATE);
            }
        };

        private final Runnable stopScanRunnable = new Runnable() {
            @Override
            public void run() {
                if (!scanInProgress) {
                    return;
                }
                Timber.i("Stopping BLE scan for sensors after timeout");
                gatt.cancelHighPriorityScan(applicationContext);
                scanInProgress = false;
            }
        };

        private void startScan() {
            if (scanInProgress) {
                return;
            }
            if (!hasActiveObservers()) {
                return;
            }
            boolean started = gatt.startHighPriorityScan(applicationContext);
            if (started) {
                Timber.i("Started BLE scan for sensors");
                scanInProgress = true;
            } else {
                Timber.e("Start BLE scan failed; will retry");
                handler.postDelayed(this::startScan, SCAN_FAILED_RETRY_DELAY);
            }
        }

        @Override
        protected void onActive() {
            super.onActive();
            handler.removeCallbacks(updateRunnable);
            handler.removeCallbacks(stopScanRunnable);
            handler.post(updateRunnable);
            startScan();
        }

        @Override
        protected void onInactive() {
            super.onInactive();
            handler.removeCallbacks(updateRunnable);
            handler.postDelayed(stopScanRunnable, STOP_SCAN_DELAY);
        }
    }
}

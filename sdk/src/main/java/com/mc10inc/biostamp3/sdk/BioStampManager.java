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
import com.mc10inc.biostamp3.sdk.db.BioStampDatabase;
import com.mc10inc.biostamp3.sdk.db.ProvisionedSensor;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

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
    private Executor dbExecutor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private MutableLiveData<Set<String>> provisionedSensors = new MutableLiveData<>();
    private final Map<String, SensorStatus> sensorsInRange =
            new PassiveExpiringMap<>(SENSOR_IN_RANGE_TTL, new HashMap<>());


    private BioStampManager(Context context) {
        this.applicationContext = context;
    }

    private void start() {
        updateProvisionedSensors();
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
        gatt.cancelHighPriorityScan(applicationContext);
    }

    public Map<String, SensorStatus> getSensorsInRange() {
        synchronized (sensorsInRange) {
            return new HashMap<>(sensorsInRange);
        }
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

    private BioStampDatabase getDb() {
        return BioStampDatabase.getDatabase(applicationContext);
    }

    public LiveData<Set<String>> getProvisionedSensors() {
        return provisionedSensors;
    }

    public void provisionSensor(String sensor) {
        dbExecutor.execute(() -> {
            ProvisionedSensor ps = new ProvisionedSensor(sensor);
            getDb().provisionedSensorDao().insert(ps);
            updateProvisionedSensors();
        });
    }

    public void deprovisionSensor(String sensor) {
        dbExecutor.execute(() -> {
            ProvisionedSensor ps = new ProvisionedSensor(sensor);
            getDb().provisionedSensorDao().delete(ps);
            updateProvisionedSensors();
        });
    }

    private void updateProvisionedSensors() {
        dbExecutor.execute(() -> {
            List<ProvisionedSensor> sensors = getDb().provisionedSensorDao().getAll();
            Set<String> ps = sensors.stream().map(ProvisionedSensor::getSerial).collect(Collectors.toSet());
            provisionedSensors.postValue(ps);
        });
    }

    private final FitbitGatt.FitbitGattCallback callback = new FitbitGatt.FitbitGattCallback() {
        @Override
        public void onBluetoothPeripheralDiscovered(GattConnection connection) {
            String serial = connection.getDevice().getName();
            sensorsInRange.put(serial, new SensorStatus(connection));
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

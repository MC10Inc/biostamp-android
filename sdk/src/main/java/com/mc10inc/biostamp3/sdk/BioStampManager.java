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
import com.fitbit.bluetooth.fbgatt.GattServerConnection;
import com.fitbit.bluetooth.fbgatt.exception.BitGattStartException;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.ble.SensorBleBitgatt;
import com.mc10inc.biostamp3.sdk.ble.StatusBroadcast;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import timber.log.Timber;

/**
 * Main entry point to BioStamp SDK.
 * <p/>
 * This singleton class provides access to all BioStamp SDK functions. It handles scanning for
 * sensors in range and managing the connections to those sensors.
 */
public class BioStampManager {
    private static BioStampManager INSTANCE;

    /**
     * Initialize the BioStamp SDK.
     * <p/>
     * This method must be called exactly once when the application starts up. It is a fatal error
     * if it is called again after the SDK is already initialized. This method must be called before
     * calling getInstance.
     * <p/>
     * It is recommended to call this method from the onCreate method of the Application class.
     *
     * @param context application context
     */
    public static void initialize(Context context) {
        if (INSTANCE != null) {
            throw new IllegalStateException("BioStampManager is already initialized!");
        }
        INSTANCE = new BioStampManager(context.getApplicationContext());
        INSTANCE.start();
    }

    /**
     * Get the BioStampManager singleton.
     * <p/>
     * initialize must be called before calling this method.
     *
     * @return BioStampManager instance
     */
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
    private final BioStampDbImpl db;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, ScannedSensorStatus> sensorsInRange =
            new PassiveExpiringMap<>(SENSOR_IN_RANGE_TTL, new HashMap<>());
    private final SensorsInRangeLiveData sensorsInRangeLiveData = new SensorsInRangeLiveData();
    private final ThroughputStats throughputStats = new ThroughputStats();

    private BioStampManager(Context context) {
        this.applicationContext = context;
        db = new BioStampDbImpl(context);
    }

    private final Runnable updateThroughput = new Runnable() {
        @Override
        public void run() {
            throughputStats.update(0);
            handler.postDelayed(updateThroughput, 1000);
        }
    };

    private void start() {
        gatt.registerGattEventListener(callback);
        gatt.startGattClient(applicationContext);
        gatt.initializeScanner(applicationContext);
        // Starting and stopping a periodical scan is currently the only way to pass in a ScanFilter
        // object. We need to do this because a method is not provided to add a scan filter for
        // manufacturer-specific data, even though methods are provided for many other items in the
        // advertisement.
        gatt.startPeriodicalScannerWithFilters(applicationContext, StatusBroadcast.getScanFilters());
        gatt.cancelPeriodicalScan(applicationContext);
        handler.post(updateThroughput);
    }

    /**
     * Check if the application has the permissions needed to communicate with the sensors.
     * <p/>
     * The ACCESS_COARSE_LOCATION permission is required for BLE scanning. If this method returns
     * false, the requestPermissions method must be called to request the permissions.
     *
     * @return true if the application has the permissions
     */
    public boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Request the permissions needed to communicate with the sensors.
     * <p/>
     * This method must be called if hasPermissions returns false. Android will show the user a
     * dialog asking them to grant the required permission. The message is:
     * <p/>
     * "Allow APP NAME to access this device's location?"
     *
     * @param activity application's activity that is currently running
     */
    public void requestPermissions(Activity activity) {
        ActivityCompat.requestPermissions(
                activity,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                0);
    }

    /**
     * Get a LiveData object that shows all sensors in range.
     * <p/>
     * Any time this LiveData is being observed, the SDK will enable BLE scanning to find sensors
     * in range. The LiveData is updated periodically, and the contents of the result may not change
     * on every update.
     * <p/>
     * The value is a map whose key is the sensor serial number and whose value is a
     * ScannedSensorStatus object containing any information about the sensor that was seen while
     * scanning. Currently this only contains the serial number.
     * <p/>
     * Sensors are removed from the result after they have not been seen for 10 seconds.
     * <p/>
     * The result only shows sensors which are currently transmitting BLE advertisements. This
     * excludes any sensors which have a connection open to this application or to any other BLE
     * device.
     * <p/>
     * Do not try to observe this LiveData until hasPermissions returns true.
     *
     * @return LiveData showing all sensors in range
     */
    public LiveData<Map<String, ScannedSensorStatus>> getSensorsInRangeLiveData() {
        return sensorsInRangeLiveData;
    }

    /**
     * Get a BioStamp object representing a sensor.
     * <p/>
     * All communication with a specific sensor is performed through the BioStamp object which
     * represents that sensor. The returned object represents that sensor for as long as the
     * application is running, regardless of whether or not there is a connection to that sensor.
     * <p/>
     * The BioStamp object provides methods to connect, disconnect, and perform all sensor
     * operations.
     *
     * @param serial serial number of sensor
     * @return BioStamp object representing the sensor
     */
    public BioStamp getBioStamp(String serial) {
        synchronized (biostamps) {
            if (!biostamps.containsKey(serial)) {
                biostamps.put(serial, new BioStampImpl(this, serial));
                updateBioStampLiveData();
            }
            return biostamps.get(serial);
        }
    }

    /**
     * Get a LiveData representing all sensors used by the SDK.
     * <p/>
     * The value of the LiveData is a map whose key is the sensor serial number and whose value is
     * the BioStamp object, as returned by getBioStamp.
     * <p/>
     * The value updates any time a sensor is added or the connection state of a sensor changes.
     * This LiveData may be observed to keep track of which sensors are currently connected.
     * <p/>
     * The value contains all sensors that have been accessed through the getBioStamp method since
     * the application was launched.
     *
     * @return LiveData representing all sensors
     */
    public LiveData<Map<String, BioStamp>> getBioStampsLiveData() {
        return biostampsLiveData;
    }

    /**
     * Access the BioStamp recording database object.
     * <p/>
     * This object provides access to the database in the application's local storage which contains
     * recordings downloaded from sensors.
     *
     * @return the recording database singleton
     */
    public BioStampDb getDb() {
        return db;
    }

    public BioStampDbImpl getDbImpl() {
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
        public void onScanStarted() {

        }

        @Override
        public void onScanStopped() {
            // The Bitgatt scan always times out after 2 minutes
            sensorsInRangeLiveData.scanStopped();
        }

        @Override
        public void onScannerInitError(BitGattStartException error) {
            Timber.e(error);
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

        @Override
        public void onGattServerStarted(GattServerConnection serverConnection) {

        }

        @Override
        public void onGattServerStartError(BitGattStartException error) {

        }

        @Override
        public void onGattClientStarted() {

        }

        @Override
        public void onGattClientStartError(BitGattStartException error) {
            Timber.e(error);
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

        private void scanStopped() {
            scanInProgress = false;
            handler.post(this::startScan);
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

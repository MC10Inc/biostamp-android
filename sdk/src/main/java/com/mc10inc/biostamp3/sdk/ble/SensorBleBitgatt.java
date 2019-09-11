package com.mc10inc.biostamp3.sdk.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;

import com.fitbit.bluetooth.fbgatt.CompositeClientTransaction;
import com.fitbit.bluetooth.fbgatt.ConnectionEventListener;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.SetClientConnectionStateTransaction;
import com.fitbit.bluetooth.fbgatt.tx.SubscribeToCharacteristicNotificationsTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction;
import com.mc10inc.biostamp3.sdk.BleException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import timber.log.Timber;

public class SensorBleBitgatt implements SensorBle, ConnectionEventListener {
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    private static final UUID BS_SERVICE_UUID =
            UUID.fromString("de771000-90e1-11e8-9a5a-34f39a69480c");
    private static final UUID COMMAND_CHAR_UUID =
            UUID.fromString("de771001-90e1-11e8-9a5a-34f39a69480c");
    private static final UUID RESPONSE_CHAR_UUID =
            UUID.fromString("de771002-90e1-11e8-9a5a-34f39a69480c");
    private static final UUID DATA_CHAR_UUID =
            UUID.fromString("de771003-90e1-11e8-9a5a-34f39a69480c");
    private static final UUID GENERIC_ACCESS_SERVICE_UUID =
            UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID DEVICE_NAME_CHAR_UUID =
            UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");

    private enum State {
        INIT,
        DISCONNECTED,
        CONNECTING,
        DISCONNECTING,
        READY,
        BUSY
    }

    private Semaphore busySemaphore = new Semaphore(1);
    private BluetoothGattCharacteristic charCommand;
    private BluetoothGattCharacteristic charData;
    private BluetoothGattCharacteristic charResponse;
    private GattConnection conn;
    private BleDisconnectListener disconnectListener;
    private CountDownLatch doneLatch;
    private String serial;
    private State state;

    public SensorBleBitgatt(GattConnection conn) {
        this.conn = conn;
        this.state = State.INIT;
    }

    @Override
    public void connect(BleDisconnectListener disconnectListener) throws BleException {
        try {
            busySemaphore.acquire();
        } catch (InterruptedException e) {
            throw new BleException();
        }
        try {
            synchronized (this) {
                if (state != State.INIT) {
                    throw new BleException();
                }
                this.disconnectListener = disconnectListener;
                state = State.CONNECTING;
                doneLatch = new CountDownLatch(1);
                GattConnectTransaction tx = new GattConnectTransaction(conn, GattState.CONNECTED);
                runTx(tx, result -> {
                    if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                        discoverServices();
                    } else {
                        failConnect("Failed to connect to sensor: " + result);
                    }
                });
            }
            try {
                doneLatch.await();
            } catch (InterruptedException e) {
                throw new BleException();
            }

        } finally {
            busySemaphore.release();
        }
    }

    @Override
    public void disconnect() throws BleException {

    }

    private void disconnectForErrorDuringConnect(TransactionResult origResult) {
        // Unblock the bitgatt transaction queue
        SetClientConnectionStateTransaction tx = new SetClientConnectionStateTransaction(
                conn, GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, GattState.DISCONNECTED);
        runTx(tx, result -> {
            if (!result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                Timber.e("Failed to disconnect after error connecting: %s", result);
            }
            failConnect("Disconnected after error connecting: " + origResult);
        });
    }

    private void discoverServices() {
        GattClientDiscoverServicesTransaction tx = new GattClientDiscoverServicesTransaction(
                conn, GattState.DISCOVERY_SUCCESS);
        runTx(tx, result -> {
            if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                handleServices(result.getServices());
            } else {
                disconnectForErrorDuringConnect(result);
            }
        });
    }

    private void done() {
        if (doneLatch != null) {
            doneLatch.countDown();
        }
    }

    private void failConnect(String error) {
        Timber.e(error);
        state = State.DISCONNECTED;
        done();
    }

    @Override
    public String getSerial() throws BleException {
        if (serial != null) {
            return serial;
        } else {
            throw new BleException();
        }
    }

    private void handleServices(List<BluetoothGattService> services) {
        BluetoothGattService bsService = null;
        BluetoothGattService gapService = null;
        for (BluetoothGattService service : services) {
            if (service.getUuid().equals(BS_SERVICE_UUID)) {
                bsService = service;
            } else if (service.getUuid().equals(GENERIC_ACCESS_SERVICE_UUID)) {
                gapService = service;
            }
        }
        if (bsService == null) {
            failConnect("Cannot find BioStamp GATT service");
            return;
        }
        if (gapService == null) {
            failConnect("Cannot find Generic Access service");
            return;
        }

        charCommand = bsService.getCharacteristic(COMMAND_CHAR_UUID);
        if (charCommand == null) {
            failConnect("Cannot find command characteristic");
            return;
        }

        charData = bsService.getCharacteristic(DATA_CHAR_UUID);
        if (charData == null) {
            failConnect("Cannot find data characteristic");
            return;
        }

        charResponse = bsService.getCharacteristic(RESPONSE_CHAR_UUID);
        if (charResponse == null) {
            failConnect("Cannot find response characteristic");
            return;
        }

        BluetoothGattCharacteristic charDeviceName = gapService.getCharacteristic(
                DEVICE_NAME_CHAR_UUID);
        if (charDeviceName == null) {
            failConnect("Cannot find device name characteristic");
            return;
        }

        BluetoothGattDescriptor cccdCommand =
                charCommand.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (cccdCommand == null) {
            failConnect("Cannot find command CCCD");
            return;
        }
        cccdCommand.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

        BluetoothGattDescriptor cccdData =
                charData.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG);
        if (cccdData == null) {
            failConnect("Cannot find data CCCD");
            return;
        }
        cccdData.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

        List<GattTransaction> txs = new ArrayList<>();
        // This must be the first transaction in the list so that we can find its result
        txs.add(new ReadGattCharacteristicTransaction(conn,
                GattState.READ_CHARACTERISTIC_SUCCESS,
                charDeviceName));
        txs.add(new WriteGattDescriptorTransaction(conn,
                GattState.WRITE_DESCRIPTOR_SUCCESS,
                cccdCommand));
        txs.add(new SubscribeToCharacteristicNotificationsTransaction(conn,
                GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS,
                charCommand));
        txs.add(new WriteGattDescriptorTransaction(conn,
                GattState.WRITE_DESCRIPTOR_SUCCESS,
                cccdData));
        txs.add(new SubscribeToCharacteristicNotificationsTransaction(conn,
                GattState.ENABLE_CHARACTERISTIC_NOTIFICATION_SUCCESS,
                charData));
        runTx(new CompositeClientTransaction(conn, txs), result -> {
            if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                byte[] deviceNameBytes = result.getTransactionResults().get(0).getData();
                if (deviceNameBytes == null) {
                    failConnect("Failed to get device name");
                    return;
                }
                serial = new String(deviceNameBytes, StandardCharsets.US_ASCII);

                state = State.READY;
                conn.registerConnectionEventListener(this);
                done();
            } else {
                failConnect("Failed to set up connection: " + result);
            }
        });
    }

    @Override
    public void onClientCharacteristicChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

    }

    @Override
    public void onClientConnectionStateChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

    }

    @Override
    public void onServicesDiscovered(@NonNull TransactionResult result, @NonNull GattConnection connection) {

    }

    @Override
    public void onMtuChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

    }

    @Override
    public void onPhyChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {

    }

    private void runTx(GattTransaction transaction, GattTransactionCallback callback) {
        conn.runTx(transaction, result -> {
            synchronized (SensorBleBitgatt.this) {
                callback.onTransactionComplete(result);
            }
        });
    }
}

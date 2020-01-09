package com.mc10inc.biostamp3.sdk.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;

import com.fitbit.bluetooth.fbgatt.CompositeClientTransaction;
import com.fitbit.bluetooth.fbgatt.ConnectionEventListener;
import com.fitbit.bluetooth.fbgatt.FitbitGatt;
import com.fitbit.bluetooth.fbgatt.GattConnection;
import com.fitbit.bluetooth.fbgatt.GattState;
import com.fitbit.bluetooth.fbgatt.GattTransaction;
import com.fitbit.bluetooth.fbgatt.GattTransactionCallback;
import com.fitbit.bluetooth.fbgatt.TransactionResult;
import com.fitbit.bluetooth.fbgatt.tx.GattClientDiscoverServicesTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattConnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.GattDisconnectTransaction;
import com.fitbit.bluetooth.fbgatt.tx.ReadGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.RequestGattConnectionIntervalTransaction;
import com.fitbit.bluetooth.fbgatt.tx.SetClientConnectionStateTransaction;
import com.fitbit.bluetooth.fbgatt.tx.SubscribeToCharacteristicNotificationsTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattCharacteristicTransaction;
import com.fitbit.bluetooth.fbgatt.tx.WriteGattDescriptorTransaction;
import com.google.protobuf.ByteString;
import com.mc10inc.biostamp3.sdk.exception.BleException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
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

    private static final byte COMMAND_CODE_RESP_FOLLOWS = 1;
    private static final byte COMMAND_CODE_RESP_LONG = 2;

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
    private DataHandler dataHandler = new DataHandler();
    private BleDataListener dataListener;
    private BleDisconnectListener disconnectListener;
    private CountDownLatch doneLatch;
    private byte[] response;
    private String serial;
    private Speed speed;
    private State state;
    private int writeFastCount;
    private List<byte[]> writeFastPackets;
    private ProgressListener writeFastProgressListener;

    public SensorBleBitgatt(GattConnection conn) {
        this.conn = conn;
        this.state = State.INIT;
        this.speed = Speed.BALANCED;
    }

    @Override
    public void connect(BleDisconnectListener disconnectListener, BleDataListener dataListener)
            throws BleException {
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
                this.dataListener = dataListener;
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
        if (state == State.DISCONNECTED
                || state == State.DISCONNECTING
                || state == State.CONNECTING) {
            throw new BleException();
        }
        state = State.DISCONNECTING;
        GattDisconnectTransaction tx = new GattDisconnectTransaction(
                conn, GattState.DISCONNECTED);
        runTx(tx, result -> {
            if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                Timber.i("Disconnect transaction succeeded");
            } else {
                Timber.e("Failed to disconnect: %s", result);
            }
        });
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

    @Override
    public byte[] execute(byte[] command) throws BleException {
        return execute(command, null);
    }

    @Override
    public byte[] execute(byte[] command, List<byte[]> writeFastData) throws BleException {
        try {
            busySemaphore.acquire();
        } catch (InterruptedException e) {
            throw new BleException();
        }
        try {
            synchronized (this) {
                doneLatch = new CountDownLatch(1);
                state = State.BUSY;
                charCommand.setValue(command);
                WriteGattCharacteristicTransaction tx = new WriteGattCharacteristicTransaction(
                        conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, charCommand);
                runTx(tx, result -> {
                    if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                        if (writeFastData != null) {
                            writeFastPackets = new LinkedList<>(writeFastData);
                            writeFastCount = writeFastPackets.size();
                            sendWriteFastPacket();
                        }
                        // TODO Set a timeout waiting for the response
                    } else {
                        handleError(result.toString());
                    }
                });
            }
            try {
                doneLatch.await();
            } catch (InterruptedException e) {
                throw new BleException();
            }
            synchronized (this) {
                writeFastPackets = null;
                writeFastProgressListener = null;
                state = State.READY;
                return response;
            }
        } finally {
            busySemaphore.release();
        }
    }

    private void sendWriteFastPacket() {
        synchronized (this) {
            if (writeFastPackets == null || writeFastPackets.isEmpty()) {
                return;
            }
            byte[] packet = writeFastPackets.remove(0);
            charData.setValue(packet);
            charData.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            WriteGattCharacteristicTransaction tx = new WriteGattCharacteristicTransaction(
                    conn, GattState.WRITE_CHARACTERISTIC_SUCCESS, charData);
            runTx(tx, result -> {
                if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                    if (writeFastProgressListener != null && writeFastPackets != null) {
                        writeFastProgressListener.updateProgress(
                                ((double)(writeFastCount - writeFastPackets.size())) / writeFastCount);
                    }
                    sendWriteFastPacket();
                } else {
                    Timber.e("Failed to send write fast packet: %s", result);
                }
            });
        }
    }

    @Override
    public void setWriteFastProgressListener(ProgressListener progressListener) {
        synchronized (this) {
            this.writeFastProgressListener = progressListener;
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

    @Override
    public void requestConnectionSpeed(Speed newSpeed) throws BleException {
        if (speed == newSpeed) {
            return;
        }
        try {
            busySemaphore.acquire();
        } catch (InterruptedException e) {
            throw new BleException();
        }
        Timber.i("Requesting connection speed %s", newSpeed);
        try {
            synchronized (this) {
                if (state != State.READY) {
                    throw new BleException(
                            String.format("Cannot request connection speed in state %s", state));
                }
                doneLatch = new CountDownLatch(1);
                state = State.BUSY;

                RequestGattConnectionIntervalTransaction.Speed bitgattSpeed;
                switch (newSpeed) {
                    case LOW_POWER:
                        bitgattSpeed = RequestGattConnectionIntervalTransaction.Speed.LOW;
                        break;
                    case BALANCED:
                    default:
                        bitgattSpeed = RequestGattConnectionIntervalTransaction.Speed.MID;
                        break;
                    case HIGH:
                        bitgattSpeed = RequestGattConnectionIntervalTransaction.Speed.HIGH;
                        break;
                }
                GattTransaction tx = new RequestGattConnectionIntervalTransaction(conn,
                        GattState.REQUEST_CONNECTION_INTERVAL_SUCCESS, bitgattSpeed);
                runTx(tx, result -> {
                    if (result.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                        Timber.i("Requested connection speed %s", newSpeed);
                        speed = newSpeed;
                    } else {
                        Timber.e("Failed to request connection speed: %s", result);
                        // TODO Should this be an error, or ignore?
                    }
                    doneLatch.countDown();
                });
            }
            try {
                doneLatch.await();
            } catch (InterruptedException e) {
                throw new BleException();
            }
            synchronized (this) {
                state = State.READY;
            }
        } finally {
            busySemaphore.release();
        }
    }

    private void handleCommandIndication(TransactionResult result) {
        if (state != State.BUSY) {
            Timber.e("Unexpected command indication in state %s: %s", state, result);
            return;
        }

        byte[] ind = result.getData();
        if (ind == null || ind.length < 1) {
            handleError("Command indication missing data");
            return;
        }
        if (ind[0] == COMMAND_CODE_RESP_FOLLOWS) {
            response = Arrays.copyOfRange(ind, 1, ind.length);
            done();
        } else if (ind[0] == COMMAND_CODE_RESP_LONG) {
            ReadGattCharacteristicTransaction tx = new ReadGattCharacteristicTransaction(
                    conn, GattState.READ_CHARACTERISTIC_SUCCESS, charResponse);
            runTx(tx, longResult -> {
                if (longResult.getResultStatus().equals(TransactionResult.TransactionResultStatus.SUCCESS)) {
                    byte[] longResp = longResult.getData();
                    if (longResp == null) {
                        handleError("Long response missing data");
                        return;
                    }
                    response = longResp;
                    done();
                } else {
                    handleError("Failed to read long response: " + longResult);
                }
            });
        } else {
            handleError("Invalid command indication");
        }
    }

    private void handleDataNotification(TransactionResult result) {
        if (result.getData() == null) {
            Timber.e("Data notification missing data");
            return;
        }
        ByteString dataBlock = dataHandler.handleDataPacket(result.getData());
        if (dataBlock != null) {
            dataListener.handleData(dataBlock);
        }
    }

    private void handleError(String error) {
        state = State.DISCONNECTING;
        Timber.e("Disconnecting after error: %s", error);
        // Unblock the bitgatt transaction queue
        SetClientConnectionStateTransaction tx = new SetClientConnectionStateTransaction(
                conn, GattState.GATT_CONNECTION_STATE_SET_SUCCESSFULLY, GattState.DISCONNECTED);
        runTx(tx, result -> {});
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
        if (COMMAND_CHAR_UUID.equals(result.getCharacteristicUuid())) {
            handleCommandIndication(result);
        } else if (DATA_CHAR_UUID.equals(result.getCharacteristicUuid())) {
            handleDataNotification(result);
        } else {
            Timber.e("Unexpected characteristic change %s", result);
        }
    }

    @Override
    public void onClientConnectionStateChanged(@NonNull TransactionResult result, @NonNull GattConnection connection) {
        if (result.getResultState().equals(GattState.DISCONNECTED)) {
            conn.unregisterConnectionEventListener(this);
            state = State.DISCONNECTED;
            Timber.i("Set disconnected for result state %s", result.getResultState());
            done();
            if (disconnectListener != null) {
                disconnectListener.disconnected();
                disconnectListener = null;
            }
        }
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

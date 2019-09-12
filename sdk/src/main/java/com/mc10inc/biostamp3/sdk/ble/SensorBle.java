package com.mc10inc.biostamp3.sdk.ble;

import com.google.protobuf.ByteString;
import com.mc10inc.biostamp3.sdk.exception.BleException;

/**
 * BioStamp Sensor BLE Interface
 * <p>
 * SensorBle manages the BLE connection to one sensor and performs all GATT operations. An instance
 * of this class is only used to manage a single connection and may not be reused; after the
 * connection is lost or an error occurs a new instance must be constructed to connect to the same
 * sensor again.
 * <p>
 * The life cycle of SensorBle is:
 * <p>
 * Object is constructed and connect() method is called
 * <p>
 * Connection is attempted and either connected() or connectFailed() is called
 * <p>
 * If connected() is called then the connection is successfully established. At this point GATT
 * operations may be performed. Only one operation may be performed at a time; the caller must wait
 * until each operation completes before performing the next operation. InvalidStateException is
 * thrown if an operation is already in progress or the connection has been lost.
 * <p>
 * Every GATT operation over an established connection must succeed; there are no recoverable
 * errors. Listeners for GATT operations are only called after those operations complete
 * successfully. If the connection is lost or an error occurs, then the listener for whatever
 * specific operation was in progress will not be called, but the disconnected() method will be
 * called.
 * <p>
 * When a connection is established and connect() is called, it is guaranteed that disconnect() will
 * be called exactly once when the connection is either intentionally disconnected, is lost, or an
 * error occurs.
 */
public interface SensorBle {
    interface BleDataListener {
        void handleData(ByteString data);
    }

    interface BleDisconnectListener {
        void disconnected();
    }

    void connect(BleDisconnectListener disconnectListener, BleDataListener dataListener)
            throws BleException;

    void disconnect() throws BleException;

    byte[] execute(byte[] command) throws BleException;

    String getSerial() throws BleException;
}

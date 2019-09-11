package com.mc10inc.biostamp3.sdk;

import android.os.Handler;
import android.os.Looper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.task.Task;

import java.util.concurrent.LinkedBlockingQueue;

import timber.log.Timber;

public class BioStampImpl implements BioStamp {
    private enum State {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private BioStampManager bioStampManager;
    private SensorBle ble;
    private ConnectListener connectListener;
    private Handler handler = new Handler(Looper.getMainLooper());
    private SensorThread sensorThread;
    private State state;
    private LinkedBlockingQueue<Task> taskQueue;

    BioStampImpl(BioStampManager bioStampManager, SensorBle ble) {
        this.bioStampManager = bioStampManager;
        this.ble = ble;
        state = State.DISCONNECTED;
    }

    @Override
    public void connect(ConnectListener connectListener) {
        if (state != State.DISCONNECTED) {
            throw new IllegalStateException("Not disconnected");
        }
        this.connectListener = connectListener;
        taskQueue = new LinkedBlockingQueue<>();
        sensorThread = new SensorThread();
        sensorThread.start();
    }

    @Override
    public void disconnect() {

    }

    public Brc3.Response execute(Brc3.Request request) throws BleException {
        byte[] respBytes = ble.execute(request.toByteArray());
        try {
            return Brc3.Response.parseFrom(respBytes);
        } catch (InvalidProtocolBufferException e) {
            Timber.e(e);
            throw new BleException();
        }
    }

    private void executeTask(Task task) {
        if (state == State.CONNECTED) {
            taskQueue.add(task);
        } else {
            task.disconnected();
        }
    }

    public Handler getHandler() {
        return handler;
    }

    private void handleData(ByteString data) {
        Timber.i("Received %d bytes", data.size());
    }

    private void handleDisconnect() {
        Timber.i("Disconnected, stopping sensor thread");
        sensorThread.interrupt();
    }

    @Override
    public void test() {
        executeTask(new Task<Void>(this, (error, result) -> {}) {
            @Override
            public void doTask() {
                try {
                    Brc3.Request req = Brc3.Request.newBuilder()
                            .setCommand(Brc3.Command.TEST_DATA)
                            .setTestData(Brc3.TestDataCommandParam.newBuilder()
                                    .setNumBytes(2000))
                            .build();
                    Brc3.Response resp = execute(req);
                    Timber.i("Resp %s", resp.toString());
                    success(null);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    private class SensorThread extends Thread {
        @Override
        public void run() {
            Timber.i("Connecting to sensor");
            state = BioStampImpl.State.CONNECTING;
            try {
                ble.connect(BioStampImpl.this::handleDisconnect, BioStampImpl.this::handleData);
                Timber.i("Connected to sensor %s", ble.getSerial());
            } catch (BleException e) {
                state = BioStampImpl.State.DISCONNECTED;
                handler.post(() -> connectListener.connectFailed());
                return;
            }
            state = BioStampImpl.State.CONNECTED;
            handler.post(() -> connectListener.connected());

            while (!Thread.interrupted()) {
                Task task;
                try {
                    task = taskQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                task.doTask();
            }
            state = BioStampImpl.State.DISCONNECTED;
            Timber.i("Sensor thread loop done");

            for (Task task : taskQueue) {
                task.disconnected();
            }

            handler.post(() -> connectListener.disconnected());
        }
    }
}

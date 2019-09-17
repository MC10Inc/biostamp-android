package com.mc10inc.biostamp3.sdk;

import android.os.Handler;
import android.os.Looper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.exception.BleException;
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
    private String serial;
    private State state;
    private LinkedBlockingQueue<Task> taskQueue;

    BioStampImpl(BioStampManager bioStampManager, String serial) {
        this.bioStampManager = bioStampManager;
        this.serial = serial;
        state = State.DISCONNECTED;
    }

    @Override
    public void connect(ConnectListener connectListener) {
        if (state != State.DISCONNECTED) {
            throw new IllegalStateException("Not disconnected");
        }
        SensorBle newBle = bioStampManager.getSensorBle(serial);
        if (newBle == null) {
            handler.post(connectListener::connectFailed);
            return;
        }
        ble = newBle;
        this.connectListener = connectListener;
        taskQueue = new LinkedBlockingQueue<>();
        sensorThread = new SensorThread();
        sensorThread.start();
    }

    @Override
    public void disconnect() {

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

    private void handleData(ByteString dataBytes) {
        Brc3.DataMessage dm;
        try {
            dm = Brc3.DataMessage.parseFrom(dataBytes);
        } catch (InvalidProtocolBufferException e) {
            Timber.e(e);
            return;
        }

        if (dm.hasTestDataTwo()) {
            Timber.e("Received %d bytes of test data", dm.getTestDataTwo().getMyDataTwo().size());
        } else {
            Timber.e("Unknown data message: %s", dm);
        }
    }

    private void handleDisconnect() {
        Timber.i("Disconnected, stopping sensor thread");
        sensorThread.interrupt();
    }

    @Override
    public void blinkLed(Listener<Void> listener) {
        executeTask(new Task<Void>(this, listener) {
            @Override
            public void doTask() {
                try {
                    Request.blinkLeds.execute(ble);
                    success(null);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    @Override
    public <TC, TR> void execute(Request<TC, TR> request, TC param, Listener<TR> listener) {
        executeTask(new Task<TR>(this, listener) {
            @Override
            public void doTask() {
                try {
                    TR response = request.execute(ble);
                    success(response);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    @Override
    public void startSensing(SensorConfig sensorConfig, Listener<Void> listener) {
        executeTask(new Task<Void>(this, listener) {
            @Override
            public void doTask() {
                try {
                    double ts = System.currentTimeMillis() / 1000.0;
                    Request.setTime.execute(ble, Brc3.TimeSetCommandParam.newBuilder()
                            .setTimestamp(ts));
                    Request.startSensing.execute(ble, Brc3.SensingStartCommandParam.newBuilder()
                            .setConfig(sensorConfig.getMsg()));
                    success(null);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    @Override
    public void stopSensing(Listener<Void> listener) {
        executeTask(new Task<Void>(this, listener) {
            @Override
            public void doTask() {
                try {
                    Request.stopSensing.execute(ble);
                    success(null);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    @Override
    public void getSensingInfo(Listener<SensingInfo> listener) {
        executeTask(new Task<SensingInfo>(this, listener) {
            @Override
            public void doTask() {
                try {
                    Brc3.SensingGetInfoResponseParam resp = Request.getSensingInfo.execute(ble);
                    success(new SensingInfo(resp));
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

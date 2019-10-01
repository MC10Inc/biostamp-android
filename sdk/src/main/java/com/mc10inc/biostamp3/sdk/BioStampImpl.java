package com.mc10inc.biostamp3.sdk;

import android.os.Handler;
import android.os.Looper;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensingInfo;
import com.mc10inc.biostamp3.sdk.sensing.SensorConfig;
import com.mc10inc.biostamp3.sdk.exception.BleException;
import com.mc10inc.biostamp3.sdk.sensing.Streaming;
import com.mc10inc.biostamp3.sdk.sensing.StreamingListener;
import com.mc10inc.biostamp3.sdk.sensing.StreamingType;
import com.mc10inc.biostamp3.sdk.task.DownloadRecording;
import com.mc10inc.biostamp3.sdk.task.GetRecordingList;
import com.mc10inc.biostamp3.sdk.task.Task;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import timber.log.Timber;

public class BioStampImpl implements BioStamp {
    public interface RecordingPagesListener {
        void handleRecordingPages(List<Brc3.RecordingPage> recordingPages);
    }

    private BioStampManager bioStampManager;
    private SensorBle ble;
    private ConnectListener connectListener;
    private volatile Task currentTask;
    private Handler handler = new Handler(Looper.getMainLooper());
    private volatile RecordingPagesListener recordingPagesListener;
    private SensorThread sensorThread;
    private String serial;
    private volatile State state;
    private Streaming streaming = new Streaming();
    private LinkedBlockingQueue<Task> taskQueue;

    BioStampImpl(BioStampManager bioStampManager, String serial) {
        this.bioStampManager = bioStampManager;
        this.serial = serial;
        state = State.DISCONNECTED;
    }

    @Override
    public String getSerial() {
        return serial;
    }

    @Override
    public State getState() {
        return state;
    }

    private void setState(State state) {
        this.state = state;
        bioStampManager.notifyConnStateChange();
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
        if (ble != null) {
            try {
                ble.disconnect();
            } catch (BleException e) {
                Timber.e(e);
            }
        }
    }

    private void executeTask(Task task) {
        if (state == State.CONNECTED) {
            taskQueue.add(task);
        } else {
            task.disconnected();
        }
    }

    public SensorBle getBle() {
        return ble;
    }

    public Handler getHandler() {
        return handler;
    }

    public void setRecordingPagesListener(RecordingPagesListener recordingPagesListener) {
        this.recordingPagesListener = recordingPagesListener;
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
        } else if (dm.hasStreamingSamples()) {
            streaming.handleStreamingSamples(dm.getStreamingSamples());
        } else if (dm.getRecordingPagesCount() > 0) {
            if (recordingPagesListener != null) {
                RecordingPagesListener l = recordingPagesListener;
                recordingPagesListener = null;
                l.handleRecordingPages(dm.getRecordingPagesList());
            }
        } else {
            Timber.e("Unknown data message: %s", dm);
        }
    }

    private void handleDisconnect() {
        Timber.i("Disconnected, stopping sensor thread");
        sensorThread.interrupt();
    }

    @Override
    public void cancelTask() {
        Task task = currentTask;
        if (task != null) {
            task.cancel();
        }
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

    @Override
    public void startStreaming(StreamingType type, Listener<Void> listener) {
        executeTask(new Task<Void>(this, listener) {
            @Override
            public void doTask() {
                try {
                    Brc3.StreamingType msgType = type.getMsgType();
                    Brc3.StreamingStartResponseParam resp = Request.startStreaming.execute(ble,
                            Brc3.StreamingStartCommandParam.newBuilder().setType(msgType));
                    streaming.setStreamingInfo(type, resp.getInfo());
                    success(null);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    @Override
    public void stopStreaming(StreamingType type, Listener<Void> listener) {
        executeTask(new Task<Void>(this, listener) {
            @Override
            public void doTask() {
                try {
                    Brc3.StreamingType msgType = type.getMsgType();
                    Request.stopStreaming.execute(ble,
                            Brc3.StreamingStopCommandParam.newBuilder().setType(msgType));
                    success(null);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    @Override
    public void addStreamingListener(StreamingType type, StreamingListener streamingListener) {
        streaming.addStreamingListener(type, streamingListener);
    }

    @Override
    public void removeStreamingListener(StreamingListener streamingListener) {
        streaming.removeStreamingListener(streamingListener);
    }

    @Override
    public void getRecordingList(Listener<List<RecordingInfo>> listener) {
        executeTask(new GetRecordingList(this, listener));
    }

    @Override
    public void clearAllRecordings(Listener<Void> listener) {
        executeTask(new Task<Void>(this, listener) {
            @Override
            public void doTask() {
                try {
                    Request.clearAllRecordings.execute(ble);
                    success(null);
                } catch (BleException e) {
                    error(e);
                }
            }
        });
    }

    @Override
    public void downloadRecording(RecordingInfo recording, Listener<Void> listener,
                                  ProgressListener progressListener) {
        executeTask(new DownloadRecording(this, listener, progressListener, recording));
    }

    private class SensorThread extends Thread {
        @Override
        public void run() {
            Timber.i("Connecting to sensor");
            setState(BioStamp.State.CONNECTING);
            try {
                ble.connect(BioStampImpl.this::handleDisconnect, BioStampImpl.this::handleData);
                Timber.i("Connected to sensor %s", ble.getSerial());
            } catch (BleException e) {
                setState(BioStamp.State.DISCONNECTED);
                handler.post(() -> connectListener.connectFailed());
                return;
            }
            setState(BioStamp.State.CONNECTED);
            handler.post(() -> connectListener.connected());

            while (!Thread.interrupted()) {
                try {
                    currentTask = taskQueue.take();
                } catch (InterruptedException e) {
                    break;
                }
                currentTask.doTask();
                currentTask = null;
            }
            currentTask = null;
            setState(BioStamp.State.DISCONNECTED);
            Timber.i("Sensor thread loop done");

            for (Task task : taskQueue) {
                task.disconnected();
            }

            handler.post(() -> connectListener.disconnected());
        }
    }
}

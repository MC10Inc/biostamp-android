package com.mc10inc.biostamp3.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.exception.BleException;
import com.mc10inc.biostamp3.sdk.exception.RequestException;

import java.util.List;

import timber.log.Timber;

public class Request<TC, TR> {
    private interface CommandParamSetter<TC> {
        void setCommandParam(Brc3.Request.Builder builder, TC param);
    }

    private interface ResponseParamGetter<TR> {
        TR getResponseParam(Brc3.Response response);
    }

    private Brc3.Command command;
    private CommandParamSetter<TC> commandParamSetter;
    private ResponseParamGetter<TR> responseParamGetter;

    private Request(Brc3.Command command,
                   CommandParamSetter<TC> commandParamSetter,
                   ResponseParamGetter<TR> responseParamGetter) {
        this.command = command;
        this.commandParamSetter = commandParamSetter;
        this.responseParamGetter = responseParamGetter;
    }

    public TR execute(SensorBle ble) throws BleException {
        return execute(ble, null);
    }

    public TR execute(SensorBle ble, TC param) throws BleException, RequestException {
        return execute(ble, param, null);
    }

    public TR execute(SensorBle ble, TC param, List<byte[]> writeFastData) throws BleException, RequestException {
        Brc3.Request.Builder reqBuilder = Brc3.Request.newBuilder();
        reqBuilder.setCommand(command);
        if (commandParamSetter != null) {
            commandParamSetter.setCommandParam(reqBuilder, param);
        }

        byte[] respBytes = ble.execute(reqBuilder.build().toByteArray(), writeFastData);
        Brc3.Response resp;
        try {
            resp = Brc3.Response.parseFrom(respBytes);
        } catch (InvalidProtocolBufferException e) {
            Timber.e(e);
            throw new BleException();
        }

        if (resp.getError() != Brc3.ErrorCode.SUCCESS) {
            throw RequestException.forResponse(resp);
        }

        if (responseParamGetter != null) {
            return responseParamGetter.getResponseParam(resp);
        } else {
            return null;
        }
    }

    public static final Request<Void, Void> blinkLeds =
            new Request<>(Brc3.Command.BLINK_LEDS, null, null);

    public static final Request<Brc3.TestDataCommandParam.Builder, Void> testData =
            new Request<>(Brc3.Command.TEST_DATA, Brc3.Request.Builder::setTestData, null);

    public static final Request<Void, Brc3.TimeGetResponseParam> getTime =
            new Request<>(Brc3.Command.TIME_GET, null, Brc3.Response::getTimeGet);

    public static final Request<Brc3.TimeSetCommandParam.Builder, Void> setTime =
            new Request<>(Brc3.Command.TIME_SET, Brc3.Request.Builder::setTimeSet, null);

    public static final Request<Brc3.SensingStartCommandParam.Builder, Void> startSensing =
            new Request<>(Brc3.Command.SENSING_START, Brc3.Request.Builder::setSensingStart, null);

    public static final Request<Void, Void> stopSensing =
            new Request<>(Brc3.Command.SENSING_STOP, null, null);

    public static final Request<Void, Brc3.SensingGetInfoResponseParam> getSensingInfo =
            new Request<>(Brc3.Command.SENSING_GET_INFO, null, Brc3.Response::getSensingGetInfo);

    public static final Request<Brc3.StreamingStartCommandParam.Builder, Brc3.StreamingStartResponseParam> startStreaming =
            new Request<>(Brc3.Command.STREAMING_START,
                    Brc3.Request.Builder::setStreamingStart, Brc3.Response::getStreamingStart);

    public static final Request<Brc3.StreamingStopCommandParam.Builder, Void> stopStreaming =
            new Request<>(Brc3.Command.STREAMING_STOP, Brc3.Request.Builder::setStreamingStop, null);

    public static final Request<Brc3.RecordingGetInfoCommandParam.Builder, Brc3.RecordingGetInfoResponseParam> getRecordingInfo =
            new Request<>(Brc3.Command.RECORDING_GET_INFO, Brc3.Request.Builder::setRecordingGetInfo, Brc3.Response::getRecordingGetInfo);

    public static final Request<Void, Void> clearAllRecordings =
            new Request<>(Brc3.Command.CLEAR_ALL_RECORDINGS, null, null);

    public static final Request<Brc3.RecordingReadCommandParam.Builder, Void> readRecording =
            new Request<>(Brc3.Command.RECORDING_READ, Brc3.Request.Builder::setRecordingRead, null);

    public static final Request<Void, Brc3.SystemStatusResponseParam> getSystemStatus =
            new Request<>(Brc3.Command.SYSTEM_STATUS, null, Brc3.Response::getSystemStatus);

    public static final Request<Brc3.UploadStartCommandParam.Builder, Brc3.UploadStartResponseParam> uploadStart =
            new Request<>(Brc3.Command.UPLOAD_START, Brc3.Request.Builder::setUploadStart, Brc3.Response::getUploadStart);

    public static final Request<Brc3.UploadWritePageCommandParam.Builder, Void> uploadWritePage =
            new Request<>(Brc3.Command.UPLOAD_WRITE_PAGE, Brc3.Request.Builder::setUploadWritePage, null);

    public static final Request<Void, Void> uploadWritePagesFast =
            new Request<>(Brc3.Command.UPLOAD_WRITE_PAGES_FAST, null, null);

    public static final Request<Void, Void> uploadFinish =
            new Request<>(Brc3.Command.UPLOAD_FINISH, null, null);

    public static final Request<Void, Void> loadFirmwareImage =
            new Request<>(Brc3.Command.LOAD_FIRMWARE_IMAGE, null, null);

    public static final Request<Void, Void> reset =
            new Request<>(Brc3.Command.RESET, null, null);

    public static final Request<Void, Void> powerOff =
            new Request<>(Brc3.Command.POWER_OFF, null, null);

    public static final Request<Void, Brc3.VersionGetResponseParam> getVersion =
            new Request<>(Brc3.Command.VERSION_GET, null, Brc3.Response::getVersionGet);

    public static final Request<Brc3.AnnotateCommandParam.Builder, Brc3.AnnotateResponseParam> annotate =
            new Request<>(Brc3.Command.ANNOTATE, Brc3.Request.Builder::setAnnotate, Brc3.Response::getAnnotate);

    public static final Request<Void, Brc3.FaultGetInfoResponseParam> getFaultInfo =
            new Request<>(Brc3.Command.FAULT_GET_INFO, null, Brc3.Response::getFaultGetInfo);

    public static final Request<Void, Void> clearFaultLog =
            new Request<>(Brc3.Command.FAULT_LOG_CLEAR, null, null);

    public static final Request<Brc3.FaultLogReadCommandParam.Builder, Brc3.FaultLogReadResponseParam> readFaultLog =
            new Request<>(Brc3.Command.FAULT_LOG_READ, Brc3.Request.Builder::setFaultLogRead, Brc3.Response::getFaultLogRead);
}

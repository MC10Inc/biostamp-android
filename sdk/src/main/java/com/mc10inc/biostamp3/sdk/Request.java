package com.mc10inc.biostamp3.sdk;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mc10inc.biostamp3.sdk.ble.SensorBle;
import com.mc10inc.biostamp3.sdk.exception.BleException;
import com.mc10inc.biostamp3.sdk.exception.RequestException;

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

    public TR execute(SensorBle ble) throws BleException, RequestException {
        return execute(ble, null);
    }

    public TR execute(SensorBle ble, TC param) throws BleException, RequestException {
        Brc3.Request.Builder reqBuilder = Brc3.Request.newBuilder();
        reqBuilder.setCommand(command);
        if (commandParamSetter != null) {
            commandParamSetter.setCommandParam(reqBuilder, param);
        }

        byte[] respBytes = ble.execute(reqBuilder.build().toByteArray());
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
}

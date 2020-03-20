package com.mc10inc.biostamp3.sdk.exception;

import com.mc10inc.biostamp3.sdk.Brc3;

public class RequestException extends BleException {
    private Brc3.ErrorCode errorCode;
    private String errorMessage;

    public RequestException(Brc3.Response response) {
        super(describe(response.getError(), response.getErrorMessage()));
        this.errorCode = response.getError();
        this.errorMessage = response.getErrorMessage();
    }

    private static String describe(Brc3.ErrorCode errorCode, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(errorCode.name());
        if (errorMessage != null) {
            sb.append(": ");
            sb.append(errorMessage);
        }
        return sb.toString();
    }

    public Brc3.ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public static RequestException forResponse(Brc3.Response response) {
        switch (response.getError()) {
            case UNSUPPORTED:
                return new SensorUnsupportedException(response);
            case FAIL:
                return new SensorFailException(response);
            case INVALID_PARAMETER:
                return new SensorInvalidParameterException(response);
            case TIME_NOT_SET:
                return new SensorTimeNotSetException(response);
            case CANNOT_START:
                return new SensorCannotStartException(response);
            case CANNOT_STOP:
                return new SensorCannotStopException(response);
            case UPLOAD_FAILED:
                return new SensorUploadFailedException(response);
            case RECORDING_NOT_FOUND:
                return new SensorRecordingNotFoundException(response);
            case MEMORY_FULL:
                return new SensorMemoryFullException(response);
            default:
                return new RequestException(response);
        }
    }
}

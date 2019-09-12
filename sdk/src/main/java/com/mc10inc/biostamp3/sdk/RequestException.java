package com.mc10inc.biostamp3.sdk;

public class RequestException extends BleException {
    private Brc3.ErrorCode errorCode;
    private String errorMessage;

    public RequestException(Brc3.ErrorCode errorCode, String errorMessage) {
        super(describe(errorCode, errorMessage));
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
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
}

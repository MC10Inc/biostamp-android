package com.mc10inc.biostamp3.sdk.exception;

import com.mc10inc.biostamp3.sdk.Brc3;

public class SensorInvalidParameterException extends RequestException {
    SensorInvalidParameterException(Brc3.Response response) {
        super(response);
    }
}

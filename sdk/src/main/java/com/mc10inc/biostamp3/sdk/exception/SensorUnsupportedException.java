package com.mc10inc.biostamp3.sdk.exception;

import com.mc10inc.biostamp3.sdk.Brc3;

public class SensorUnsupportedException extends RequestException {
    SensorUnsupportedException(Brc3.Response response) {
        super(response);
    }
}

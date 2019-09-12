package com.mc10inc.biostamp3.sdk.exception;

import com.mc10inc.biostamp3.sdk.Brc3;

public class SensorMemoryFullException extends RequestException {
    SensorMemoryFullException(Brc3.Response response) {
        super(response);
    }
}

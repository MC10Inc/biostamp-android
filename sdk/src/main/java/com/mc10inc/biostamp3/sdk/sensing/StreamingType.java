package com.mc10inc.biostamp3.sdk.sensing;

import com.mc10inc.biostamp3.sdk.Brc3;

public enum StreamingType {
    AD5940(Brc3.StreamingType.AD5940),
    AFE4900(Brc3.StreamingType.AFE4900),
    ENVIRONMENT(Brc3.StreamingType.ENVIRONMENT),
    MOTION(Brc3.StreamingType.MOTION),
    ROTATION(Brc3.StreamingType.MOTION);

    Brc3.StreamingType msgType;

    StreamingType(Brc3.StreamingType msgType) {
        this.msgType = msgType;
    }

    public Brc3.StreamingType getMsgType() {
        return msgType;
    }
}

package com.mc10inc.biostamp3.sdk.task;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampImpl;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.Request;
import com.mc10inc.biostamp3.sdk.exception.BleException;
import com.mc10inc.biostamp3.sdk.exception.SensorRecordingNotFoundException;
import com.mc10inc.biostamp3.sdk.recording.RecordingInfo;

import java.util.ArrayList;
import java.util.List;

public class GetRecordingList extends Task<List<RecordingInfo>> {
    public GetRecordingList(BioStampImpl bs, BioStamp.Listener<List<RecordingInfo>> taskListener) {
        super(bs, taskListener);
    }

    @Override
    public void doTask() {
        try {
            List<RecordingInfo> recs = new ArrayList<>();
            int i = 0;
            while (true) {
                try {
                    Brc3.RecordingGetInfoResponseParam resp = Request.getRecordingInfo.execute(
                            bs.getBle(),
                            Brc3.RecordingGetInfoCommandParam.newBuilder().setIndex(i));
                    recs.add(new RecordingInfo(resp.getInfo(), bs.getBle().getSerial()));
                    i++;
                } catch (SensorRecordingNotFoundException e) {
                    break;
                }
            }
            success(recs);
        } catch (BleException e) {
            error(e);
        }
    }
}

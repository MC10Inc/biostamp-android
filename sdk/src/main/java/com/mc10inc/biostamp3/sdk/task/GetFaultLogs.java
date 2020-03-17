package com.mc10inc.biostamp3.sdk.task;

import com.mc10inc.biostamp3.sdk.BioStamp;
import com.mc10inc.biostamp3.sdk.BioStampImpl;
import com.mc10inc.biostamp3.sdk.Brc3;
import com.mc10inc.biostamp3.sdk.Request;
import com.mc10inc.biostamp3.sdk.exception.BleException;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;


public class GetFaultLogs extends Task<List<String>> {
    public GetFaultLogs(BioStampImpl bs, BioStamp.Listener<List<String>> taskListener) {
        super(bs, taskListener);
    }

    @Override
    public void doTask() {
        List<String> faults = new ArrayList<>();
        try {
            int i = 0;
            while (true) {
                Brc3.FaultLogReadResponseParam resp = Request.readFaultLog.execute(
                        bs.getBle(),
                        Brc3.FaultLogReadCommandParam.newBuilder().setIndex(i));
                if (resp.hasFaultInfo()) {
                    faults.add(resp.getFaultInfo().toString());
                } else {
                    break;
                }
                i++;
            }
            success(faults);
        } catch (BleException e) {
            error(e);
        }
    }
}

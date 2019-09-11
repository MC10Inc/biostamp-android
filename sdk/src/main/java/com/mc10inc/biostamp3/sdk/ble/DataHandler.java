package com.mc10inc.biostamp3.sdk.ble;

import com.google.protobuf.ByteString;

import timber.log.Timber;

class DataHandler {
    private ByteString block = null;
    private int currentSeq = -1;

    ByteString handleDataPacket(byte[] data) {
        if (data.length < 4) {
            Timber.e("Invalid data packet of length %d", data.length);
            currentSeq = -1;
            return null;
        }

        int seqAndFinalFlag = (data[0] & 0xff) + ((data[1] & 0xff) << 8);
        boolean finalFlag = (seqAndFinalFlag & 0x8000) > 0;
        int seq = seqAndFinalFlag & 0x7fff;
        int crc = (data[2] & 0xff) + ((data[3] & 0xff) << 8);
        ByteString payload = ByteString.copyFrom(data, 4, data.length - 4);

        if (seq == 0) {
            currentSeq = 0;
            block = payload;
        } else {
            if (seq != currentSeq + 1) {
                Timber.e("Invalid sequence %d, expected %d", seq, currentSeq);
                currentSeq = -1;
                return null;
            }
            block = block.concat(payload);
            currentSeq = seq;
        }

        if (finalFlag) {
            currentSeq = -1;
            if (crc != calculateCrc(block)) {
                Timber.e("Invalid CRC of %d bytes", block.size());
                return null;
            }
            return block;
        }

        return null;
    }

    private static int calculateCrc(ByteString bytes) {
        int crc = 0xffff;
        int polynomial = 0x1021;

        ByteString.ByteIterator iter = bytes.iterator();
        while (iter.hasNext()) {
            byte b = iter.nextByte();
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        crc &= 0xffff;

        return crc;
    }
}

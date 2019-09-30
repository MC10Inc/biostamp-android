package com.mc10inc.biostamp3.sdkexample;

import java.io.IOException;
import java.io.OutputStream;

public class NumberWriter {
    private static final int MAX_DECIMALS = 20;
    private static final long[] POWER_OF_10;
    static {
        POWER_OF_10 = new long[MAX_DECIMALS];
        for (int i = 0; i < MAX_DECIMALS; i++) {
            POWER_OF_10[i] = (long)Math.pow(10, i);
        }
    }

    private static final byte[] DECIMAL_DIGITS =
            new byte[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};

    public static void write(double doubleValue, int dec, OutputStream os)
            throws IOException {
        long v = Math.round(doubleValue * POWER_OF_10[dec]);
        boolean negative = v < 0;
        if (negative) {
            v = -v;
        }

        byte[] s = new byte[30];
        int si = s.length - 1;
        while (v > 0 || (s.length - si - 1) < (dec + 2)) {
            s[si--] = DECIMAL_DIGITS[(int)(v % 10)];
            v /= 10;
            if (dec > 0 && si == s.length - dec - 1) {
                s[si--] = '.';
            }
        }
        if (negative) {
            s[si--] = '-';
        }
        si++;
        os.write(s, si, s.length - si);
    }
}

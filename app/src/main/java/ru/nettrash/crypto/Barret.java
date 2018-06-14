package ru.nettrash.crypto;

import ru.nettrash.math.BigInteger;

/**
 * Created by nettrash on 12.01.2018.
 */

public class Barret {

    public BigInteger r2;
    public BigInteger q3;
    public BigInteger mu;
    public BigInteger m;

    public Barret(BigInteger biQ) throws Exception {
        r2 = new BigInteger(1).dlShift(2 * biQ.t);
        q3 = new BigInteger();
        mu = r2.divide(biQ);
        m = biQ;
    }

    public BigInteger reduce(BigInteger v) {
        BigInteger x = v;
        r2 = x.drShift(m.t-1);
        if (x.t > m.t+1) {
            x.t = m.t + 1;
            x.clamp();
        }
        q3 = mu.multiplyUpper(r2, m.t+1);
        r2 = m.multiplyLower(q3, m.t+1);
        while (x.compareTo(r2) < 0) {
            x.dAddOffset(1, m.t+1);
        }
        x = x.subtract(r2);
        while (x.compareTo(m) >= 0) {
            x = x.subtract(m);
        }
        return x;
    }
}

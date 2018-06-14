package ru.nettrash.crypto;

import ru.nettrash.math.BigInteger;
import ru.nettrash.util.Arrays;

/*
 * Created by nettrash on 12.01.2018.
 */

public class CurveFP {

    BigInteger q;
    FieldElementFP a;
    FieldElementFP b;
    public PointFP infinity;
    Barret reducer;

    CurveFP(BigInteger biQ, BigInteger biA, BigInteger biB) throws Exception {
        q = biQ;
        a = new FieldElementFP(biQ, biA);
        b = new FieldElementFP(biQ, biB);
        infinity = new PointFP(this);
        reducer = new Barret(biQ);
    }

    public PointFP decodePoint(byte[] data) throws Exception {
        switch (data[0]) {
            case 0:
                return infinity;
    		/*case 2,3: //compressed, compressed
			let tilde = data[0] & 1
			let X1 = BigInteger(Data([UInt8](data[1...data.count - 1])))
			return try! decompressPoint(tilde, X1)*/
            case 4:
            case 6:
            case 7: //uncompressed, hybrid, hybrid
                int len = (data.length - 1) / 2;
                FieldElementFP x = new FieldElementFP(q, new BigInteger(Arrays.subarray(data, 1, len)));
                FieldElementFP y = new FieldElementFP(q, new BigInteger(Arrays.subarray(data, len + 1, len)));
                return new PointFP(this, x, y);
            default:
                return null;
        }
    }

    public FieldElementFP fromBigInteger(BigInteger v) {
        return new FieldElementFP(q, v);
    }

    public BigInteger reduce(BigInteger r) {
        return reducer.reduce(r);
    }
}

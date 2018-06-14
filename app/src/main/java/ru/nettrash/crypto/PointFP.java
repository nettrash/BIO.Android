package ru.nettrash.crypto;

import ru.nettrash.math.BigInteger;

/**
 * Created by nettrash on 12.01.2018.
 */

public class PointFP {

    CurveFP curve;
    FieldElementFP x;
    FieldElementFP y;
    BigInteger z;
    BigInteger zinv;
    boolean compressed;

    public PointFP(CurveFP c) throws Exception {
        curve = c;
        x = null;
        y = null;
        z = new BigInteger(1);
        zinv = null;
        compressed = false;
    }

    public PointFP(CurveFP c, FieldElementFP biX, FieldElementFP biY) throws Exception {
        curve = c;
        x = biX;
        y = biY;
        z = new BigInteger(1);
        zinv = null;
        compressed = false;
    }

    public PointFP(CurveFP c, FieldElementFP biX, FieldElementFP biY, boolean compress) throws Exception {
        curve = c;
        x = biX;
        y = biY;
        z = new BigInteger(1);
        zinv = null;
        compressed = compress;
    }

    public PointFP(CurveFP c, BigInteger biZ) throws Exception {
        curve = c;
        x = null;
        y = null;
        z = biZ;
        zinv = null;
        compressed = false;
    }

    public PointFP(CurveFP c, FieldElementFP biX, FieldElementFP biY, BigInteger biZ, boolean compress) throws Exception {
        curve = c;
        x = biX;
        y = biY;
        z = biZ;
        zinv = null;
        compressed = compress;
    }

    private boolean _isInfinity() {
        if (x == null && y == null) { return true; }
        return z.equals(new BigInteger(0)) && !y.toBigInteger().equals(new BigInteger(0));
    }

    public PointFP negate() throws Exception {
        return new PointFP(curve, x, y.negate(), compressed);
    }

    public PointFP multiply(BigInteger k) throws Exception {
        if (_isInfinity()) { return this; }
        if (k.signum() == 0) { return curve.infinity; }
        BigInteger e = k.clone();
        BigInteger h = e.multiply(new BigInteger(3));
        PointFP neg = negate();
        PointFP R = this;
        for (int i=h.bitLength()-2; i>0; i--) {
            R = R.twice();
            boolean hBit = h.testBit(i);
            boolean eBit = e.testBit(i);

            if (hBit != eBit) {
                R = R.add(hBit ? this : neg);
            }
        }
        return R;
    }

    public PointFP twice() throws Exception {
        if (_isInfinity()) { return this; }
        if (y.toBigInteger().signum() == 0) { return curve.infinity; }
        BigInteger three = new BigInteger(3);
        BigInteger x1 = x.toBigInteger();
        BigInteger y1 = y.toBigInteger();
        BigInteger y1z1 = y1.multiply(z);
        BigInteger y1sqz1 = y1z1.multiply(y1).mod(curve.q);
        BigInteger a = curve.a.toBigInteger();
        BigInteger w = x1.square().multiply(three);
        if (!(new BigInteger(0)).equals(a)) {
            w = w.add(z.square().multiply(a));
        }
        w = w.mod(curve.q);
        BigInteger x3 = w.square().subtract(x1.shiftLeft(3).multiply(y1sqz1)).shiftLeft(1).multiply(y1z1).mod(curve.q);
        BigInteger y3 = w.multiply(three).multiply(x1).subtract(y1sqz1.shiftLeft(1)).shiftLeft(2).multiply(y1sqz1).subtract(w.square().multiply(w)).mod(curve.q);
        BigInteger z3 = y1z1.square().multiply(y1z1).shiftLeft(3).mod(curve.q);
        return new PointFP(curve, curve.fromBigInteger(x3), curve.fromBigInteger(y3), z3, false);
    }

    public PointFP add(PointFP b) throws Exception {
        if (_isInfinity()) { return b; }
        if (b._isInfinity()) { return this; }
        BigInteger u = b.y.toBigInteger().multiply(z).subtract(y.toBigInteger().multiply(b.z)).mod(curve.q);
        BigInteger v = b.x.toBigInteger().multiply(z).subtract(x.toBigInteger().multiply(b.z)).mod(curve.q);
        if (new BigInteger(0).equals(v)) {
            if (new BigInteger(0).equals(u)) {
                return twice();
            }
            return curve.infinity;
        }
        BigInteger three = new BigInteger(3);
        BigInteger x1 = x.toBigInteger();
        BigInteger y1 = y.toBigInteger();
        //let x2 = b.x!.toBigInteger()
        //let y2 = b.y!.toBigInteger()
        BigInteger v2 = v.square();
        BigInteger v3 = v2.multiply(v);
        BigInteger x1v2 = x1.multiply(v2);
        BigInteger zu2 = u.square().multiply(z);

        BigInteger x3 = zu2.subtract(x1v2.shiftLeft(1)).multiply(b.z).subtract(v3).multiply(v).mod(curve.q);
        BigInteger y3 = x1v2.multiply(three).multiply(u).subtract(y1.multiply(v3)).subtract(zu2.multiply(u)).multiply(b.z).add(u.multiply(v3)).mod(curve.q);
        BigInteger z3 = v3.multiply(z).multiply(b.z).mod(curve.q);

        return new PointFP(curve, curve.fromBigInteger(x3), curve.fromBigInteger(y3), z3, false);
    }

    public FieldElementFP getX() {
        if (zinv == null) {
            zinv = z.modInverse(curve.q);
        }
        BigInteger r = x.toBigInteger().multiply(zinv);
        r = curve.reduce(r);
        return curve.fromBigInteger(r);
    }

    public FieldElementFP getY() {
        if (zinv == null) {
            zinv = z.modInverse(curve.q);
        }
        BigInteger r = y.toBigInteger().multiply(zinv);
        r = curve.reduce(r);
        return curve.fromBigInteger(r);
    }
}

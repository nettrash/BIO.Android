package ru.nettrash.crypto;

import ru.nettrash.math.BigInteger;
import ru.nettrash.util.Arrays;

/**
 * Created by nettrash on 12.01.2018.
 */

public class EllipticCurve {

    public CurveFP curve;
    public BigInteger N;
    public BigInteger H;
    public PointFP G;

    public EllipticCurve() throws Exception {
        BigInteger p = new BigInteger(Arrays.hexStringToByteArray("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEFFFFFC2F"));
        BigInteger a = new BigInteger(0);
        BigInteger b = new BigInteger(7);
        N = new BigInteger(Arrays.hexStringToByteArray("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFEBAAEDCE6AF48A03BBFD25E8CD0364141"));
        H = new BigInteger(1);
        curve = new CurveFP(p, a, b);
        G = curve.decodePoint(Arrays.hexStringToByteArray("04" +
                "79BE667EF9DCBBAC55A06295CE870B07029BFCDB2DCE28D959F2815B16F81798" +
                "483ADA7726A3C4655DA4FBFC0E1108A8FD17B448A68554199C47D08FFB10D4B8"));
    }

    public static int[] integerToBytes(BigInteger i, int len) {
        int[] bytes = i.toByteArrayUnsigned();
        if (len < bytes.length) {
            int[] b = new int[len];
            System.arraycopy(bytes, bytes.length-len, b, 0, len);
            bytes = b;
        } else {
            while (len > bytes.length) {
                bytes = Arrays.shifted(bytes, 0);
            }
        }
        return bytes;
    }

}

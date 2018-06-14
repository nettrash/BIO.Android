package ru.nettrash.crypto;

import ru.nettrash.math.BigInteger;

/**
 * Created by nettrash on 12.01.2018.
 */

public class FieldElementFP {

    private BigInteger Q;
    private BigInteger X;

    public FieldElementFP(BigInteger q, BigInteger x) {
        Q = q;
        X = x;
    }

    public BigInteger toBigInteger() {
        return X;
    }

    public FieldElementFP negate() throws Exception{
        return new FieldElementFP(Q, X.negate().mod(Q));
    }
}

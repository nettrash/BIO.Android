package ru.nettrash.bio;

import ru.nettrash.math.BigInteger;

/**
 * Created by nettrash on 14.06.2018.
 */

public final class bioTransactionOutput {

    public BigInteger Amount;
    public int[] ScriptedAddress;
    public long Satoshi;

    public bioTransactionOutput(int[] ScriptedAddress, BigInteger Amount, long Satoshi) {
        this.ScriptedAddress = ScriptedAddress.clone();
        this.Amount = Amount.clone();
        this.Satoshi = Satoshi;
    }
}

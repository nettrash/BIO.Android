package ru.nettrash.bio;

import ru.nettrash.util.Arrays;

/**
 * Created by nettrash on 14.06.2018.
 */

public final class bioTransactionInput {

    public bioTransactionInputOutpoint Outpoint;
    public int[] Script;
    public Long Sequence;

    public bioTransactionInput(String Address, String Hash, Integer Index, String sScript, Integer lockTime) {
        Outpoint = new bioTransactionInputOutpoint(Address, Hash, Index);
        Script = Arrays.toUnsignedByteArray(Arrays.hexStringToByteArray(sScript));
        Sequence = lockTime == 0 ? 4294967295L : 0;

    }

    public bioTransactionInput(String Address, String Hash, Integer Index, int[] Script, Integer lockTime) {
        Outpoint = new bioTransactionInputOutpoint(Address, Hash, Index);
        this.Script = Script.clone();
        Sequence = lockTime == 0 ? 4294967295L : 0;
    }
}

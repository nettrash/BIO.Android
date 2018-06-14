package ru.nettrash.bio;

/**
 * Created by nettrash on 14.06.2018.
 */

public final class bioTransactionInputOutpoint {

    public String Address;
    public String Hash;
    public Integer Index;

    public bioTransactionInputOutpoint(String Address, String Hash, Integer Index) {
        this.Address = Address;
        this.Hash = Hash;
        this.Index = Index;
    }
}

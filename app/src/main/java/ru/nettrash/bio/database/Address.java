package ru.nettrash.bio.database;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

/**
 * Created by nettrash on 21.01.2018.
 */

@Entity
public class Address {
    @PrimaryKey
    @NonNull
    private String address;

    @ColumnInfo(name = "type")
    private short addressType; //0 - for Input, 1 - for Charge

    @ColumnInfo(name = "compressed")
    private boolean compressed;

    @ColumnInfo(name = "privateKey")
    @TypeConverters(ConvertIntArrayToString.class)
    private int[] privateKey;

    @ColumnInfo(name = "publicKey")
    @TypeConverters(ConvertIntArrayToString.class)
    private int[] publicKey;

    @ColumnInfo(name = "wif")
    private String wif;

    // Getters and setters are ignored for brevity,
    // but they're required for Room to work.
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public short getAddressType() {
        return addressType;
    }

    public void setAddressType(short addressType) {
        this.addressType = addressType;
    }

    public boolean getCompressed() {
        return compressed;
    }

    public void setCompressed(boolean compressed) {
        this.compressed = compressed;
    }

    public int[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(int[] privateKey) {
        this.privateKey = privateKey.clone();
    }

    public int[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(int[] publicKey) {
        this.publicKey = publicKey.clone();
    }

    public String getWif() {
        return wif;
    }

    public void setWif(String wif) {
        this.wif = wif;
    }
}
package ru.nettrash.bio.models;

/**
 * Created by nettrash on 18.01.2018.
 */

import android.arch.persistence.room.Room;
import android.content.Context;

import org.jetbrains.annotations.Contract;

import java.util.List;
import ru.nettrash.bio.database.Address;
import ru.nettrash.bio.database.AddressDao;
import ru.nettrash.bio.database.BIODatabase;
import ru.nettrash.bio.bioWallet;

public final class rootModel extends baseModel {

    private BIODatabase database;
    private Double balance;
    private Double sellRate;
    private Double buyRate;
    private String buyOpKey;

    private void _init(Context context) {
        database = BIODatabase.getBIODatabase(context);
    }

    public rootModel(Context context) {

        _init(context);

    }

    public boolean firstRun() {
        return database.addressDao().count() == 0;
    }

    public void storeWallet(bioWallet wallet, short nWalletType) throws Exception {
        for (Address a: getAddresses()) {
            if (a.getAddress().equals(wallet.Address))
                return;
        }
        Address address = new Address();
        address.setAddress(wallet.Address);
        address.setAddressType(nWalletType);
        address.setCompressed(wallet.Compressed);
        address.setPrivateKey(wallet.PrivateKey);
        address.setPublicKey(wallet.PublicKey);
        address.setWif(wallet.WIF);
        database.addressDao().insertAll(address);
    }

    public List<Address> getAddresses() throws Exception { return database.addressDao().getAll(); }

    public List<Address> getAddressesForInput() throws Exception { return database.addressDao().findOnlyIncoming(); }

    public Address getAddressForInput() throws Exception {
        List<Address> incoming = database.addressDao().findOnlyIncoming();
        Address[] addresses = incoming.toArray(new Address[0]);
        return addresses[addresses.length-1];
    }

    public void setBalance(Double value) { balance = value; }

    @Contract(pure = true)
    public Double getBalance() { return balance; }

    public void setSellRate(Double value) { sellRate = value; }

    @Contract(pure = true)
    public Double getSellRate() { return sellRate; }

    public void setBuyRate(Double value) { buyRate = value; }

    @Contract(pure = true)
    public Double getBuyRate() { return buyRate; }

    @Contract(pure = true)
    public String getBuyOpKey() { return buyOpKey; }

    public void setBuyOpKey(String value) { buyOpKey = value; }

}

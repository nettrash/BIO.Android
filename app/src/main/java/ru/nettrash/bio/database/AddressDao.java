package ru.nettrash.bio.database;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by nettrash on 21.01.2018.
 */

@Dao
public interface AddressDao {
    @Query("SELECT * FROM address")
    List<Address> getAll();

    @Query("SELECT * FROM address WHERE address IN (:addresses)")
    List<Address> loadAllByAddresses(String[] addresses);

    @Query("SELECT * FROM address WHERE type = :type")
    Address loadAllByType(short type);

    @Query("SELECT * FROM address WHERE type = 0")
    List<Address> findOnlyIncoming();

    @Query("SELECT COUNT(*) FROM address")
    long count();

    @Insert
    void insertAll(Address... addresses);

    @Delete
    void delete(Address address);
}

package ru.nettrash.bio.database;

import android.arch.persistence.room.TypeConverter;
import android.support.annotation.NonNull;
import android.util.Base64;

import org.jetbrains.annotations.Contract;
import ru.nettrash.util.Arrays;

/**
 * Created by nettrash on 21.01.2018.
 */

public final class ConvertIntArrayToString {

    @NonNull
    @Contract(pure = true)
    @TypeConverter
    public String fromIntArray(int[] values) {
        return Base64.encodeToString(Arrays.toByteArray(values), Base64.DEFAULT);
    }

    @TypeConverter
    public int[] stringToIntArray(String values) {
        return Arrays.toUnsignedByteArray(Base64.decode(values, Base64.DEFAULT));
    }
}

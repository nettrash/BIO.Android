package ru.nettrash.bio.classes;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import ru.nettrash.bioandroid.R;

/**
 * Created by nettrash on 14.06.2018.
 */

public final class bioRateItem {

    public double Rate;
    public String Currency;

    public bioRateItem(double Rate, String Currency) {
        this.Rate = Rate;
        this.Currency = Currency;
    }

    public bioRateItem(JSONObject object) throws Exception {
        this.Currency = object.getString("Currency");
        this.Rate = object.getDouble("Rate");
    }

    public HashMap<String, Object> getHashMap() {
        HashMap retVal = new HashMap<String, Object>();
        if (Currency.toUpperCase().equals("RUB"))
            retVal.put("rate", String.format("~ %.2f %s", Rate, Currency));
        else
            retVal.put("rate", String.format("~ %.6f %s", Rate, Currency));

        return retVal;
    }

    @NonNull
    @Contract(pure = true)
    public static String[] getListAdapterFrom() {
        return new String[] { "rate" };
    }

    @NonNull
    @Contract(pure = true)
    public static int[] getListAdapterTo() {
        return new int[] { R.id.rate_value };
    }
}

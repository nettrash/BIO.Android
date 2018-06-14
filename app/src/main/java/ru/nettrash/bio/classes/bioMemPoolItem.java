package ru.nettrash.bio.classes;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import ru.nettrash.bioandroid.R;

/**
 * Created by nettrash on 02.02.2018.
 */

public final class bioMemPoolItem extends bioTransactionListItem {

    public String Address;
    public String txId;
    public int N;
    public long value;
    public long seconds;
    public String prev_txId;
    public int prev_N;
    public boolean isInput;

    public bioMemPoolItem(JSONObject object) {
        try {
            Address = object.getString("Address");
            txId = object.getString("TransactionId");
            N = object.getInt("N");
            value = object.getLong("Value");
            seconds = object.getLong("Seconds");
            prev_txId = object.getString("PrevTransactionId");
            prev_N = object.getInt("PrevN");
            isInput = value >= 0;
        }
        catch (Exception ex) {

        }
    }

    @Override
    public HashMap<String, Object> getHashMap() {
        HashMap retVal = new HashMap<String, Object>();
        retVal.put("amount", (value>=0 ? "+ " : "- ")+String.format("%.2f", (value / Math.pow(10, 8)) * (value >=0 ? 1 : -1)));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        retVal.put("date", formatter.format(new Date()));
        retVal.put("icon", value>=0 ? R.drawable.memincoming : R.drawable.memoutgoing);
        return retVal;
    }
}

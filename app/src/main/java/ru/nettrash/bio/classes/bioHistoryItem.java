package ru.nettrash.bio.classes;

import android.support.annotation.NonNull;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import ru.nettrash.bioandroid.R;

/**
 * Created by nettrash on 14.06.2018.
 */

public final class bioHistoryItem extends bioTransactionListItem {

    public String txId;
    public short txType;
    public Date txDate;
    public double Amount;
    public String Address;

    public bioHistoryItem(String txId, short txType, Date txDate, double Amount, String Address) {
        this.txId = txId;
        this.txType = txType;
        this.txDate = txDate;
        this.Amount = Amount;
        this.Address = Address;
    }

    public bioHistoryItem(JSONObject object, String[] incomingAddresses, String[] changeAddresses) throws Exception {
        this.txId = object.getString("Id");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        this.txDate = formatter.parse(object.getString("TransactionDate"));

        JSONArray outs = object.getJSONArray("Out");

        Double outInAmount = 0.0;
        Double outChangeAmount = 0.0;
        Double outExternalAmount = 0.0;
        String outAddressExt = "";
        String outAddressIn= "";

        for (int idx=0; idx < outs.length(); idx++) {
            JSONObject out = outs.getJSONObject(idx);
            JSONArray addrs = out.getJSONArray("Addresses");
            Double amount = out.getDouble("Amount");

            for (int a=0; a < addrs.length(); a++) {
                String address = addrs.getString(a);
                if (ru.nettrash.util.Arrays.contains(incomingAddresses, address)) {
                    outInAmount += amount;
                    outAddressIn = address;
                    continue;
                }
                if (ru.nettrash.util.Arrays.contains(changeAddresses, address)) {
                    outChangeAmount += amount;
                    continue;
                }
                outExternalAmount += amount;
                outAddressExt = address;
            }
        }

        this.txType = outInAmount == 0 ? (short) 1 : (short) 0;
        this.Amount = (outInAmount == 0 ? outExternalAmount : outInAmount) / Math.pow(10, 8);
        this.Address = outAddressExt == "" ? outAddressIn : outAddressExt;
    }

    @Override
    public HashMap<String, Object> getHashMap() {
        HashMap retVal = new HashMap<String, Object>();
        retVal.put("amount", (txType==0 ? "+ " : "- ")+String.format("%.2f", Amount));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        retVal.put("date", formatter.format(txDate));
        retVal.put("icon", txType==0 ? R.drawable.incoming : R.drawable.outgoing);
        retVal.put("address", Address);
        return retVal;
    }

    @NonNull
    @Contract(pure = true)
    public static String[] getListAdapterFrom() {
        return new String[] { "amount", "date", "icon", "address" };
    }

    @NonNull
    @Contract(pure = true)
    public static int[] getListAdapterTo() {
        return new int[] { R.id.history_item_amount, R.id.history_item_date, R.id.history_item_icon, R.id.history_item_address };
    }
}

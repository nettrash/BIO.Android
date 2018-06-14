package ru.nettrash.bioandroid;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import org.jetbrains.annotations.Contract;

import java.util.Date;

import ru.nettrash.bio.models.rootModel;

/**
 * Created by nettrash on 21.01.2018.
 */

public final class BIOApplication extends Application {

    public rootModel model;

    public void initialize() {
        model = new rootModel(this);
    }

    public void setPIN(String sPIN) {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("PIN", Base64.encodeToString((sPIN + "NETTRASH_BIO_WALLET").getBytes(), Base64.NO_WRAP));
        editor.commit();
    }

    public boolean checkPIN(String sPIN) {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        String storedPIN = preferences.getString("PIN", "");
        String enteredPIN = Base64.encodeToString((sPIN + "NETTRASH_BIO_WALLET").getBytes(), Base64.NO_WRAP);
        return !sPIN.equals("") && enteredPIN.equals(storedPIN);
    }

    public boolean needSetPIN() {
        try {
            SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
            String storedPIN = preferences.getString("PIN", "");
            if (storedPIN == null) return true;
            if (storedPIN.equals("")) return true;
            String decodedPIN = new String(Base64.decode(storedPIN, Base64.NO_WRAP));
            if ("1234NETTRASH_BIO_WALLET".length() != decodedPIN.length()) return true;
        } catch (Exception ex) {
            return true;
        }
        return false;
    }

    @NonNull
    @Contract(pure = true)
    public String getCurrencySymbol() {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        return preferences.getString("CurrencySymbol", "₽");
    }

    @NonNull
    @Contract(pure = true)
    public String getCurrency() {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        return preferences.getString("Currency", "RUB");
    }

    public void setCurrency(String currency) {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        switch (currency) {
            case "USD":
                editor.putString("Currency",  "USD");
                editor.putString("CurrencySymbol",  "$");
                break;
            case "EUR":
                editor.putString("Currency",  "EUR");
                editor.putString("CurrencySymbol",  "€");
                break;
            default:
                editor.putString("Currency",  "RUB");
                editor.putString("CurrencySymbol",  "₽");
                break;
        }
        editor.commit();
    }

    @Contract(pure = true)
    public boolean needCheckPIN() {
        Date lastStopDate = getLastStopDate();
        if (lastStopDate == null) return false;
        Date d = new Date();
        return (d.getTime() - lastStopDate.getTime()) / 1000 > getInactiveSeconds();
    }

    public int getInactiveSeconds() {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        return preferences.getInt("InactiveSeconds", Variables.inactiveSecondsDefault);
    }

    public void setInactiveSeconds(int value) {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("InactiveSeconds", value);
        editor.commit();
    }

    @Nullable
    public Date getLastStopDate() {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        long time = preferences.getLong("LastStopDate", 0);
        if (time > 0)
            return new Date(time);
        return null;
    }

    public void setLastStopDate(Date value) {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (value != null) {
            editor.putLong("LastStopDate", value.getTime());
        } else {
            editor.putLong("LastStopDate", 0);
        }
        editor.commit();
    }

    @Nullable
    public String getLastStoppedActivityClassName() {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        return preferences.getString("LastStoppedActivityClassName", "");
    }

    public void setLastStoppedActivityClassName(String value) {
        SharedPreferences preferences = this.getSharedPreferences("BIOPreferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("LastStoppedActivityClassName", value);
        editor.commit();
    }

}


package ru.nettrash.bio.bitpay;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.Semaphore;

import javax.net.ssl.HttpsURLConnection;

import ru.nettrash.bio.bioAPI;
import ru.nettrash.bioandroid.R;
import ru.nettrash.util.Json;

/**
 * Created by nettrash on 14.03.2018.
 */

public class Invoice {

    public String source;

    public String url;
    public String posData;
    public String status;
    public Double btcPrice;
    public Double btcDue;
    public Double price;
    public String currency;
    public String itemDesc;
    public String orderId;
    public Date invoiceTime;
    public Date expirationTime;
    public Date currentTime;
    public String id;
    public Boolean lowFeeDetected;
    public Double amountPaid;
    public Double btcPaid;
    public Double rate;
    public Boolean exceptionStatus;
    public String redirectURL;
    public Boolean refundAddressRequestPending;
    public String buyerProvidedEmail;
    public buyerProvidedInfoType buyerProvidedInfo;
    public HashMap<String, String> addresses;
    public HashMap<String, Double> paymentSubtotals;
    public HashMap<String, Double> paymentTotals;
    public String bitcoinAddress;
    public exchangeRatesType exchangeRates;
    public minerFeesType minerFees;
    public Double buyerPaidBtcMinerFee;
    public supportedTransactionCurrenciesType supportedTransactionCurrencies;
    public HashMap<String, Double> exRates;
    public HashMap<String, String> paymentUrls;
    public paymentCodesType paymentCodes;
    public String token;


    public Invoice(String invoiceUrl) throws Exception {

        final class getInvoiceInfoAsyncTask extends AsyncTask<String, Void, String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Nullable
            @Override
            protected String doInBackground(String... params) {
                try {
                    URL src = new URL(params[0].replace("/i/", "/invoices/"));
                    HttpsURLConnection c = (HttpsURLConnection)src.openConnection();
                    c.connect();
                    if (c.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        InputStream input = c.getInputStream();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        return new String(baos.toByteArray(), "UTF-8");
                    } else {
                        throw new Exception(Integer.toString(c.getResponseCode()) + " " + c.getResponseMessage());
                    }
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
            }

            @Override
            protected void onCancelled(String result) {
                super.onCancelled(result);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }
        }

        getInvoiceInfoAsyncTask task = new getInvoiceInfoAsyncTask();
        task.execute(invoiceUrl);
        String json = task.get();
        parse(json);
    }

    private void parse(String json) {
        source = json;

        try {

            JSONObject obj = new JSONObject(json);
            JSONObject data = obj.getJSONObject("data");

            url = data.optString("url");
            posData = data.optString("posData");
            status = data.optString("status");
            btcPrice = data.optDouble("btcPrice");
            btcDue = data.optDouble("btcDue");
            price = data.optDouble("price");
            currency = data.optString("currency");
            itemDesc = data.optString("itemDesc");
            orderId = data.optString("orderId");
            invoiceTime = new Date(data.optLong("invoiceTime"));
            expirationTime = new Date(data.optLong("expirationTime"));
            currentTime = new Date(data.optLong("currentTime"));
            id = data.optString("id");
            lowFeeDetected = data.optBoolean("lowFeeDetected", false);
            amountPaid = data.optDouble("amountPaid");
            btcPaid = data.optDouble("btcPaid");
            rate = data.optDouble("rate");
            exceptionStatus = data.optBoolean("exceptionStatus", false);
            redirectURL = data.optString("redirectURL");
            refundAddressRequestPending = data.optBoolean("refundAddressRequestPending", false);
            buyerProvidedEmail = data.optString("buyerProvidedEmail");
            buyerProvidedInfo = new buyerProvidedInfoType();
            obj = data.getJSONObject("buyerProvidedInfo");
            buyerProvidedInfo.emailAddress = obj.optString("emailAddress");
            buyerProvidedInfo.selectedTransactionCurrency = obj.optString("selectedTransactionCurrency");
            addresses = Json.jsonToMap(data.getJSONObject("addresses"));
            paymentSubtotals = Json.jsonToMap(data.getJSONObject("paymentSubtotals"));
            paymentTotals = Json.jsonToMap(data.getJSONObject("paymentTotals"));
            bitcoinAddress = data.optString("bitcoinAddress");
            obj = data.getJSONObject("exchangeRates");
            exchangeRates = new exchangeRatesType();
            exchangeRates.BTC = Json.jsonToMap(obj.getJSONObject("BTC"));
            obj = data.getJSONObject("minerFees");
            minerFees = new minerFeesType();
            minerFees.BTC = new minerFeeType();
            obj = obj.getJSONObject("BTC");
            minerFees.BTC.totalFee = obj.optLong("totalFee");
            minerFees.BTC.satoshisPerByte = obj.optLong("satoshisPerByte");
            buyerPaidBtcMinerFee = data.optDouble("buyerPaidBtcMinerFee");
            supportedTransactionCurrencies = new supportedTransactionCurrenciesType();
            obj = data.getJSONObject("supportedTransactionCurrencies");
            obj = obj.getJSONObject("BTC");
            supportedTransactionCurrencies.BTC = new supportedTransactionCurrencyInfoType();
            supportedTransactionCurrencies.BTC.enabled = obj.optBoolean("enabled", false);
            exRates = Json.jsonToMap(data.getJSONObject("exRates"));
            paymentUrls = Json.jsonToMap(data.getJSONObject("paymentUrls"));
            obj = data.getJSONObject("paymentCodes");
            paymentCodes = new paymentCodesType();
            paymentCodes.BTC = Json.jsonToMap(obj.getJSONObject("BTC"));
            token = data.optString("token");

        } catch (Exception ex) {
        }
    }

    public Boolean isValid() {
        return posData != null && status != null && id != null;
    }

    public Boolean isExpired() {
        return status.toLowerCase().equals("expired") || expirationTime.before(new Date());
    }

    public Boolean isAvailibleForProcess() {
        return isValid() && !isExpired() && supportedTransactionCurrencies.BTC.enabled &&
                buyerProvidedInfo.selectedTransactionCurrency.toUpperCase().equals("BTC");
    }

    @NonNull
    public static Boolean canParse(String url) {
        return url.contains("bitpay");
    }

    public String invoiceInformation() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s\n\n", itemDesc));
        sb.append("");
        sb.append(String.format(Locale.getDefault(), "\t%.2f %s\n", price, currency));
        sb.append(String.format(Locale.getDefault(), "\t%.8f BTC\n\n", btcDue));
        sb.append("");
        sb.append(String.format(Locale.getDefault(), "\t1 BTC = %.2f %s\n", rate, currency));
        sb.append("");
        sb.append(String.format("\n\t%s\n", status.toUpperCase()));
        sb.append("");
        return sb.toString();
    }

}

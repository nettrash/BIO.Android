package ru.nettrash.bio;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import org.jetbrains.annotations.Contract;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import ru.nettrash.crypto.MD5;
import ru.nettrash.bio.classes.bioBuyState;
import ru.nettrash.bio.classes.bioHistoryItem;
import ru.nettrash.bio.classes.bioMemPoolItem;
import ru.nettrash.bio.classes.bioRateItem;
import ru.nettrash.bio.classes.bioUnspentTransaction;
import ru.nettrash.bio.models.rootModel;
import ru.nettrash.util.Arrays;

import static java.lang.Math.pow;

/**
 * Created by nettrash on 14.06.2018.
 */

public final class bioAPI {

    private String urlAPIRoot;
    private String userAPI;
    private String passwordAPI;

    @NonNull
    private String _calcAuth() {
        String md5src = userAPI+passwordAPI;
        MD5 md5 = new MD5();
        md5.update(md5src.getBytes());
        byte[] md5digest = md5.digest();
        String ServicePassword = Arrays.toHexString(md5digest).toUpperCase();
        String data = userAPI+":"+ ServicePassword;
        return new String(Base64.encode(data.getBytes(), Base64.NO_WRAP));
    }

    @NonNull
    private String _sendPOST(String sUrl, String sRequest) throws Exception {
        URL url = new URL(sUrl);
        HttpsURLConnection c = (HttpsURLConnection)url.openConnection();
        c.setHostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                //return HttpsURLConnection.getDefaultHostnameVerifier().verify("your_domain.com", session);
                return true;
            }
        });
        c.setSSLSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());
        c.setUseCaches(false);
        c.setDoInput(true);
        c.setDoOutput(true);
        c.setRequestMethod("POST");
        c.setRequestProperty("Content-Type", "application/json");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Authorization", "Basic " + _calcAuth());
        c.setRequestProperty("Content-Length", String.valueOf(sRequest.length()));
        c.setConnectTimeout(30000);
        c.setReadTimeout(30000);
        c.setInstanceFollowRedirects(true);

        OutputStream output = c.getOutputStream();
        BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(output, "UTF-8"));
        writer.write(sRequest);
        writer.flush();
        writer.close();
        output.close();

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
    }

    public bioAPI() {
        urlAPIRoot = "https://service.biocoin.pro/wallet/bio/bio.svc";
        userAPI = "BIO";
        passwordAPI = "DE679233-8A45-4845-AA4D-EFCA1350F0A0";
    }

    public bioAPI(String url) {
        urlAPIRoot = url;
        userAPI = "BIO";
        passwordAPI = "DE679233-8A45-4845-AA4D-EFCA1350F0A0";
    }

    public bioAPI(String url, String user, String password) {
        urlAPIRoot = url;
        userAPI = user;
        passwordAPI = password;
    }

    @Contract(pure = true)
    public double getBalance(String[] addresses) throws Exception {
        String url = urlAPIRoot + "/balance";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("addresses", new JSONArray(addresses));

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("BalanceResult");
        if (result.getBoolean("Success")) {

            return result.getDouble("Value") / pow(10, 8);
        } else {
            throw new Exception("Error get balance");
        }
    }

    @Nullable
    public ArrayList<bioHistoryItem> getLastTransactions(int last, String[] addresses, String[] inputAddresses, String[] changeAddresses) throws Exception {
        String url = urlAPIRoot + "/transactions";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("addresses", new JSONArray(addresses));
        postDataParams.put("last", last);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("TransactionsResult");
        if (result.getBoolean("Success")) {

            ArrayList<bioHistoryItem> retVal = new ArrayList<bioHistoryItem>();

            try {
                JSONArray items = result.getJSONArray("Items");
                for (int idx = 0; idx < items.length(); idx++) {
                    JSONObject obj = items.getJSONObject(idx);

                    retVal.add(new bioHistoryItem(obj, inputAddresses, changeAddresses));

                }
            } catch (Exception ex) {

            }

            return retVal;


        } else {
            throw new Exception("Error get last ops");
        }
    }

    @Nullable
    public ArrayList<bioMemPoolItem> getMemPoolTransactions(String[] addresses) throws Exception {
        String url = urlAPIRoot + "/mempool";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("addresses", new JSONArray(addresses));

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("MemoryPoolResult");
        if (result.getBoolean("Success")) {

            ArrayList<bioMemPoolItem> retVal = new ArrayList<bioMemPoolItem>();

            try {
                JSONArray items = result.getJSONArray("Items");
                for (int idx = 0; idx < items.length(); idx++) {
                    JSONObject obj = items.getJSONObject(idx);

                    retVal.add(new bioMemPoolItem(obj));

                }
            } catch (Exception ex) {

            }

            return retVal;


        } else {
            throw new Exception("Error get balance");
        }
    }

    public boolean checkInputExists(String address) throws Exception {
        String url = urlAPIRoot + "/hasInput";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("address", address);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("InputExistsResult");
        return result.getBoolean("Success") && result.getBoolean("Exists");
    }

    @Nullable
    public ArrayList<bioUnspentTransaction> getUnspentTransactions(String[] addresses) throws Exception {
        String url = urlAPIRoot + "/unspentTransactions";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("addresses", new JSONArray(addresses));
        postDataParams.put("last", 3);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("UnspentTransactionsResult");
        if (result.getBoolean("Success")) {

            ArrayList<bioUnspentTransaction> retVal = new ArrayList<bioUnspentTransaction>();

            try {
                JSONArray items = result.getJSONArray("Items");
                for (int idx = 0; idx < items.length(); idx++) {
                    JSONObject obj = items.getJSONObject(idx);

                    retVal.add(new bioUnspentTransaction(obj));
                }
            } catch (Exception ex) {

            }

            return retVal;


        } else {
            throw new Exception("Error get unspent");
        }
    }

    public bioBroadcastTransactionResult broadcastTransaction(int[] sign) throws Exception {
        String url = urlAPIRoot + "/broadcastTransaction";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("rawtx", Base64.encodeToString(Arrays.toByteArray(sign), Base64.NO_WRAP));

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("BroadcastTransactionResult");
        bioBroadcastTransactionResult retVal = new bioBroadcastTransactionResult();
        retVal.IsBroadcasted = result.getBoolean("Success");
        if (result.getBoolean("Success")) {
            retVal.TransactionId = result.getString("TransactionId");
        } else {
            retVal.Message = result.getString("Message");
        }
        return retVal;
    }

    public ArrayList<bioRateItem> getRates() throws Exception {
        String url = urlAPIRoot + "/currentRates";

        String sResponse = _sendPOST(url, "");
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("CurrentRatesResult");
        if (result.getBoolean("Success")) {

            ArrayList<bioRateItem> retVal = new ArrayList<bioRateItem>();

            try {
                JSONArray items = result.getJSONArray("Items");
                for (int idx = 0; idx < items.length(); idx++) {
                    JSONObject obj = items.getJSONObject(idx);

                    retVal.add(new bioRateItem(obj));

                }
            } catch (Exception ex) {

            }

            return retVal;


        } else {
            throw new Exception("Error get rates");
        }
    }

    @Contract(pure = true)
    public double getSellRate(String currency) throws Exception {
        String url = urlAPIRoot + "/sellRate";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("currency", currency);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("SellRateResult");

        if (result.getBoolean("Success")) {
            return result.getDouble("Rate");
        } else {
            throw new Exception("Error get sell rate");
        }
    }

    @Contract(pure = true)
    public double getSellRateWithAmount(String currency, Double amount) throws Exception {
        String url = urlAPIRoot + "/sellRateWithAmount";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("currency", currency);
        postDataParams.put("amount", amount);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("SellRateWithAmountResult");

        if (result.getBoolean("Success")) {
            return result.getDouble("Rate");
        } else {
            throw new Exception("Error get sell rate");
        }
    }

    @Contract(pure = true)
    public String processSell(String currency, Double amountBIO, Double amount, String pan) throws Exception {
        String url = urlAPIRoot + "/registerSell";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("pan", pan);
        postDataParams.put("amountBIO", amountBIO);
        postDataParams.put("amount", amount);
        postDataParams.put("currency", currency);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("RegisterSellResult");

        if (result.getBoolean("Success")) {
            return result.getString("Address");
        } else {
            throw new Exception("Error process sell");
        }
    }

    @Contract(pure = true)
    public double getBuyRate(String currency) throws Exception {
        String url = urlAPIRoot + "/buyRate";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("currency", currency);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("BuyRateResult");

        if (result.getBoolean("Success")) {
            return result.getDouble("Rate");
        } else {
            throw new Exception("Error get buy rate");
        }
    }

    @Contract(pure = true)
    public double getBuyRateWithAmount(String currency, Double amount) throws Exception {
        String url = urlAPIRoot + "/buyRateWithAmount";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("currency", currency);
        postDataParams.put("amount", amount);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("BuyRateWithAmountResult");

        if (result.getBoolean("Success")) {
            return result.getDouble("Rate");
        } else {
            throw new Exception("Error get buy rate");
        }
    }

    @NonNull
    @Contract(pure = true)
    public bioBuyState processBuy(String currency, Double amountBIO, Double amount, String pan, String exp, String cvv, String account, String address) throws Exception {
        String url = urlAPIRoot + "/registerBuy";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("account", account);
        postDataParams.put("pan", pan);
        postDataParams.put("exp", exp);
        postDataParams.put("cvv", cvv);
        postDataParams.put("amountBIO", amountBIO);
        postDataParams.put("amount", amount);
        postDataParams.put("currency", currency);
        postDataParams.put("address", address);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("RegisterBuyResult");

        if (result.getBoolean("Success")) {
            return new bioBuyState(result.getString("State"), result.getString("RedirectUrl"));
        } else {
            throw new Exception("Error process buy");
        }
    }

    @NonNull
    @Contract(pure = true)
    public bioBuyState checkOperation(String opKey) throws Exception {
        String url = urlAPIRoot + "/checkOp";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("OpKey", opKey);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("CheckOpResult");

        if (result.getBoolean("Success")) {
            return new bioBuyState(result.getString("State"), "");
        } else {
            throw new Exception("Error process buy");
        }
    }

    @NonNull
    public String getNewBitPayAddress() throws Exception {
        String url = urlAPIRoot + "/getNewBitPayAddress";

        String sResponse = _sendPOST(url, "");
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("GetNewBitPayAddressResult");
        if (result.getBoolean("Success")) {
            return result.getString("Address");
        } else {
            throw new Exception("Unable to get BIO address for payment.");
        }
    }

    public bioBroadcastTransactionResult payInvoice(String invoice, int[] sign, String bioAddress, Double bioAmount, String otherAddress, Double otherAmount) throws Exception {
        String url = urlAPIRoot + "/payInvoice";

        JSONObject postDataParams = new JSONObject();
        postDataParams.put("invoice", Base64.encodeToString(invoice.getBytes(), Base64.NO_WRAP));
        postDataParams.put("tx", Base64.encodeToString(Arrays.toByteArray(sign), Base64.NO_WRAP));
        postDataParams.put("address", bioAddress);
        postDataParams.put("amount", bioAmount);
        postDataParams.put("otherAddress", otherAddress);
        postDataParams.put("otherAmount", otherAmount);

        String sResponse = _sendPOST(url, postDataParams.toString());
        JSONObject resp = new JSONObject(sResponse);
        JSONObject result = resp.getJSONObject("PayInvoiceResult");
        bioBroadcastTransactionResult retVal = new bioBroadcastTransactionResult();
        retVal.IsBroadcasted = result.getBoolean("Success");
        if (result.getBoolean("Success")) {
            retVal.TransactionId = result.getString("TransactionId");
            //retVal.BTCTransactionId = result.getString("BTCTransactionId");
            retVal.Message = result.getString("Message");
        } else {
            retVal.Message = result.getString("Message");
        }
        return retVal;
    }

}


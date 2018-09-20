package ru.nettrash.bioandroid;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.card.payment.CardIOActivity;
import io.card.payment.CreditCard;
import ru.nettrash.bio.classes.bioBuyState;
import ru.nettrash.bio.classes.bioHistoryItem;
import ru.nettrash.bio.classes.bioMemPoolItem;
import ru.nettrash.bio.classes.bioRateItem;
import ru.nettrash.bio.classes.bioUnspentTransaction;
import ru.nettrash.bio.database.Address;
import ru.nettrash.bio.bioAPI;
import ru.nettrash.bio.bioBroadcastTransactionResult;
import ru.nettrash.bio.bioTransaction;

public class BalanceActivity extends BaseActivity {

    private final int REQUEST_SELL_CARD_SCAN = 0;
    private final int REQUEST_BUY_CARD_SCAN = 1;

    private int _refreshLastOpsCount = 0;
    private int LAST_HISTORY_MAX_COUNT = 3;

    private int _firstX;
    private int _firstY;
    private static final int SWIPE_MIN_X_DISTANCE = 100;
    private static final int SWIPE_MIN_Y_DISTANCE = 10;
    private int mSelectedSegment = 0;

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private TextView mBalanceView;
    private ImageButton mActionButton;
    private ImageButton mActionButtonReceive;
    private ImageButton mActionButtonSend;
    private ImageButton mActionButtonHistory;
    private TextView mActionButtonReceiveText;
    private TextView mActionButtonSendText;
    private TextView mActionButtonHistoryText;
    private ImageButton mActionButtonSettings;
    private LinearLayout mLayoutActionButtonReceive;
    private LinearLayout mLayoutActionButtonSend;
    private LinearLayout mLayoutActionButtonHistory;
    private ImageView mImageTap;

    private ViewFlipper mViewFlipper;

    private ListView mLastHistoryListView;
    private TextView mLabelNoOps;
    private SwipeRefreshLayout mSwipeRefreshLastOps;

    private ListView mRatesListView;
    private SwipeRefreshLayout mSwipeRefreshRates;

    private Button mSegmentButtonBIO;
    private Button mSegmentButtonRates;
    private Button mSegmentButtonBuy;
    private Button mSegmentButtonSell;

    private EditText mSellCardNumber;
    private ImageButton mSellScan;
    private EditText mSellAmount;
    private Button mSellButton;
    private TextView mSellRate;
    private Button mSellInfoButton;

    private EditText mBuyCardNumber;
    private EditText mBuyCardExp;
    private EditText mBuyCardCVV;
    private ImageButton mBuyScan;
    private EditText mBuyAmount;
    private Button mBuyButton;
    private TextView mBuyRate;

    private WebView mWebView;

    private final Handler mSellAmountFocusHandler = new Handler();
    private final Runnable mSellAmountFocusRunnable = new Runnable() {
        @Override
        public void run() {

            mSellAmount.setFocusableInTouchMode(true);
            mSellAmount.requestFocus();

            final InputMethodManager inputMethodManager = (InputMethodManager) BalanceActivity.this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(mSellAmount, InputMethodManager.SHOW_IMPLICIT);

        }
    };

    private final Handler mBuyExpFocusHandler = new Handler();
    private final Runnable mBuyExpFocusRunnable = new Runnable() {
        @Override
        public void run() {

            mBuyCardExp.setFocusableInTouchMode(true);
            mBuyCardExp.requestFocus();

            final InputMethodManager inputMethodManager = (InputMethodManager) BalanceActivity.this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(mBuyCardExp, InputMethodManager.SHOW_IMPLICIT);

        }
    };

    private final Handler mBuyCVVFocusHandler = new Handler();
    private final Runnable mBuyCVVFocusRunnable = new Runnable() {
        @Override
        public void run() {

            mBuyCardCVV.setFocusableInTouchMode(true);
            mBuyCardCVV.requestFocus();

            final InputMethodManager inputMethodManager = (InputMethodManager) BalanceActivity.this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(mBuyCardCVV, InputMethodManager.SHOW_IMPLICIT);

        }
    };

    private final Handler mBuyAmountFocusHandler = new Handler();
    private final Runnable mBuyAmountFocusRunnable = new Runnable() {
        @Override
        public void run() {

            mBuyAmount.setFocusableInTouchMode(true);
            mBuyAmount.requestFocus();

            final InputMethodManager inputMethodManager = (InputMethodManager) BalanceActivity.this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(mBuyAmount, InputMethodManager.SHOW_IMPLICIT);

        }
    };

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
        }
    };

    private boolean mVisible;

    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    private void refreshMemPool() {
        //get last history

        final class memPoolTransactionsAsyncTask extends AsyncTask<Void, Void, ArrayList<bioMemPoolItem>> {

            protected bioAPI api = new bioAPI();
            protected String[] addresses = new String[0];

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mSwipeRefreshLastOps.setRefreshing(true);
                ArrayList<String> addrs = new ArrayList<String>();
                try {
                    for (Address a : bioApplication.model.getAddresses()) {
                        addrs.add(a.getAddress());
                    }
                    addresses = addrs.toArray(new String[0]);
                } catch (Exception ex) {
                    this.cancel(true);
                }
            }

            @Nullable
            @Override
            protected ArrayList<bioMemPoolItem> doInBackground(Void... params) {
                try {
                    return api.getMemPoolTransactions(addresses);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<bioMemPoolItem> result) {
                super.onPostExecute(result);
                if (result != null) {
                    ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                    for (bioMemPoolItem item: result) {
                        data.add(item.getHashMap());
                    }
                    if (data.size() < LAST_HISTORY_MAX_COUNT) {
                        refreshLastHistory(data);
                    } else {
                        SimpleAdapter adapter = new SimpleAdapter(BalanceActivity.this, data, R.layout.last_history_item, bioHistoryItem.getListAdapterFrom(), bioHistoryItem.getListAdapterTo());
                        mLastHistoryListView.setAdapter(adapter);
                        mLabelNoOps.setVisibility(result.size() > 0 ? View.INVISIBLE : View.VISIBLE);
                    }
                }
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }

            @Override
            protected void onCancelled(ArrayList<bioMemPoolItem> result) {
                super.onCancelled(result);
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }
        }

        _refreshLastOpsCount++;
        new memPoolTransactionsAsyncTask().execute();
    }

    private void refreshLastHistory(ArrayList<Map<String, Object>> memPool) {
        //get last history

        final class lastTransactionsAsyncTask extends AsyncTask<Void, Void, ArrayList<bioHistoryItem>> {

            protected bioAPI api = new bioAPI();
            protected String[] addresses = new String[0];
            protected String[] addressesInput = new String[0];
            protected String[] addressesChange = new String[0];
            public ArrayList<Map<String, Object>> memPoolData = null;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mSwipeRefreshLastOps.setRefreshing(true);
                ArrayList<String> addrs = new ArrayList<String>();
                ArrayList<String> addrsInput = new ArrayList<String>();
                ArrayList<String> addrsChange = new ArrayList<String>();
                try {

                    for (Address a : bioApplication.model.getAddresses()) {
                        addrs.add(a.getAddress());
                        switch (a.getAddressType()) {
                            case (short) 0: {
                                addrsInput.add(a.getAddress());
                                break;
                            }
                            case (short) 1: {
                                addrsChange.add(a.getAddress());
                                break;
                            }
                            default:
                                break;
                        }
                    }

                    addresses = addrs.toArray(new String[0]);
                    addressesInput = addrsInput.toArray(new String[0]);
                    addressesChange = addrsChange.toArray(new String[0]);

                } catch (Exception ex) {
                    this.cancel(true);
                }
            }

            @Nullable
            @Override
            protected ArrayList<bioHistoryItem> doInBackground(Void... params) {
                try {
                    return api.getLastTransactions(LAST_HISTORY_MAX_COUNT-memPoolData.size(), addresses, addressesInput, addressesChange);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<bioHistoryItem> result) {
                super.onPostExecute(result);
                if (result != null) {
                    if (memPoolData == null) {
                        memPoolData = new ArrayList<Map<String, Object>>();
                    }
                    for (bioHistoryItem item: result) {
                        memPoolData.add(item.getHashMap());
                    }
                    SimpleAdapter adapter = new SimpleAdapter(BalanceActivity.this, memPoolData, R.layout.last_history_item, bioHistoryItem.getListAdapterFrom(), bioHistoryItem.getListAdapterTo());
                    mLastHistoryListView.setAdapter(adapter);
                    mLabelNoOps.setVisibility(result.size() > 0 ? View.INVISIBLE : View.VISIBLE);
                }
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }

            @Override
            protected void onCancelled(ArrayList<bioHistoryItem> result) {
                super.onCancelled(result);
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }
        }

        _refreshLastOpsCount++;
        lastTransactionsAsyncTask task = new lastTransactionsAsyncTask();
        task.memPoolData = memPool;
        task.execute();
    }

    private void refreshBalance()  {
        final class balanceAsyncTask extends AsyncTask<Void, Void, Double> {

            protected bioAPI api = new bioAPI();
            protected String[] addresses = new String[0];

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mBalanceView.setText(R.string.balanceRefreshInProgress);
                ArrayList<String> addrs = new ArrayList<String>();
                try {
                    for (Address a : bioApplication.model.getAddresses()) {
                        addrs.add(a.getAddress());
                    }
                    addresses = addrs.toArray(new String[0]);
                } catch (Exception ex) {
                    this.cancel(true);
                }
            }

            @Nullable
            @Override
            protected Double doInBackground(Void... params) {
                try {
                    return Double.valueOf(api.getBalance(addresses));
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Double result) {
                super.onPostExecute(result);
                if (result != null) {
                    mBalanceView.setText(String.format("%.2f", result.doubleValue()).replace(",", "."));
                    bioApplication.model.setBalance(result);
                }
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }

            @Override
            protected void onCancelled(Double result) {
                super.onCancelled(result);
                mBalanceView.setText(R.string.balanceRefreshError);
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                mBalanceView.setText(R.string.balanceRefreshError);
                _refreshLastOpsCount--;
                if (_refreshLastOpsCount<=0) {
                    _refreshLastOpsCount = 0;
                    mSwipeRefreshLastOps.setRefreshing(false);
                }
            }
        }

        _refreshLastOpsCount++;
        new balanceAsyncTask().execute();
    }

    private void refreshRates() {
        //get rates

        final class ratesAsyncTask extends AsyncTask<Void, Void, ArrayList<bioRateItem>> {

            protected bioAPI api = new bioAPI();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mSwipeRefreshRates.setRefreshing(true);
            }

            @Nullable
            @Override
            protected ArrayList<bioRateItem> doInBackground(Void... params) {
                try {
                    return api.getRates();
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<bioRateItem> result) {
                super.onPostExecute(result);
                if (result != null) {
                    ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                    for (bioRateItem item: result) {
                        data.add(item.getHashMap());
                    }
                    SimpleAdapter adapter = new SimpleAdapter(BalanceActivity.this, data, R.layout.rate_item, bioRateItem.getListAdapterFrom(), bioRateItem.getListAdapterTo());
                    mRatesListView.setAdapter(adapter);
                }
                mSwipeRefreshRates.setRefreshing(false);
            }

            @Override
            protected void onCancelled(ArrayList<bioRateItem> result) {
                super.onCancelled(result);
                mSwipeRefreshRates.setRefreshing(false);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                mSwipeRefreshRates.setRefreshing(false);
            }
        }

        new ratesAsyncTask().execute();
    }

    private void refreshSellRate()  {
        final class sellRateAsyncTask extends AsyncTask<Void, Void, Double> {

            protected bioAPI api = new bioAPI();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mSellRate.setText(R.string.sellraterefresh);
            }

            @Nullable
            @Override
            protected Double doInBackground(Void... params) {
                try {
                    return Double.valueOf(api.getSellRate(bioApplication.getCurrency()));
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Double result) {
                super.onPostExecute(result);
                if (result != null) {
                    bioApplication.model.setSellRate(result);
                    int commission = -1;
                    switch (bioApplication.getCurrency()) {
                        case "USD":
                            commission = R.string.sell_commission_usd;
                            break;
                        case "EUR":
                            commission = R.string.sell_commission_eur;
                            break;
                        default:
                            commission = R.string.sell_commission;
                            break;
                    }
                    mSellRate.setText("1BIO ~ " + String.format("%.2f ", result.doubleValue()) +
                            bioApplication.getCurrencySymbol() + "\n" +
                            getResources().getString(commission) + "\n" +
                            getResources().getString(R.string.sell_cardtransfer_info));
                }
            }

            @Override
            protected void onCancelled(Double result) {
                super.onCancelled(result);
                mSellRate.setText(R.string.sell_rate_refresh_error);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                mSellRate.setText(R.string.sell_rate_refresh_error);
            }
        }

        new sellRateAsyncTask().execute();
    }

    private void refreshBuyRate()  {
        final class buyRateAsyncTask extends AsyncTask<Void, Void, Double> {

            protected bioAPI api = new bioAPI();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mBuyRate.setText(R.string.buyraterefresh);
            }

            @Nullable
            @Override
            protected Double doInBackground(Void... params) {
                try {
                    return Double.valueOf(api.getBuyRate(bioApplication.getCurrency()));
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Double result) {
                super.onPostExecute(result);
                if (result != null) {
                    bioApplication.model.setBuyRate(result);
                    mBuyRate.setText("1BIO ~ " + String.format("%.2f ", 1.0 / result.doubleValue()) +
                            bioApplication.getCurrencySymbol() + "\n" +
                            getResources().getString(R.string.buy_biotransfer_info));
                }
            }

            @Override
            protected void onCancelled(Double result) {
                super.onCancelled(result);
                mBuyRate.setText(R.string.buy_rate_refresh_error);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                mBuyRate.setText(R.string.buy_rate_refresh_error);
            }
        }

        new buyRateAsyncTask().execute();
    }

    private void refreshBuyRateWithAmount(Double amount)  {
        final class buyRateAsyncTask extends AsyncTask<Double, Void, Double> {

            protected bioAPI api = new bioAPI();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                //mBuyRate.setText(R.string.buyraterefresh);
            }

            @Nullable
            @Override
            protected Double doInBackground(Double... params) {
                try {
                    return Double.valueOf(api.getBuyRateWithAmount(bioApplication.getCurrency(), params[0]));
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Double result) {
                super.onPostExecute(result);
                if (result != null) {
                    bioApplication.model.setBuyRate(result);
                    mBuyRate.setText("1BIO ~ " + String.format("%.2f ", 1.0 / result.doubleValue()) +
                            bioApplication.getCurrencySymbol() + "\n" +
                            getResources().getString(R.string.buy_biotransfer_info));
                    try {
                        Double amount = Double.valueOf(mBuyAmount.getText().toString());
                        mBuyButton.setText(getResources().getString(R.string.buyby) + String.format(" %.2f", amount * (1.0 / bioApplication.model.getBuyRate())) + bioApplication.getCurrencySymbol());
                    } catch (Exception ex) {
                        mBuyButton.setText(getResources().getString(R.string.buy));
                    }
                }
            }

            @Override
            protected void onCancelled(Double result) {
                super.onCancelled(result);
                mBuyRate.setText(R.string.buy_rate_refresh_error);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                mBuyRate.setText(R.string.buy_rate_refresh_error);
            }
        }

        new buyRateAsyncTask().execute(amount);
    }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_balance);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        mBalanceView = findViewById(R.id.balance_value);
        mActionButton = findViewById(R.id.button_action);
        mActionButtonReceive = findViewById(R.id.button_receive);
        mActionButtonSend = findViewById(R.id.button_send);
        mActionButtonHistory = findViewById(R.id.button_history);
        mActionButtonReceiveText = findViewById(R.id.button_receive_text);
        mActionButtonSendText = findViewById(R.id.button_send_text);
        mActionButtonHistoryText = findViewById(R.id.button_history_text);
        mActionButtonSettings = findViewById(R.id.button_settings);
        mImageTap = findViewById(R.id.image_tap);

        mLayoutActionButtonReceive = findViewById(R.id.action_layout_receive);
        mLayoutActionButtonSend = findViewById(R.id.action_layout_send);
        mLayoutActionButtonHistory = findViewById(R.id.action_layout_history);

        mViewFlipper = findViewById(R.id.view_flipper);

        mLastHistoryListView = findViewById(R.id.last_history_view);
        mLabelNoOps = findViewById(R.id.label_no_ops);
        mSwipeRefreshLastOps = findViewById(R.id.last_ops_refresh);

        mRatesListView = findViewById(R.id.rates_view);
        mSwipeRefreshRates = findViewById(R.id.rates_refresh);

        mSegmentButtonBIO = findViewById(R.id.segment_button_bio);
        mSegmentButtonRates = findViewById(R.id.segment_button_rates);
        mSegmentButtonBuy = findViewById(R.id.segment_button_buy);
        mSegmentButtonSell = findViewById(R.id.segment_button_sell);

        mActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionAnimation();
            }
        });

        mActionButtonReceive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doReceive();
            }
        });

        mActionButtonReceiveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doReceive();
            }
        });

        mActionButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSend();
            }
        });

        mActionButtonSendText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doSend();
            }
        });

        mActionButtonHistory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHistory();
            }
        });

        mActionButtonHistoryText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doHistory();
            }
        });

        mActionButtonSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionAnimation();
                Intent intent = new Intent(BalanceActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        mBalanceView.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                mSwipeRefreshLastOps.setRefreshing(true);
                doRefresh();
                return true;
            }
        });

        mSwipeRefreshLastOps.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        doRefresh();
                    }
                }
        );

        mSwipeRefreshRates.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshRates();
                    }
                }
        );

        /*mViewFlipper.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    _firstX = (int) event.getX();
                    _firstY = (int) event.getY();
                }

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    int LastX = (int) event.getX();

                    if (_firstX - LastX > SWIPE_MIN_X_DISTANCE) {
                        mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(BalanceActivity.this, R.anim.flip_right_in));
                        mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(BalanceActivity.this, R.anim.flip_left_out));
                        mViewFlipper.showNext();
                        mSelectedSegment++;
                        if (mSelectedSegment > 3) mSelectedSegment = 0;
                        updateButtonState();
                    } else if (LastX - _firstX > SWIPE_MIN_X_DISTANCE) {
                        mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(BalanceActivity.this, R.anim.flip_left_in));
                        mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(BalanceActivity.this, R.anim.flip_right_out));
                        mViewFlipper.showPrevious();
                        mSelectedSegment--;
                        if (mSelectedSegment < 0) mSelectedSegment = 3;
                        updateButtonState();
                    }
                }

                return true;
            }

        });*/

        mSellCardNumber = findViewById(R.id.sell_card_number);
        mSellScan = findViewById(R.id.sell_scan);

        mSellScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scanIntent = new Intent(BalanceActivity.this, CardIOActivity.class);

                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, false);
                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false);
                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false);
                scanIntent.putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, true);
                scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, true);
                scanIntent.putExtra(CardIOActivity.EXTRA_USE_CARDIO_LOGO, true);
                scanIntent.putExtra(CardIOActivity.EXTRA_HIDE_CARDIO_LOGO, true);

                startActivityForResult(scanIntent, REQUEST_SELL_CARD_SCAN);
            }
        });

        mSellAmount = findViewById(R.id.sell_amount);
        mSellButton = findViewById(R.id.sell_button);
        mSellRate = findViewById(R.id.sell_rate);
        mSellInfoButton = findViewById(R.id.sellInfo_button);

        mSellButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkValidForSell()) {
                    findViewById(R.id.fullscreen_balance_wait).setVisibility(View.VISIBLE);
                    doSell();
                }
            }
        });

        mSellInfoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open URL
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.talkbank.io/biocoin/"));
                startActivity(browserIntent);
            }
        });

        mSellCardNumber.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (luhnValid(s.toString()) && s.toString().length() == 16)
                    delayedFocusSellAmount(100);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }

        });

        mSellAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    Double amount = Double.valueOf(s.toString());
                    mSellButton.setText(getResources().getString(R.string.sellfor) + String.format(" %.2f", amount * bioApplication.model.getSellRate()) + bioApplication.getCurrencySymbol());
                } catch (Exception ex) {
                    mSellButton.setText(getResources().getString(R.string.sell));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }

        });

        mBuyCardNumber = findViewById(R.id.buy_card_number);
        mBuyScan = findViewById(R.id.buy_scan);

        mBuyScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent scanIntent = new Intent(BalanceActivity.this, CardIOActivity.class);

                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_EXPIRY, true);
                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_CVV, false);
                scanIntent.putExtra(CardIOActivity.EXTRA_REQUIRE_POSTAL_CODE, false);
                scanIntent.putExtra(CardIOActivity.EXTRA_KEEP_APPLICATION_THEME, true);
                scanIntent.putExtra(CardIOActivity.EXTRA_SUPPRESS_MANUAL_ENTRY, true);
                scanIntent.putExtra(CardIOActivity.EXTRA_USE_CARDIO_LOGO, true);
                scanIntent.putExtra(CardIOActivity.EXTRA_HIDE_CARDIO_LOGO, true);

                startActivityForResult(scanIntent, REQUEST_BUY_CARD_SCAN);
            }
        });

        mBuyCardExp = findViewById(R.id.buy_exp);
        mBuyCardCVV = findViewById(R.id.buy_cvv);
        mBuyAmount = findViewById(R.id.buy_amount);
        mBuyRate = findViewById(R.id.buy_rate);
        mBuyButton = findViewById(R.id.buy_button);

        mBuyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkValidForBuy()) {
                    findViewById(R.id.fullscreen_balance_wait).setVisibility(View.VISIBLE);
                    doBuy();
                }
            }
        });

        mBuyAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    Double amount = Double.valueOf(s.toString());
                    refreshBuyRateWithAmount(amount);
                    mBuyButton.setText(getResources().getString(R.string.buyby) + String.format(" %.2f", amount * (1.0 / bioApplication.model.getBuyRate())) + bioApplication.getCurrencySymbol());
                } catch (Exception ex) {
                    mBuyButton.setText(getResources().getString(R.string.buy));
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }

        });

        mBuyCardExp.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 4)
                    delayedFocusBuyCardCVV(100);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }

        });

        mBuyCardNumber.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (luhnValid(s.toString()) && s.toString().length() == 16)
                    delayedFocusBuyCardExp(100);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }

        });

        mBuyCardCVV.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().length() == 3)
                    delayedFocusBuyAmount(100);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {

            }

        });

        mWebView = findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);

        class sibWebViewClient extends WebViewClient {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("https://sib.cards/WS/State")) {
                    Uri uri = Uri.parse(url);
                    bioApplication.model.setBuyOpKey(uri.getQueryParameter("OpKey"));
                    mWebView.setVisibility(View.INVISIBLE);
                    checkBuyOp();
                    bioApplication.setInactiveSeconds(Variables.inactiveSecondsDefault);
                }
                return false;
            }
        }

        mWebView.setWebViewClient(new sibWebViewClient());

        try {
            String opkey = bioApplication.model.getBuyOpKey();
            if (opkey != null && !opkey.equals(""))
                mWebView.setVisibility(View.VISIBLE);
        } catch (Exception ex) {

        }
    }

    @Contract(pure = true)
    private boolean checkValidForSell() {
        try {
            if (bioApplication.model.getSellRate() == null || bioApplication.model.getSellRate().doubleValue() <= 0.0)
                throw new Exception(getResources().getString(R.string.sellRateEmpty));
            Double dbl = Double.valueOf(mSellAmount.getText().toString());
            if (dbl.doubleValue() + Variables.commissionDefault > bioApplication.model.getBalance().doubleValue())
                throw new Exception(getResources().getString(R.string.exceptionSumBig));
            if (!luhnValid(mSellCardNumber.getText().toString()))
                throw new Exception(getResources().getString(R.string.exceptionInvalidCardNumber));
            return true;
        } catch (Exception ex) {
            showError(ex);
            return false;
        }
    }

    @Contract(pure = true)
    private boolean checkValidForBuy() {
        try {
            if (bioApplication.model.getBuyRate() == null || 1.0 / bioApplication.model.getBuyRate().doubleValue() <= 0.0)
                throw new Exception(getResources().getString(R.string.buyRateEmpty));
            Double dbl = Double.valueOf(mBuyAmount.getText().toString());
            if (!luhnValid(mBuyCardNumber.getText().toString()))
                throw new Exception(getResources().getString(R.string.exceptionInvalidCardNumber));
            return true;
        } catch (Exception ex) {
            showError(ex);
            return false;
        }
    }

    private void doRefresh() {
        refreshBalance();
        refreshMemPool();
    }

    private void actionAnimation() {
        float deg = mActionButton.getRotation() + 270F;
        mActionButton.animate().rotation(deg).setInterpolator(new AccelerateDecelerateInterpolator());
        if (mImageTap.getVisibility() == View.VISIBLE) {
            mImageTap.setVisibility(View.INVISIBLE);
            mLayoutActionButtonReceive.setVisibility(View.VISIBLE);
            mLayoutActionButtonSend.setVisibility(View.VISIBLE);
            mLayoutActionButtonHistory.setVisibility(View.VISIBLE);
            mActionButtonSettings.setVisibility(View.VISIBLE);
        } else {
            mImageTap.setVisibility(View.VISIBLE);
            mLayoutActionButtonReceive.setVisibility(View.INVISIBLE);
            mLayoutActionButtonSend.setVisibility(View.INVISIBLE);
            mLayoutActionButtonHistory.setVisibility(View.INVISIBLE);
            mActionButtonSettings.setVisibility(View.INVISIBLE);
        }
    }

    private void doReceive() {
        actionAnimation();
        Intent intent = new Intent(BalanceActivity.this, ReceiveActivity.class);
        startActivity(intent);
    }

    private void doSend() {
        actionAnimation();
        Intent intent = new Intent(BalanceActivity.this, SendActivity.class);
        startActivity(intent);
    }

    private void doHistory() {
        actionAnimation();
        Intent intent = new Intent(BalanceActivity.this, HistoryActivity.class);
        startActivity(intent);
    }

    private void doSell() {
        hideKeyboard();

        final String currency = bioApplication.getCurrency();
        final Double sellRate = bioApplication.model.getSellRate();
        final Double amountSIB = Double.valueOf(mSellAmount.getText().toString());
        final Double amount = amountSIB * sellRate;
        final String pan = mSellCardNumber.getText().toString();

        final class processSellAsyncTask extends AsyncTask<Void, Void, String> {

            protected bioAPI api = new bioAPI();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                mSellCardNumber.setText("");
                mSellAmount.setText("");
            }

            @Nullable
            @Override
            protected String doInBackground(Void... params) {
                try {
                    return api.processSell(currency, amountSIB, amount, pan);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                String Address = result;
                sendSIBToAddress(Address, amountSIB);
            }

            @Override
            protected void onCancelled(String result) {
                super.onCancelled(result);
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE);
            }
        }

        new processSellAsyncTask().execute();
    }

    private void doBuy() {
        hideKeyboard();

        final String currency = bioApplication.getCurrency();
        final Double buyRate = bioApplication.model.getBuyRate();
        final Double amountSIB = Double.valueOf(mBuyAmount.getText().toString());
        final Double amount = amountSIB * (1.0 / buyRate);
        final String pan = mBuyCardNumber.getText().toString();
        final String exp = mBuyCardExp.getText().toString();
        final String cvv = mBuyCardCVV.getText().toString();

        final class processBuyAsyncTask extends AsyncTask<Void, Void, bioBuyState> {

            protected bioAPI api = new bioAPI();
            protected String account = "";
            protected String address = "";

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                bioApplication.model.setBuyOpKey("");

                try {
                    List<Address> addrs = bioApplication.model.getAddressesForInput();
                    account = addrs.get(0).getAddress();
                    address = addrs.get(addrs.size()-1).getAddress();
                    mBuyCardNumber.setText("");
                    mBuyCardExp.setText("");
                    mBuyCardCVV.setText("");
                    mBuyAmount.setText("");
                } catch (Exception ex) {
                    this.cancel(true);
                }
            }

            @Nullable
            @Override
            protected bioBuyState doInBackground(Void... params) {
                try {
                    return api.processBuy(currency, amountSIB, amount, pan, exp, cvv, account, address);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(bioBuyState result) {
                super.onPostExecute(result);
                processBuyState(result);
            }

            @Override
            protected void onCancelled(bioBuyState result) {
                super.onCancelled(result);
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE);
            }
        }

        new processBuyAsyncTask().execute();
    }

    public void segmentButtonClick(View view) {
        delayedHide(100);
        int nOldSelectedSegment = mSelectedSegment;
        int nNewSelectedSegment = nOldSelectedSegment;
        if (view == mSegmentButtonBIO) {
            nNewSelectedSegment = 0;
        }
        if (view == mSegmentButtonRates) {
            nNewSelectedSegment = 1;
        }
        if (view == mSegmentButtonBuy) {
            nNewSelectedSegment = 2;
        }
        if (view == mSegmentButtonSell) {
            nNewSelectedSegment = 3;
        }
        int delta = nOldSelectedSegment > nNewSelectedSegment ? -1 : 1;
        while (mSelectedSegment != nNewSelectedSegment) {
            if (mSelectedSegment + delta == nNewSelectedSegment) {
                mViewFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flip_right_in));
                mViewFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flip_left_out));
            }
            if (delta > 0) {
                mViewFlipper.showNext();
            } else {
                mViewFlipper.showPrevious();
            }
            mSelectedSegment += delta;
            updateButtonState();
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);

        mSelectedSegment = mViewFlipper.getDisplayedChild();
        updateButtonState();

        doRefresh();
    }

    @Override
    protected void onResume() {
        super.onResume();

        bioApplication.setInactiveSeconds(Variables.inactiveSecondsDefault);

        delayedHide(100);

        mSelectedSegment = mViewFlipper.getDisplayedChild();
        updateButtonState();

        doRefresh();
        refreshRates();
        refreshBuyRate();
        refreshSellRate();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SELL_CARD_SCAN) {
            if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
                CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
                mSellCardNumber.setText(scanResult.cardNumber);
                delayedFocusSellAmount(100);
            }
        }

        if (requestCode == REQUEST_BUY_CARD_SCAN) {
            if (data != null && data.hasExtra(CardIOActivity.EXTRA_SCAN_RESULT)) {
                CreditCard scanResult = data.getParcelableExtra(CardIOActivity.EXTRA_SCAN_RESULT);
                mBuyCardNumber.setText(scanResult.cardNumber);
                if (scanResult.isExpiryValid()) {
                    mBuyCardExp.setText(String.format("%02d%02d", scanResult.expiryMonth, scanResult.expiryYear - (scanResult.expiryYear > 2000 ? 2000 : 0)));
                    delayedFocusBuyCardCVV(100);
                } else {
                    delayedFocusBuyCardExp(100);
                }
            }

        }
    }

    private void deselectAllSegments() {
        mSegmentButtonBIO.setBackground(getResources().getDrawable(R.drawable.segment_button_start, this.getTheme()));
        mSegmentButtonBIO.setTextColor(getResources().getColor(R.color.colorWhite, this.getTheme()));
        mSegmentButtonRates.setBackground(getResources().getDrawable(R.drawable.segment_button_middle, this.getTheme()));
        mSegmentButtonRates.setTextColor(getResources().getColor(R.color.colorWhite, this.getTheme()));
        mSegmentButtonBuy.setBackground(getResources().getDrawable(R.drawable.segment_button_middle, this.getTheme()));
        mSegmentButtonBuy.setTextColor(getResources().getColor(R.color.colorWhite, this.getTheme()));
        mSegmentButtonSell.setBackground(getResources().getDrawable(R.drawable.segment_button_end, this.getTheme()));
        mSegmentButtonSell.setTextColor(getResources().getColor(R.color.colorWhite, this.getTheme()));
    }

    private void updateButtonState() {
        deselectAllSegments();
        switch (mSelectedSegment) {
            case 1:
                mSegmentButtonRates.setBackground(getResources().getDrawable(R.drawable.segment_button_middle_selected, this.getTheme()));
                mSegmentButtonRates.setTextColor(getResources().getColor(R.color.colorBlack, this.getTheme()));
                refreshRates();
                break;
            case 2:
                mSegmentButtonBuy.setBackground(getResources().getDrawable(R.drawable.segment_button_middle_selected, this.getTheme()));
                mSegmentButtonBuy.setTextColor(getResources().getColor(R.color.colorBlack, this.getTheme()));
                refreshBuyRate();
                break;
            case 3:
                mSegmentButtonSell.setBackground(getResources().getDrawable(R.drawable.segment_button_end_selected, this.getTheme()));
                mSegmentButtonSell.setTextColor(getResources().getColor(R.color.colorBlack, this.getTheme()));
                refreshSellRate();
                break;
            default:
                mSegmentButtonBIO.setBackground(getResources().getDrawable(R.drawable.segment_button_start_selected, this.getTheme()));
                mSegmentButtonBIO.setTextColor(getResources().getColor(R.color.colorBlack, this.getTheme()));
                doRefresh();
                break;
        }
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        //Hide UI first
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    private void delayedFocusSellAmount(int delayMillis) {
        mSellAmountFocusHandler.removeCallbacks(mSellAmountFocusRunnable);
        mSellAmountFocusHandler.postDelayed(mSellAmountFocusRunnable, delayMillis);
    }

    private void delayedFocusBuyCardExp(int delayMillis) {
        mBuyExpFocusHandler.removeCallbacks(mBuyExpFocusRunnable);
        mBuyExpFocusHandler.postDelayed(mBuyExpFocusRunnable, delayMillis);
    }

    private void delayedFocusBuyCardCVV(int delayMillis) {
        mBuyCVVFocusHandler.removeCallbacks(mBuyCVVFocusRunnable);
        mBuyCVVFocusHandler.postDelayed(mBuyCVVFocusRunnable, delayMillis);
    }

    private void delayedFocusBuyAmount(int delayMillis) {
        mBuyAmountFocusHandler.removeCallbacks(mBuyAmountFocusRunnable);
        mBuyAmountFocusHandler.postDelayed(mBuyAmountFocusRunnable, delayMillis);
    }

    private boolean luhnValid(String number) {
        int sum = 0;
        boolean alternate = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = Integer.parseInt(number.substring(i, i + 1));
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n = (n % 10) + 1;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return (sum % 10 == 0);
    }

    private void sendSIBToAddress(final String Address, final Double amountSIB) {

        final class unspentTransactionsAsyncTask extends AsyncTask<Void, Void, ArrayList<bioUnspentTransaction>> {

            protected bioAPI api = new bioAPI();
            protected String[] addresses = new String[0];

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                ArrayList<String> addrs = new ArrayList<String>();
                try {

                    for (Address a : bioApplication.model.getAddresses()) {
                        addrs.add(a.getAddress());
                    }

                    addresses = addrs.toArray(new String[0]);

                } catch (Exception ex) {
                    this.cancel(true);
                }
            }

            @Nullable
            @Override
            protected ArrayList<bioUnspentTransaction> doInBackground(Void... params) {
                try {
                    return api.getUnspentTransactions(addresses);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<bioUnspentTransaction> result) {
                super.onPostExecute(result);
                if (result != null) {
                    try {

                        bioTransaction tx = prepareTransaction(result.toArray(new bioUnspentTransaction[0]), amountSIB, Address);
                        sendTransaction(tx);

                    } catch (Exception ex) {
                        findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
                    }
                }
            }

            @Override
            protected void onCancelled(ArrayList<bioUnspentTransaction> result) {
                super.onCancelled(result);
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
            }
        }

        hideKeyboard();

        new unspentTransactionsAsyncTask().execute();

    }

    private bioTransaction prepareTransaction(bioUnspentTransaction[] unspent, Double amount, String Address) throws Exception {
        Double spent = 0.0;
        Double commission = Variables.commissionDefault;

        bioTransaction tx = new bioTransaction();
        tx.addOutput(Address, amount);

        for (bioUnspentTransaction u: unspent) {
            if (spent < amount + commission) {
                spent += u.Amount;
                tx.addInput(u);
            } else {
                break;
            }
        }
        tx.addChange(spent - amount - commission);
        return tx;
    }

    private void sendTransaction(bioTransaction tx) throws Exception {
        bioApplication.model.storeWallet(tx.getChange(), (short)1);
        int[] sign = tx.sign(bioApplication.model.getAddresses().toArray(new Address[0]));

        final class broadcastTransactionAsyncTask extends AsyncTask<int[], Void, bioBroadcastTransactionResult> {

            protected bioAPI api = new bioAPI();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Nullable
            @Override
            protected bioBroadcastTransactionResult doInBackground(int[]... params) {
                try {
                    return api.broadcastTransaction(params[0]);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(bioBroadcastTransactionResult result) {
                super.onPostExecute(result);
                String message = "";
                if (result.IsBroadcasted) {
                    message = getResources().getString(R.string.successBroadcasted) + " " + result.TransactionId;
                } else {
                    message = result.Message;
                }

                final String txid = result.TransactionId;

                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );

                AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                builder.setTitle(R.string.alertDialogBroadcastTitle)
                        .setMessage(message)
                        .setCancelable(false)
                        .setNeutralButton(R.string.CopyToClipboard,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();

                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText(getResources().getString(R.string.bioTransactionId), txid);
                                        clipboard.setPrimaryClip(clip);

                                        AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                                        builder.setTitle(R.string.alertDialogClipboardTitle)
                                                .setMessage(R.string.alertDialogClipboardMessage)
                                                .setCancelable(false)
                                                .setNegativeButton(R.string.OK,
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                dialog.cancel();
                                                            }
                                                        });
                                        AlertDialog alert = builder.create();
                                        alert.show();
                                    }
                                })
                        .setNegativeButton(R.string.OK,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                AlertDialog alert = builder.create();
                alert.show();

                refreshMemPool();
                segmentButtonClick(mSegmentButtonBIO);
            }

            @Override
            protected void onCancelled(bioBroadcastTransactionResult result) {
                super.onCancelled(result);
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
            }
        }

        new broadcastTransactionAsyncTask().execute(sign);
    }

    private void processBuyState(bioBuyState state) {
        if (state.State.equals("Redirect") && !state.RedirectUrl.equals("")) {
            bioApplication.setInactiveSeconds(600);
            mWebView.setVisibility(View.VISIBLE);
            mWebView.loadUrl(state.RedirectUrl);
            return;
        }

        switch (state.State) {
            case "ERROR":
                showError(new Exception(getResources().getString(R.string.buyOpCheckError)));
                bioApplication.model.setBuyOpKey("");
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
                break;
            case "Done":
                final String opkey = bioApplication.model.getBuyOpKey();
                String message = getResources().getString(R.string.buyOpDone) + "\n" + "OpKey: " + opkey;
                AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                builder.setTitle(R.string.alertDialogBuyTitle)
                        .setMessage(message)
                        .setCancelable(false)
                        .setNeutralButton(R.string.CopyToClipboard,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();

                                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                                        ClipData clip = ClipData.newPlainText(getResources().getString(R.string.bioTransactionId), opkey);
                                        clipboard.setPrimaryClip(clip);

                                        AlertDialog.Builder builder = new AlertDialog.Builder(BalanceActivity.this);
                                        builder.setTitle(R.string.alertDialogClipboardTitle)
                                                .setMessage(R.string.alertDialogClipboardMessage)
                                                .setCancelable(false)
                                                .setNegativeButton(R.string.OK,
                                                        new DialogInterface.OnClickListener() {
                                                            public void onClick(DialogInterface dialog, int id) {
                                                                dialog.cancel();
                                                            }
                                                        });
                                        AlertDialog alert = builder.create();
                                        alert.show();                                    }
                                })
                        .setNegativeButton(R.string.OK,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();

                bioApplication.model.setBuyOpKey("");
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
                break;
            case "Cancel":
                showMessage(getResources().getString(R.string.buyOpCancel));
                bioApplication.model.setBuyOpKey("");
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE );
                break;
            default:
                checkBuyOp();
                break;
        }
    }

    private void checkBuyOp() {
        final class checkBuyOpAsyncTask extends AsyncTask<Void, Void, bioBuyState> {

            protected bioAPI api = new bioAPI();
            protected String opKey = bioApplication.model.getBuyOpKey();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Nullable
            @Override
            protected bioBuyState doInBackground(Void... params) {
                try {
                    return api.checkOperation(opKey);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(bioBuyState result) {
                super.onPostExecute(result);
                processBuyState(result);
            }

            @Override
            protected void onCancelled(bioBuyState result) {
                super.onCancelled(result);
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                findViewById(R.id.fullscreen_balance_wait).setVisibility(View.INVISIBLE);
            }
        }

        new checkBuyOpAsyncTask().execute();
    }
}

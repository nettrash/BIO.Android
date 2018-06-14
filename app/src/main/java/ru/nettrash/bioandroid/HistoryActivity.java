package ru.nettrash.bioandroid;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Map;

import ru.nettrash.bio.classes.bioHistoryItem;
import ru.nettrash.bio.database.Address;
import ru.nettrash.bio.bioAPI;

public class HistoryActivity extends BaseActivity {

    private int HISTORY_MAX_COUNT = 5000;

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
    private ListView mHistoryListView;
    private SwipeRefreshLayout mSwipeRefreshHistory;
    private ArrayList<bioHistoryItem> mHistory;

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
        setContentView(R.layout.activity_history);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);

        mHistoryListView = findViewById(R.id.history_view);
        mHistoryListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        registerForContextMenu(mHistoryListView);
        mSwipeRefreshHistory = findViewById(R.id.history_refresh);


        mSwipeRefreshHistory.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        refreshHistory();
                    }
                }
        );

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
        refreshHistory();
    }

    @Override
    protected void onResume() {
        super.onResume();
        delayedHide(100);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.history_list, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        String txId = mHistory.get(info.position).txId;
        switch (item.getItemId()) {
            case R.id.historyItemMenuDetails:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://block-explorer.biocoin.bio/tx/" + txId));
                startActivity(browserIntent);
                return true;
            case R.id.historyItemMenuCopy:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getResources().getString(R.string.bioTransactionId), txId);
                clipboard.setPrimaryClip(clip);

                AlertDialog.Builder builder = new AlertDialog.Builder(HistoryActivity.this);
                builder.setTitle(R.string.alertDialogClipboardTitle)
                        .setMessage(R.string.alertDialogClipboardMessage)
                        .setCancelable(false)
                        .setNegativeButton(R.string.OK,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        delayedHide(100);
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            default:
                return super.onContextItemSelected(item);
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
        // Hide UI first
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

    private void refreshHistory() {

        final HistoryActivity self = this;

        final class historyAsyncTask extends AsyncTask<Void, Void, ArrayList<bioHistoryItem>> {

            protected bioAPI api = new bioAPI();
            protected String[] addresses = new String[0];
            protected String[] addressesInput = new String[0];
            protected String[] addressesChange = new String[0];

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mSwipeRefreshHistory.setRefreshing(true);

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

            @Override
            protected ArrayList<bioHistoryItem> doInBackground(Void... params) {
                try {
                    return api.getLastTransactions(HISTORY_MAX_COUNT, addresses, addressesInput, addressesChange);
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<bioHistoryItem> result) {
                super.onPostExecute(result);
                if (result != null) {
                    HistoryActivity.this.mHistory = result;
                    ArrayList<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
                    for (bioHistoryItem item: result) {
                        data.add(item.getHashMap());
                    }
                    SimpleAdapter adapter = new SimpleAdapter(self, data, R.layout.history_item, bioHistoryItem.getListAdapterFrom(), bioHistoryItem.getListAdapterTo());
                    mHistoryListView.setAdapter(adapter);
                }
                mSwipeRefreshHistory.setRefreshing(false);
            }

            @Override
            protected void onCancelled(ArrayList<bioHistoryItem> result) {
                super.onCancelled(result);
                mSwipeRefreshHistory.setRefreshing(false);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
                mSwipeRefreshHistory.setRefreshing(false);
            }
        }

        new historyAsyncTask().execute();

    }
}

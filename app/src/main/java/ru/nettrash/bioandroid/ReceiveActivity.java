package ru.nettrash.bioandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.UUID;

import ru.nettrash.bio.database.Address;
import ru.nettrash.bio.bioAPI;
import ru.nettrash.bio.bioWallet;

public class ReceiveActivity extends BaseActivity {

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
    private TextView mAddressView;
    private EditText mAmountView;
    private ImageButton mButtonShareView;

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
            mContentView.requestFocus();
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
        setContentView(R.layout.activity_receive);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        mAddressView = findViewById(R.id.receive_address_value);
        mAmountView = findViewById(R.id.receive_amount_value);
        mButtonShareView = findViewById(R.id.btn_share);

        try {
            Address address = bioApplication.model.getAddressForInput();
            mAddressView.setText(address.getAddress());
        } catch (Exception ex) {
            mAddressView.setText("");
        }

        final ReceiveActivity self = this;

        mAddressView.setOnTouchListener(new View.OnTouchListener() {

            public boolean onTouch(View v, MotionEvent event) {

                if (event.getAction() == MotionEvent.ACTION_UP) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(getResources().getString(R.string.bioAddress), mAddressView.getText());
                    clipboard.setPrimaryClip(clip);

                    AlertDialog.Builder builder = new AlertDialog.Builder(self);
                    builder.setTitle(R.string.alertDialogClipboardTitle)
                            .setMessage(R.string.alertDialogClipboardMessage)
                            .setCancelable(false)
                            .setNegativeButton(R.string.OK,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            self.hide();
                                            dialog.cancel();
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
                return true;
            }

        });

        mAmountView.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
                refreshQR();
            }

        });

        mAmountView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    hide();
                }
                return false;
            }
        });

        mButtonShareView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    ReceiveActivity.this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                } catch (Exception ex) {
                }
            }
        });

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
        refreshQR();
        refreshInputAddress();
    }

    @Override
    protected void onResume() {
        super.onResume();
        delayedHide(100);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    shareImage();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }

    }

    private void shareImage() {

        //Share image bitmap
        try {
            ImageView img = findViewById(R.id.image_receive_qr);
            img.setDrawingCacheEnabled(true);
            Bitmap mBitmap = img.getDrawingCache();

            File cachePath = File.createTempFile("bioQR", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            try
            {
                cachePath.createNewFile();
                FileOutputStream ostream = new FileOutputStream(cachePath);
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                ostream.close();
            }
            catch (Exception e)
            {
                throw e;
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), "ru.nettrash.bioandroid.fileprovider", cachePath));
            startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.addressShareTitle)));
        } catch (Exception ex) {
            showError(ex);
        }

    }

    private void refreshQR() {
        final class qrAsyncTask extends AsyncTask<String, Void, Bitmap> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected Bitmap doInBackground(String... params) {
                QRCodeWriter writer = new QRCodeWriter();
                try {
                    BitMatrix bitMatrix = writer.encode(params[0], BarcodeFormat.QR_CODE, 512, 512);
                    int width = bitMatrix.getWidth();
                    int height = bitMatrix.getHeight();
                    Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                    for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                            bmp.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                        }
                    }
                    return bmp;

                } catch (WriterException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                ImageView img = findViewById(R.id.image_receive_qr);
                img.setImageBitmap(result);
            }

            @Override
            protected void onCancelled(Bitmap result) {
                super.onCancelled(result);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }
        }

        new qrAsyncTask().execute("biocoin:"+mAddressView.getText().toString()+(!mAmountView.getText().toString().equals("") ? "?amount="+mAmountView.getText().toString() : ""));
    }

    private void refreshInputAddress()  {
        final class hasInputAsyncTask extends AsyncTask<String, Void, Boolean> {

            protected bioAPI api = new bioAPI();

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Nullable
            @Override
            protected Boolean doInBackground(String... params) {
                try {
                    return Boolean.valueOf(api.checkInputExists(params[0]));
                } catch (Exception ex) {
                    this.cancel(true);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (result.booleanValue()) {
                    try {
                        bioWallet wallet = new bioWallet();
                        wallet.initialize(UUID.randomUUID().toString());
                        bioApplication.model.storeWallet(wallet, (short)0);
                        mAddressView.setText(wallet.Address);
                        refreshQR();
                    } catch (Exception ex) {

                    }
                }
            }

            @Override
            protected void onCancelled(Boolean result) {
                super.onCancelled(result);
            }

            @Override
            protected void onCancelled() {
                super.onCancelled();
            }
        }

        new hasInputAsyncTask().execute(mAddressView.getText().toString());
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
}

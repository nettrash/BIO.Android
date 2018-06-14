package ru.nettrash.bioandroid;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.app.AlertDialog;

import com.google.zxing.integration.android.IntentIntegrator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import ru.nettrash.crypto.MD5;
import ru.nettrash.crypto.SHA256;
import ru.nettrash.bio.database.Address;
import ru.nettrash.bio.bioWallet;
import ru.nettrash.util.Arrays;

public class SettingsActivity extends BaseActivity {

    private final int REQUEST_CODE_SHARE = 0;
    private final int REQUEST_CODE_LOAD = 1;

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
    private Button mSaveKeys;
    private Button mLoadKeys;
    private RadioGroup mCurrency;

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
        setContentView(R.layout.activity_settings);

        mVisible = true;
        mContentView = findViewById(R.id.fullscreen_content);
        mSaveKeys = findViewById(R.id.save_keys);
        mLoadKeys = findViewById(R.id.load_keys);
        mCurrency = findViewById(R.id.currency_radiogroup);

        switch (bioApplication.getCurrency()) {
            case "USD":
                mCurrency.check(R.id.currency_usd_radiobutton);
                break;
            case "EUR":
                mCurrency.check(R.id.currency_eur_radiobutton);
                break;
            default:
                mCurrency.check(R.id.currency_rub_radiobutton);
                break;
        }

        mCurrency.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.currency_usd_radiobutton:
                        bioApplication.setCurrency("USD");
                        break;
                    case R.id.currency_eur_radiobutton:
                        bioApplication.setCurrency("EUR");
                        break;
                    default:
                        bioApplication.setCurrency("RUB");
                        break;
                }
            }
        });

        mSaveKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try {
                    findViewById(R.id.fullscreen_wait).setVisibility(View.VISIBLE);
                    SettingsActivity.this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                } catch (Exception ex) {
                }
            }
        });

        mLoadKeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
                // browser.
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

                // Filter to only show results that can be "opened", such as a
                // file (as opposed to a list of contacts or timezones)
                intent.addCategory(Intent.CATEGORY_OPENABLE);

                // Filter to show only images, using the image MIME data type.
                // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
                // To search for all documents available via installed storage providers,
                // it would be "*/*".
                intent.setType("*/*");

                startActivityForResult(intent, REQUEST_CODE_LOAD);
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
    }

    @Override
    protected void onResume() {
        bioApplication.setInactiveSeconds(600);

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
                    saveKeys();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    findViewById(R.id.fullscreen_wait).setVisibility(View.INVISIBLE);

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE_SHARE) {
            findViewById(R.id.fullscreen_wait).setVisibility(View.INVISIBLE);
        }
        if (requestCode == REQUEST_CODE_LOAD) {
            if (resultCode == Activity.RESULT_OK) {
                // The document selected by the user won't be returned in the intent.
                // Instead, a URI to that document will be contained in the return intent
                // provided to this method as a parameter.
                // Pull that URI using resultData.getData().
                Uri uri = null;
                if (intent != null) {
                    uri = intent.getData();
                    if (uri != null) {
                        findViewById(R.id.fullscreen_wait).setVisibility(View.VISIBLE);
                        loadKeys(uri);
                    }
                }
            }

        }
    }

    private void saveKeys() {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingsActivity.this);
        alertDialog.setTitle(getResources().getString(R.string.dialogPasswordTitle));
        alertDialog.setMessage(getResources().getString(R.string.dialogPasswordMessage));

        final EditText input = new EditText(SettingsActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        alertDialog.setView(input);
        alertDialog.setNegativeButton(R.string.Cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        alertDialog.setPositiveButton(R.string.OK,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        String password = input.getText().toString();

                        if (!password.equals("")) {
                            findViewById(R.id.fullscreen_wait).setVisibility(View.VISIBLE);
                            try {
                                ArrayList<String> v = new ArrayList<String>();
                                String hs = "";
                                for (Address a : SettingsActivity.this.bioApplication.model.getAddresses()) {
                                    String s = String.format("%d", a.getAddressType());

                                    int[] key = a.getPrivateKey();
                                    String b64key = Base64.encodeToString(Arrays.toByteArray(key), Base64.NO_WRAP);

                                    String iv = "00000020219510518024419136177230";
                                    MD5 md5 = new MD5();
                                    md5.update(iv.getBytes());
                                    byte[] ivb = md5.digest();

                                    md5 = new MD5();
                                    md5.update(password.getBytes());
                                    byte[] passwordb = md5.digest();

                                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");

                                    SecretKeySpec secretKeySpec = new SecretKeySpec(passwordb, "AES");
                                    IvParameterSpec ivParameterSpec = new IvParameterSpec(ivb);
                                    cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);

                                    byte[] encrypted = cipher.doFinal(b64key.getBytes());

                                    s += Base64.encodeToString(encrypted, Base64.NO_WRAP);
                                    hs += s;
                                    v.add(s);
                                }
                                hs += password;
                                SHA256 sha256 = new SHA256();
                                sha256.update(hs.getBytes());
                                byte[] hash = sha256.digest();
                                String b64Hash = Base64.encodeToString(hash, Base64.NO_WRAP);

                                JSONObject keyData = new JSONObject();
                                keyData.put("version", "1.1");
                                keyData.put("hash", b64Hash);
                                keyData.put("keys", new JSONArray(v));

                                File cachePath = File.createTempFile("keys", ".bio", getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS));
                                //save to file and share
                                try
                                {
                                    cachePath.createNewFile();
                                    FileOutputStream ostream = new FileOutputStream(cachePath);
                                    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(ostream);
                                    outputStreamWriter.write(keyData.toString());
                                    outputStreamWriter.close();
                                    ostream.close();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    throw e;
                                }


                                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                shareIntent.setType("*/*");
                                shareIntent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getApplicationContext(), "ru.nettrash.bioandroid.fileprovider", cachePath));
                                startActivity(Intent.createChooser(shareIntent, getResources().getString(R.string.keysShareTitle)));

                                findViewById(R.id.fullscreen_wait).setVisibility(View.INVISIBLE);
                            } catch (Exception ex) {
                                findViewById(R.id.fullscreen_wait).setVisibility(View.INVISIBLE);
                                showError(ex);
                            }
                        }
                    }
                });
        alertDialog.show();
    }

    private void loadKeys(Uri uri) {

        final Uri fileUri = uri;

        findViewById(R.id.fullscreen_wait).setVisibility(View.VISIBLE);
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(SettingsActivity.this);
        alertDialog.setTitle(getResources().getString(R.string.dialogPasswordTitle));
        alertDialog.setMessage(getResources().getString(R.string.dialogPasswordMessage));

        final EditText input = new EditText(SettingsActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        input.setLayoutParams(lp);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setTransformationMethod(PasswordTransformationMethod.getInstance());
        alertDialog.setView(input);
        alertDialog.setNegativeButton(R.string.Cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        findViewById(R.id.fullscreen_wait).setVisibility(View.INVISIBLE);
                        dialog.cancel();
                    }
                });
        alertDialog.setPositiveButton(R.string.OK,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        String password = input.getText().toString();

                        if (!password.equals("")) {
                            try {
                                InputStream inputStream = getContentResolver().openInputStream(fileUri);
                                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                                StringBuilder stringBuilder = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    stringBuilder.append(line);
                                }
                                inputStream.close();
                                JSONObject keysData = new JSONObject(stringBuilder.toString());
                                String version = keysData.getString("version");
                                if (!version.equals("1.1")) {
                                    throw new Exception(getResources().getString(R.string.keystoreLoadErrorUnsupportedVersion));
                                }
                                String hash = keysData.getString("hash");
                                JSONArray keys = keysData.getJSONArray("keys");
                                String hs = "";
                                for (int idx=0; idx < keys.length(); idx++) {
                                    hs += keys.getString(idx);
                                }
                                hs += password;
                                SHA256 sha256 = new SHA256();
                                sha256.update(hs.getBytes());
                                byte[] bhash = sha256.digest();
                                String b64Hash = Base64.encodeToString(bhash, Base64.NO_WRAP);
                                if (!b64Hash.equals(hash))
                                    throw new Exception(getResources().getString(R.string.keystoreLoadErrorInvalidPassword));

                                for (int idx=0; idx < keys.length(); idx++) {
                                    String keyinfo = keys.getString(idx);
                                    short keyType = Short.valueOf(String.valueOf(keyinfo.charAt(0))).shortValue();
                                    String pkeyencrypted = keyinfo.substring(1);

                                    String iv = "00000020219510518024419136177230";
                                    MD5 md5 = new MD5();
                                    md5.update(iv.getBytes());
                                    byte[] ivb = md5.digest();

                                    md5 = new MD5();
                                    md5.update(password.getBytes());
                                    byte[] passwordb = md5.digest();

                                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");

                                    SecretKeySpec secretKeySpec = new SecretKeySpec(passwordb, "AES");
                                    IvParameterSpec ivParameterSpec = new IvParameterSpec(ivb);
                                    cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);

                                    byte[] decrypted = cipher.doFinal(Base64.decode(pkeyencrypted, Base64.NO_WRAP));
                                    int[] privateKey = Arrays.toUnsignedByteArray(Base64.decode(decrypted, Base64.NO_WRAP));

                                    bioApplication.model.storeWallet(new bioWallet(privateKey), keyType);
                                }
                            } catch (Exception ex) {
                                showError(ex);
                                findViewById(R.id.fullscreen_wait).setVisibility(View.INVISIBLE);
                                return;
                            }
                            showMessage(getResources().getString(R.string.alertKeyStoreLoadedMessage));
                        }
                        findViewById(R.id.fullscreen_wait).setVisibility(View.INVISIBLE);
                    }
                });
        alertDialog.show();
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

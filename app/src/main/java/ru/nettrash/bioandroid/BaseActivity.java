package ru.nettrash.bioandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.FragmentManager;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Layout;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Date;

import ru.nettrash.bio.classes.bioUnspentTransaction;
import ru.nettrash.bio.database.Address;
import ru.nettrash.bio.bioAPI;
import ru.nettrash.bio.bioBroadcastTransactionResult;
import ru.nettrash.bio.bioTransaction;

/**
 * Created by nettrash on 20.01.2018.
 */

class BaseActivity extends Activity {

    private EditText mPINEditor;
    private final Handler mPINFocusHandler = new Handler();
    private final Runnable mPINFocusRunnable = new Runnable() {
        @Override
        public void run() {

            mPINEditor.setFocusableInTouchMode(true);
            mPINEditor.requestFocus();

            final InputMethodManager inputMethodManager = (InputMethodManager) BaseActivity.this
                    .getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(mPINEditor, InputMethodManager.SHOW_IMPLICIT);

        }
    };

    protected BIOApplication bioApplication;
    protected FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bioApplication = (BIOApplication)getApplicationContext();
        if (bioApplication.model == null) {
            bioApplication.initialize();
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransitionExit();
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        overridePendingTransitionEnter();
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            if (!bioApplication.model.firstRun() && !bioApplication.needSetPIN() && !setupActivity()) {
                String lastStoppedActivityClassName = bioApplication.getLastStoppedActivityClassName();
                if (lastStoppedActivityClassName.equals(this.getClass().getName()) || lastStoppedActivityClassName.equals("") || lastStoppedActivityClassName.equals("ru.nettrash.sibliteandroid.RootActivity")) {
                    if (bioApplication.needCheckPIN() || lastStoppedActivityClassName.equals("ru.nettrash.sibliteandroid.RootActivity")) {
                        checkPIN();
                    }
                }
            }
        } catch (Exception ex) {

        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!bioApplication.model.firstRun() && !bioApplication.needSetPIN() && !setupActivity()) {
            bioApplication.setLastStopDate(new Date());
            bioApplication.setLastStoppedActivityClassName(this.getClass().getName());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!bioApplication.model.firstRun() && !bioApplication.needSetPIN() && !setupActivity()) {
            bioApplication.setLastStopDate(new Date());
            bioApplication.setLastStoppedActivityClassName(this.getClass().getName());
        }
    }

    protected void afterPINChecked() {
        bioApplication.setLastStoppedActivityClassName("all running");
        BaseActivity.this.onCreate(null);
        BaseActivity.this.onPostCreate(null);
    }

    protected void checkPIN() {
        this.setContentView(R.layout.pin_check);
        mPINEditor = findViewById(R.id.pin_value_editor);

        findViewById(R.id.verify_pin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (bioApplication.checkPIN(mPINEditor.getText().toString())) {
                    afterPINChecked();
                } else {
                    mPINEditor.setText("");
                    try {
                        showMessage(getResources().getString(R.string.alertDialogVerifyPINError));
                    } catch (Exception ex) {

                    }
                }
            }
        });

        mPINEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    if (bioApplication.checkPIN(mPINEditor.getText().toString())) {
                        afterPINChecked();
                    } else {
                        mPINEditor.setText("");
                        try {
                            showMessage(getResources().getString(R.string.alertDialogVerifyPINError));
                        } catch (Exception ex) {

                        }
                    }
                    return true;
                }
                return false;
            }
        });

        delayedPINFocus(1000);

        FingerprintManager fingerprintManager = getSystemService(FingerprintManager.class);

        if (fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints()) {
            //use fingerprint
        }
    }

    /**
     * Overrides the pending Activity transition by performing the "Enter" animation.
     */
    protected void overridePendingTransitionEnter() {
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    /**
     * Overrides the pending Activity transition by performing the "Exit" animation.
     */
    protected void overridePendingTransitionExit() {
        overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransitionExit();
        if (bioApplication.needCheckPIN())
            moveTaskToBack(true);
    }

    protected void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

    }

    protected void showError(Exception ex) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alertDialogErrorTitle)
                .setMessage(ex.getLocalizedMessage())
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

    protected void showMessage(String sMessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alertDialogMessageTitle)
                .setMessage(sMessage)
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

    private void delayedPINFocus(int delayMillis) {
        mPINFocusHandler.removeCallbacks(mPINFocusRunnable);
        mPINFocusHandler.postDelayed(mPINFocusRunnable, delayMillis);
    }

    private boolean setupActivity() {
        return
                this.getClass().getName().equals("ru.nettrash.bioandroid.InitializeActivity") ||
                        this.getClass().getName().equals("ru.nettrash.bioandroid.SetPINActivity") ||
                        this.getClass().getName().equals("ru.nettrash.bioandroid.VerifyPINActivity");
    }
}

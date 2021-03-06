package com.app.beb.bebapp.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.telephony.TelephonyManager;
import android.widget.TextView;

import com.app.beb.bebapp.BuildConfig;
import com.app.beb.bebapp.R;

public class AboutActivity extends AppCompatActivity {

    private final int kPermissionsRequestReadPhoneState = 0;
    private String _allowReadPhoneState;

    private TextView _imeiTextView;
    private TextView _versionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        _allowReadPhoneState = getResources().getString(R.string.allowReadPhoneState);

        setupViews();

        getVersionNumber();
        getPhoneIMEI();
    }

    private void setupViews()
    {
        _versionTextView = findViewById(R.id.versionTextView);
        _imeiTextView = findViewById(R.id.imeiTextView);

        Toolbar toolbar = findViewById(R.id.about_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    private void getVersionNumber()
    {
        _versionTextView.setText(String.format("%s: %s", getResources().getString(R.string.version),
                BuildConfig.VERSION_NAME));
    }

    private void getPhoneIMEI()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_PHONE_STATE)) {

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
                dialogBuilder.setMessage(_allowReadPhoneState);
                dialogBuilder.setTitle("Please, allow Read phone state permission so we can " +
                        "read IMEI of this device and send it to KGB");
                dialogBuilder.setCancelable(true);
                dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(AboutActivity.this,
                                new String[]{Manifest.permission.READ_PHONE_STATE},
                                kPermissionsRequestReadPhoneState);
                    }
                });

                dialogBuilder.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_PHONE_STATE},
                        kPermissionsRequestReadPhoneState);
            }

        } else {
            showIMEI();
        }

    }

    private void showIMEI()
    {
        int permissionType = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE);

        if (permissionType == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String imei = tm.getDeviceId();
            _imeiTextView.setText(String.format("%s: %s", getResources().getString(R.string.IMEI), imei));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
    {
        switch (requestCode) {
            case kPermissionsRequestReadPhoneState: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    showIMEI();
                } else {
                    _imeiTextView.setText(_allowReadPhoneState);
                }
            }
        }
    }
}

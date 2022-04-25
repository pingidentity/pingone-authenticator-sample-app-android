package com.pingidentity.authenticatorsampleapp;

import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class QrAuthenticationActivity extends FragmentActivity {

    /*
     * QR Authentication activity is built according to Android Navigation pattern.
     * See more at https://developer.android.com/guide/navigation
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_authentication);
    }
}

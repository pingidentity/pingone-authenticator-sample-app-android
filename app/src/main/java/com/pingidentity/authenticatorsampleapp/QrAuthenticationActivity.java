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
        /*
         * Window FLAG_SECURE: treat the content of the window as secure, preventing it from
         * appearing in screenshots or from being viewed on non-secure displays. Note that this
         * flag must be set before the window decoration is created.
         */
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_qr_authentication);
    }
}

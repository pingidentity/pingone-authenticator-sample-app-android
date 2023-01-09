package com.pingidentity.authenticatorsampleapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

public class AuthenticationActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*
         * Window FLAG_SECURE: treat the content of the window as secure, preventing it from
         * appearing in screenshots or from being viewed on non-secure displays. Note that this
         * flag must be set before the window decoration is created.
         */
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_authentication);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultIntent){
        super.onActivityResult(requestCode, resultCode, resultIntent);
    }
}

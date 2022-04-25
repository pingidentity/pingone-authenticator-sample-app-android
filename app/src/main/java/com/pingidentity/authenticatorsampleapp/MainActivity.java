package com.pingidentity.authenticatorsampleapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.firebase.messaging.FirebaseMessaging;
import com.pingidentity.authenticatorsampleapp.fragments.SplashFragmentDirections;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.authenticatorsampleapp.viewmodels.NetworkViewModel;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getCanonicalName();
    /*
     * view models separated by underlying fragments' logic
     * each used to communicate with corresponding fragment,
     * when activity is necessary
     */
    private NetworkViewModel networkViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        logFcmRegistrationIdToken();
        setUpNetworkListeners();
        networkViewModel = new ViewModelProvider(this).get(NetworkViewModel.class);

    }

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            runOnUiThread(() -> networkViewModel.updateNetwork(true));
        }

        @Override
        public void onLost(@NonNull Network network) {
            runOnUiThread(() -> networkViewModel.updateNetwork(false));
        }
    };

    private void setUpNetworkListeners(){
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager!=null) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }
    }

    private void logFcmRegistrationIdToken() {
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences("AuthenticatorSharedPreferences", Context.MODE_PRIVATE);
        String token = sharedPreferences.getString("FCM_TOKEN", null);
        if (token==null){
            FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task ->
                    Log.d(TAG, "FCM Token = " + task.getResult()));
        }else {
            Log.d(TAG, "FCM Token = " + token);
        }
    }
}

package com.pingidentity.authenticatorsampleapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.firebase.messaging.FirebaseMessaging;
import com.pingidentity.authenticatorsampleapp.fragments.SplashFragmentDirections;
import com.pingidentity.authenticatorsampleapp.managers.NotificationsManager;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.authenticatorsampleapp.viewmodels.NetworkViewModel;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;

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
        /*
         * Window FLAG_SECURE: treat the content of the window as secure, preventing it from
         * appearing in screenshots or from being viewed on non-secure displays. Note that this
         * flag must be set before the window decoration is created.
         */
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        setContentView(R.layout.activity_main);
        /*
         * check if Activity was created by triggering an Intent from PingOneNotification
         */
        checkAndApproveNotificationObjectIntent(getIntent());
        logFcmRegistrationIdToken();
        setUpNetworkListeners();
        networkViewModel = new ViewModelProvider(this).get(NetworkViewModel.class);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        /*
         * check if Activity was re-created by triggering an Intent from PingOneNotification
         */
        checkAndApproveNotificationObjectIntent(intent);
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

    /*
     * approves a notification (silently) as a result of user clicking "Approve" when push
     * category was set to "auth_open"
     */
    private void checkAndApproveNotificationObjectIntent(Intent mainActivityStarterIntent){
        if (mainActivityStarterIntent!=null && mainActivityStarterIntent.hasExtra("NotificationObject")){
            //cancel the notification manually
            NotificationManagerCompat.from(this).cancel(NotificationsManager.AUTHENTICATOR_NOTIFICATION_ID);
            //approve the authentication
            NotificationObject notificationObject = mainActivityStarterIntent.getParcelableExtra("NotificationObject");
            notificationObject.approve(this, "auth_open", pingOneSDKError -> {
                if (pingOneSDKError!=null){
                    Log.w(MainActivity.TAG, "Authentication approval returned error " + pingOneSDKError.getMessage());
                }else {
                    Log.i(MainActivity.TAG, "Authentication approval succeeded");
                }
            });

        }
    }
}

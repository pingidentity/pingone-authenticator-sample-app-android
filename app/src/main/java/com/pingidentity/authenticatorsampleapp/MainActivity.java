package com.pingidentity.authenticatorsampleapp;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.vision.barcode.Barcode;
import com.pingidentity.authenticatorsampleapp.qr.QrTracker;
import com.pingidentity.authenticatorsampleapp.viewmodels.BarcodeViewModel;
import com.pingidentity.authenticatorsampleapp.viewmodels.NetworkViewModel;

public class MainActivity extends AppCompatActivity implements QrTracker.QrCodeUpdateListener {

    /*
     * view models separated by underlying fragments' logic
     * each used to communicate with corresponding fragment,
     * when activity is necessary
     */
    private BarcodeViewModel barcodeViewModel;
    private NetworkViewModel networkViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpNetworkListeners();

        barcodeViewModel = new ViewModelProvider(this).get(BarcodeViewModel.class);
        networkViewModel = new ViewModelProvider(this).get(NetworkViewModel.class);

    }

    @Override
    public void onQrCodeDetected(final Barcode barcode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                barcodeViewModel.updateBarcode(barcode.displayValue);
            }
        });
    }

    ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback(){
        @Override
        public void onAvailable(@NonNull Network network) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkViewModel.updateNetwork(true);
                }
            });
        }

        @Override
        public void onLost(@NonNull Network network) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkViewModel.updateNetwork(false);
                }
            });
        }
    };

    private void setUpNetworkListeners(){
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager!=null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            } else {
                NetworkRequest request = new NetworkRequest.Builder()
                        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();
                connectivityManager.registerNetworkCallback(request, networkCallback);
            }
        }
    }
}

package com.pingidentity.authenticatorsampleapp.fragments;

import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.error.PingOneSDKErrorType;

public class AuthenticationFragment extends Fragment {

    private RelativeLayout layoutSuccess;
    private RelativeLayout layoutBlocked;
    private RelativeLayout layoutTimeout;
    private LinearLayout fallbackLayout;
    private ProgressBar progressBar;

    private NotificationObject notificationObject;
    private CountDownTimer countDownTimer;
    public AuthenticationFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationObject = getActivity().getIntent().getParcelableExtra("PingOneNotificationObject");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_authentication, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (canAuthenticateWithBiometrics()) {
            NavController navController = Navigation.findNavController(view);
            NavDirections navDirections = AuthenticationFragmentDirections.actionAuthenticationFragmentToBiometricsAuthenticationFragment();
            navController.navigate(navDirections);
        } else {

            fallbackLayout = view.findViewById(R.id.buttons_fallback_layout);
            layoutSuccess = view.findViewById(R.id.layout_auth_success);
            layoutBlocked = view.findViewById(R.id.layout_auth_denied);
            layoutTimeout = view.findViewById(R.id.layout_auth_timeout);
            progressBar = view.findViewById(R.id.progress_bar_auth);

            showFallbackLayout(view);
            setTimer(notificationObject.getTimeoutDuration());

        }
    }

    /**
     * Indicate whether this device can authenticate the user with biometrics
     * @return true if there are any available biometric sensors and biometrics
     * are enrolled on the device, if not, return false
     */
    private boolean canAuthenticateWithBiometrics() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false;
        }
        BiometricManager biometricManager = BiometricManager.from(requireContext());
        return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
    }

    /*
     * Called when the user clicks the approve button
     */
    private void approveAuth() {
        progressBar.setVisibility(View.VISIBLE);
        notificationObject.approve(requireContext(), "user", new PingOne.PingOneSDKCallback() {
            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                if(pingOneSDKError!=null && pingOneSDKError.getCode()== PingOneSDKErrorType.PUSH_CONFIRMATION_TIMEOUT.getErrorCode()){
                    showTimeoutLayout();
                }
                if (pingOneSDKError == null) {
                    showSuccessLayout();
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            requireActivity().finish();
                        }catch (IllegalStateException e){
                            //activity already closed, do nothing
                        }
                    }
                }, 2000);
            }
        });
    }

    /*
     * Called when the user clicks the deny button
     */
    private void denyAuth() {
        progressBar.setVisibility(View.VISIBLE);
        notificationObject.deny(requireContext(), new PingOne.PingOneSDKCallback() {
            @Override
            public void onComplete(@Nullable PingOneSDKError pingOneSDKError) {
                if(pingOneSDKError!=null && pingOneSDKError.getCode()== PingOneSDKErrorType.PUSH_CONFIRMATION_TIMEOUT.getErrorCode()){
                    showTimeoutLayout();
                }
                if (pingOneSDKError==null) {
                    showBlockedLayout();
                }
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            requireActivity().finish();
                        }catch (IllegalStateException e){
                            //activity already closed, do nothing
                        }
                    }
                }, 2000);
            }
        });
    }

    private void showFallbackLayout(View view){
        fallbackLayout.setVisibility(View.VISIBLE);
        Button approve = view.findViewById(R.id.auth_button_approve);
        approve.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countDownTimer!=null) {
                    countDownTimer.cancel();
                }
                approveAuth();
            }
        });
        Button deny = view.findViewById(R.id.auth_button_deny);
        deny.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (countDownTimer!=null) {
                    countDownTimer.cancel();
                }
                denyAuth();
            }
        });
    }

    private void showSuccessLayout(){
        try {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressBar.getVisibility() != View.GONE) {
                        progressBar.setVisibility(View.GONE);
                    }
                    layoutSuccess.setVisibility(View.VISIBLE);
                }
            });
        }catch (IllegalStateException e){
            //activity already closed, do nothing
        }
    }

    private void showBlockedLayout(){
        try {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressBar.getVisibility() != View.GONE) {
                        progressBar.setVisibility(View.GONE);
                    }
                    layoutBlocked.setVisibility(View.VISIBLE);
                }
            });
        }catch (IllegalStateException e){
            //activity already closed, do nothing
        }
    }

    private void showTimeoutLayout(){
        try {
            requireActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressBar.getVisibility() != View.GONE) {
                        progressBar.setVisibility(View.GONE);
                    }
                    layoutTimeout.setVisibility(View.VISIBLE);
                }
            });
        }catch (IllegalStateException e){
            //activity already closed, do nothing
        }
    }

    private void setTimer(int secondsLeft){
        if (countDownTimer!=null) {
            countDownTimer.cancel();
        }
        countDownTimer = new CountDownTimer((secondsLeft) * 1000 , 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                showTimeoutLayout();
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        requireActivity().finish();
                    }
                }, 2000);

            }
        };
        countDownTimer.start();
    }










}


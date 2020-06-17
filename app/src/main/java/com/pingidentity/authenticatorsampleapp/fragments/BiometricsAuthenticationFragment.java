package com.pingidentity.authenticatorsampleapp.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.biometric.BiometricConstants;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.error.PingOneSDKErrorType;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BiometricsAuthenticationFragment extends Fragment {

    private RelativeLayout layoutSuccess;
    private RelativeLayout layoutBlocked;
    private RelativeLayout layoutTimeout;
    private ProgressBar progressBar;

    private CountDownTimer countDownTimer;
    private NavController navController;

    private NotificationObject notificationObject;

    private String notificationTitle;
    private String notificationSubtitle;

    private BiometricPrompt biometricPrompt;
    private boolean timeoutCancelled = false;
    public BiometricsAuthenticationFragment(){

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        notificationObject = getActivity().getIntent().getParcelableExtra("PingOneNotificationObject");
        notificationTitle = getActivity().getIntent().getStringExtra("title");
        notificationSubtitle = getActivity().getIntent().getStringExtra("body");

        navController = Navigation.findNavController(view);

        //initialize all possible layouts
        layoutSuccess = view.findViewById(R.id.layout_auth_success);
        layoutBlocked = view.findViewById(R.id.layout_auth_denied);
        layoutTimeout = view.findViewById(R.id.layout_auth_timeout);
        progressBar = view.findViewById(R.id.progress_bar_auth);

        setTimer(notificationObject.getTimeoutDuration());
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                buildAndShowBiometricPrompt();
            }

            @Override
            public void onViewDetachedFromWindow(View v) {
                if(countDownTimer!=null){
                    countDownTimer.cancel();
                }
            }
        });

    }

    private void buildAndShowBiometricPrompt(){
        biometricPrompt = new BiometricPrompt(this,
                Executors.newFixedThreadPool(1), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                if (countDownTimer!=null){
                    countDownTimer.cancel();
                }
                if (!timeoutCancelled) {
                    denyAuth();
                }
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if(countDownTimer!=null){
                    countDownTimer.cancel();
                }
                approveAuth();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });

        BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder();
            builder.setTitle(notificationTitle)
                    .setSubtitle(notificationSubtitle);

        if (!(BiometricManager.from(requireContext()).canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS)){
            builder.setDeviceCredentialAllowed(true);
        }else{
            builder.setDeviceCredentialAllowed(false)
                    .setNegativeButtonText(getString(android.R.string.cancel));
        }

        builder.setConfirmationRequired(true);
        new Handler().postDelayed(() -> biometricPrompt.authenticate(builder.build()), 500);
    }

    private void approveAuth() {

        requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        notificationObject.approve(requireContext(), "bio", pingOneSDKError -> {
            if(pingOneSDKError!=null && pingOneSDKError.getCode()== PingOneSDKErrorType.PUSH_CONFIRMATION_TIMEOUT.getErrorCode()){
                showTimeoutLayout();
            }
            if (pingOneSDKError == null) {
                showSuccessLayout();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    requireActivity().finish();
                }catch (IllegalStateException e){
                    //activity already closed do nothing
                }
            }, 2000);
        });
    }


    private void denyAuth() {
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        if (getActivity()!=null){
            requireActivity().runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE));
        }
        notificationObject.deny(requireContext(), pingOneSDKError -> {
            if(pingOneSDKError!=null && pingOneSDKError.getCode()== PingOneSDKErrorType.PUSH_CONFIRMATION_TIMEOUT.getErrorCode()){
                showTimeoutLayout();
            }
            if (pingOneSDKError==null) {
                showBlockedLayout();
            }
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    requireActivity().finish();
                }catch (IllegalStateException e){
                    //activity already closed do nothing
                }
            }, 2000);
        });
    }

    private void showTimeoutLayout(){
        try {
            requireActivity().runOnUiThread(() -> {
                if (progressBar.getVisibility() != View.GONE) {
                    progressBar.setVisibility(View.GONE);
                }
                layoutTimeout.setVisibility(View.VISIBLE);
            });
        }catch (IllegalStateException e){
            //activity already closed, do nothing
        }

    }
    private void showSuccessLayout(){
        try {
            requireActivity().runOnUiThread(() -> {
                if (progressBar.getVisibility() != View.GONE) {
                    progressBar.setVisibility(View.GONE);
                }
                layoutSuccess.setVisibility(View.VISIBLE);
            });
        }catch (IllegalStateException e){
            //activity already closed, do nothing
        }
    }

    private void showBlockedLayout() {
        try {
            requireActivity().runOnUiThread(() -> {
                if (progressBar.getVisibility() != View.GONE) {
                    progressBar.setVisibility(View.GONE);
                }
                layoutBlocked.setVisibility(View.VISIBLE);
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
                timeoutCancelled = true;

                if(biometricPrompt!=null) {
                    biometricPrompt.cancelAuthentication();
                }
                navController.navigate(BiometricsAuthenticationFragmentDirections.actionBiometricsAuthenticationFragmentToTimeoutFragment());


                }
            };

        countDownTimer.start();
    }
}

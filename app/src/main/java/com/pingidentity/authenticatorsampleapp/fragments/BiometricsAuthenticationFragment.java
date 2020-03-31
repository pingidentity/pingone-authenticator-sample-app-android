package com.pingidentity.authenticatorsampleapp.fragments;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
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
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.pingidsdkv2.NotificationObject;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.error.PingOneSDKErrorType;

import java.util.concurrent.Executor;

public class BiometricsAuthenticationFragment extends Fragment {

    private RelativeLayout layoutSuccess;
    private RelativeLayout layoutBlocked;
    private RelativeLayout layoutTimeout;
    private ProgressBar progressBar;

    private CountDownTimer countDownTimer;
    private KeyguardManager mKeyguardManager;
    private NavController navController;
    private static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 12;

    private NotificationObject notificationObject;

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
        navController = Navigation.findNavController(view);
        mKeyguardManager = (KeyguardManager) requireActivity().getSystemService(Context.KEYGUARD_SERVICE);

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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS){
            switch (resultCode){
                case Activity.RESULT_CANCELED:
                    denyAuth();
                    break;
                case Activity.RESULT_OK:
                    approveAuth();
                    break;
            }
        }
    }

    private void buildAndShowBiometricPrompt(){
        BiometricPrompt biometricPrompt = new BiometricPrompt(this,
                getMainThreadExecutor(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                switch (errorCode) {
                    //the user pressed PIN-fallback button
                    case BiometricConstants.ERROR_NEGATIVE_BUTTON:
                        showAuthenticationScreen();
                        break;
                    //the user cancelled the operation
                    case BiometricConstants.ERROR_LOCKOUT:
                    case BiometricConstants.ERROR_LOCKOUT_PERMANENT:
                    case BiometricConstants.ERROR_USER_CANCELED:
                        denyAuth();
                        break;
                    case BiometricConstants.ERROR_CANCELED:
                    case BiometricConstants.ERROR_HW_NOT_PRESENT:
                    case BiometricConstants.ERROR_HW_UNAVAILABLE:
                    case BiometricConstants.ERROR_NO_BIOMETRICS:

                        break;
                    case BiometricConstants.ERROR_NO_DEVICE_CREDENTIAL:
                        break;
                    case BiometricConstants.ERROR_NO_SPACE:
                        break;
                    case BiometricConstants.ERROR_TIMEOUT:
                        break;
                    case BiometricConstants.ERROR_UNABLE_TO_PROCESS:
                        break;
                    case BiometricConstants.ERROR_VENDOR:
                        break;
                }

            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                approveAuth();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
            }
        });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(getString(R.string.app_name))
                .setNegativeButtonText(getString(R.string.confirm_device_credential_password))
                .setConfirmationRequired(false)
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void approveAuth() {
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
            }
        });
        notificationObject.approve(requireContext(), "bio", new PingOne.PingOneSDKCallback() {
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
                            //activity already closed do nothing
                        }
                    }
                }, 2000);
            }
        });
    }


    private void denyAuth() {
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);

            }
        });
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
                            //activity already closed do nothing
                        }
                    }
                }, 2000);
            }
        });
    }

    private void showAuthenticationScreen() {
        /*
         * Create the Confirm Credentials screen. You can customize the title and description. Or
         * we will provide a generic one for you if you leave it null
         */
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
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

    private void showBlockedLayout() {
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



    private Executor getMainThreadExecutor() {
        return new MainThreadExecutor(requireActivity());
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private Activity activity;
        MainThreadExecutor(Activity activity){
            this.activity = activity;
        }
        @Override
        public void execute(@NonNull Runnable r) {
            if(!activity.isFinishing()) {
                handler.post(r);
            }
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
                navController.navigate(BiometricsAuthenticationFragmentDirections.actionBiometricsAuthenticationFragmentToTimeoutFragment());
            }
        };
        countDownTimer.start();
    }
}

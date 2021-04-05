package com.pingidentity.authenticatorsampleapp.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;
import com.pingidentity.pingidsdkv2.types.PairingInfo;


/**
 * A simple {@link Fragment} subclass.
 */
public class PairingFragment extends Fragment {

    private RelativeLayout verifyingLayout;
    private ImageView spinnerImage;
    private RelativeLayout successLayout;
    private RelativeLayout errorLayout;
    private TextView errorTextView;

    public PairingFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_pairing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        verifyingLayout = view.findViewById(R.id.layout_verifying);
        spinnerImage = view.findViewById(R.id.image_spinner);
        successLayout = view.findViewById(R.id.layout_success);
        errorLayout = view.findViewById(R.id.layout_invalid);
        errorTextView = view.findViewById(R.id.text_view_invalid);
        if (getArguments()!=null && getArguments().containsKey("pairingKey")) {
            showVerifyingLayout();
            tryToPairDevice(getArguments().getString("pairingKey"));
        }
    }

    private void showSuccessLayout(){
        successLayout.setVisibility(View.VISIBLE);
        PreferencesManager preferencesManager = new PreferencesManager();
        preferencesManager.setIsDeviceActive(requireContext(), true);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                final NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                NavDirections action = PairingFragmentDirections.actionPairingFragment2ToMainFragment();
                navController.navigate(action);
            }
        }, 2000);
    }

    private void showErrorLayout(String errorMessage){
        errorLayout.setVisibility(View.VISIBLE);
        errorTextView.setText(errorMessage);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            final NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            NavDirections action = PairingFragmentDirections.actionPairingFragment2ToCamera2Fragment();
            navController.navigate(action);
        }, 2000);
    }

    private void showVerifyingLayout(){
        verifyingLayout.setVisibility(View.VISIBLE);
        RotateAnimation rotate = new RotateAnimation(
                0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        rotate.setInterpolator(new LinearInterpolator());
        rotate.setDuration(2000);
        rotate.setRepeatCount(Animation.INFINITE);
        spinnerImage.startAnimation(rotate);
    }

    private void hideVerifyingLayout(){
        verifyingLayout.setVisibility(View.GONE);
    }

    private void tryToPairDevice(String barcode){
        /*
         * Try to start pairing when QR code value is recognized
         */

        PingOne.pair(requireContext(), barcode, new PingOne.PingOneSDKPairingCallback() {
            @Override
            public void onComplete(PairingInfo pairingInfo, @Nullable PingOneSDKError error) {
                this.onComplete(error);
            }

            @Override
            public void onComplete(@Nullable PingOneSDKError error) {
                if(error == null){
                    requireActivity().runOnUiThread(() -> {
                        hideVerifyingLayout();
                        showSuccessLayout();
                    });
                }else{
                    if(error.getMessage() != null){
                        requireActivity().runOnUiThread(() -> {
                            hideVerifyingLayout();
                            showErrorLayout(error.getMessage());
                        });
                    }

                }
            }
        });
    }
}
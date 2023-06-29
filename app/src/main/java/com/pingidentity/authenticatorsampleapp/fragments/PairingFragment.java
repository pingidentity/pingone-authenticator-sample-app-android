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
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.authenticatorsampleapp.util.UserInterfaceUtil;
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
        drawStatusBar(getResources().getColor(R.color.layout_success_background_color, null));
        PreferencesManager preferencesManager = new PreferencesManager();
        preferencesManager.setIsDeviceActive(requireContext(), true);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            drawStatusBar(getResources().getColor(R.color.color_toolbar_background, null));
            final NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            NavDirections action = PairingFragmentDirections.actionPairingFragment2ToMainFragment();
            navController.navigate(action);
        }, 2000);
    }

    private void showErrorLayout(String errorMessage){
        errorLayout.setVisibility(View.VISIBLE);
        drawStatusBar(getResources().getColor(R.color.layout_invalid_background_color, null));
        errorTextView.setText(errorMessage);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            drawStatusBar(getResources().getColor(R.color.color_toolbar_background, null));
            final NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
            NavDirections action = PairingFragmentDirections.actionPairingFragment2ToCamera2Fragment();
            navController.navigate(action);
        }, 2000);
    }

    private void showVerifyingLayout(){
        verifyingLayout.setVisibility(View.VISIBLE);
        drawStatusBar(getResources().getColor(R.color.layout_verifying_background_color, null));
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
        drawStatusBar(getResources().getColor(R.color.color_toolbar_background, null));
    }

    private void tryToPairDevice(String barcode){
        /*
         * Try to start pairing when QR code value is recognized
         */

        PingOne.pair(requireContext(), barcode, new PingOne.PingOneSDKPairingCallback() {
            /*
             * On successful completion of the pairing task the PairingInfo object will be returned.
             * PingOneSDSKError object will be returned if pairing process wasn't successful
             */
            @Override
            public void onComplete(@Nullable PairingInfo pairingInfo, @Nullable PingOneSDKError error) {
                /*
                 * Do something with PairingInfo object if needed, than check if error happened
                 */
                if(error == null){
                    requireActivity().runOnUiThread(() -> {
                        hideVerifyingLayout();
                        showSuccessLayout();
                    });
                }else{
                    requireActivity().runOnUiThread(() -> {
                        hideVerifyingLayout();
                        showErrorLayout(UserInterfaceUtil
                                .handlePingOneSDKErrorMessage(requireContext(), error));
                    });
                }
            }
        });
    }

    //set color of the status bar according to the underlying view
    private void drawStatusBar(int color){
        Window window = requireActivity().getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(color);
    }
}
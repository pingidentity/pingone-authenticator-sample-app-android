package com.pingidentity.authenticatorsampleapp.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;

public class SplashFragment extends Fragment {

    private static final int SPLASH_DURATION_MILLIS = 1500;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Handler().postDelayed(() -> {
            /*
             * Create an action that will load appropriate fragment.
             */
            final NavController navController = Navigation.findNavController(view);
            PreferencesManager preferencesManager = new PreferencesManager();
            /*
             * navigate to NotificationsPermission fragment if needed
             */
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED && !preferencesManager.isNotificationsPermissionDenied(requireContext())) {
                NavDirections action = SplashFragmentDirections.actionSplashFragmentToNotificationsPermissionFragment();
                navController.navigate(action);
            /*
             * navigate to main fragment if device is active
             */
            }else if(preferencesManager.isDeviceActive(requireContext())){
                NavDirections action = SplashFragmentDirections.actionSplashFragmentToMainFragment();
                navController.navigate(action);
            /*
             * navigate to pairing fragment
             */
            }else{
                NavDirections action = SplashFragmentDirections.actionSplashFragmentToCamera2Fragment();
                navController.navigate(action);
            }
        }, SPLASH_DURATION_MILLIS);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        requireActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
    }
}

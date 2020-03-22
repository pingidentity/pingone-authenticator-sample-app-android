package com.pingidentity.authenticatorsampleapp.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;

public class SplashFragment extends Fragment {

    private static final int SPLASH_DURATION_MILLIS = 1500;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_splash, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                /*
                 * Create an action that will load the appropriate fragment.
                 */
                final NavController navController = Navigation.findNavController(view);
                PreferencesManager preferencesManager = new PreferencesManager();
                if(preferencesManager.isDeviceActive(requireContext())){
                    NavDirections action = SplashFragmentDirections.actionSplashFragmentToMainFragment();
                    navController.navigate(action);
                }else{
                    NavDirections action = SplashFragmentDirections.actionSplashFragmentToCamera2Fragment();
                    navController.navigate(action);
                }
            }
        }, SPLASH_DURATION_MILLIS);
    }
}

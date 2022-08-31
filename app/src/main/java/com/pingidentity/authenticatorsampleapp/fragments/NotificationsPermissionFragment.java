package com.pingidentity.authenticatorsampleapp.fragments;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;

/*
 * Notifications Permission fragment
 */
public class NotificationsPermissionFragment extends Fragment {

    private final String TAG = NotificationsPermissionFragment.class.getCanonicalName();
    private PreferencesManager preferencesManager;

    /*
     * ActivityResultLauncher, as an instance variable.
     */
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private NavController controller;

    public NotificationsPermissionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_notifications_permission, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        preferencesManager = new PreferencesManager();

        //set appropriate text to the subtitle according to the application name
        TextView notificationFragmentSubtitle = view.findViewById(R.id.text_view_notification_subtitle);
        notificationFragmentSubtitle.setText(createNotificationFragmentSubtitle());
        //register permission's launcher receiver
        registerPermissionLauncherReceiver();
        controller = Navigation.findNavController(view);

        Button enableNotificationPermissions = view.findViewById(R.id.button_enable_notifications);
        enableNotificationPermissions.setOnClickListener(view1 -> requestNotificationsPermission());
    }

    private void registerPermissionLauncherReceiver() {
        /*
         * Register the permissions callback, which handles the user's response to the
         * system permissions dialog.
         */
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        Log.i(TAG, "Notifications permission was granted");
                        /*
                         * Permission is granted. Continue the action or workflow in your
                         * app.
                         */
                        proceedWithNavigation();
                    } else {
                        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                            Log.i(TAG, "Providing a rationale regarding notifications permission");
                            /*
                             * Explain to the user that the feature is unavailable because the
                             * features requires a permission that the user has denied.
                             */
                            //buildAndShowPermissionRationaleDialog();
                            preferencesManager.setIsNotificationPermissionStatusDenied(requireContext(), true);
                            proceedWithNavigation();
                        }else{
                            Log.i(TAG, "Notifications permission was rejected");
                            preferencesManager.setIsNotificationPermissionStatusDenied(requireContext(), true);
                            proceedWithNavigation();
                        }
                    }
                });
    }

    private String createNotificationFragmentSubtitle(){
        return String.format(getResources().getString(R.string.notifications_permission_subtitle), getApplicationName());
    }

    private String getApplicationName(){
        return requireContext().getApplicationInfo().loadLabel(requireContext().getPackageManager()).toString();
    }

    private void requestNotificationsPermission(){
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void proceedWithNavigation(){
        if(preferencesManager.isDeviceActive(requireContext())){
            NavDirections action = NotificationsPermissionFragmentDirections.actionNotificationsPermissionFragmentToMainFragment();
            controller.navigate(action);
            /*
             * navigate to pairing fragment
             */
        }else{
            NavDirections action = NotificationsPermissionFragmentDirections.actionNotificationsPermissionFragmentToCamera2Fragment();
            controller.navigate(action);
        }
    }
}
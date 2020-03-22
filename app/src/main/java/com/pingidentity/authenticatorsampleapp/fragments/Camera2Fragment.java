package com.pingidentity.authenticatorsampleapp.fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.pingidentity.authenticatorsampleapp.R;
import com.pingidentity.authenticatorsampleapp.managers.PreferencesManager;
import com.pingidentity.authenticatorsampleapp.qr.Camera2Source;
import com.pingidentity.authenticatorsampleapp.qr.QrCameraPreview;
import com.pingidentity.authenticatorsampleapp.qr.QrTrackerFactory;
import com.pingidentity.authenticatorsampleapp.viewmodels.BarcodeViewModel;
import com.pingidentity.pingidsdkv2.PingOne;
import com.pingidentity.pingidsdkv2.PingOneSDKError;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link Camera2Fragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Camera2Fragment extends Fragment {

    private static final String TAG = "Camera2Fragment";

    /*
     * permission request codes (need to be < 256)
     */
    private static final int RC_HANDLE_CAMERA_PERM = 2;
    private static final int RC_HANDLE_GMS = 3;

    private Camera2Source mCameraSource;
    private QrCameraPreview mPreview;

    /*
     * layouts
     */
    private LinearLayout cameraDisableLayout;
    private RelativeLayout verifyingLayout;
    private ImageView spinnerImage;
    private RelativeLayout successLayout;
    private RelativeLayout errorLayout;
    private TextView errorTextView;
    private EditText pairingKeyInput;
    private Button pairingButton;

    public Camera2Fragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     * @return A new instance of fragment Camera2Fragment.
     */
    public static Camera2Fragment newInstance(String param1, String param2) {
        return new Camera2Fragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera2, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mPreview = view.findViewById(R.id.camera_preview);

        cameraDisableLayout = view.findViewById(R.id.layout_camera_disabled);
        verifyingLayout = view.findViewById(R.id.layout_verifying);
        spinnerImage = view.findViewById(R.id.image_spinner);
        successLayout = view.findViewById(R.id.layout_success);
        errorLayout = view.findViewById(R.id.layout_invalid);
        errorTextView = view.findViewById(R.id.text_view_invalid);
        pairingKeyInput = view.findViewById(R.id.pairing_key_input);
        pairingButton  = view.findViewById(R.id.button_pair);
        /*
         * View Model to communicate with other components
         */
        final BarcodeViewModel barcodeViewModel = new ViewModelProvider(requireActivity()).get(BarcodeViewModel.class);
        barcodeViewModel.getBarcode().observe(requireActivity(), new Observer<String>() {
            @Override
            public void onChanged(String barcode) {
                if(barcode==null){
                    return;
                }
                mPreview.stop();
                /*
                 * Try to start pairing when QR code value is recognized
                 */
                PingOne.pair(requireContext(), barcode, new PingOne.PingOneSDKCallback() {
                    @Override
                    public void onComplete(@Nullable final PingOneSDKError pingOneSDKError) {
                        if(pingOneSDKError == null){
                            Log.i(TAG, "Pairing succeeded");
                            requireActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    hideVerifyingLayout();
                                    showSuccessLayout();
                                    barcodeViewModel.updateBarcode(null);
                                    barcodeViewModel.getBarcode().removeObservers(requireActivity());

                                }
                            });
                        }else{
                            Log.w(TAG, "Pairing failed");
                            if(pingOneSDKError.getMessage() != null){
                                Log.w(TAG, "Received error from SDK" + " " + pingOneSDKError.getMessage());
                                requireActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        hideVerifyingLayout();
                                        showErrorLayout(pingOneSDKError.getMessage());
                                    }
                                });
                            }

                        }
                    }
                });
                requireActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showVerifyingLayout();
                    }
                });
            }
        });

        pairingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showVerifyingLayout();
                PingOne.pair(requireContext(), pairingKeyInput.getText().toString(), new PingOne.PingOneSDKCallback() {
                    @Override
                    public void onComplete(@Nullable final PingOneSDKError pingOneSDKError) {
                        /*
                         * applying UI changes on the callback should be triggered
                         * in UI Thread.
                         */
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                hideVerifyingLayout();
                                if (pingOneSDKError==null){
                                    showSuccessLayout();
                                }else{
                                    showErrorLayout(pingOneSDKError.getMessage());
                                }
                            }
                        });
                    }
                });
            }
        });

        /*
         * adding a textChangedListener to editText to activate the Pair button at
         * the moment pairing key reaches needed length
         */
        pairingKeyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int before, int count) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if(charSequence.length() == 14){
                    pairingButton.setEnabled(true);
                }else{
                    pairingButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        /*
         * Check for the camera permission before accessing the camera.  If the
         * permission is not granted yet, request permission.
         */
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            hideCameraDisabledLayout();
            createCameraSource();
        } else {
            showCameraDisabledLayout();
            requestCameraPermission();
        }
        startCameraSource();
    }

    /**
     * Stops the camera.
     */
    @Override
    public void onPause() {
        super.onPause();
        if (mPreview != null) {
            mPreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPreview != null) {
            mPreview.release();
        }
    }

    private void createCameraSource() {
        /*
         * A barcode detector is created to track barcodes.  An associated multi-processor instance
         * is set to receive the barcode detection results, track the barcodes, and maintain
         * graphics for each barcode on screen.  The factory is used by the multi-processor to
         * create a separate tracker instance for each barcode.
         */
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(requireContext()).build();
        QrTrackerFactory barcodeFactory = new QrTrackerFactory(requireContext());
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());

        /*
         * Note: The first time that an app using the barcode API is installed on a device,
         * GMS will download a native libraries to the device in order to do detection.
         * Usually this completes before the app is run for the first time.  But if that
         * download has not yet completed, then the above call will not detect any barcodes
         *
         * isOperational() can be used to check if the required native libraries are currently
         * available. The detectors will automatically become operational once the library
         * downloads complete on device.
         */
        if (!barcodeDetector.isOperational()) {

            Log.w(TAG, "Detector dependencies are not yet available.");
            /*
             * Check for low storage.  If there is low storage, the native library will not be
             * downloaded, so detection will not become operational.
             */
//            IntentFilter lowstorageFilter = new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW);
//            boolean hasLowStorage = registerReceiver(null, lowstorageFilter) != null;
//
//            if (hasLowStorage) {
//                // Toast.makeText(this, R.string.low_storage_error, Toast.LENGTH_LONG).show();
//                // Log.w(TAG, getString(R.string.low_storage_error));
//            }
        }

        /*
         * Creates and starts the camera.  Note that this uses a higher resolution in comparison
         * to other detection examples to enable the QR code detector to detect small QR codes
         * at long distances.
         */
        mCameraSource = new Camera2Source.Builder(requireContext(), barcodeDetector).build();
    }

    /*
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg =
                    GoogleApiAvailability.getInstance().getErrorDialog(requireActivity(), code, RC_HANDLE_GMS);
            dlg.show();
        }

        /*
         * returning from setting screen we need to check for persmission granted and try
         * to create a surface again
         */
        if(mCameraSource == null && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED){
            hideCameraDisabledLayout();
            createCameraSource();
        }

        if (mCameraSource != null) {
            mPreview.start(mCameraSource);
        }
    }
    /*
     * Handles the requesting of the camera permission.
     */
    private void requestCameraPermission() {
        Log.w(TAG, "Camera permission is not granted. Requesting permission");

        final String[] permissions = new String[]{Manifest.permission.CAMERA};
        /*
         * returns true if the user has previously denied the request, and returns false if a user
         * has denied a permission and selected the Don't ask again option in the permission request
         * dialog
         */
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                Manifest.permission.CAMERA)) {
            /*
             * Show an explanation to the user *asynchronously* -- don't block
             * this thread waiting for the user's response! After the user
             * sees the explanation, try again to request the permission.
             */
            Log.w(TAG, "Should request permission rationale");
            showCameraDisabledLayout();
        } else {
            requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
                Log.i(TAG, "Camera Permission granted - initialize the camera source");
            } else {
                showCameraDisabledLayout();
            }
            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    private void showCameraDisabledLayout(){
        cameraDisableLayout.setVisibility(View.VISIBLE);
        Button enable = cameraDisableLayout.findViewById(R.id.button_camera_enable);
        enable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openApplicationSettings();
            }
        });
    }

    private void hideCameraDisabledLayout(){
        cameraDisableLayout.setVisibility(View.INVISIBLE);
    }

    private void showVerifyingLayout(){
        mPreview.stop();
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

    private void showSuccessLayout(){
        successLayout.setVisibility(View.VISIBLE);
        PreferencesManager preferencesManager = new PreferencesManager();
        preferencesManager.setIsDeviceActive(requireContext(), true);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                final NavController navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment);
                NavDirections action = Camera2FragmentDirections.actionCamera2FragmentToMainFragment();
                navController.navigate(action);
            }
        }, 1500);
    }


    private void showErrorLayout(String errorMessage){
        errorLayout.setVisibility(View.VISIBLE);
        errorTextView.setText(errorMessage);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                startCameraSource();
                errorLayout.setVisibility(View.GONE);
            }
        }, 1500);
    }

    private void openApplicationSettings(){
        /*
         * Opens Details setting page for App.
         * From here user have to manually assign desired permission.
         */
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        /*
         * As we don't want to have this in history stack we remove it using intent flags.
         */
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);

        /*
         * Prepares or creates URI, whereas, getPackageName() returns name of your application package.
         */
        Uri uri = Uri.fromParts("package", requireActivity().getPackageName(), null);

        /* Don't forget to set this. Otherwise, you will get android.content.ActivityNotFoundException.
         * Because you have set your intent as Settings.ACTION_APPLICATION_DETAILS_SETTINGS
         * and android expects some name to search.
         */
        intent.setData(uri);
        startActivity(intent);
    }
}

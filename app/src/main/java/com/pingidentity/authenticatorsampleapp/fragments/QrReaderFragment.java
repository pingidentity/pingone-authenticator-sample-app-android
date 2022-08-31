package com.pingidentity.authenticatorsampleapp.fragments;

import static java.lang.Math.abs;
import static java.lang.Math.min;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.pingidentity.authenticatorsampleapp.R;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QrReaderFragment extends Fragment {

    private PreviewView previewView;

    /*
     * Blocking camera operations are performed using this executor
     */
    private ExecutorService cameraExecutor;

    /*
     * ActivityResultLauncher, as an instance variable.
     */
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private  NavController controller;
    private boolean detected = false;

    /*
     * layouts
     */
    private LinearLayout cameraDisableLayout;

    private EditText pairingKeyInput;
    private Button pairingButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr, container, false);
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*
         * Register the permissions callback, which handles the user's response to the
         * system permissions dialog.
         */
         requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        // Permission is granted. Continue the action or workflow in your
                        // app.
                        startCameraPreview();
                    } else {
                        // Explain to the user that the feature is unavailable because the
                        // features requires a permission that the user has denied.
                        showCameraDisabledLayout();
                    }
                });
        controller = Navigation.findNavController(view);

        // Initialize background executor each time the view is recreated
        cameraExecutor = Executors.newSingleThreadExecutor();

        previewView = view.findViewById(R.id.pairing_camera_preview);

        cameraDisableLayout = view.findViewById(R.id.layout_camera_disabled);

        pairingKeyInput = view.findViewById(R.id.pairing_key_input);
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
                if (charSequence.length()==14 || charSequence.length()==18){
                    pairingButton.setEnabled(true);
                    pairingButton.setTextColor(getResources().
                            getColor(R.color.color_button_blue_enabled, null));
                }else{
                    pairingButton.setEnabled(false);
                    pairingButton.setTextColor(getResources().
                            getColor(R.color.color_title_text_color, null));
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        pairingButton  = view.findViewById(R.id.button_pair);
        pairingButton.setOnClickListener(v -> {
            if(pairingButton.isEnabled()){
                pairingButton.setEnabled(false);
                detected = true;
                if(cameraExecutor!=null && !cameraExecutor.isShutdown()) {
                    cameraExecutor.shutdown();
                }
                onQrCodeDetected(pairingKeyInput.getText().toString());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        /*
         * Check for the camera permission before accessing the camera. If the
         * permission is not granted yet, request permission.
         */
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            hideCameraDisabledLayout();
            startCameraPreview();
        }else if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)){
            showCameraDisabledLayout();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraExecutor!=null && !cameraExecutor.isShutdown()){
            cameraExecutor.shutdown();
        }
    }


    /**
     * Declare and bind preview and analysis use cases
     */
    private void bindCameraUseCases(){

        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);
        final int rotation = previewView.getDisplay().getRotation();
        final int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);
        //camera selector
        final CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        final ListenableFuture<ProcessCameraProvider> futureCameraProvider = ProcessCameraProvider.getInstance(requireContext());
        futureCameraProvider.addListener(() -> {
            try{
                ProcessCameraProvider processCameraProvider = (ProcessCameraProvider) futureCameraProvider.get();

                // Preview
                Preview preview = new Preview.Builder()
                        // We request aspect ratio but no resolution
                        .setTargetAspectRatio(screenAspectRatio)
                        // Set initial target rotation
                        .setTargetRotation(rotation)
                        .build();
               preview.setSurfaceProvider(cameraExecutor, previewView.getSurfaceProvider());

                //QrCode analyzer
                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        // We request aspect ratio but no resolution
                        .setTargetAspectRatio(screenAspectRatio)
                        // Set initial target rotation, we will have to call this again if rotation changes
                        // during the lifecycle of this use case
                        .setTargetRotation(rotation)
                        .build();
                analysis.setAnalyzer(cameraExecutor, new QrCodeAnalyzer());

                processCameraProvider.unbindAll();

                processCameraProvider.bindToLifecycle(QrReaderFragment.this, cameraSelector, preview, analysis);
            }catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }


        }, ContextCompat.getMainExecutor(requireContext()));
    }

    /**
     *  [androidx.camera.core.ImageAnalysisConfig] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private int aspectRatio(int width, int height) {
        double previewRatio = (double) Math.max(width, height) /min(width, height);
        double RATIO_16_9_VALUE = 16.0 / 9.0;
        double RATIO_4_3_VALUE = 4.0 / 3.0;
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }

    @OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private class QrCodeAnalyzer implements ImageAnalysis.Analyzer{

        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            Image image = imageProxy.getImage();
            if (image == null) {
                return;
            }
            InputImage inputImage =
                    InputImage.fromMediaImage(image, imageProxy.getImageInfo().getRotationDegrees());
            BarcodeScanner scanner = BarcodeScanning.getClient();

            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        if (barcodes.size() > 0 && !detected) {
                            detected = true;
                            cameraExecutor.shutdown();
                            onQrCodeDetected(barcodes.get(0).getRawValue());

                        }
                        imageProxy.close();

                    })
                    .addOnFailureListener(e -> {
                        e.printStackTrace();
                        imageProxy.close();
                    });


        }

    }

    private void startCameraPreview(){
        previewView.post(this::bindCameraUseCases);
    }

    private void onQrCodeDetected(String barcode){
        requireActivity().runOnUiThread(() -> {
            NavDirections action = QrReaderFragmentDirections.actionCamera2FragmentToPairingFragment2(barcode);
            controller.navigate(action);
        });
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

    private void showCameraDisabledLayout(){
        cameraDisableLayout.setVisibility(View.VISIBLE);
        Button enable = cameraDisableLayout.findViewById(R.id.button_camera_enable);
        enable.setOnClickListener(view -> openApplicationSettings());
    }

    private void hideCameraDisabledLayout(){
        cameraDisableLayout.setVisibility(View.INVISIBLE);
    }



}

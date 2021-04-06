package com.pingidentity.authenticatorsampleapp.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.pingidentity.authenticatorsampleapp.R;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.Math.abs;
import static java.lang.Math.min;

public class QrReaderFragment extends Fragment {

    private PreviewView previewView;

    /*
     * permission request codes (need to be < 256)
     */
    private static final int RC_HANDLE_CAMERA_PERM = 2;

    private final double  RATIO_4_3_VALUE = 4.0 / 3.0;
    private final double RATIO_16_9_VALUE = 16.0 / 9.0;

    /** Blocking camera operations are performed using this executor */
    private ExecutorService cameraExecutor;

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

        controller = Navigation.findNavController(view);

        // Initialize background executor each time the view is recreated
        cameraExecutor = Executors.newSingleThreadExecutor();

        previewView = view.findViewById(R.id.camera_preview);

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
         * Check for the camera permission before accessing the camera.  If the
         * permission is not granted yet, request permission.
         */
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED) {
            hideCameraDisabledLayout();
            startCameraPreview();
        } else {
            showCameraDisabledLayout();
            requestCameraPermission();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (requestCode == RC_HANDLE_CAMERA_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted
            } else {
                showCameraDisabledLayout();
            }
            // other 'case' lines to check for other
            // permissions this app might request.
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

        final ListenableFuture futureCameraProvider = ProcessCameraProvider.getInstance(requireContext());
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
               preview.setSurfaceProvider(previewView.createSurfaceProvider());

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
            }catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
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
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        return AspectRatio.RATIO_16_9;
    }


    private class QrCodeAnalyzer implements ImageAnalysis.Analyzer{

        @SuppressLint("UnsafeExperimentalUsageError")
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {

            Image image = imageProxy.getImage();
            if (image == null) {
                return;
            }
            FirebaseVisionBarcodeDetectorOptions options =
                    new FirebaseVisionBarcodeDetectorOptions.Builder()
                            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_ALL_FORMATS)
                            .build();
            FirebaseVisionBarcodeDetector detector =
                    FirebaseVision.getInstance().getVisionBarcodeDetector(options);
            FirebaseVisionImage firebaseVisionImage =
                    FirebaseVisionImage.fromMediaImage(image, 0);

            detector.detectInImage(firebaseVisionImage).addOnSuccessListener(firebaseVisionBarcodes -> {
                if (firebaseVisionBarcodes.size() > 0 && !detected) {
                    detected = true;
                    cameraExecutor.shutdown();
                    onQrCodeDetected(firebaseVisionBarcodes.get(0).getRawValue());
                }
            }).addOnFailureListener(Throwable::printStackTrace);

            imageProxy.close();
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

    /*
     * Handles the requesting of the camera permission.
     */
    private void requestCameraPermission() {

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
            showCameraDisabledLayout();
        } else {
            requestPermissions(permissions, RC_HANDLE_CAMERA_PERM);
        }

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

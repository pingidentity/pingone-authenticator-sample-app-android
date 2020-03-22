package com.pingidentity.authenticatorsampleapp.qr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.TextureView;
import android.view.ViewGroup;

import androidx.annotation.RequiresPermission;


import java.io.IOException;

public class QrCameraPreview extends ViewGroup {
    private static final String TAG = "CameraPreview";

    private Context mContext;
    private Camera2Source mCamera2Source;
    private AutoFitTextureView mAutoFitTextureView;

    private boolean mStartRequested;
    private boolean mSurfaceAvailable;

    private int screenRotation;

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @SuppressLint("MissingPermission")
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            mSurfaceAvailable = true;
            startIfReady();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {}

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            mSurfaceAvailable = false;
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture texture) {}
    };


    public QrCameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mStartRequested = false;
        mSurfaceAvailable = false;


        mAutoFitTextureView = new AutoFitTextureView(context);
        mAutoFitTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        addView(mAutoFitTextureView);
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    public void start(Camera2Source cameraSource){
        if (cameraSource == null) {
            stop();
        }
        mCamera2Source = cameraSource;
        if (mCamera2Source != null) {
            mStartRequested = true;
            startIfReady();
        }
    }

    @RequiresPermission(Manifest.permission.CAMERA)
    private void startIfReady(){
        if (mStartRequested && mSurfaceAvailable) {
            mCamera2Source.start(mAutoFitTextureView, screenRotation);
            mStartRequested = false;
        }
    }

    public void stop() {
        Log.w(TAG, "stopping cameraSource");
        if (mCamera2Source != null) {
            mCamera2Source.stop();
        }
    }

    public void release() {
        if (mCamera2Source != null) {
            mCamera2Source.release();
            mCamera2Source = null;
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = 480;
        int height = 720;
        if (mCamera2Source != null) {
            Size size = mCamera2Source.getPreviewSize();
            if (size != null) {
                width = size.getWidth();
                height = size.getHeight();
            }
        }
        // Swap width and height sizes when in portrait, since it will be rotated 90 degrees
        if (isPortraitMode()) {
            int tmp = width;
            //noinspection SuspiciousNameCombination
            width = height;
            height = tmp;
        }

        final int layoutWidth = right - left;
        final int layoutHeight = bottom - top;

        // Computes height and width for potentially doing fit width.
        int childWidth = layoutWidth;
        int childHeight = (int)(((float) layoutWidth / (float) width) * height);

        // If height is too tall using fit width, does fit height instead.
        if (childHeight > layoutHeight) {
            childHeight = layoutHeight;
            childWidth = (int)(((float) layoutHeight / (float) height) * width);
        }

        for (int i = 0; i < getChildCount(); ++i) {
            getChildAt(i).layout(0, 0, childWidth, childHeight);
        }

        try {
            startIfReady();
        } catch (SecurityException se) {
            Log.e(TAG,"Do not have permission to start the camera", se);
        }
    }

    private boolean isPortraitMode() {
        int orientation = mContext.getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            return false;
        }
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            return true;
        }

        Log.d(TAG, "isPortraitMode returning false by default");
        return false;
    }


}

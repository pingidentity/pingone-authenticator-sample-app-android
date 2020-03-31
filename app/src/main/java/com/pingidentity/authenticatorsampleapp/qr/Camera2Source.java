package com.pingidentity.authenticatorsampleapp.qr;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static com.google.android.gms.vision.L.TAG;

public class Camera2Source {

    /**
     * Max preview width that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_WIDTH = 1920;

    /**
     * Max preview height that is guaranteed by Camera2 API
     */
    private static final int MAX_PREVIEW_HEIGHT = 1080;

    private static final double ratioTolerance = 0.1;
    private static final double maxRatioTolerance = 0.18;

    private Context mContext;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    /**
     * A reference to the opened {@link CameraDevice}.
     */
    private CameraDevice mCameraDevice;

    /**
     * A {@link CameraCaptureSession } for camera preview.
     */
    private CameraCaptureSession mCaptureSession;

    /**
     * An {@link AutoFitTextureView} for camera preview.
     */
    private AutoFitTextureView mTextureView;

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private Thread mProcessingThread;
    private FrameProcessingRunnable mFrameProcessor;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread mBackgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler mBackgroundHandler;

    /**
     * An {@link ImageReader} that handles live preview.
     */
    private ImageReader mImageReaderPreview;

    /**
     * The {@link Size} of camera preview.
     */
    private Size mPreviewSize;
    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;

    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    private boolean cameraStarted = false;
    private boolean swappedDimensions = false;
    private int mDisplayOrientation;
    private int mSensorOrientation;

    /**
     * Builder for configuring and creating an associated camera source.
     */
    public static class Builder {

        private final Detector<?> mDetector;
        private Camera2Source mCameraSource = new Camera2Source();
        /**
         * Creates a camera source builder with the supplied context and detector.  Camera preview
         * images will be streamed to the associated detector upon starting the camera source.
         */
        public Builder (Context context, Detector detector){
            if (context == null) {
                throw new IllegalArgumentException("No context supplied.");
            }
            if (detector == null) {
                throw new IllegalArgumentException("No detector supplied.");
            }

            mDetector = detector;
            mCameraSource.mContext = context;
        }
        /**
         * Creates an instance of the camera source.
         */
        public Camera2Source build() {
            mCameraSource.mFrameProcessor = mCameraSource.new FrameProcessingRunnable(mDetector);
            return mCameraSource;
        }
    }

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its state.
     */
    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            mCameraDevice = cameraDevice;
            createCameraPreviewSession();
        }
        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            mCameraOpenCloseLock.release();
            cameraDevice.close();
            mCameraDevice = null;
        }
    };

    /**
     * This is a callback object for the {@link ImageReader}. "onImageAvailable" will be called when a
     * preview frame is ready to be processed.
     */
    private final ImageReader.OnImageAvailableListener mOnPreviewAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image mImage = reader.acquireNextImage();
            if(mImage == null) {
                return;
            }
            mFrameProcessor.setNextFrame(convertYUV420888ToNV21(mImage));
            mImage.close();
        }
    };


    /**
     * Opens the camera and starts sending preview frames to the underlying detector.  The supplied
     * texture view is used for the preview so frames can be displayed to the user.
     *
     * @param textureView the surface holder to use for the preview frames
     * @param displayOrientation the display orientation for a non stretched preview
     */
    @SuppressWarnings("SuspiciousNameCombination")
    @RequiresPermission(Manifest.permission.CAMERA)
    public Camera2Source start(@NonNull AutoFitTextureView textureView, int displayOrientation) {
        mDisplayOrientation = displayOrientation;

        if (cameraStarted){
            return this;
        }
        cameraStarted = true;
        /*
         * Starts a background thread and its {@link Handler}.
         */
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mProcessingThread = new Thread(mFrameProcessor);
        mFrameProcessor.setActive(true);
        mProcessingThread.start();

        mTextureView = textureView;
        if (mTextureView.isAvailable()){
          try{
              if(!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                  throw new RuntimeException("Time out waiting to lock camera opening.");
              }
              CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
              String mCameraId = manager.getCameraIdList()[0];
              CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraId);
              StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
              if (map == null) {
                  return null;
              }

              /*
               * Find out if we need to swap dimension to get the preview size relative to sensor coordinates
               */
              Integer sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
              if(sensorOrientation != null) {
                  mSensorOrientation = sensorOrientation;
                  switch (mDisplayOrientation) {
                      case Surface.ROTATION_0:
                      case Surface.ROTATION_180:
                          if (mSensorOrientation == 90 || mSensorOrientation == 270) {
                              swappedDimensions = true;
                          }
                          break;
                      case Surface.ROTATION_90:
                      case Surface.ROTATION_270:
                          if (mSensorOrientation == 0 || mSensorOrientation == 180) {
                              swappedDimensions = true;
                          }
                          break;
                      default:
                          Log.e(TAG, "Display rotation is invalid: " + mDisplayOrientation);
                  }
              }
              int width = mTextureView.getWidth();
              int height = mTextureView.getHeight();
              Point displaySize = new Point(getScreenWidth(mContext), getScreenHeight(mContext));
              int rotatedPreviewWidth = width;
              int rotatedPreviewHeight = height;
              int maxPreviewWidth = displaySize.x;
              int maxPreviewHeight = displaySize.y;

              if (swappedDimensions) {
                  rotatedPreviewWidth = height;
                  rotatedPreviewHeight = width;
                  maxPreviewWidth = displaySize.y;
                  maxPreviewHeight = displaySize.x;
              }

              if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
                  maxPreviewWidth = MAX_PREVIEW_WIDTH;
              }

              if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
                  maxPreviewHeight = MAX_PREVIEW_HEIGHT;
              }

              // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
              // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
              // garbage capture data.
              Size[] outputSizes = map.getOutputSizes(SurfaceTexture.class);

              Size largest = getBestAspectPictureSize(outputSizes);
              if (largest == null){
                  largest = Collections.max(Arrays.asList(outputSizes), new CompareSizesByArea());;
              }
              mPreviewSize = chooseOptimalSize(outputSizes, rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth, maxPreviewHeight, largest);

              // We fit the aspect ratio of TextureView to the size of preview we picked.
              int orientation = mDisplayOrientation;
              if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                  mTextureView.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
              } else {
                  mTextureView.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
              }

              configureTransform(width, height);

              manager.openCamera(mCameraId, mStateCallback, mBackgroundHandler);

          } catch (InterruptedException e) {
              e.printStackTrace();
          } catch (CameraAccessException e) {
              Log.e(TAG, "Error accessing camera2");
          }
        }else{
        }
        return this;
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     * <p/>
     * This camera source may be restarted again by calling {@link #start(AutoFitTextureView, int)}.
     * <p/>
     * Call {@link #release()} instead to completely shut down this camera source and release the
     * resources of the underlying detector.
     */
    public void stop() {
        try {
            mFrameProcessor.setActive(false);
            if (mProcessingThread != null) {
                try {
                    // Wait for the thread to complete to ensure that we can't have multiple threads
                    // executing at the same time (i.e., which would happen if we called start too
                    // quickly after stop).
                    mProcessingThread.join();
                } catch (InterruptedException e) {
                    Log.d(TAG, "Frame processing thread interrupted on release.");
                }
                mProcessingThread = null;
            }
            mCameraOpenCloseLock.acquire();
            if (null != mCaptureSession) {
                mCaptureSession.close();
                mCaptureSession = null;
            }
            if (null != mCameraDevice) {
                mCameraDevice.close();
                mCameraDevice = null;
            }
            if (null != mImageReaderPreview) {
                mImageReaderPreview.close();
                mImageReaderPreview = null;
            }
            cameraStarted = false;
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        } finally {
            mCameraOpenCloseLock.release();
            stopBackgroundThread();
        }
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        try {
            if(mBackgroundThread != null) {
                mBackgroundThread.quitSafely();
                mBackgroundThread.join();
                mBackgroundThread = null;
                mBackgroundHandler = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the camera and releases the resources of the camera and underlying detector.
     */
    public void release() {
        mFrameProcessor.release();
        stop();
    }

    /**
     * Creates a new {@link CameraCaptureSession} for camera preview.
     */
    private void createCameraPreviewSession() {
        try{
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            assert texture != null;
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());

            mImageReaderPreview = ImageReader.newInstance(mPreviewSize.getWidth(), mPreviewSize.getHeight(), ImageFormat.YUV_420_888, 1);
            mImageReaderPreview.setOnImageAvailableListener(mOnPreviewAvailableListener, mBackgroundHandler);
            // This is the output Surface we need to start preview.
            Surface surface = new Surface(texture);

            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewRequestBuilder.addTarget(surface);
            mPreviewRequestBuilder.addTarget(mImageReaderPreview.getSurface());
            // Here, we create a CameraCaptureSession for camera preview.
            mCameraDevice.createCaptureSession(Arrays.asList(surface, mImageReaderPreview.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    // The camera is already closed
                    if (null == mCameraDevice) {
                        return;
                    }
                    // When the session is ready, we start displaying the preview.
                    mCaptureSession = cameraCaptureSession;
                    try {
                        // Auto focus should be continuous for camera preview.
                        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);

                        // Finally, we start displaying the camera preview.
                        mPreviewRequest = mPreviewRequestBuilder.build();
                        mCaptureSession.setRepeatingRequest(mPreviewRequest, null, mBackgroundHandler);
                    } catch (CameraAccessException | IllegalStateException e ) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e(TAG, "Camera Configuration failed!");
                }
            }, mBackgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing camera2 API");
            e.printStackTrace();
        }
    }

    private void closePreviewSession() {
        if(mCaptureSession != null) {
            mCaptureSession.close();
            mCaptureSession = null;
        }
    }

    /**
     * Returns the preview size that is currently in use by the underlying camera.
     */
    public Size getPreviewSize() {
        return mPreviewSize;
    }


    /**
     * This runnable controls access to the underlying receiver, calling it to process frames when
     * available from the camera.  This is designed to run detection on frames as fast as possible
     * (i.e., without unnecessary context switching or waiting on the next frame).
     * <p/>
     * While detection is running on a frame, new frames may be received from the camera.  As these
     * frames come in, the most recent frame is held onto as pending.  As soon as detection and its
     * associated processing are done for the previous frame, detection on the mostly recently
     * received frame will immediately start on the same thread.
     */
    private class FrameProcessingRunnable implements Runnable {

        private Detector<?> mDetector;
        private long mStartTimeMillis = SystemClock.elapsedRealtime();

        // This lock guards all of the member variables below.
        private final Object mLock = new Object();
        private boolean mActive = true;

        // These pending variables hold the state associated with the new frame awaiting processing.
        private long mPendingTimeMillis;
        private int mPendingFrameId = 0;
        private byte[] mPendingFrameData;
        /*
         * constructor binding detector
         */
        FrameProcessingRunnable(Detector<?> detector) {
            mDetector = detector;
        }

        /**
         * As long as the processing thread is active, this executes detection on frames
         * continuously.  The next pending frame is either immediately available or hasn't been
         * received yet.  Once it is available, we transfer the frame info to local variables and
         * run detection on that frame.  It immediately loops back for the next frame without
         * pausing.
         * <p/>
         * If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context
         * switching or frame acquisition time latency.
         * <p/>
         * If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        @Override
        public void run() {
            Frame outputFrame;

            while (true) {
                synchronized (mLock) {
                    while (mActive && (mPendingFrameData == null)) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            mLock.wait();
                        } catch (InterruptedException e) {
                            Log.d(TAG, "Frame processing loop terminated.", e);
                            return;
                        }
                    }
                    if (!mActive) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return;
                    }
                    outputFrame = new Frame.Builder()
                            .setImageData(ByteBuffer.wrap(quarterNV21(mPendingFrameData, mPreviewSize.getWidth(), mPreviewSize.getHeight())), mPreviewSize.getWidth() / 4, mPreviewSize.getHeight() / 4, ImageFormat.NV21)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation(getDetectorOrientation(mSensorOrientation))
                            .build();

                    // We need to clear mPendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    mPendingFrameData = null;
                }
                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.

                try {
                    mDetector.receiveFrame(outputFrame);
                } catch (Throwable t) {
                    Log.e(TAG, "Exception thrown from receiver.", t);
                }
            }
        }

        /**
         * Releases the underlying receiver.  This is only safe to do after the associated thread
         * has completed, which is managed in camera source's release method above.
         */
        @SuppressLint("Assert")
        void release() {
            assert (mProcessingThread.getState() == Thread.State.TERMINATED);
            mDetector.release();
            mDetector = null;
        }

        /**
         * Sets the frame data received from the camera.
         */
        void setNextFrame(byte[] data) {
            synchronized (mLock) {
                if (mPendingFrameData != null) {
                    mPendingFrameData = null;
                }

                /*
                 * Timestamp and frame ID are maintained here, which will give downstream code some
                 * idea of the timing of frames received and when frames were dropped along the way.
                 */
                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis;
                mPendingFrameId++;
                mPendingFrameData = data;

                // Notify the processor thread if it is waiting on the next frame (see below).
                mLock.notifyAll();
            }
        }

        /**
         * Marks the runnable as active/not active.  Signals any blocked threads to continue.
         */
        void setActive(boolean active) {
            synchronized (mLock) {
                mActive = active;
                mLock.notifyAll();
            }
        }
    }

    /*
     * Converting YUV_420_888 data to NV21.
     */
    private byte[] convertYUV420888ToNV21(Image imgYUV420) {
        byte[] data;
        ByteBuffer buffer0 = imgYUV420.getPlanes()[0].getBuffer();
        ByteBuffer buffer2 = imgYUV420.getPlanes()[2].getBuffer();
        int buffer0_size = buffer0.remaining();
        int buffer2_size = buffer2.remaining();
        data = new byte[buffer0_size + buffer2_size];
        buffer0.get(data, 0, buffer0_size);
        buffer2.get(data, buffer0_size, buffer2_size);
        return data;
    }

    /*
     * Reduce to quarter size the NV21 frame
     */
    private byte[] quarterNV21(byte[] data, int iWidth, int iHeight) {
        byte[] yuv = new byte[iWidth/4 * iHeight/4 * 3 / 2];
        // halve yuma
        int i = 0;
        for (int y = 0; y < iHeight; y+=4) {
            for (int x = 0; x < iWidth; x+=4) {
                yuv[i] = data[y * iWidth + x];
                i++;
            }
        }
        return yuv;
    }

    private float getScreenRatio(Context c) {
        DisplayMetrics metrics = c.getResources().getDisplayMetrics();
        return ((float)metrics.heightPixels / (float)metrics.widthPixels);
    }

    private int getScreenHeight(Context c) {
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.y;
    }

    private int getScreenWidth(Context c) {
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        assert wm != null;
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        return size.x;
    }

    private Size getBestAspectPictureSize(Size[] supportedPictureSizes) {
        float targetRatio = getScreenRatio(mContext);
        Size bestSize = null;
        TreeMap<Double, List<Size>> diffs = new TreeMap<>();

        //Select supported sizes which ratio is less than ratioTolerance
        for (Size size : supportedPictureSizes) {
            float ratio = (float) size.getWidth() / size.getHeight();
            double diff = Math.abs(ratio - targetRatio);
            if (diff < ratioTolerance){
                if (diffs.keySet().contains(diff)){
                    //add the value to the list
                    diffs.get(diff).add(size);
                } else {
                    List<Size> newList = new ArrayList<>();
                    newList.add(size);
                    diffs.put(diff, newList);
                }
            }
        }

        //If no sizes were supported, (strange situation) establish a higher ratioTolerance
        if(diffs.isEmpty()) {
            for (Size size : supportedPictureSizes) {
                float ratio = (float)size.getWidth() / size.getHeight();
                double diff = Math.abs(ratio - targetRatio);
                if (diff < maxRatioTolerance){
                    if (diffs.keySet().contains(diff)){
                        //add the value to the list
                        diffs.get(diff).add(size);
                    } else {
                        List<Size> newList = new ArrayList<>();
                        newList.add(size);
                        diffs.put(diff, newList);
                    }
                }
            }
        }

        //Select the highest resolution from the ratio filtered ones.
        for (Map.Entry entry: diffs.entrySet()){
            List<?> entries = (List) entry.getValue();
            for (int i=0; i<entries.size(); i++) {
                Size s = (Size) entries.get(i);
                if(bestSize == null) {
                    bestSize = new Size(s.getWidth(), s.getHeight());
                } else if(bestSize.getWidth() < s.getWidth() || bestSize.getHeight() < s.getHeight()) {
                    bestSize = new Size(s.getWidth(), s.getHeight());
                }
            }
        }
        return bestSize;
    }

    /**
     * Given {@code choices} of {@code Size}s supported by a camera, choose the smallest one that
     * is at least as large as the respective texture view size, and that is at most as large as the
     * respective max size, and whose aspect ratio matches with the specified value. If such size
     * doesn't exist, choose the largest one that is at most as large as the respective max size,
     * and whose aspect ratio matches with the specified value.
     *
     * @param choices           The list of sizes that the camera supports for the intended output
     *                          class
     * @param textureViewWidth  The width of the texture view relative to sensor coordinate
     * @param textureViewHeight The height of the texture view relative to sensor coordinate
     * @param maxWidth          The maximum width that can be chosen
     * @param maxHeight         The maximum height that can be chosen
     * @param aspectRatio       The aspect ratio
     * @return The optimal {@code Size}, or an arbitrary one if none were big enough
     */
    private Size chooseOptimalSize(Size[] choices, int textureViewWidth, int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {

        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }

        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }


    /**
     * Configures the necessary {@link Matrix} transformation to `mTextureView`.
     * This method should be called after the camera preview size is determined in
     * setUpCameraOutputs and also the size of `mTextureView` is fixed.
     *
     * @param viewWidth  The width of `mTextureView`
     * @param viewHeight The height of `mTextureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        if (null == mTextureView || null == mPreviewSize) {
            return;
        }
        int rotation = mDisplayOrientation;
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / mPreviewSize.getHeight(),
                    (float) viewWidth / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        mTextureView.setTransform(matrix);
    }

    private int getDetectorOrientation(int sensorOrientation) {
        switch (sensorOrientation) {
            case 0:
            case 360:
                return Frame.ROTATION_0;
            case 180:
                return Frame.ROTATION_180;
            case 270:
                return Frame.ROTATION_270;
            case 90:
            default:
                return Frame.ROTATION_90;
        }
    }

    /**
     * Compares two {@code Size}s based on their areas.
     */
    private static class CompareSizesByArea implements Comparator<Size> {

        @Override
        public int compare(Size lhs, Size rhs) {
            // We cast here to ensure the multiplications won't overflow
            return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                    (long) rhs.getWidth() * rhs.getHeight());
        }

    }
}

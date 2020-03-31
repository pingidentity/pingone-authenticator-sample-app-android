package com.pingidentity.authenticatorsampleapp.qr;

import android.content.Context;

import androidx.annotation.UiThread;

import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Generic tracker which is used for tracking or reading a barcode (and can really be used for
 * any type of item).  This is used to receive newly detected items, add a graphical representation
 * to an overlay, update the graphics as the item changes, and remove the graphics when the item
 * goes away.
 */
public class QrTracker extends Tracker<Barcode> {

    private QrCodeUpdateListener mQrCodeUpdateListener;

    /**
     * Consume the item instance detected from an Activity or Fragment level by implementing the
     * QrCodeUpdateListener interface method onQrCodeDetected.
     */
    public interface QrCodeUpdateListener {
        @UiThread
        void onQrCodeDetected(Barcode barcode);
    }

    QrTracker(Context context) {
        if (context instanceof QrCodeUpdateListener) {
            this.mQrCodeUpdateListener = (QrCodeUpdateListener) context;
        } else {
            throw new RuntimeException("Hosting activity must implement BarcodeUpdateListener");
        }
    }

    /**
     * Start tracking the detected item instance within the item overlay.
     */
    @Override
    public void onNewItem(int i, Barcode barcode) {
        mQrCodeUpdateListener.onQrCodeDetected(barcode);
    }
}

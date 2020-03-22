package com.pingidentity.authenticatorsampleapp.qr;

import android.content.Context;

import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.barcode.Barcode;

/**
 * Factory for creating a tracker and associated graphic to be associated with a new barcode.  The
 * multi-processor uses this factory to create barcode trackers as needed -- one for each barcode.
 */
public class QrTrackerFactory implements MultiProcessor.Factory<Barcode> {
    private Context mContext;

    public QrTrackerFactory(Context mContext) {
        this.mContext = mContext;
    }

    @Override
    public Tracker<Barcode> create(Barcode barcode) {
        return new QrTracker(mContext);
    }

}

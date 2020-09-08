package com.t2m;

import android.util.Log;
import android.util.Size;
import android.view.Surface;

public abstract class CameraHelper {
    private static final String TAG = CameraHelper.class.getSimpleName();

    public static final int STATUS_IDLE = 1;
    public static final int STATUS_OPENED = 2;
    public static final int STATUS_OPENING = 4;
    public static final int STATUS_CLOSING = 8;

    private final Object mStatusLock = new Object();
    private int mStatus = STATUS_IDLE;

    public int status() {
        return mStatus;
    }

    public void setStatus(int status) {
        synchronized (mStatusLock) {
            mStatus = status;
            mStatusLock.notifyAll();
        }
    }

    public boolean checkStatusAny(int status) {
        return (mStatus & status) != 0;
    }

    public void waitForStatusAny(int status) {
        synchronized (mStatusLock) {
            while (!checkStatusAny(status)) {
                try {
                    mStatusLock.wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "wait for status any interrupted.");
                }
            }
        }
    }

    public abstract void open(String cameraId, Surface surface, OnOpenedListener listener);
    public abstract void close(OnClosedListener listener);
    public abstract int orientation(String cameraId);
    public abstract boolean mirror(String cameraId);
    public abstract Size[] getAvailableVideoSize(String cameraId);
    public abstract Size[] getAvailablePictureSize(String cameraId);

    public abstract void setFpsRange(int lower, int upper);

    public interface OnOpenedListener {
        void onOpened(boolean error);
    }

    public interface OnClosedListener {
        void onClosed(boolean error);
    }
}

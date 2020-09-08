package com.t2m;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;

import com.t2m.util.SurfaceUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Camera1Helper extends CameraHelper {
    private static final String TAG = Camera1Helper.class.getSimpleName();

    @SuppressWarnings("deprecation")
    private Camera mCamera;
    @SuppressWarnings("deprecation")
    private final Map<String, Camera.Parameters> mReadonlyParameters = new HashMap<>();
    @SuppressWarnings("deprecation")
    private final Map<String, Camera.CameraInfo> mCameraInfos = new HashMap<>();

    private Range<Integer> mFpsRange = new Range<>(30 , 30);

    @Override
    @SuppressWarnings("deprecation")
    public void open(String cameraId, Surface surface, OnOpenedListener listener) {
        if (checkStatusAny(STATUS_OPENED | STATUS_OPENING)) {
            // already opened or opening
            return;
        }
        waitForStatusAny(STATUS_IDLE);

        // begin open
        setStatus(STATUS_OPENING);
        try {
            // open camera
            mCamera = Camera.open(id2Index(cameraId));

            // update parameters for preview
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewSize(SurfaceUtil.width(surface), SurfaceUtil.height(surface));
            parameters.setPreviewFpsRange(mFpsRange.getLower() * 1000, mFpsRange.getUpper() * 1000);
            mCamera.setParameters(parameters);

            // set preview surface
            mCamera.setPreviewDisplay(new PreviewSurfaceHolder(surface));

            // start preview
            mCamera.startPreview();

            // callback
            if (listener != null) {
                listener.onOpened(false);
            }

            setStatus(STATUS_OPENED);
        } catch (IOException e) {
            Log.e(TAG, "open camera failed.", e);
            if (mCamera != null) {
                mCamera.release();
                mCamera = null;
            }

            // callback
            if (listener != null) {
                listener.onOpened(true);
            }

            setStatus(STATUS_IDLE);
        }
    }

    @Override
    public void close(OnClosedListener listener) {
        if (checkStatusAny(STATUS_IDLE | STATUS_CLOSING)) {
            // already opened or opening
            return;
        }
        waitForStatusAny(STATUS_OPENED);

        // begin close
        setStatus(STATUS_CLOSING);

        // release camera
        mCamera.release();

        // callback
        if (listener != null) {
            listener.onClosed(false);
        }

        setStatus(STATUS_IDLE);
    }

    @Override
    public int orientation(String cameraId) {
        return getCameraInfo(cameraId).orientation + 180;
    }

    @Override
    public boolean mirror(String cameraId) {
        return !Objects.equals("1", cameraId);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Size[] getAvailableVideoSize(String cameraId) {
        Camera.Parameters parameters = getReadonlyParameters(cameraId);
        ArrayList<Size> list = new ArrayList<>();
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            list.add(new Size(size.width, size.height));
        }
        return list.toArray(new Size[0]);
    }

    @Override
    @SuppressWarnings("deprecation")
    public Size[] getAvailablePictureSize(String cameraId) {
        Camera.Parameters parameters = getReadonlyParameters(cameraId);
        ArrayList<Size> list = new ArrayList<>();
        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            list.add(new Size(size.width, size.height));
        }
        return list.toArray(new Size[0]);
    }

    @Override
    public void setFpsRange(int lower, int upper) {
        mFpsRange = new Range<>(lower, upper);
    }

    @SuppressWarnings("deprecation")
    private Camera.CameraInfo getCameraInfo(String cameraId) {
        synchronized (mCameraInfos) {
            Camera.CameraInfo info = mCameraInfos.get(cameraId);
            if (info == null) {
                info = new Camera.CameraInfo();
                Camera.getCameraInfo(id2Index(cameraId), info);
                mCameraInfos.put(cameraId, info);
            }
            return info;
        }
    }

    @SuppressWarnings({"unused", "deprecation"})
    private Camera.Parameters getReadonlyParameters(String cameraId) {
        synchronized (mReadonlyParameters) {
            Camera.Parameters parameters = mReadonlyParameters.get(cameraId);
            if (parameters == null) {
                Camera camera = Camera.open(id2Index(cameraId));
                parameters = camera.getParameters();
                camera.release();
                mReadonlyParameters.put(cameraId, parameters);
            }
            return parameters;
        }
    }

    private static int id2Index(String id) {
        try {
            return Integer.valueOf(id, 10);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @SuppressWarnings("unused")
    private static String index2Id(int index) {
        return Integer.toString(index, 10);
    }

    private static class PreviewSurfaceHolder implements SurfaceHolder {
        private Surface mSurface;
        private int mWidth;
        private int mHeight;
        private Rect mRect;
        private final List<Callback> mCallbacks = new ArrayList<>();

        PreviewSurfaceHolder(Surface surface) {
            mSurface = surface;
            mWidth = SurfaceUtil.width(mSurface);
            mHeight = SurfaceUtil.height(mSurface);
            mRect = new Rect(0, 0, mWidth, mHeight);
        }

        @SuppressWarnings("unused")
        int width() {
            return mWidth;
        }

        @SuppressWarnings("unused")
        int height() {
            return mHeight;
        }

        @SuppressWarnings("unused")
        void release() {
            synchronized (mCallbacks) {
                for (Callback callback : mCallbacks) {
                    callback.surfaceDestroyed(this);
                }
                mCallbacks.clear();
                mSurface = null;
            }
        }

        @Override
        public void addCallback(Callback callback) {
            synchronized (mCallbacks) {
                if (!mCallbacks.contains(callback)) {
                    mCallbacks.add(callback);

                    callback.surfaceCreated(this);
                    callback.surfaceChanged(this,
                            SurfaceUtil.format(mSurface),
                            SurfaceUtil.width(mSurface),
                            SurfaceUtil.height(mSurface));
                }
            }
        }

        @Override
        public void removeCallback(Callback callback) {
            synchronized (mCallbacks) {
                mCallbacks.remove(callback);
            }
        }

        @Override
        public boolean isCreating() {
            return false;
        }

        @Override
        public void setType(int i) {
            // ignore
        }

        @Override
        public void setFixedSize(int i, int i1) {
            // ignore
        }

        @Override
        public void setSizeFromLayout() {
            // ignore
        }

        @Override
        public void setFormat(int i) {
            // ignore
        }

        @Override
        public void setKeepScreenOn(boolean b) {
            // ignore
        }

        @Override
        public Canvas lockCanvas() {
            return mSurface.lockCanvas(null);
        }

        @Override
        public Canvas lockCanvas(Rect rect) {
            return mSurface.lockCanvas(rect);
        }

        @Override
        public void unlockCanvasAndPost(Canvas canvas) {
            mSurface.unlockCanvasAndPost(canvas);
        }

        @Override
        public Rect getSurfaceFrame() {
            return mRect;
        }

        @Override
        public Surface getSurface() {
            return mSurface;
        }
    }
}

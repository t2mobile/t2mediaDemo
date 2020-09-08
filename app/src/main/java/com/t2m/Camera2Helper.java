package com.t2m;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Camera2Helper extends CameraHelper {
    private static final String TAG = Camera2Helper.class.getSimpleName();

    private Context mContext;
    private HandlerThread mCameraThread = null;
    private Handler mCameraHandler = null;

    private Surface mOutputSurface = null;
    private CameraDevice mCameraDevice = null;
    private CameraCaptureSession mCameraSession = null;

    private Range<Integer> mFpsRange = new Range<>(30 , 30);

    public Camera2Helper(Context context) {
        mContext = context;
    }

    @Override
    public void open(String cameraId, Surface surface, OnOpenedListener listener) {
        if (checkStatusAny(STATUS_OPENED | STATUS_OPENING)) {
            // already opened or opening
            return;
        }
        waitForStatusAny(STATUS_IDLE);

        // begin open
        setStatus(STATUS_OPENING);

        onOpenCamera(cameraId, surface, listener);
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

        onCloseCamera(listener);
    }

    @Override
    public int orientation(String cameraId) {
        CameraManager cm = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        assert cm != null;
        int orientation = 0;
        try {
            Integer i = cm.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SENSOR_ORIENTATION);
            orientation = i == null ? 0 : i;
        } catch (CameraAccessException e) {
            Log.e(TAG, "getSensorOrientation() failed.", e);
        }

        return orientation + 180;
    }

    @Override
    public boolean mirror(String cameraId) {
        return !Objects.equals("1", cameraId);
    }

    @Override
    public Size[] getAvailableVideoSize(String cameraId) {
        CameraManager cm = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        assert cm != null;
        try {
            StreamConfigurationMap map = cm.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            if (map != null) {
                return map.getOutputSizes(SurfaceTexture.class);
            } else {
                return new Size[0];
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "getAvailableSurfaceSize() failed.", e);
            return new Size[0];
        }
    }

    @Override
    public Size[] getAvailablePictureSize(String cameraId) {
        return getAvailableVideoSize(cameraId);
    }

    @Override
    public void setFpsRange(int lower, int upper) {
        mFpsRange = new Range<>(lower, upper);
    }

    @SuppressLint("MissingPermission")
    private void onOpenCamera(String cameraId, Surface surface, OnOpenedListener listener) {
        mOutputSurface = surface;
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        CameraManager cm = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        assert cm != null;
        try {
            cm.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    onCameraOpened(cameraDevice, listener);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    if (listener != null) {
                        listener.onOpened(true);
                    }
                    onCloseCamera(null);
                    setStatus(STATUS_IDLE);
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int i) {
                    if (listener != null) {
                        listener.onOpened(true);
                    }
                    onCloseCamera(null);
                    setStatus(STATUS_IDLE);
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "open camera failed.", e);
            if (listener != null) {
                listener.onOpened(true);
            }
            onCloseCamera(null);
            setStatus(STATUS_IDLE);
        }
    }

    private void onCameraOpened(CameraDevice device, OnOpenedListener listener) {
        mCameraDevice = device;

        List<Surface> surfaceList = new ArrayList<>();
        surfaceList.add(mOutputSurface);
        try {
            mCameraDevice.createCaptureSession(surfaceList, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraSession = cameraCaptureSession;

                    try {
                        CaptureRequest.Builder cb = mCameraDevice.createCaptureRequest(
                                supportZsl(mCameraSession.getDevice().getId()) ? CameraDevice.TEMPLATE_ZERO_SHUTTER_LAG: CameraDevice.TEMPLATE_RECORD);
                        cb.addTarget(mOutputSurface);

                        cb.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                        cb.set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFpsRange);
                        cb.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_AUTO);

                        mCameraSession.setRepeatingRequest(cb.build(), null, mCameraHandler);

                        if (listener != null) {
                            listener.onOpened(false);
                        }

                        setStatus(STATUS_OPENED);
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "start capture failed.", e);
                        if (listener != null) {
                            listener.onOpened(true);
                        }
                        onCloseCamera(null);
                        setStatus(STATUS_IDLE);
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    if (listener != null) {
                        listener.onOpened(true);
                    }

                    onCloseCamera(null);
                    setStatus(STATUS_IDLE);
                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "start camera session failed.", e);
            if (listener != null) {
                listener.onOpened(true);
            }
            onCloseCamera(null);
            setStatus(STATUS_IDLE);
        }
    }

    private void onCloseCamera(OnClosedListener listener) {
        try {
            if (mCameraSession != null) {
                mCameraSession.stopRepeating();
                mCameraSession.close();
                mCameraSession = null;
            }

            if (mCameraDevice != null) {
                mCameraDevice.close();
                mCameraDevice = null;
            }

            if (mCameraThread != null) {
                mCameraThread.quitSafely();
                mCameraThread = null;
                mCameraHandler = null;
            }

            if (listener != null) {
                listener.onClosed(false);
            }

            setStatus(STATUS_IDLE);
        } catch (CameraAccessException e) {
            Log.e(TAG, "close camera failed.", e);

            if (listener != null) {
                listener.onClosed(true);
            }

            setStatus(STATUS_IDLE);
        }
    }

    private boolean supportZsl(String cameraId) {
        try {
            CameraManager cm = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            assert cm != null;
            CameraCharacteristics characteristics = cm.getCameraCharacteristics(cameraId);
            int[] caps = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
            assert caps != null;
            for (int cap : caps) {
                if (cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING
                        || cap == CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING) {
                    return true;
                }
            }
            return false;
        } catch (CameraAccessException e) {
            return false;
        }
    }
}

package com.hf.t2mediademo;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.t2m.CameraHelper;
import com.t2m.media.ImageRender;
import com.t2m.media.OnNV21SnapshotListener;
import com.t2m.media.OnSnapshotListener;
import com.t2m.media.OnSnapshotSavedListener;
import com.t2m.movedetect2.detector.MoveDetector;
import com.t2m.tts.Tts;
import com.t2m.util.SurfaceUtil;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

public abstract class CameraBase_1_3 extends AppCompatActivity {
    private static final String TAG = CameraBase_1_3.class.getSimpleName();

    private String mCameraId = "0";

    private static final int INPUT_WIDTH = 1920;
    private static final int INPUT_HEIGHT = 1080;

    private static final int RECORD1_WIDTH = 1920;
    private static final int RECORD1_HEIGHT = 1080;
    private static final int RECORD1_BITRATE = 5625000;
    private static final int RECORD1_FPS = 25;

    private static final int RECORD2_WIDTH = 1280;
    private static final int RECORD2_HEIGHT = 720;
    private static final int RECORD2_BITRATE = 2500000;
    private static final int RECORD2_FPS = 30;

    private static final int WATERMARK_LOGO = 0;
    private static final int WATERMARK_TIME = 1;

    private TextureView mPreviewView;
    private Button mBtnMoveDetect;
    private Button mBtnRecord1;
    private Button mBtnRecord2;
    private Button mBtnSnapshot;
    private TextView mHintMoveDetect;
    private TextView mHintRecord1;
    private TextView mHintRecord2;
    private TextView mHintSnapshot;

    private int mColorBtnOn;
    private int mColorBtnOff;
    private String mHintRecord1Prefix;
    private String mHintRecord2Prefix;
    private String mHintSnapshotPrefix;

    private boolean mPreviewDirty = true;
    private boolean mMoveDetectDirty = true;
    private boolean mRecord1Dirty = true;
    private boolean mRecord2Dirty = true;
    private boolean mSnapshotDirty = true;

    private boolean mPreviewOn = false;
    private boolean mMoveDetectOn = false;
    private boolean mRecord1On = false;
    private boolean mRecord2On = false;

    private boolean mDetected = false;
    private String mRecord1Path = "";
    private String mRecord2Path = "";
    private String mSnapshotPath = "";

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private CameraHelper mCamera = createCameraHelper();
    private ImageRender mImageRender = null;

    private Surface mRenderInputSurface = null;
    private OutputStream mPreviewStream = null;
    private OutputStream mRecord1Stream = null;
    private OutputStream mRecord2Stream = null;

    private final Object mPreviewSurfaceLock = new Object();
    private Surface mPreviewSurface = null;

    private Matrix mOrigMatrix = null;

    private MoveDetector mMoveDetector = null;

    private Timer mMoveDetectTimer = null;
    private Timer mWatermarkTimer = null;

    protected abstract CameraHelper createCameraHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()# begin");
        setContentView(R.layout.activity_camera_1_3);

        // find views
        mPreviewView = findViewById(R.id.preview);
        mBtnMoveDetect = findViewById(R.id.btn_move_detect);
        mBtnRecord1 = findViewById(R.id.btn_record1);
        mBtnRecord2 = findViewById(R.id.btn_record2);
        mBtnSnapshot = findViewById(R.id.btn_snapshot);
        mHintMoveDetect = findViewById(R.id.hint_move_detect);
        mHintRecord1 = findViewById(R.id.hint_record1);
        mHintRecord2 = findViewById(R.id.hint_record2);
        mHintSnapshot = findViewById(R.id.hint_snapshot);

        // bind button click event
        mBtnMoveDetect.setOnClickListener(this::onMoveDetectClicked);
        mBtnRecord1.setOnClickListener(this::onRecord1Clicked);
        mBtnRecord2.setOnClickListener(this::onRecord2Clicked);
        mBtnSnapshot.setOnClickListener(this::onSnapshotClicked);

        // other resources
        //noinspection deprecation
        mColorBtnOn = getResources().getColor(R.color.color_btn_on);
        //noinspection deprecation
        mColorBtnOff = getResources().getColor(R.color.color_btn_off);
        mHintRecord1Prefix = getResources().getString(R.string.hint_record1);
        mHintRecord2Prefix = getResources().getString(R.string.hint_record2);
        mHintSnapshotPrefix = getResources().getString(R.string.hint_snapshot);

        // listen preview view
        mPreviewView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                synchronized (mPreviewSurfaceLock) {
                    mPreviewSurface = new Surface(surfaceTexture);
                    mPreviewOn = true;
                    mPreviewDirty = true;
                    mOrigMatrix = new Matrix();
                    mPreviewView.getTransform(mOrigMatrix);
                    mPreviewSurfaceLock.notifyAll();
                    updatePreviewView();
                    postUpdate();
                }
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
                synchronized (mPreviewSurfaceLock) {
                    mPreviewSurface = new Surface(surfaceTexture);
                    mPreviewOn = true;
                    mPreviewDirty = true;
                    mOrigMatrix = new Matrix();
                    mPreviewView.getTransform(mOrigMatrix);
                    mPreviewSurfaceLock.notifyAll();
                    updatePreviewView();
                    postUpdate();
                }
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
                synchronized (mPreviewSurfaceLock) {
                    mPreviewSurface = null;
                    mPreviewOn = false;
                    mPreviewDirty = true;
                    mOrigMatrix = null;
                    mPreviewSurfaceLock.notifyAll();
                    postUpdate();
                    return false;
                }
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {
                // ignore
            }
        });

        Log.d(TAG, "onCreate()# end");
    }

    @Override
    protected void onResume() {
        super.onResume();

        mPreviewOn = mRecord1On = mRecord2On = false;
        updatePreviewView();
        restartCamera();
        startWatermark();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mPreviewOn = mRecord1On = mRecord2On = false;
        restartCamera();

        closeCamera();
        stopWatermark();
    }

    private void startWatermark() {
        stopWatermark();

        mWatermarkTimer = new Timer();
        mWatermarkTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String timeStr = Util.currentTimeStr();
                if (mPreviewStream != null && mPreviewStream.stream() != null) {
                    mPreviewStream.stream().updateWatermarkText(WATERMARK_TIME, timeStr);
                }

                if (mRecord1Stream != null && mRecord1Stream.stream() != null) {
                    mRecord1Stream.stream().updateWatermarkText(WATERMARK_TIME, timeStr);
                }

                if (mRecord2Stream != null && mRecord2Stream.stream() != null) {
                    mRecord2Stream.stream().updateWatermarkText(WATERMARK_TIME, timeStr);
                }
            }
        }, 0, 1000);
    }

    private void stopWatermark() {
        if (mWatermarkTimer != null) {
            mWatermarkTimer.cancel();
            mWatermarkTimer = null;
        }
    }

    private void updatePreviewView() {
        if (mOrigMatrix != null) {
            int orientation = mCamera.orientation(mCameraId);
            int viewWidth = mPreviewView.getMeasuredWidth();
            int viewHeight = mPreviewView.getMeasuredHeight();
            int previewWidth;
            int previewHeight;
            if (orientation == 0 || orientation == 180) {
                // landscape
                previewWidth = viewWidth;
                previewHeight = viewWidth * 9 / 16;
            } else {
                // portrait
                previewWidth = viewHeight * 9 / 16;
                previewHeight = viewHeight;
            }
            Log.d(TAG, "updatePreviewView()# [" + mPreviewView.getMeasuredWidth() + "x" + mPreviewView.getMeasuredHeight() + "], [" + previewWidth + "x" + previewHeight + "]");
            Matrix matrix = new Matrix(mOrigMatrix);
            matrix.postScale((float) previewWidth / (float) viewWidth, (float) previewHeight / (float) viewHeight);
            mPreviewView.setTransform(matrix);
            mPreviewView.getSurfaceTexture().setDefaultBufferSize(previewWidth, previewHeight);
        }
    }

    private Surface getPreviewSurfaceAwait() {
        synchronized (mPreviewSurfaceLock) {
            while (mPreviewSurface == null) {
                try {
                    mPreviewSurfaceLock.wait();
                } catch (InterruptedException e) {
                    Log.w(TAG, "wait for preview size interrupted.");
                    return null;
                }
            }
            return mPreviewSurface;
        }
    }

    @SuppressLint("SetTextI18n")
    private void updateUi() {
        if (mPreviewDirty) {
            mPreviewDirty = false;
            // nothing to do for UI
        }

        if (mMoveDetectDirty) {
            mMoveDetectDirty = false;
            if (mMoveDetectOn) {
                mBtnMoveDetect.setBackgroundColor(mColorBtnOn);
                mHintMoveDetect.setBackgroundColor(mDetected ? mColorBtnOn : mColorBtnOff);
                mHintMoveDetect.setText(mDetected ? R.string.hint_move_detect_success : R.string.hint_move_detect_fail);
            } else {
                mBtnMoveDetect.setBackgroundColor(mColorBtnOff);
                mHintMoveDetect.setBackgroundColor(mColorBtnOff);
                mHintMoveDetect.setText(R.string.hint_move_detect_fail);
            }
        }

        if (mRecord1Dirty) {
            mRecord1Dirty = false;
            mBtnRecord1.setBackgroundColor(mRecord1On ? mColorBtnOn : mColorBtnOff);
            mHintRecord1.setText(mHintRecord1Prefix + (mRecord1On ? fileName(mRecord1Path) : ""));
        }

        if (mRecord2Dirty) {
            mRecord2Dirty = false;
            mBtnRecord2.setBackgroundColor(mRecord2On ? mColorBtnOn : mColorBtnOff);
            mHintRecord2.setText(mHintRecord2Prefix + (mRecord2On ? fileName(mRecord2Path) : ""));
        }

        if (mSnapshotDirty) {
            mSnapshotDirty = false;
            mHintSnapshot.setText(mHintSnapshotPrefix + fileName(mSnapshotPath));
        }
    }

    private static String fileName(String name) {
        File file = new File(name);
        return file.getName();
    }

    private void update() {
        restartCamera();
        updateUi();
    }

    private void postUpdate() {
        mHandler.post(this::update);
    }

    private void postUpdateUi() {
        mHandler.post(this::updateUi);
    }

    private void onMoveDetectClicked(View view) {
        // update ui
        mMoveDetectOn = !mMoveDetectOn;
        mMoveDetectDirty = true;

        // TTS
        speak(mMoveDetectOn ? "开始移动侦测" : "结束移动侦测");

        // wait for thread finished
        if (mMoveDetectTimer != null) {
            mMoveDetectTimer.cancel();
            mMoveDetectTimer = null;
        }

        // turn on move detect if necessary
        if (mMoveDetectOn) {
            mMoveDetector = new MoveDetector(CameraBase_1_3.this);
            mMoveDetector.setThreshold(5); // 侦测灵敏度阈值，数值越小越灵敏。取值范围(>=0)

            mMoveDetectTimer = new Timer();
            mMoveDetectTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (mMoveDetectOn && mPreviewStream != null) {
                        // snapshot from preview & start detect
                        mPreviewStream.snapshot((OnNV21SnapshotListener) (buff, width, height, stride) -> {
                            // 输入侦测图片不用很大，可以缩减图片尺寸来提升侦测效率。detect()后两位参数可以指定
                            // 图片检测的尺寸，API会自动做缩放。
                            mDetected = mMoveDetector.detect(buff, width, height, width, height);
                            mMoveDetectDirty = true; // ui dirty
                            postUpdateUi();
                        });
                    }
                }
            }, 0, 200);
        }

        // update
        postUpdateUi();
    }

    private void onRecord1Clicked(View view) {
        // update flag & path
        mRecord1On = !mRecord1On;
        mRecord1Dirty = true;

        // TTS
        speak(mRecord1On ? "开始录像1" : "结束录像1");

        // update
        postUpdate();
    }

    private void onRecord2Clicked(View view) {
        // update flag & path
        mRecord2On = !mRecord2On;
        mRecord2Dirty = true;

        // TTS
        speak(mRecord2On ? "开始录像2" : "结束录像2");

        // update
        postUpdate();
    }

    private void onSnapshotClicked(View view) {
        // update ui
        mSnapshotPath = Util.snapshotFilePath();
        mSnapshotDirty = true;
        postUpdateUi();

        // TTS
        speak("抽帧");

        // prepare snapshot dir
        Util.prepareDir(mSnapshotPath);

        // find an output stream to snapshot (max size)
        OutputStream[] outputStreams = {mPreviewStream, mRecord1Stream, mRecord2Stream};
        OutputStream outputStream = null;
        for (OutputStream stream : outputStreams) {
            if ((stream != null)
                    && (outputStream == null
                        || ((outputStream.outputWidth() * outputStream.outputHeight()) < (stream.outputWidth() * stream.outputHeight())))) {
                outputStream = stream;
            }
        }
        if (outputStream == null) {
            Toast.makeText(this, "No stream available for snapshot.", Toast.LENGTH_SHORT).show();
            return;
        }

        // do snapshot
        outputStream.snapshot(mSnapshotPath, (path, width, height, length)->Toast.makeText(CameraBase_1_3.this, "Snapshot saved!!", Toast.LENGTH_SHORT).show());

    }

    private void closeCamera() {
        Log.d(TAG, ">>>> closeCamera() begin");

        mCamera.waitForStatusAny(CameraHelper.STATUS_OPENED | CameraHelper.STATUS_IDLE);

        // close camera first if already opened
        if (mCamera.checkStatusAny(CameraHelper.STATUS_OPENED)) {
            mCamera.close(error->{
                if (error) {
                    Log.e(TAG, "close camera failed.");
                    return;
                }

                if (mImageRender != null) {
                    mImageRender.stop(true);
                    mImageRender = null;
                }

                if (mPreviewStream != null) {
                    mPreviewStream.destroy();
                    mPreviewStream = null;
                }

                if (mRecord1Stream != null) {
                    mRecord1Stream.destroy();
                    mRecord1Stream = null;
                }

                if (mRecord2Stream != null) {
                    mRecord2Stream.destroy();
                    mRecord2Stream = null;
                }

                if (mRenderInputSurface != null) {
                    mRenderInputSurface.release();
                    mRenderInputSurface = null;
                }

                Log.d(TAG, ">> camera closed");
            });

            mCamera.waitForStatusAny(CameraHelper.STATUS_IDLE);
        }

        Log.d(TAG, ">>>> closeCamera()# end");
    }

    private void restartCamera() {
        Log.d(TAG, ">>>> restartCamera()# begin");

        // check has output stream
        if (!mPreviewOn && !mRecord1On && !mRecord2On) {
            Log.d(TAG, ">>>> restartCamera()# end. No output stream found.");
            return;
        }

        // close first
        closeCamera();

        if (mPreviewOn || mRecord1On || mRecord2On) {
            // create ImageRender && config input stream
            mImageRender = new ImageRender("Camera1Render");
            mImageRender.configInputStream(INPUT_WIDTH, INPUT_HEIGHT, mCamera.orientation(mCameraId), mCamera.mirror(mCameraId));

            // config preview output stream
            if (mPreviewOn) {
                mPreviewStream = new PreviewStream("preview", mImageRender, getPreviewSurfaceAwait());
                mPreviewStream.create();
            } else {
                mPreviewStream = null;
            }

            // config record1 stream
            if (mRecord1On) {
                mRecord1Path = Util.videoFilePath("1");
                Util.prepareDir(mRecord1Path);
                mRecord1Dirty = true;
                Log.d(TAG, "start record1: " + mRecord1Path);
                mRecord1Stream = RecordStream.createH265("Record1", mImageRender, RECORD1_WIDTH, RECORD1_HEIGHT, RECORD1_BITRATE, RECORD1_FPS, 0, mRecord1Path);
                mRecord1Stream.create();
            } else {
                mRecord1Stream = null;
            }

            // config record2 stream
            if (mRecord2On) {
                mRecord2Path = Util.videoFilePath("2");
                Util.prepareDir(mRecord2Path);
                mRecord2Dirty = true;
                Log.d(TAG, "start record2: " + mRecord2Path);
                mRecord2Stream = RecordStream.createH265("Record2", mImageRender, RECORD2_WIDTH, RECORD2_HEIGHT, RECORD2_BITRATE, RECORD2_FPS, 0, mRecord2Path);
                mRecord2Stream.create();
            } else {
                mRecord2Stream = null;
            }

            // start ImageRender
            mImageRender.start();

            // get ImageRender input surface
            mRenderInputSurface = mImageRender.getInputSurfaceAwait();

            // open camera
            mCamera.open(mCameraId, mRenderInputSurface, error -> {
                if (error) {
                    Log.e(TAG, "open camera failed.");
                    return;
                }

                Log.d(TAG, ">> camera opened");
            });
        }

        Log.d(TAG, ">>>> restartCamera()# end");
    }

    private void speak(String speech) {
        Tts.speak(speech);
    }

    private abstract static class OutputStream {
        protected String mName;
        protected ImageRender mImageRender;
        protected ImageRender.OutputStream mOutputStream = null;

        OutputStream(String name, ImageRender render) {
            mName = name;
            mImageRender = render;
        }

        ImageRender.OutputStream stream() {
            return mOutputStream;
        }

        abstract void create();
        @SuppressWarnings("unused")
        abstract void destroy();
        @SuppressWarnings("unused")
        abstract void snapshot(OnSnapshotListener listener);
        abstract void snapshot(String path, OnSnapshotSavedListener listener);
        abstract int outputWidth();
        abstract int outputHeight();
    }

    private static class PreviewStream extends OutputStream {
        protected Surface mOutputSurface;

        PreviewStream(String name, ImageRender render, Surface outputSurface) {
            super(name, render);
            mOutputSurface = outputSurface;
        }

        @Override
        void create() {
            mOutputStream = mImageRender.createOutputStream("preview"); // 创建输出流
            mOutputStream.configSurface(mOutputSurface); // 设置视频目标输出Surface
            mOutputStream.configFps(false, 30); // fpsLock: true: 锁定输出帧率，如果Camera输出帧率不足则补帧; false: 不锁定输出帧率; fps: 目标帧率
            mOutputStream.configSkipMs(0); // 丢弃视频流的前几毫秒 (有些设备因为AE收敛慢等问题，视频流头几帧会存在色彩异常的问题，可用这个配置跳过这些异常帧)
            mOutputStream.configSnapshotEnable(true); // 允许对这个视频流进行拍抽帧
            mOutputStream.configSnapshotWatermark(false); // 是否为抽帧图像添加水印
            mOutputStream.configVideoWatermark(false); // 是否为视频流添加水印
            mOutputStream.addWatermark(WATERMARK_LOGO, "T2M", 20, 20, Color.WHITE, 50); // 添加水印
            mOutputStream.addWatermark(WATERMARK_TIME, "00:00:00", 20, 100, Color.WHITE, 50); // 添加水印

        }

        @Override
        void destroy() {
            mOutputStream = null;
        }

        @Override
        void snapshot(OnSnapshotListener listener) {
            if (mOutputStream != null) {
                mOutputStream.snapshot(listener);
            }
        }

        @Override
        void snapshot(String path, OnSnapshotSavedListener listener) {
            if (mOutputStream != null) {
                mOutputStream.snapshot(path, listener);
            }
        }

        @Override
        int outputWidth() {
            return SurfaceUtil.width(mOutputSurface);
        }

        @Override
        int outputHeight() {
            return SurfaceUtil.height(mOutputSurface);
        }
    }

    private static class RecordStream extends OutputStream {
        private int mWidth;
        private int mHeight;
        private String mMimeType;
        private int mBitRate;
        private int mFrameRate;
        private int mOrientation;
        private String mOutputPath;

        private MediaMuxer mMuxer;
        private int mVideoTrackIndex;
        private MediaCodec mCodec;

        private Thread mFeedThread = null;
        private boolean mStopped = false;
        private final Object mThreadLock = new Object();

        private long mFirstTimestamp = -1;

        RecordStream(String name, ImageRender render, int width, int height, String mimeType, int bitRate, int frameRate, int orientation, String outputPath) {
            super(name, render);
            mWidth = width;
            mHeight = height;
            mMimeType = mimeType;
            mBitRate = bitRate;
            mFrameRate = frameRate;
            mOrientation = orientation;
            mOutputPath = outputPath;
        }

        @SuppressWarnings("unused")
        static RecordStream createH264(String name, ImageRender render, int width, int height, int bitRate, int frameRate, int orientation, String outputPath) {
            return new RecordStream(name, render, width, height, MediaFormat.MIMETYPE_VIDEO_AVC, bitRate, frameRate, orientation, outputPath);
        }

        static RecordStream createH265(String name, ImageRender render, int width, int height, int bitRate, int frameRate, int orientation, String outputPath) {
            return new RecordStream(name, render, width, height, MediaFormat.MIMETYPE_VIDEO_HEVC, bitRate, frameRate, orientation, outputPath);
        }

        @Override
        void create() {
            try {
                // config media codec
                MediaFormat format = createFormat(mWidth, mHeight, mMimeType, mBitRate, mFrameRate, mOrientation);
                mCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_HEVC);
                mCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                Surface codecInputSurface = mCodec.createInputSurface();
                mCodec.start();

                // config media muxer
                mFirstTimestamp = -1;
                mMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

                // config render output stream
                mOutputStream = mImageRender.createOutputStream("record1");
                mOutputStream.configSurface(codecInputSurface);
                mOutputStream.configFps(mFrameRate > 0, mFrameRate);
                mOutputStream.configSkipMs(0);
                mOutputStream.configSnapshotEnable(true);
                mOutputStream.configSnapshotWatermark(true);
                mOutputStream.configVideoWatermark(true);
                mOutputStream.addWatermark(WATERMARK_LOGO, "T2M", 20, 20, Color.WHITE, 50); // 添加水印
                mOutputStream.addWatermark(WATERMARK_TIME, "00:00:00", 20, 100, Color.WHITE, 50); // 添加水印

                // start feed thread
                mFeedThread = new Thread(this::onFeedThreadProc, "FeedThread");
                mFeedThread.start();
            } catch (IOException e) {
                Log.e(TAG, "create RecordStream failed.", e);
            }
        }

        @Override
        void destroy() {
            if (mFeedThread != null) {
                try {
                    synchronized (mThreadLock) {
                        mStopped = true;
                    }
                    mFeedThread.join();
                    mFeedThread = null;
                } catch (InterruptedException e) {
                    Log.w(TAG, "wait for feed thread finish interrupted.", e);
                }
            }
        }

        @Override
        void snapshot(OnSnapshotListener listener) {
            if (mOutputStream != null) {
                mOutputStream.snapshot(listener);
            }
        }

        @Override
        void snapshot(String path, OnSnapshotSavedListener listener) {
            if (mOutputStream != null) {
                mOutputStream.snapshot(path, listener);
            }
        }

        @Override
        int outputWidth() {
            return mWidth;
        }

        @Override
        int outputHeight() {
            return mHeight;
        }

        void onFeedThreadProc() {
            Log.d(TAG, "[" + mName + "] onFeedThreadProc()# begin");
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (!mStopped && ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) == 0)) {
                // dequeue a buffer from codec
                int index = MediaCodec.INFO_TRY_AGAIN_LATER;
                while (!mStopped && !Thread.currentThread().isInterrupted() && (index = mCodec.dequeueOutputBuffer(info, 1000000)) < 0) {
                    Log.w(TAG, "[" + mName + "] dequeue output buffer from codec timeout");
                }

                // check error
                if (mStopped || index < 0) {
                    Log.w(TAG, "[" + mName + "] end stream with error: " + index);
                    info.flags |= MediaCodec.BUFFER_FLAG_END_OF_STREAM;
                    continue;
                }

                // feed buffer to muxer
                ByteBuffer buffer = mCodec.getOutputBuffer(index);
                assert buffer != null;
                if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    info.presentationTimeUs = 0;
                    mVideoTrackIndex = mMuxer.addTrack(mCodec.getOutputFormat());
                    mMuxer.start();
                } else {
                    if (mFirstTimestamp < 0) {
                        mFirstTimestamp = info.presentationTimeUs;
                    }
                    info.presentationTimeUs -= mFirstTimestamp;
                }
                buffer.position(info.offset);
                mMuxer.writeSampleData(mVideoTrackIndex, buffer, info);

                // release buffer
                mCodec.releaseOutputBuffer(index, false);
            }

            // close
            mMuxer.stop();
            mMuxer.release();
            mCodec.stop();
            Log.d(TAG, "[" + mName + "] onFeedThreadProc()# end");
        }

        private static MediaFormat createFormat(int width, int height, String mimeType, int bit, int frameRate, int orientation) {
            Size size = rotatedSize(width, height, orientation);
            MediaFormat format = MediaFormat.createVideoFormat(mimeType, size.getWidth(), size.getHeight());
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);	// API >= 18
            format.setInteger(MediaFormat.KEY_BIT_RATE, bit);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);// 1 seconds between I-frames

            return format;
        }

        private static Size rotatedSize(int width, int height, int orientation) {
            if (width > height) {
                if (orientation == 0 || orientation == 180) {
                    return new Size(width, height);
                } else {
                    return new Size(height, width);
                }
            } else {
                if (orientation == 0 || orientation == 180) {
                    return new Size(height, width);
                } else {
                    return new Size(width, height);
                }
            }
        }
    }
}

package com.example.exoplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;


public class BaseVideoView extends FrameLayout implements TextureView.SurfaceTextureListener, IMediaPlayer.OnPreparedListener, IMediaPlayer.OnStartListener, IMediaPlayer.OnErrorListener {
    private static final String TAG = "MediaPlayerView";
    protected static final int STATE_ERROR = -1;
    protected static final int STATE_IDLE = 0;
    protected static final int STATE_PREPARING = 1;
    protected static final int STATE_PREPARED = 2;
    protected static final int STATE_TEXTURE_AVAILABLE = 3;
    protected static final int STATE_PLAYING = 4;
    protected static final int STATE_PAUSED = 5;

    private String mUrl;
    protected int mCurrentState = STATE_IDLE;
    private int mTargetState = STATE_IDLE;
    private int mSeekPositionForReUrl = -1;
    protected int mVideoWidth;
    protected int mVideoHeight;
    protected int mVideoRotationDegree;
    protected TextureView mTextureView;
    private Surface mSurface;
    private OnPlayStatusListener onPlayStatusListener;
    private IMediaPlayer videoPlayer;

    private static final int MSG_PROCESS_UPDATE = 1;
    private static final int MSG_AUTO_START_PREPARING = 2;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_PROCESS_UPDATE) {
                if (videoPlayer != null && mCurrentState == STATE_PLAYING && onPlayStatusListener != null) {
                    int pos = videoPlayer.getCurrentPosition();
                    onPlayStatusListener.onProgressUpdate(pos);
                    mHandler.removeMessages(MSG_AUTO_START_PREPARING);
                    mHandler.sendEmptyMessageDelayed(MSG_PROCESS_UPDATE, 50);
                }
            }
        }
    };

    public BaseVideoView(Context context) {
        super(context);
    }

    public BaseVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setPath(String url, boolean autoStart) {
        Log.e(TAG, "setPath.url=" + url);
        if (TextUtils.isEmpty(url)) {
            return;
        }
        if (videoPlayer == null) {
            videoPlayer = new MediaPlayerExo(getContext());
            videoPlayer.init();
            setCurrentState(STATE_IDLE);
            mTargetState = STATE_IDLE;
        }
        if (mCurrentState == STATE_PLAYING) {
            return;
        }

        if (mCurrentState == STATE_PAUSED) {
            mTargetState = STATE_PLAYING;
            start();
            return;
        }

        if (mCurrentState == STATE_PREPARING || mCurrentState == STATE_PREPARED) {
            if (autoStart) {
                mTargetState = STATE_PLAYING;
            }
            return;
        }

        if (mCurrentState == STATE_TEXTURE_AVAILABLE) {
            if (autoStart) {
                mTargetState = STATE_PLAYING;
                start();
            }
            return;
        }

        this.mUrl = url;
        videoPlayer.setOnErrorListener(this);
        videoPlayer.setOnPreparedListener(this);
        videoPlayer.setOnStartListener(this);

        if (this.mUrl == null) {
            return;
        }

        mTargetState = autoStart ? STATE_PLAYING : STATE_TEXTURE_AVAILABLE;
        setCurrentState(STATE_PREPARING);
        videoPlayer.prepare(this.mUrl);
    }

    public void start() {
        if (mUrl == null) {
            return;
        }
        if (mCurrentState == STATE_IDLE || mCurrentState == STATE_ERROR) {
            mTargetState = STATE_PLAYING;
            releasePlayer();
            setPath(mUrl, true);
            return;
        }

        if (this.videoPlayer == null) {
            return;
        }

        if (mCurrentState == STATE_PREPARING || mCurrentState == STATE_PREPARED || mCurrentState == STATE_PLAYING) {
            mTargetState = STATE_PLAYING;
            return;
        }

        if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_TEXTURE_AVAILABLE) {
            setCurrentState(STATE_PLAYING);
            mTargetState = STATE_PLAYING;
            this.videoPlayer.start();
            if (this.onPlayStatusListener != null) {
                this.onPlayStatusListener.onPlayStart();
            }
            startProcessUpdate();
        }
    }

    public void pause() {
        if (videoPlayer != null) {
            if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_IDLE) {
                return;
            }
            videoPlayer.stop();
            setCurrentState(STATE_PAUSED);
            mTargetState = STATE_PAUSED;
            stopProcessUpdate();
            if (this.onPlayStatusListener != null) {
                this.onPlayStatusListener.onPlayPause();
            }
        }

    }

    public int getCurrentPosition() {
        if (videoPlayer != null) {
            return videoPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (videoPlayer == null) {
            return 0;
        }
        return videoPlayer.getDuration();
    }

    public void seek(int position) {
        mSeekPositionForReUrl = -1;
        if (videoPlayer != null) {
            if (mCurrentState == STATE_PAUSED || mCurrentState == STATE_TEXTURE_AVAILABLE || mCurrentState == STATE_PLAYING || mCurrentState == STATE_PREPARED) {
                videoPlayer.seek(position);
            }
        }
    }


    public void setOnPlayStatusListener(OnPlayStatusListener onPlayStatusListener) {
        this.onPlayStatusListener = onPlayStatusListener;
    }

    public void releasePlayer() {
        setCurrentState(STATE_IDLE);
        mTargetState = STATE_IDLE;
        if (videoPlayer != null) {
            videoPlayer.release();
            videoPlayer = null;
        }
        Log.i(TAG, "releasePlayer");
        mHandler.removeMessages(MSG_AUTO_START_PREPARING);
        mHandler.removeMessages(MSG_PROCESS_UPDATE);
        int childCount = getChildCount();
        if (childCount > 0) {
            removeAllViews();
        }
        if (mTextureView != null) {
            mTextureView.setSurfaceTextureListener(null);
        }
        mTextureView = null;
        mSurface = null;
        mSeekPositionForReUrl = -1;
        onPlayStatusListener = null;
    }

    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releasePlayer();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mSurface = new Surface(surface);
        if (videoPlayer != null) {
            videoPlayer.setSurface(mSurface);
            setCurrentState(STATE_TEXTURE_AVAILABLE);
            if (mTargetState == STATE_PLAYING) {
                start();
            }
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        mSurface = null;
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    @Override
    public void onPrepared(IMediaPlayer mp) {
        mVideoWidth = mp.getWidth();
        mVideoHeight = mp.getHeight();
        mVideoRotationDegree = mp.getRotationDegrees();
        if (videoPlayer != null) {
            if (onPlayStatusListener != null) {
                onPlayStatusListener.onPrepared(mp);
            }
            layoutVideoView(mp.getRotationDegrees());
            setCurrentState(STATE_PREPARED);
            if (mSeekPositionForReUrl > 0) {
                seek(mSeekPositionForReUrl);
                mSeekPositionForReUrl = -1;
            }
        }
    }

    @Override
    public void OnStart(IMediaPlayer mp) {

    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, Exception error) {
        Log.i("MediaPlayer", "url:" + mUrl, error);
        if (onPlayStatusListener != null) {
            onPlayStatusListener.onPlayError(error);
        }
        return false;
    }

    private void setCurrentState(int state) {
        mCurrentState = state;
        setKeepScreenOn(state == STATE_PLAYING);
    }

    private void startProcessUpdate() {
        Log.i(TAG, "startProcessUpdate");
        mHandler.removeMessages(MSG_PROCESS_UPDATE);
        mHandler.sendEmptyMessageDelayed(MSG_PROCESS_UPDATE, 50);
    }

    private void stopProcessUpdate() {
        Log.i(TAG, "stopProcessUpdate");
        mHandler.removeMessages(MSG_PROCESS_UPDATE);
    }

    protected void layoutVideoView(int videoRotation) {
        Point info = calcTextureLayout(videoRotation);
        if (info == null) {
            return;
        }
        int paramW = info.x;
        int paramH = info.y;

        TextureView textureView = findTextureView();
        if (textureView != null) {
            if (mTextureView == null) {
                mTextureView = textureView;
                mTextureView.setSurfaceTextureListener(this);
            }
            LayoutParams params = (LayoutParams) textureView.getLayoutParams();
            if (paramW != params.width || paramH != params.height) {
                params.width = paramW;
                params.height = paramH;
                params.gravity = Gravity.CENTER;
                textureView.setLayoutParams(params);
            }
        } else {
            mTextureView = new TextureView(getContext());
            mTextureView.setSurfaceTextureListener(this);
            LayoutParams params = new LayoutParams(paramW, paramH);
            params.gravity = Gravity.CENTER;
            addView(mTextureView, 0, params);
        }
    }

    protected Point calcTextureLayout(int videoRotation) {
        if (mVideoWidth <= 0 || mVideoHeight <= 0) {
            return null;
        }
        int parentWidth = getWidth();
        int parentHeight = getHeight();
        if (parentWidth <= 0 || parentHeight <= 0) {
            return null;
        }

        int rotation = videoRotation % 360;
        float vW = (float) mVideoWidth;
        float vH = (float) mVideoHeight;
        if (rotation % 180 == 90) {
            vW = mVideoHeight;
            vH = mVideoWidth;
        }

        int paramW;
        int paramH;
        float vr = vW / vH;   //video ratio
        float sr = parentWidth / (float) parentHeight;  //screen ratio
        if (vr >= sr) {
            paramW = parentWidth;
            paramH = (int) (parentWidth / vr + 0.5f);
        } else {
            paramW = (int) (parentHeight * vr + 0.5f);
            paramH = parentHeight;
        }
        return new Point(paramW, paramH);
    }

    protected TextureView findTextureView() {
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View view = getChildAt(i);
            if (view instanceof TextureView) {
                return ((TextureView) view);
            }
        }
        return null;
    }
}

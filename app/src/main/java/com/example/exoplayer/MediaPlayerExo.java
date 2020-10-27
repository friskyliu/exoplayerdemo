package com.example.exoplayer;

import android.content.Context;
import android.net.Uri;
import android.view.Surface;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SeekParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.analytics.AnalyticsListener;
import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.FileDataSource;
import com.google.android.exoplayer2.upstream.cache.CacheDataSink;
import com.google.android.exoplayer2.upstream.cache.CacheDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Log;
import com.google.android.exoplayer2.util.Util;

import java.io.File;

public class MediaPlayerExo implements IMediaPlayer {
    private static int count = 0;
    private String TAG = "MediaPlayer, Activity id:" + (count++);
    public static final int MIN_BUFFER_MS = 5000; //最低数据加载时机,默认值为15秒
    public static final int MAX_BUFFER_MS = 10000; //最高数据加载时机，默认值为30秒
    public static final int BUFFER_FOR_PLAYBACK_MS = 1500; //最低数据播放时机， 默认值为2.5秒
    public static final int BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 3500; //最低数据重新播放时机，默认值为5秒

    private SimpleExoPlayer exoPlayer;
    private Context context;

    private OnPreparedListener mOnPreparedListener;
    private OnStartListener mOnStartListener;
    private OnErrorListener mOnErrorListener;
    private int mWidth;
    private int mHeight;
    private int mRotationDegrees;
    private Surface mSurface;
    private boolean isPrepared = false;

    public MediaPlayerExo(Context context) {
        this.context = context;
    }

    private AnalyticsListener analyticsListener = new AnalyticsListener() {
        @Override
        public void onPlayerError(EventTime eventTime, ExoPlaybackException error) {
            if (mOnErrorListener != null) {
                Log.e(TAG, "msg", error);
                mOnErrorListener.onError(MediaPlayerExo.this, error.type, error);
                Log.e(TAG, "error, video size:" + mWidth + "x" + mHeight);
            }
        }

        @Override
        public void onSeekStarted(EventTime eventTime) {
            if (mOnStartListener != null) {
                mOnStartListener.OnStart(MediaPlayerExo.this);
            }
        }

        @Override
        public void onPlayerStateChanged(EventTime eventTime, boolean playWhenReady, int playbackState) {
            Log.i(TAG, "onPlayerStateChanged, playWhenReady=" + playWhenReady + ",playbackState=" + playbackState);
            switch (playbackState) {
                case Player.STATE_READY:
                    if (exoPlayer != null) {
                        Format format = exoPlayer.getVideoFormat();
                        if (format == null) {
                            if (mOnErrorListener != null) {
                                mOnErrorListener.onError(MediaPlayerExo.this, 999, new IllegalArgumentException("video format is null"));
                            }
                            return;
                        }
                        mWidth = format.width;
                        mHeight = format.height;
                        mRotationDegrees = format.rotationDegrees;
                        Log.e(TAG, "STATE_READY.width=" + format.width + "  height=" + format.height + "  duration=" + exoPlayer.getDuration() + "   frameRate=" + format.frameRate);
                        if (mOnPreparedListener != null && !isPrepared) {
                            mOnPreparedListener.onPrepared(MediaPlayerExo.this);
                            isPrepared = true;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void init() {
        if (exoPlayer != null) {
            return;
        }
        // 创建轨道选择工厂
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory();
        // 创建轨道选择器实例
        TrackSelector trackSelector = new DefaultTrackSelector(context, videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(MIN_BUFFER_MS, MAX_BUFFER_MS, BUFFER_FOR_PLAYBACK_MS, BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS)
                .createDefaultLoadControl();
        this.exoPlayer = new SimpleExoPlayer.Builder(context)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .setClock(Clock.DEFAULT)
                .build();
        this.exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
        this.exoPlayer.setSeekParameters(SeekParameters.EXACT);
        this.exoPlayer.addAnalyticsListener(analyticsListener);
        isPrepared = false;
        Log.e(TAG, "init");
    }

    @Override
    public void prepare(String url) {
        if (this.exoPlayer != null) {
            isPrepared = false;
            this.exoPlayer.prepare(buildMediaSource(Uri.parse(url), null));
            this.exoPlayer.seekTo(0);
            Log.e(TAG, "prepare:" + url);
        }
    }

    @Override
    public void start() {
        if (this.exoPlayer != null) {
            this.exoPlayer.setPlayWhenReady(true);
            Log.e(TAG, "setPlayWhenReady true");
        }
    }

    @Override
    public void stop() {
        if (this.exoPlayer != null) {
            this.exoPlayer.setPlayWhenReady(false);
            Log.e(TAG, "setPlayWhenReady false");
        }
    }

    @Override
    public void seek(int position) {
        if (this.exoPlayer != null) {
            this.exoPlayer.seekTo(position);
        }
    }

    @Override
    public void setSpeed(float speed) {
        //nothing
    }

    @Override
    public void setLoop(boolean looping) {
        if (this.exoPlayer != null) {
            this.exoPlayer.setRepeatMode(looping ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        }
    }

    @Override
    public void release() {
        reset();
    }

    @Override
    public void reset() {
        if (this.exoPlayer != null) {
            this.exoPlayer.release();
            this.exoPlayer = null;
        }
        context = null;
        isPrepared = false;
        mSurface = null;
        mWidth = 0;
        mHeight = 0;
    }

    @Override
    public boolean isPlaying() {
        if (this.exoPlayer == null) {
            return false;
        }
        int state = this.exoPlayer.getPlaybackState();
        switch (state) {
            case Player.STATE_BUFFERING:
            case Player.STATE_READY:
                return this.exoPlayer.getPlayWhenReady();
            case Player.STATE_IDLE:
            case Player.STATE_ENDED:
            default:
                return false;
        }
    }

    @Override
    public int getCurrentPosition() {
        if (this.exoPlayer == null) {
            return 0;
        }
        return (int) this.exoPlayer.getCurrentPosition();
    }

    @Override
    public int getDuration() {
        if (this.exoPlayer == null) {
            return 0;
        }
        return (int) this.exoPlayer.getDuration();
    }

    @Override
    public int getWidth() {
        if (this.exoPlayer == null) {
            return 0;
        }
        return mWidth;
    }

    @Override
    public int getHeight() {
        if (this.exoPlayer == null) {
            return 0;
        }
        return mHeight;
    }

    @Override
    public int getRotationDegrees() {
        if (this.exoPlayer == null) {
            return 0;
        }
        return mRotationDegrees;
    }

    @Override
    public void setSurface(Surface surface) {
        mSurface = surface;
        if (this.exoPlayer != null) {
            if (surface != null && !surface.isValid()) {
                mSurface = null;
            }
            this.exoPlayer.setVideoSurface(surface);
        }
    }

    @Override
    public void setOnPreparedListener(OnPreparedListener listener) {
        this.mOnPreparedListener = listener;
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        this.mOnErrorListener = listener;
    }

    @Override
    public void setOnStartListener(OnStartListener listener) {
        this.mOnStartListener = listener;
    }

    private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
        MediaSource videoSource;
        String userAgent = Util.getUserAgent(this.context, "demo");
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this.context, null, new DefaultHttpDataSourceFactory(userAgent));
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir != null) {
            cacheDir = new File(cacheDir, "exo-video-cache");
            cacheDir.mkdirs();
            CacheDataSourceFactory cacheDataSourceFactory = new CacheDataSourceFactory(context, dataSourceFactory, cacheDir);
            videoSource = new ExtractorMediaSource.Factory(cacheDataSourceFactory).createMediaSource(uri);
        } else {
            videoSource = new ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        }
        //循环播放
        Log.e(TAG, "buildMediaSource");
        return new LoopingMediaSource(videoSource);
    }

    private static class CacheDataSourceFactory implements DataSource.Factory {
        private final Context context;
        private final DataSource.Factory factory;
        private final long maxFileSize, maxCacheSize;
        private static SimpleCache sSimpleCache = null;
        private static final byte[] lock = new byte[0];
        private final File cacheDir;

        CacheDataSourceFactory(Context context, DataSource.Factory factory, File cacheDir) {
            this(context, factory, cacheDir, 80 * 1024 * 1024, 5 * 1024 * 1024);
        }

        CacheDataSourceFactory(Context context, DataSource.Factory factory, File cacheDir, long maxCacheSize, long maxFileSize) {
            super();
            this.context = context;
            this.maxCacheSize = maxCacheSize;
            this.maxFileSize = maxFileSize;
            this.factory = factory;
            this.cacheDir = cacheDir;
        }

        @Override
        public DataSource createDataSource() {
            if (sSimpleCache == null) {
                synchronized (lock) {
                    if (sSimpleCache == null) {
                        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(maxCacheSize);
                        sSimpleCache = new SimpleCache(cacheDir, evictor, new ExoDatabaseProvider(context.getApplicationContext()));
                    }
                }
            }
            return new CacheDataSource(sSimpleCache, factory.createDataSource(),
                    new FileDataSource(), new CacheDataSink(sSimpleCache, maxFileSize),
                    CacheDataSource.FLAG_BLOCK_ON_CACHE | CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR, null);
        }
    }
}

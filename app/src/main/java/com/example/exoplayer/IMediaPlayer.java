package com.example.exoplayer;

import android.view.Surface;

public interface IMediaPlayer {
  void init();

  void prepare(String url);

  void start();

  void stop();

  void seek(int position);

  void setSpeed(float speed);

  void setLoop(boolean isLoop);

  void release();

  void reset();

  boolean isPlaying();

  int getCurrentPosition();

  int getDuration();

  int getWidth();

  int getHeight();

  int getRotationDegrees();

  void setSurface(Surface surface);

  void setOnPreparedListener(OnPreparedListener listener);

  void setOnErrorListener(OnErrorListener listener);

  void setOnStartListener(OnStartListener listener);


  interface OnPreparedListener {
    void onPrepared(IMediaPlayer mp);
  }

  interface OnStartListener {
    void OnStart(IMediaPlayer mp);
  }

  interface OnErrorListener {
    boolean onError(IMediaPlayer mp, int what, Exception error);
  }
}

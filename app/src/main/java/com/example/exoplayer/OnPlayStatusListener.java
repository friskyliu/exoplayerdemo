package com.example.exoplayer;


/**
 * @Date 2020/2/24
 * @Author qiuyu
 * @Description
 */
public interface OnPlayStatusListener {
  void onPrepared(IMediaPlayer mediaPlayer);

  void onPlayStart();

  void onPlayPause();

  void onProgressUpdate(int progress);

  void onPlayError(Exception error);
}

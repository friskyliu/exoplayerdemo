package com.example.exoplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements OnPlayStatusListener {

    private BaseVideoView videoView;
    private TextView time;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        String path = "http://vfx.mtime.cn/Video/2019/03/21/mp4/190321153853126488.mp4";
        videoView = findViewById(R.id.videoView);
        time = findViewById(R.id.time);
        progressBar = findViewById(R.id.processBar);

        videoView.setPath(path, false);
        videoView.setOnPlayStatusListener(this);

        findViewById(R.id.newActivity).setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), MainActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.pause();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoView.releasePlayer();
    }

    @Override
    public void onPrepared(IMediaPlayer mediaPlayer) {
        progressBar.setMax((int) (mediaPlayer.getDuration() / 10));
    }

    @Override
    public void onPlayStart() {

    }

    @Override
    public void onPlayPause() {

    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onProgressUpdate(int progress) {
        progressBar.setProgress(progress / 10);
        int second = progress / 1000;
        int remind = (progress - second * 1000) / 100;
        time.setText(second + "." + remind);
    }

    @Override
    public void onPlayError(Exception error) {

    }
}
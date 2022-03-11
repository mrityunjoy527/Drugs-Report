package com.example.stopdrugs;

import android.Manifest;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AudioRec extends AppCompatActivity {

    public static final int CAP_AUDIO = 102;
    ImageView start, play;
    TextView tvTime;
    boolean isRecording = false;
    boolean isPlaying = false;
    MediaRecorder mediaRecorder;
    MediaPlayer mediaPlayer;
    String path = null;
    LottieAnimationView lavPlaying;
    int second;
    int dummySeconds = 0;
    int playableSeconds = 0;
    ExecutorService executorService = Executors.newSingleThreadExecutor();
    Handler handler;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_audio);
        start = findViewById(R.id.startRec);
        play = findViewById(R.id.play);
        tvTime = findViewById(R.id.tv_time);
        lavPlaying = findViewById(R.id.lav_playing);
        mediaPlayer = new MediaPlayer();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPermissionForAudio();
                if(!isRecording) {
                    isRecording = true;
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            mediaRecorder = new MediaRecorder();
                            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                            mediaRecorder.setOutputFile(getRecordAudioPath());
                            path = getRecordAudioPath();
                            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
                            try {
                                mediaRecorder.prepare();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            mediaRecorder.start();
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Recoding Started", Toast.LENGTH_SHORT).show();
                                    playableSeconds = 0;
                                    second = 0;
                                    dummySeconds = 0;
                                    start.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.recording_active));
                                    lavPlaying.setVisibility(View.VISIBLE);
                                    runTimer();
                                }
                            });
                        }
                    });
                } else {
                    mediaRecorder.stop();
                    mediaRecorder.release();
                    mediaRecorder = null;
                    playableSeconds = second;
                    dummySeconds = second;
                    second = 0;
                    isRecording = false;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            handler.removeCallbacksAndMessages(null);
                            start.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.recording_in_active));
                            lavPlaying.setVisibility(View.GONE);
                            runTimer();
                        }
                    });
                }
            }
        });
        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isPlaying) {
                    if(path != null) {
                        try {
                            mediaPlayer.setDataSource(path);
                            mediaPlayer.prepare();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }else {
                        Toast.makeText(getApplicationContext(), "No Recording Present", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    mediaPlayer.start();
                    isPlaying = true;
                    play.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.recording_pause));
                    lavPlaying.setVisibility(View.VISIBLE);
                    runTimer();
                }else {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                    mediaPlayer = new MediaPlayer();
                    isPlaying = false;
                    second = 0;
                    handler.removeCallbacksAndMessages(null);
                    lavPlaying.setVisibility(View.GONE);
                    play.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.recording_play));
                }
            }
        });
    }

    private void runTimer() {
        handler = new Handler();
        handler.post(new Runnable() {
            @Override
            public void run() {
                int minutes = (second % 3600)/60;
                int secs = second % 60;
                String time = String.format(Locale.getDefault(), "%02d:%02d", minutes, secs);
                tvTime.setText(time);
                if(isRecording || (isPlaying && playableSeconds != -1)) {
                    second++;
                    playableSeconds--;
                    if(playableSeconds == -1 && isPlaying) {
                        isPlaying = false;
                        mediaPlayer.stop();
                        mediaPlayer.release();
                        mediaPlayer = null;
                        mediaPlayer = new MediaPlayer();
                        playableSeconds = dummySeconds;
                        second = 0;
                        handler.removeCallbacksAndMessages(null);
                        play.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.recording_play));
                        return;
                    }
                }
                handler.postDelayed(this,1000);
            }
        });
    }

    private void getPermissionForAudio() {
        if(ContextCompat.checkSelfPermission(AudioRec.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AudioRec.this, new String[]{Manifest.permission.RECORD_AUDIO}, CAP_AUDIO);
        }else Toast.makeText(getApplicationContext(), "Audio Permission Granted", Toast.LENGTH_SHORT).show();
    }

    private String getRecordAudioPath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        File file = new File(musicDirectory, "testRecordingFile"+".mp3");
        return file.getPath();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Permission Granted", Toast.LENGTH_SHORT).show();
        }else {
            getPermissionForAudio();
            Toast.makeText(getApplicationContext(), "Need Recording Permission", Toast.LENGTH_SHORT).show();
        }
    }
}

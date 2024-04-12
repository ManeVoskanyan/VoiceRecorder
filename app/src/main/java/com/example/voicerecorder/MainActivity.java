package com.example.voicerecorder;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int MICROPHONE_PERMISSION_CODE = 200;
    private MediaRecorder mediaRecorder;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Button go_button = findViewById(R.id.go_to_zapis);
        go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRecordingsActivity(v);
            }
        });

        if (isMicrophonePresent()) {
            getMicrophonePermission();
        }
    }

    public void btnRecordPressed(View view) {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            mediaRecorder.setOutputFile(getRecordingFilePath());
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mediaRecorder.prepare();
            mediaRecorder.start();

            Toast.makeText(this, "Recording is started", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void btnStopPressed(View view) {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.release();
            mediaRecorder = null;

            Toast.makeText(this, "Recording is stopped", Toast.LENGTH_LONG).show();

            // Save recording to Firestore
            saveRecordingToFirestore("testRecordingFiles", getRecordingFilePath());
        } else {
            Toast.makeText(this, "No recording to stop", Toast.LENGTH_LONG).show();
        }
    }

    public void btnPlayPressed(View view) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(getRecordingFilePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

            Toast.makeText(this, "Recording is playing", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isMicrophonePresent() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_MICROPHONE);
    }

    private void getMicrophonePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, MICROPHONE_PERMISSION_CODE);
        }
    }

    private String getRecordingFilePath() {
        ContextWrapper contextWrapper = new ContextWrapper(getApplicationContext());
        File musicDirectory = contextWrapper.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
        if (musicDirectory == null) {
            musicDirectory = new File(contextWrapper.getExternalFilesDir(null), "Music");
            musicDirectory.mkdirs(); // Создаем директорию, если ее нет
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file = new File(musicDirectory, "recording_" + timeStamp + ".mp3");

        // Проверяем существование файла
        if (!file.exists()) {
            try {
                file.createNewFile(); // Создаем новый файл, если он не существует
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return file.getPath();
    }

    private void saveRecordingToFirestore(String fileName, String filePath) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a new recording
        Map<String, Object> recording = new HashMap<>();
        recording.put("name", fileName);
        recording.put("filePath", filePath);

        // Add a new document with a generated ID
        db.collection("recordings")
                .add(recording)
                .addOnSuccessListener(documentReference -> Toast.makeText(MainActivity.this, "Recording saved to Firestore", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Error saving recording to Firestore", Toast.LENGTH_SHORT).show());
    }
    public void openRecordingsActivity(View view) {
        Intent intent = new Intent(this, RecordingsActivity.class);
        startActivityForResult(intent, 1);
    }
}

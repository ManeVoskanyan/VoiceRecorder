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
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

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
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button go_button = findViewById(R.id.go_to_zapis);
        go_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRecordingsActivity(v);
            }
        });

        if (isMicrophonePresent()) {
            getMicrophonePermission();
        }

        databaseReference = FirebaseDatabase.getInstance().getReference("recordings");
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
            saveRecordingToDatabase("testRecordingFiles", getRecordingFilePath());
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
        File storageDirectory = new File(contextWrapper.getExternalFilesDir(null), "Storage/Recordings");
        storageDirectory.mkdirs(); // Создаем директорию, если ее нет

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file = new File(storageDirectory, "recording_" + timeStamp + ".mp3");

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

    private void saveRecordingToDatabase(String fileName, String filePath) {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("Recordings/" + fileName);

        storageRef.putFile(Uri.fromFile(new File(filePath)))
                .addOnSuccessListener(taskSnapshot -> {
                    // Запись успешно загружена
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d("RecordingsActivity", "File uploaded successfully. Download URL: " + downloadUrl);
                        // Теперь вы можете сохранить downloadUrl в базу данных Firebase Realtime Database или Firestore
                        saveRecordingToRealtimeDatabase(fileName, downloadUrl);
                    });
                })
                .addOnFailureListener(exception -> {
                    // Обработка ошибки при загрузке записи
                    Log.e("RecordingsActivity", "Failed to upload file", exception);
                });
    }

    private void saveRecordingToRealtimeDatabase(String fileName, String downloadUrl) {
        DatabaseReference recordingsRef = FirebaseDatabase.getInstance().getReference().child("recordings");
        String recordingId = recordingsRef.push().getKey();

        Map<String, Object> recordingData = new HashMap<>();
        recordingData.put("name", fileName);
        recordingData.put("downloadUrl", downloadUrl);

        if (recordingId != null) {
            recordingsRef.child(recordingId).setValue(recordingData)
                    .addOnSuccessListener(aVoid -> {
                        // Запись успешно сохранена в Realtime Database
                        Log.d("RecordingsActivity", "Recording saved to Realtime Database");
                    })
                    .addOnFailureListener(e -> {
                        // Обработка ошибки при сохранении записи в Realtime Database
                        Log.e("RecordingsActivity", "Failed to save recording to Realtime Database", e);
                    });
        }
    }




    public void openRecordingsActivity(View view) {
        Intent intent = new Intent(this, RecordingsActivity.class);
        startActivity(intent);
    }
}

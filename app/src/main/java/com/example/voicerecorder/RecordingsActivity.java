package com.example.voicerecorder;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RecordingsActivity extends AppCompatActivity {

    private ListView recordingsListView;
    private List<String> recordingNames;
    private List<String> recordingPaths;
    private ArrayAdapter<String> adapter;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recordings);

        recordingsListView = findViewById(R.id.recordingsListView);
        recordingNames = new ArrayList<>();
        recordingPaths = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, recordingNames);
        recordingsListView.setAdapter(adapter);

        // Retrieve recordings from Firestore
        retrieveRecordingsFromFirestore();

        // Set click listener to play recording when ListView item is clicked
        recordingsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String filePath = recordingPaths.get(position); // Получаем путь к файлу записи, соответствующей позиции в списке
                playRecording(filePath); // Воспроизводим запись, используя полученный путь к файлу
            }
        });
    }


    private void retrieveRecordingsFromFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recordings")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            String name = document.getString("name");
                            String path = document.getString("filePath");
                            recordingNames.add(name);
                            recordingPaths.add(path);
                        }
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RecordingsActivity.this, "Failed to retrieve recordings", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void playRecording(String filePath) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(filePath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to play recording", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}

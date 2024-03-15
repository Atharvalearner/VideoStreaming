package com.impetus.videosteaming;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_VIDEO = 1;
    ProgressBar progressBar;
    EditText editText;
    VideoView videoView;
    private Uri videoUri;
    MediaController mediaController;
    AppCompatButton button;
    StorageReference storageReference;
    DatabaseReference databaseReference;
    Model member;
    UploadTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.p_bar);
        editText = findViewById(R.id.v_name);
        videoView = findViewById(R.id.video_view);
        button = findViewById(R.id.v_upload);

        mediaController = new MediaController(this);
        videoView.setMediaController(mediaController);
        videoView.start();

        member = new Model();
        storageReference = FirebaseStorage.getInstance().getReference("Video");
        databaseReference = FirebaseDatabase.getInstance().getReference("Video");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadVideo();
            }
        });
    }

    private void uploadVideo() {
        String videoName = editText.getText().toString();
        String searchVideo = editText.getText().toString().toLowerCase();
        if(videoUri != null || !TextUtils.isEmpty(videoName)){
            progressBar.setVisibility(View.VISIBLE);
            final StorageReference reference = storageReference.child(System.currentTimeMillis() + "." + getExtensionOfVideo(videoUri));
            uploadTask = reference.putFile(videoUri);

            Task<Uri> UriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return reference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (!task.isSuccessful()){
                        Uri downloadUrl = task.getResult();
                        progressBar.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, "video saved successfully", Toast.LENGTH_SHORT).show();
                        member.setName(videoName);
                        member.setVideoUri(downloadUrl.toString());
                        member.setSearch(searchVideo);

                        String i = databaseReference.push().getKey();
                        databaseReference.child(i).setValue(member);
                    }else {
                        Toast.makeText(MainActivity.this, "Failed to save", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else {
            Toast.makeText(MainActivity.this, "All Field are required to upload", Toast.LENGTH_SHORT).show();
        }
    }

    public void chooseVideo(View view) {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent,PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_VIDEO || resultCode == RESULT_OK || data != null || data.getData() != null ){
            videoUri = data.getData();
            videoView.setVideoURI(videoUri);
        }
    }

    private  String getExtensionOfVideo(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    public void showVideo(View view) {
    }
}
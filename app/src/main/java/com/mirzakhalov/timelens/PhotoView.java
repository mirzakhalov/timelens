package com.mirzakhalov.timelens;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.UUID;

public class PhotoView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);
    }


    protected void uploadImage(Uri file) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageReference = storage.getReference();
        StorageReference ref = storageReference.child(UUID.randomUUID().toString());
        ref.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                        progressDialog.dismiss();
//                        Toast.makeText(MainActivity.this, "Uploaded", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
//                        progressDialog.dismiss();
//                        Toast.makeText(MainActivity.this, "Failed "+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
//                        double progress = (100.0*taskSnapshot.getBytesTransferred()/taskSnapshot
//                                .getTotalByteCount());
//                        progressDialog.setMessage("Uploaded "+(int)progress+"%");
                    }
                });
    }

    protected void updateDB(double lat, double lng, String author, String caption) {

    }

}

package com.mirzakhalov.timelens;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;



/*
 *  Huge thanks and recognition to Sue Smith for her blog at https://code.tutsplus.com/tutorials/capture-and-crop-an-image-with-the-device-camera--mobile-11458.
 */

public class PhotoView extends Activity implements View.OnClickListener {

    ImageView image;

    Button takePicture;
    Button proceedButton;
    EditText caption;
    ProgressBar progress;
    //keep track of camera capture intent
    final int CAMERA_CAPTURE = 1;

    Bitmap thePic = null;
    Uri picUri = null;

    private static final int WRITE_PERMISSION = 786;


    private FusedLocationProviderClient fusedLocationClient;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);

        // layout elements
        image = (ImageView) findViewById(R.id.image);
        takePicture = (Button) findViewById(R.id.retake);
        proceedButton = (Button) findViewById(R.id.upload);
        caption = (EditText) findViewById(R.id.caption);

        takePicture.setOnClickListener(this);
        proceedButton.setOnClickListener(this);

        try {
            //use standard intent to capture an image
            Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            //we will handle the returned data in onActivityResult
            startActivityForResult(captureIntent, CAMERA_CAPTURE);
        } catch (ActivityNotFoundException anfe) {
            //display an error message
            Toast toast = Toast.makeText(PhotoView.this, "No support", Toast.LENGTH_SHORT);
            toast.show();
        }

        // request permission in the runtime
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_PERMISSION);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();



    }
    public void onClick(View v) {
        if( v.getId() == R.id.retake){
            try {
                //use standard intent to capture an image
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //we will handle the returned data in onActivityResult
                startActivityForResult(captureIntent, CAMERA_CAPTURE);
            } catch (ActivityNotFoundException anfe) {
                //display an error message
                Toast toast = Toast.makeText(PhotoView.this, "No support", Toast.LENGTH_SHORT);
                toast.show();
            }
        }
        else if(v.getId() == R.id.upload) {

            if(thePic != null){
                uploadAvatar(thePic);

            }
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == RESULT_OK) {
            //user is returning from capturing an image using the camera
            if (requestCode == CAMERA_CAPTURE) {
                //get the Uri for the captured image
                if(data != null) {
                    picUri = data.getData();
                    Bundle extras = data.getExtras();
                    //get the cropped bitmap
                    thePic = extras.getParcelable("data");
                    if(thePic != null){
                        // resizing the picture to 256 by 256 to save space
                        //thePic = RoundedImageView.getCroppedBitmap(thePic, 256);
                        //uploadAvatar(thePic);
                        image.setImageBitmap(thePic);
                    }
                    else{
                        //TODO handle
                    }

                }

            }

        }
    }

    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 786: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED){


                }
                else{

                    //  String errorMessage = "Are you sure you don't want to choose a picture for your avatar?";
                    Toast toast = Toast.makeText(PhotoView.this, "Permission to use camera needed", Toast.LENGTH_SHORT);
                    toast.show();
                    // Permission denied - Show a message to inform the user that this app only works
                    // with these permissions granted

                }
                return;
            }

        }
    }

    private void getLocation() {

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            lastLatitude = location.getLatitude();
                            lastLongitude = location.getLongitude();
                            Log.d("Location", "Longitude: " + lastLongitude + " Latitude: " + lastLatitude);
                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {

                            }
                        }
                    });
        } catch (SecurityException e) {
            e.fillInStackTrace();
            Log.d("Location Error", e.toString());
        }
    }

    public void uploadAvatar(Bitmap bitmap){

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();
        //TODO update this reference
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child("");

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Toast toast = Toast.makeText(PhotoView.this, "Could not upload the photo", Toast.LENGTH_SHORT);
                toast.show();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast toast = Toast.makeText(PhotoView.this, "Image posted successfully", Toast.LENGTH_SHORT);
                toast.show();

                // retrieve URI for the picture
                imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {


                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                    }
                });
            }
        });
    }



    public void uploadImageMetadata(Uri uri, String caption){


        if(lastLatitude != 0.0 && lastLongitude != 0.0){



        }

    }






}


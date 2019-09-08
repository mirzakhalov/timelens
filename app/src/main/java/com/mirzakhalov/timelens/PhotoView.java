package com.mirzakhalov.timelens;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.content.ActivityNotFoundException;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.location.Location;
import android.os.Build;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmark;
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmarkDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionLatLng;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mirzakhalov.timelens.fbService.FirebaseService;
import com.mirzakhalov.timelens.fbService.ImageLoc;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;


import java.util.UUID;

import static java.util.UUID.randomUUID;

public class PhotoView extends AppCompatActivity implements View.OnClickListener {
    Button takePicture;
    Button proceedButton;
    EditText caption;
    ProgressBar progress;
    ImageView image;
    //keep track of camera capture intent
    final int CAMERA_CAPTURE = 1;

    Bitmap thePic = null;
    Uri picUri = null;
    FirebaseService firebaseService;

    private static final int WRITE_PERMISSION = 786;


    private FusedLocationProviderClient fusedLocationClient;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;

    private double landMLatitude = 0.0;
    private double landMLongitude = 0.0;
    private boolean useLM = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);


        firebaseService = new FirebaseService();

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


    private void imageFromPath() {
        // [START image_from_path]

        Bitmap bitmap = ((BitmapDrawable)image.getDrawable()).getBitmap();
        FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);

        // [END image_from_path]

        recognizeLandmarksCloud(image);
    }

    private void recognizeLandmarksCloud(FirebaseVisionImage image) {
        // [START set_detector_options_cloud]
        FirebaseVisionCloudDetectorOptions options = new FirebaseVisionCloudDetectorOptions.Builder()
                .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                .setMaxResults(30)
                .build();
        // [END set_detector_options_cloud]

        // [START get_detector_cloud]
        FirebaseVisionCloudLandmarkDetector detector = FirebaseVision.getInstance()
                .getVisionCloudLandmarkDetector();
        // Or, to change the default settings:
        // FirebaseVisionCloudLandmarkDetector detector = FirebaseVision.getInstance()
        //         .getVisionCloudLandmarkDetector(options);
        // [END get_detector_cloud]

        // [START run_detector_cloud]
        Task<List<FirebaseVisionCloudLandmark>> result = detector.detectInImage(image)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionCloudLandmark>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionCloudLandmark> firebaseVisionCloudLandmarks) {
                        // Task completed successfully
                        // [START_EXCLUDE]
                        // [START get_landmarks_cloud]
                        FirebaseVisionCloudLandmark confLM = null;

                        for (FirebaseVisionCloudLandmark landmark: firebaseVisionCloudLandmarks) {

                            Rect bounds = landmark.getBoundingBox();
                            String landmarkName = landmark.getLandmark();
                            Log.d("LANDMARK", "onSuccess: " + landmarkName);
                            String entityId = landmark.getEntityId();
                            float confidence = landmark.getConfidence();

                            Log.d("info", "onSuccess: " + landmark.toString());

                            if(confidence > 0.65 && (confLM == null || (confLM.getConfidence() < confidence))) {
                                confLM = landmark;
                            }

                            // Multiple locations are possible, e.g., the location of the depicted
                            // landmark and the location the picture was taken.

                        }

                        final FirebaseVisionCloudLandmark finalLM = confLM;

                        if(finalLM != null) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(PhotoView.this);
                            builder.setMessage("You seem to be at " + confLM.getLandmark() + ". Do you want to use this location?")
                                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // FIRE ZE MISSILES!
                                            useLM = true;
                                            for (FirebaseVisionLatLng loc: finalLM.getLocations()) {
                                                lastLatitude = loc.getLatitude();
                                                lastLongitude = loc.getLongitude();
                                                Log.d("OUTPUT", "onSuccess: lat: " + ((Double) lastLatitude).toString());
                                                Log.d("OUTPUT", "onSuccess: long: " + ((Double) lastLongitude).toString());
                                            }
                                            uploadAvatar(thePic);
                                        }
                                    })
                                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            // User cancelled the dialog
                                            uploadAvatar(thePic);
                                        }
                                    });

                            Dialog dialog = builder.create();
                            dialog.show();


                        } else {
                            uploadAvatar(thePic);
                        }


                        // [END get_landmarks_cloud]
                        // [END_EXCLUDE]
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Task failed with an exception
                        // ...
                        Log.d("info", "Failure");
                    }
                });
        // [END run_detector_cloud]
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
                uploadAvatar();
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
        StorageReference imageRef = FirebaseStorage.getInstance().getReference().child(randomUUID().toString());

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
                        uploadImageMetadata(uri);
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



    public void uploadImageMetadata(Uri uri){
        double targetLat;
        double targetLong;
        boolean currLoc = !useLM && lastLatitude != 0.0 && lastLongitude != 0.0;
        boolean lmLoc = useLM && landMLatitude != 0.0 && landMLongitude != 0.0;

        if(!currLoc && !lmLoc){
            return;
        } else if(currLoc){
            targetLat = lastLatitude;
            targetLong = lastLongitude;

        } else {
            targetLat = landMLatitude;
            targetLong = landMLongitude;
        }

        String latTrim = this.firebaseService.trimNumByDecPlace(targetLat, 2);
        String lngTrim = this.firebaseService.trimNumByDecPlace(targetLong, 2);
        String latKey = latTrim + "_" + lngTrim;

        Log.d("Test", uri.toString());
        HashMap<String, Double> location = new HashMap<>();

        location.put("latitude", targetLat);
        location.put("longitude", targetLong);

        Double timestamp = (double) new Date().getTime();
        String captionStr = caption.getText().toString();
        String url = uri.toString();

        HashMap<String, Object> inpObj = new HashMap<>();
        HashMap<String, Object> inpObjStuff = new HashMap<>();
        inpObjStuff.put("location", location);
        inpObjStuff.put("caption", captionStr);
        inpObjStuff.put("url", url);
        inpObjStuff.put("timestamp", timestamp);
        inpObj.put(UUID.randomUUID().toString(), inpObjStuff);

        this.firebaseService.DB.getReference().child(latKey).updateChildren(inpObj);
    }
}


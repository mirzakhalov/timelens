package com.mirzakhalov.timelens;

import android.content.Context;
import android.net.Uri;
import android.content.ActivityNotFoundException;
import android.content.Intent;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.location.Location;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.core.content.FileProvider;
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
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmark;
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmarkDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionLatLng;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.mirzakhalov.timelens.fbService.FirebaseService;
import com.mirzakhalov.timelens.fbService.ImageLoc;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
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

    String currentPhotoPath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_view);
        imageFromPath(this.getApplicationContext());

        firebaseService = new FirebaseService();

        // layout elements
        image = (ImageView) findViewById(R.id.image);
        takePicture = (Button) findViewById(R.id.retake);
        proceedButton = (Button) findViewById(R.id.upload);
        caption = (EditText) findViewById(R.id.caption);

        takePicture.setOnClickListener(this);
        proceedButton.setOnClickListener(this);

        if (savedInstanceState == null)
        {
            try {
                //use standard intent to capture an image
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File

                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    Uri photoURI = FileProvider.getUriForFile(this,
                            "com.example.android.fileprovider",
                            photoFile);
                    captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(captureIntent, CAMERA_CAPTURE);
                }
            } catch (ActivityNotFoundException anfe) {
                //display an error message
                Toast toast = Toast.makeText(PhotoView.this, "No support", Toast.LENGTH_SHORT);
                toast.show();
            }
        }




        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocation();

    }


    private void imageFromPath(Context context) {
        // [START image_from_path]

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.raw.monument1);
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
                        for (FirebaseVisionCloudLandmark landmark: firebaseVisionCloudLandmarks) {

                            Rect bounds = landmark.getBoundingBox();
                            String landmarkName = landmark.getLandmark();
                            Log.d("LANDMARK", "onSuccess: " + landmarkName);
                            String entityId = landmark.getEntityId();
                            float confidence = landmark.getConfidence();

                            // Multiple locations are possible, e.g., the location of the depicted
                            // landmark and the location the picture was taken.
                            for (FirebaseVisionLatLng loc: landmark.getLocations()) {
                                Double latitude = loc.getLatitude();
                                Double longitude = loc.getLongitude();
                                Log.d("OUTPUT", "onSuccess: lat: " + latitude.toString());
                                Log.d("OUTPUT", "onSuccess: long: " + longitude.toString());
                            }
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
                    }
                });
        // [END run_detector_cloud]
    }

    public void onClick(View v) {
        if( v.getId() == R.id.retake){
            try {

                //use standard intent to capture an image
                Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (captureIntent.resolveActivity(getPackageManager()) != null) {

                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File

                    }
                    // Continue only if the File was successfully created
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "com.example.android.fileprovider",
                                photoFile);
                        captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(captureIntent, CAMERA_CAPTURE);
                    }
                }
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
                if (data != null) {
                    picUri = data.getData();
                    image.setImageURI(picUri);
                    Toast.makeText(PhotoView.this, "Data is not null", Toast.LENGTH_LONG).show();
                }
                if (picUri == null && currentPhotoPath != null) {
                    picUri = Uri.fromFile(new File(currentPhotoPath));

                    image.setImageURI(picUri);
                    Toast.makeText(PhotoView.this, "Creating URI from file", Toast.LENGTH_LONG).show();
                }
                try {
                    thePic = MediaStore.Images.Media.getBitmap(this.getContentResolver(), picUri);
                } catch (Exception e){
                    e.fillInStackTrace();
                }
                File file = new File(currentPhotoPath);
                if (!file.exists()) {
                    file.mkdir();
                }



            }

        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState )
    {
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
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


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }



    public void uploadImageMetadata(Uri uri){
        if(lastLatitude != 0.0 && lastLongitude != 0.0){
            String latTrim = this.firebaseService.trimNumByDecPlace(lastLatitude, 2);
            String lngTrim = this.firebaseService.trimNumByDecPlace(lastLongitude, 2);
            String latKey = latTrim + "_" + lngTrim;

            Log.d("Test", uri.toString());
            HashMap<String, Double> location = new HashMap<>();

            location.put("latitude", lastLatitude);
            location.put("longitude", lastLongitude);

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
}


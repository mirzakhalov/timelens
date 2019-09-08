package com.mirzakhalov.timelens;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Trackable;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mirzakhalov.timelens.fbService.FirebaseService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class ARView extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;

    private ArFragment arFragment;
    private ModelRenderable andyRenderable;

    private ImageButton switchButton;
    private ImageButton cameraLaunch;

    private Scene.OnUpdateListener updateListener = null;

    private FirebaseService firebaseService;

    private ArrayList<HashMap<String, String>> imageDetailList;






    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        imageDetailList = new ArrayList<>();
        firebaseService = new FirebaseService();


        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_main);


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);

        switchButton = findViewById(R.id.switchButton);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ARView.this.startActivity(new Intent(ARView.this, MapboxView.class));
            }
        });

        cameraLaunch = findViewById(R.id.camera);

        cameraLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ARView.this.startActivity(new Intent(ARView.this, PhotoView.class));
               // ARView.this.startActivity(new Intent(ARView.this, GalleryView.class));

            }
        });

        updateListener = frameTime -> {

            Frame frame = arFragment.getArSceneView().getArFrame();
            if(frame == null) {
                return;
            }

            for(Plane plane : frame.getUpdatedTrackables(Plane.class)) {
                Log.d("AR", "Found a plane");
                addObjectModel(Uri.parse("model_pluto_20171119_162330052.sfb"));
                break;
            }
        };


        // call this function every 10 seconds to get the new location
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(ARView.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(ARView.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            100);
                } else{

                   getLocation();
                }
            }
        }, 0, 10000);//put here time 1000 milliseconds=1 second



    }

    public void getData(){

        // Check if permissions are enabled and if not request
        if(lastLongitude != 0.0 && lastLatitude != 0.0) {
            String lonTrim = firebaseService.trimNumByDecPlace(lastLongitude, 2);
            String latTrim = firebaseService.trimNumByDecPlace(lastLatitude, 2);
            //private ArrayList imageDetailList;


            String llStr = latTrim + '_' + lonTrim;
            Log.d("Reference", llStr);


            this.firebaseService.DB.getReference(llStr).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("STATE", "Hi");


                    if (dataSnapshot.getValue() != null) {
                        HashMap<String, HashMap<String, Object>> hm = (HashMap) dataSnapshot.getValue();
                        Log.d("hm", hm.toString());
                        for (HashMap<String, Object> obj : hm.values()) {
                            HashMap<String, String> imageHM = new HashMap<>();
                            if (obj.get("url") != null) {
                                imageHM.put("url", obj.get("url").toString());
                            }
                            if (obj.get("caption") != null) {
                                imageHM.put("caption", obj.get("caption").toString());
                            }
                            if (obj.get("location") != null) {
                                HashMap<String, Object> lcHM = (HashMap<String, Object>) obj.get("location");
                                imageHM.put("latitude", lcHM.get("latitude").toString());
                                imageHM.put("longitude", lcHM.get("longitude").toString());
                            }
                            if (obj.get("timestamp") != null) {
                                imageHM.put("timestamp", obj.get("timestamp").toString());
                            }

                            imageDetailList.add(imageHM);
                            Log.d("Data", "Adding data");
                        }

                        Log.d("Data", imageDetailList.toString());

                        if(imageDetailList.size() > 0) {
                            arFragment.getArSceneView().getScene()
                                    .addOnUpdateListener(updateListener);
                        } else{
                            arFragment.getArSceneView().getScene()
                                    .removeOnUpdateListener(updateListener);
                        }
                    }



                    Log.d("Data", imageDetailList.toString());
                }




                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });


        }
    }


    private void getLocation(){

        try{
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {

                            // Got last known location. In some rare situations this can be null.
                            if (location != null) {
                                lastLatitude = location.getLatitude();
                                lastLongitude = location.getLongitude();
                                Log.d("Location", "Longitude: " + lastLongitude + " Latitude: " + lastLatitude);
                                getData();
                            }
                        }
                    });
        }
        catch(SecurityException e){
            e.fillInStackTrace();
            Log.d("Location Error", e.toString());
        }
    }

    private void addObjectModel(Uri object) {

        Frame frame = arFragment.getArSceneView().getArFrame();
        Point center = getScreenCenter();
//

        if(frame != null) {
            List<HitResult> result = frame.hitTest(center.x, center.y);
            for(HitResult hit : result) {
                Trackable trackable = hit.getTrackable();
                if (trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) {
                    placeObject(hit.createAnchor(), object);

                    arFragment.getArSceneView().getScene()
                            .removeOnUpdateListener(updateListener);
                }
            }
        } else{
            Log.d("AR", "Frame is null");
        }
    }

    private Point getScreenCenter() {

        if(arFragment == null || arFragment.getView() == null) {
            return new android.graphics.Point(0,0);
        }

        int w = arFragment.getView().getWidth()/2;
        int h = arFragment.getView().getHeight()/2;
        return new android.graphics.Point(w, h);
    }

    private void placeObject(Anchor anchor, Uri object) {
        try {
            ModelRenderable.builder()
                    .setSource(ARView.this, R.raw.hourglass)
                    .build()
                    .thenAccept(modelRenderable -> addNodeToScene(anchor, modelRenderable, object))
                    .exceptionally(throwable -> {
                        Toast toast =
                                Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                        toast.setGravity(Gravity.CENTER, 0, 0);
                        toast.show();
                        return null;
                    });
        } catch (Exception e){
            Log.d("AR", e.toString());
        }
    }

    private void addNodeToScene(Anchor createAnchor, ModelRenderable renderable, Uri object) {

        Log.d("AR", "Starting to add node");

        AnchorNode anchorNode = new AnchorNode(createAnchor);
        TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
        transformableNode.setName(object.toString());
        transformableNode.setRenderable(renderable);
        transformableNode.setParent(anchorNode);

        arFragment.getArSceneView().getScene().addChild(anchorNode);

        transformableNode.setOnTapListener((hitTestResult, motionEvent) -> {
            Toast.makeText(ARView.this, "You can't touch me", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(ARView.this, GalleryView.class);
            intent.putExtra("images", imageDetailList);
            finish();
            ARView.this.startActivity(intent);
        });
        transformableNode.select();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    getLocation();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Location permission", "Not granted");

                }
                return;
            }

        }
    }

    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e("App", "Sceneform requires Android N or later");
            Toast.makeText(activity, "Sceneform requires Android N or later", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < 3.0) {
            Log.e("App", "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}

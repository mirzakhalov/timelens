package com.mirzakhalov.timelens;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mirzakhalov.timelens.fbService.FirebaseService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapboxView extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener {


    private FusedLocationProviderClient fusedLocationClient;
    private double lastLatitude = 0.0;
    private double lastLongitude = 0.0;

    private FirebaseService firebaseService;

    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;
    private ImageButton switchButton;
    private ImageButton cameraLaunch;


    private HashMap<String, ArrayList<HashMap<String, String>>> imageDetailList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseService = new FirebaseService();
        imageDetailList = new HashMap<>();

        // Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.mapbox_token));
        setContentView(R.layout.activity_mapbox_view);


// This contains the MapView in XML and needs to be called after the access token is configured.

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        switchButton = findViewById(R.id.switchButton);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapboxView.this.startActivity(new Intent(MapboxView.this, ARView.class));
            }
        });

        cameraLaunch = findViewById(R.id.camera);


        cameraLaunch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MapboxView.this.startActivity(new Intent(MapboxView.this, PhotoView.class));

            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
                                // do something
                            }
                        }
                    });
        } catch (SecurityException e) {
            e.fillInStackTrace();
            Log.d("Location Error", e.toString());
        }
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {

    }

    @Override
    public void onPermissionResult(boolean granted) {

    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {

        MapboxView.this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/mapbox/cjerxnqt3cgvp2rmyuxbeqme7"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style) {
                        enableLocationComponent(style);
                    }
                });

    }


    @SuppressWarnings({"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle) {

        // Check if permissions are enabled and if not request
        if(this.lastLongitude != 0 && this.lastLatitude != 0) {
            String latTrim = this.firebaseService.trimNumByDecPlace(this.lastLongitude, 2);
            String lonTrim = this.firebaseService.trimNumByDecPlace(this.lastLatitude, 2);
            //private ArrayList imageDetailList;

            Context meContext = this;
            PermissionsListener mePL = this;
            Activity meAct = this;

            String llStr = latTrim + '_' + lonTrim;

            this.firebaseService.DB.getReference().child(llStr).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Log.d("STATE", "Hi");


                    if (dataSnapshot.getValue() != null) {
                        imageDetailList = new HashMap<>();
                        ArrayList<HashMap<String, String>> ahmp = new ArrayList<>();
                        HashMap<String, HashMap<String, Object>> hm = (HashMap) dataSnapshot.getValue();
                        for (HashMap<String, Object> obj : hm.values()) {
                            HashMap<String, String> imageHM = new HashMap<>();
                            imageHM.put("url", obj.get("url").toString());
                            imageHM.put("caption", obj.get("caption").toString());
                            ahmp.add(imageHM);

                        }
                        imageDetailList.put(llStr, ahmp);

                    }

                    if (PermissionsManager.areLocationPermissionsGranted(meContext)) {

                        // Get an instance of the component
                        LocationComponent locationComponent = mapboxMap.getLocationComponent();


                        // Activate with options
                        locationComponent.activateLocationComponent(
                                LocationComponentActivationOptions.builder(meContext, loadedMapStyle).build());

                        // Enable to make component visible
                        locationComponent.setLocationComponentEnabled(true);

                        // Set the component's camera mode
                        locationComponent.setCameraMode(CameraMode.TRACKING);

                        // Set the component's render mode
                        locationComponent.setRenderMode(RenderMode.COMPASS);
                    } else {
                        permissionsManager = new PermissionsManager(mePL);
                        permissionsManager.requestLocationPermissions(meAct);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
        }

    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }
}
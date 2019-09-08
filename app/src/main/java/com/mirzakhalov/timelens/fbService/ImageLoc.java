package com.mirzakhalov.timelens.fbService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;

public class ImageLoc implements DatabaseReference.CompletionListener {
    String url;
    String caption;
    HashMap<String, Double> location;
    Double timestamp;

    public ImageLoc(String url, String caption, HashMap<String, Double> location, Double timestamp) {
        this.url = url;
        this.caption = caption;
        this.location = location;
        this.timestamp = timestamp;
    }

    @Override
    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

    }
}

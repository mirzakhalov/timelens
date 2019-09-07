package com.mirzakhalov.timelens.fbService;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class FirebaseService {

    public FirebaseDatabase DB;
    HashMap<String, ChildEventListener> listenerMap;
    CountDownLatch done;

    public FirebaseService() {
        this.DB = FirebaseDatabase.getInstance();
    }

    public ArrayList<String> getImageLatLong(double lat, double lon) {
        final CountDownLatch done = new CountDownLatch(1);
        final GenericTypeIndicator<HashMap<String, ImageLoc>> t = new GenericTypeIndicator<HashMap<String, ImageLoc>>() {};
        final ArrayList<String> listOfImages = new ArrayList<>();
        String latTrim = trimNumByDecPlace(lat, 2).toString().replace('.', '_');
        String lonTrim = trimNumByDecPlace(lon, 2).toString().replace('.', '_');


        this.DB.getReference().child(latTrim + '_' + lonTrim).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.d("STATE", "Hi");
                if(dataSnapshot.exists()) {
                    HashMap<String, HashMap<String, Object>> hm = (HashMap) dataSnapshot.getValue();
                    for (HashMap<String, Object> obj : hm.values()) {
                        String url = obj.get("url").toString();
                        listOfImages.add(url);
                    }

                }
                done.countDown();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
        try {
            done.await(); //it will wait till the response is received from firebase.
            return listOfImages;

        } catch(InterruptedException e) {
            e.printStackTrace();
        }

        return listOfImages;
    }

    public void removeListenerLatLong(double lat, double lon) {
        String latTrim = trimNumByDecPlace(lat, 2).toString();
        String lonTrim = trimNumByDecPlace(lon, 2).toString();

        String key = latTrim + '_' + lonTrim;

        if(this.listenerMap.get(key) != null) {
            this.listenerMap.get(key);
        }

        //this.listenerMap;
    }



    public Double trimNumByDecPlace(double num, int num_spaces){
        double bigNum = num * (num_spaces * 10);
        long newNum = Math.round(bigNum);
        double returnNum = newNum / (num_spaces * 10);
        return returnNum;
    }

}

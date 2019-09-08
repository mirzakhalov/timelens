package com.mirzakhalov.timelens;

import android.net.Uri;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.HashMap;

public class GalleryView extends AppCompatActivity {


    ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_view);

        pager = findViewById(R.id.pager);

        ArrayList<HashMap<String, String>> images = (ArrayList<HashMap<String, String>>) getIntent().getSerializableExtra("images");


        GalleryAdapter adapter = new GalleryAdapter(this, images);

        pager.setAdapter(adapter);




    }

}

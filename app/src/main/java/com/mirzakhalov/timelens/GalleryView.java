package com.mirzakhalov.timelens;

import android.net.Uri;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class GalleryView extends AppCompatActivity {


    ViewPager pager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery_view);

        pager = findViewById(R.id.pager);

        Uri[] images = new Uri[]{
            Uri.parse("https://firebasestorage.googleapis.com/v0/b/timelens-58640.appspot.com/o/1.jpg?alt=media&token=f45cbe0c-6554-4429-a101-c33857f9056e"),
            Uri.parse("https://firebasestorage.googleapis.com/v0/b/timelens-58640.appspot.com/o/2.jpeg?alt=media&token=d2a2f16e-7c7f-4320-857b-c2036087267b"),
            Uri.parse("https://firebasestorage.googleapis.com/v0/b/timelens-58640.appspot.com/o/3.jpeg?alt=media&token=bc488a24-b8f9-4b34-9697-cfc0076f7215")
        };


        GalleryAdapter adapter = new GalleryAdapter(this, images);

        pager.setAdapter(adapter);




    }

}

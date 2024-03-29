package com.mirzakhalov.timelens;

import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class CameraPreview extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);


        String image = getIntent().getStringExtra("image");

        if (null == savedInstanceState) {

            BasicFragment fragment = BasicFragment.newInstance();
            Bundle arguments = new Bundle();
            arguments.putString( "image" , image);
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();


        }



    }

}
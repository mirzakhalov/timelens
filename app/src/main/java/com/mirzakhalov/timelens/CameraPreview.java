package com.mirzakhalov.timelens;

import android.app.Activity;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.widget.ImageButton;

import com.squareup.picasso.Picasso;

import java.io.IOException;

public class CameraPreview extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_preview);


        Uri image = (Uri) getIntent().getParcelableExtra("image");

        if (null == savedInstanceState) {

            BasicFragment fragment = BasicFragment.newInstance();
            Bundle arguments = new Bundle();
            arguments.putParcelable( "image" , image);
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commit();


        }



    }

}
package com.mirzakhalov.timelens;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewpager.widget.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;

public class GalleryAdapter extends PagerAdapter {


    Context context;

    //private Uri[] images;
    private ArrayList<HashMap<String, String>> images;

    LayoutInflater mLayoutInflater;

    GalleryAdapter(Context context, ArrayList<HashMap<String, String>> images){
        this.context=context;
        this.images = images;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == ((ConstraintLayout) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View itemView = mLayoutInflater.inflate(R.layout.single_image, container, false);

        ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
        TextView caption = (TextView) itemView.findViewById(R.id.caption);

        imageView.setImageBitmap(null);
        Picasso.with(context).load(images.get(position).get("url")).into(imageView);


        caption.setText(images.get(position).get("caption"));

        imageView.setOnTouchListener(new OnSwipeTouchListener(context) {
            public void onSwipeTop() {
                //Toast.makeText(context, "top", Toast.LENGTH_SHORT).show();
                imageView.setAlpha(0.3f);
                caption.setVisibility(View.VISIBLE);

            }

            public void onSwipeBottom() {
                //Toast.makeText(context, "bottom", Toast.LENGTH_SHORT).show();
                if(caption.getVisibility() == View.GONE){
                    Intent intent = new Intent(context, CameraPreview.class);
                    intent.putExtra("image", images.get(position).get("url"));

                    context.startActivity(intent);
                } else {
                    imageView.setAlpha(1.0f);
                    caption.setVisibility(View.GONE);
                }
            }

        });


        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ConstraintLayout)object);
    }
}

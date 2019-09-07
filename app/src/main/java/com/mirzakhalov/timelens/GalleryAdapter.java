package com.mirzakhalov.timelens;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

public class GalleryAdapter extends PagerAdapter {


    Context context;

    private Uri[] images;

    LayoutInflater mLayoutInflater;

    GalleryAdapter(Context context, Uri[] images){
        this.context=context;
        this.images = images;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return images.length;
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
        Button capsule = (Button) itemView.findViewById(R.id.capsule);


        imageView.setOnTouchListener(new OnSwipeTouchListener(context) {
            public void onSwipeTop() {
                //Toast.makeText(context, "top", Toast.LENGTH_SHORT).show();
                imageView.setAlpha(0.3f);
                caption.setVisibility(View.VISIBLE);

            }

            public void onSwipeBottom() {
                //Toast.makeText(context, "bottom", Toast.LENGTH_SHORT).show();
                imageView.setAlpha(1.0f);
                caption.setVisibility(View.GONE);
            }

        });

        capsule.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, CameraPreview.class);
                intent.putExtra("image", images[position]);

                context.startActivity(intent);
            }
        });

        Picasso.with(context).load(images[position]).into(imageView);

        container.addView(itemView);

        return itemView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((ConstraintLayout)object);
    }
}

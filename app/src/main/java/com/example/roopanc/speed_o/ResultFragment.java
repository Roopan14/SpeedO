package com.example.roopanc.speed_o;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Created by Roopan C on 9/26/2018.
 */

public class ResultFragment extends DialogFragment {

    private TextView finish, distancetv, durationtv, avgspeedtv, maxspeedtv;
    CoordinatorLayout.LayoutParams params;
    CoordinatorLayout.Behavior behavior;
    ImageView transit;

    @Override
    public void onStart() {
        super.onStart();

        final View decorView = getDialog()
                .getWindow()
                .getDecorView();

        ObjectAnimator zoomon = ObjectAnimator.ofPropertyValuesHolder(decorView,
                PropertyValuesHolder.ofFloat("scaleX", 0.0f, 1.0f),
                PropertyValuesHolder.ofFloat("scaleY", 0.0f, 1.0f),
                PropertyValuesHolder.ofFloat("alpha", 0.0f, 1.0f));
        zoomon.setDuration(500);
        zoomon.start();
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.resultfragment, container, false);
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        avgspeedtv = view.findViewById(R.id.avgspeedtvfz);
        maxspeedtv = view.findViewById(R.id.maxspeedtvfz);
        durationtv = view.findViewById(R.id.durationtvfz);
        distancetv = view.findViewById(R.id.distancetvfz);

        finish = view.findViewById(R.id.finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View decorView = getDialog()
                        .getWindow()
                        .getDecorView();

                ObjectAnimator zoomoff = ObjectAnimator.ofPropertyValuesHolder(decorView,
                        PropertyValuesHolder.ofFloat("scaleX", 1.0f, 0.0f),
                        PropertyValuesHolder.ofFloat("scaleY", 1.0f, 0.0f),
                        PropertyValuesHolder.ofFloat("alpha", 1.0f, 0.0f));
                zoomoff.setDuration(500);
                zoomoff.start();
                zoomoff.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        dismiss();
                    }
                });
            }
        });

        transit = view.findViewById(R.id.transit);
        Resources res = getResources();
        Bitmap src = BitmapFactory.decodeResource(res, R.drawable.transit);
        RoundedBitmapDrawable dr =
                RoundedBitmapDrawableFactory.create(res, src);
        dr.setCornerRadius(48f);
        transit.setImageDrawable(dr);

        Bundle bundle = getArguments();
        if (bundle != null)
        {
            String avgspeed = bundle.getString("avgspeed");
            String maxspeed = bundle.getString("maxspeed");
            String duration = bundle.getString("duration");
            String distance = bundle.getString("distance");

            avgspeedtv.setText(avgspeed);
            maxspeedtv.setText(maxspeed);
            durationtv.setText(duration);
            distancetv.setText(distance);

            if (avgspeed.equalsIgnoreCase("-"))
            {
                avgspeedtv.setText("0 KM/H");
            }
            if (maxspeed.equalsIgnoreCase("-"))
            {
                maxspeedtv.setText("0 KM/H");
            }
        }

        return view;
    }
}

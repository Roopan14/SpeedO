package com.example.roopanc.speed_o.UI;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

import com.example.roopanc.speed_o.R;

/**
 * Created by Roopan C on 9/26/2018.
 */

public class CompassView extends View {

    private float luminance = 0;
    private String colorHex = "#000000";
    private final float scale = getResources().getDisplayMetrics().density;
    private boolean showText;
    private Paint paint = new Paint();
    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.updirection);
    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 96, 96, true);
    private float rotationAngle = 0.0f;

    public CompassView(Context context) {
        super(context);
    }

    public CompassView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setBackgroundResource(R.drawable.compass);

    }

    public CompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CompassView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int x1 = canvas.getWidth();
        int y1 = canvas.getHeight();

        int x2 = scaledBitmap.getWidth();
        int y2 = scaledBitmap.getHeight();

        int x = (x1 - x2)/2;
        int y = (y1 - y2)/2;

        int bitmapCenterX = x + (scaledBitmap.getWidth() / 2);
        int bitmapCenterY = y + (scaledBitmap.getHeight() / 2);
        canvas.save();
        canvas.rotate(rotationAngle, bitmapCenterX, bitmapCenterY);
        canvas.drawBitmap(scaledBitmap, x, y, null);
        canvas.restore();

    }

    public void setRotationAngle(float angle){
        rotationAngle = angle;
        invalidate();
    }
}

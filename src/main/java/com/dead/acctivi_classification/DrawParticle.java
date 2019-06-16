package com.dead.acctivi_classification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

class DrawParticle extends View {

    private static final String TAG = "Draw";
    private final Bitmap mBitmap;
    // private final Bitmap bitmap;
    private final Bitmap mutableBitmap;
    private final Canvas mCanvas;
    private final Paint mPaint;
    private final Bitmap workingBitmap;
    //private final Bitmap mutable1Bitmap;


    private float radius = 1;
    private float xScale = 2613/48.99f;
    private float yScale = 810/15.04f;
    private Paint m1Paint;

///
    public DrawParticle(Context context) {
        super(context);
        BitmapFactory.Options myOptions = new BitmapFactory.Options();
        //myOptions.inDither = true;
        myOptions.inScaled = true;
        myOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;// important
        //myOptions.inPurgeable = true;


        mBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.capture, myOptions);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mPaint.setColor(Color.RED);

        workingBitmap = Bitmap.createBitmap(mBitmap);

        workingBitmap.getWidth();
        mutableBitmap = workingBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Log.d(TAG, "working"+" " +String.valueOf(workingBitmap.getHeight()) + " " +String.valueOf(workingBitmap.getWidth()));
        Log.d(TAG, "Mutable"+" " + String.valueOf(mutableBitmap.getHeight()) + " " +String.valueOf(mutableBitmap.getWidth()));
        //  mutable1Bitmap = mutableBitmap.copy(Bitmap.Config.ARGB_8888, true);




        mCanvas = new Canvas(mutableBitmap);

        // mBitmap = BitmapFactory.decodeResource(getResources(), R.id.imageView);//get the floormap
        // bitmap = Bitmap.createBitmap(mBitmap);//create the bitmap

        //mutableBitmap = bitmap.reconfigure(500,300, Bitmap.Config.ARGB_8888);
        // mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);//configure it to abe able to change ant runtime

        //mCanvas = new Canvas(mutableBitmap);//create a canvas object

        //  mPaint = new Paint();//initialize paint object
        //  mPaint.setColor(Color.BLACK);//colour of the particle

    }

    public int DrawParticleView(ImageView floorPlan, Particle[] particles,PointF[]landmarks) {
        for (Particle particle : particles) {
            //set the x and y dimensions
            float x = particle.x*xScale ;//this has to be changed according to our x and y
            float y = particle.y*yScale ;
            mCanvas.drawCircle(x, y, radius, mPaint);//draw the particle
            floorPlan.setAdjustViewBounds(true);

            floorPlan.setImageBitmap(mutableBitmap);

            m1Paint = new Paint();
            m1Paint.setColor(Color.GREEN);
            for (int i=0;i<landmarks.length;i++){
//                Log.d(TAG, String.valueOf(landmarks[i].x) +" " +String.valueOf(landmarks[i].y));
                float x1 = landmarks[i].x*xScale;
                float y1 = landmarks[i].y*yScale;
//                Log.d(TAG, String.valueOf(x1) +" " +String.valueOf(y1));
                mCanvas.drawCircle(x1,y1,5,m1Paint);
            }
            floorPlan.setAdjustViewBounds(true);

            floorPlan.setImageBitmap(mutableBitmap);


        }
        return 0;
    }
  /*  public void DrawLandView(ImageView floorPlan, PointF[] landmarks){
        mPaint.setColor(Color.RED);
        for (int i=0;i<landmarks.length;i++){
            Log.d(TAG, String.valueOf(landmarks[i].x) +" " +String.valueOf(landmarks[i].y));
            float x = landmarks[i].x*xScale;
            float y = landmarks[i].y*yScale;
            Log.d(TAG, String.valueOf(x) +" " +String.valueOf(y));
            mCanvas.drawCircle(x,y,4,mPaint);
        }
        floorPlan.setAdjustViewBounds(true);

        floorPlan.setImageBitmap(mutableBitmap);

    }*/
}


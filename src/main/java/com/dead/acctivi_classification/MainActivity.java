package com.dead.acctivi_classification;

import android.content.Context;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.dead.acctivi_classification.distanceAlgorithm.DistanceAlgorithm;
import com.dead.acctivi_classification.distanceAlgorithm.EuclideanDistance;


import java.io.BufferedReader;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;


public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private static final String TAG = "MainActivity";


    private ParticleFilter particleFilter;
    private final int NUM_PARTICLES = 10000;
    private PointF[] landmarks = new PointF[]{
            new PointF(2.3f, 14.04f), new PointF(2.3f, 9.04f), new PointF(5.89f, 14.04f), new PointF(5.89f, 9.04f),
            new PointF(9.09f, 14.04f),
            new PointF(9.09f, 9.04f), new PointF(9.09f, 10.69f), new PointF(13.02f, 10.69f),
            new PointF(13.02f, 9.04f),
            new PointF(16.84f, 10.69f), new PointF(16.84f, 9.04f), new PointF(16.84f, 13.44f), new PointF(19.14f, 9.04f),
            new PointF(19.14f, 13.44f),
            new PointF(19.14f, 10.69f), new PointF(16.84f, 6.18f), new PointF(19.14f, 6.18f), new PointF(22.45f, 10.69f),
            new PointF(22.45f, 9.04f),
            new PointF(26.88f, 10.69f), new PointF(26.88f, 9.04f), new PointF(31.43f, 10.69f), new PointF(31.43f, 9.04f)
    };
    private DrawParticle drawLandmarks;

    final int WORLD_WIDTH = 32;
    final int WORLD_HEIGHT = 10;

//    private final ReentrantLock bufferLock = new ReentrantLock();

    RunTimeCalculations calculations = new RunTimeCalculations();
    private ArrayList<Float> dataX = new ArrayList<Float>();
    private ArrayList<Float> dataY = new ArrayList<Float>();
    private ArrayList<Float> dataZ = new ArrayList<Float>();

    private DistanceAlgorithm[] distanceAlgorithms = {new EuclideanDistance()};
    private Classifier classifier;
    private List<DataPoint> listDataPoint = new ArrayList<>();
    private List<DataPoint> listDataPointOriginal = new ArrayList<>();


    private TextView activity;
    private ImageView floor_map;
    private SensorManager mSensorManager;
    private Sensor mSensorAccelero;
    private Sensor mAccelero;
    private Sensor magneto;

    float avgX, avgY, avgZ, varX, varY, varZ, sdX, sdY, sdZ;
    private Button btStart, btStop;

    private int K;
    private double spRatio;

    private DrawParticle drawParticles;
    private ImageView image;


    private int mAzimuth = 0; // degree
    float[] gData = new float[3]; // accelerometer
    float[] mData = new float[3]; // magnetometer
    float[] rMat = new float[9];
    float[] iMat = new float[9];
    float[] orientation = new float[3];

    private HandlerThread movementThread = new HandlerThread("thread");
    private Handler threadHandler;


    private String peviousState;
    String state;
    private int stepCount = 0;
    private int walkingFrequency = 1;
    private int lastDirection = 0, currentDirection = 0, orient = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btStart = (Button) findViewById(R.id.btStart);
        btStop = (Button) findViewById(R.id.btStop);
        activity = (TextView) findViewById(R.id.textViewZ);
        image = (ImageView) findViewById(R.id.imageView);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelero = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mAccelero = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneto = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        K = 11;
        spRatio = 0.8;

        String sensor_error = getResources().getString(R.string.error_no_sensor);
        start();

        if (mSensorAccelero == null) {
            activity.setText(sensor_error);
        }
        if (mAccelero == null) {
            activity.setText(sensor_error);
        }
        if (magneto == null) {
            activity.setText(sensor_error);
        } else {
            activity.setText(" Waiting for Data");
        }

        classifier = new Classifier();
        populateList();
        //runClassifier();


        btStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //start();
            }
        });

        btStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // stop();
            }
        });
        state = "Idle";


        particleFilter = new ParticleFilter(NUM_PARTICLES, landmarks, WORLD_WIDTH, WORLD_HEIGHT);
        drawParticles = new DrawParticle(this);
        for (int i = 0; i < NUM_PARTICLES; i++)
        { particleFilter.particles[i].probability = 1/NUM_PARTICLES;}
        drawParticles.DrawParticleView(image, ParticleFilter.particles);

        movementThread.start();
        threadHandler = new Handler(movementThread.getLooper());
        threadHandler.postDelayed(new movementDetector(),500);

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//
        int sensorType = event.sensor.getType();
        float[] data;
        if (sensorType == Sensor.TYPE_LINEAR_ACCELERATION) {
            try {
                if (dataX.size() > 30) {
                    dataX.remove(0);
                }
                dataX.add(event.values[0]);

                if (dataY.size() > 30) {
                    dataY.remove(0);
                }
                dataY.add(event.values[1]);

                if (dataZ.size() > 30) {
                    dataZ.remove(0);
                }
                dataZ.add(event.values[2]);
            } finally {
            }
            processsData();
        }

        if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
            mData = event.values.clone();
        }
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            gData = event.values.clone();
        }
        calculateAzimuth();

    }

    void calculateAzimuth() {
        if (SensorManager.getRotationMatrix(rMat, iMat, gData, mData)) {
            mAzimuth = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
            //Float aa = Float.valueOf(mAzimuth);

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        movementThread.quit();
        stop();
    }


    private void populateList() {
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open
                    ("analised.csv")));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] point = line.split(",");
                double vX = Double.parseDouble(point[0]);
                double vY = Double.parseDouble(point[1]);
                double vZ = Double.parseDouble(point[2]);
                double sdX = Double.parseDouble(point[3]);
                double sdY = Double.parseDouble(point[4]);
                double sdZ = Double.parseDouble(point[5]);
                int category = Integer.parseInt(point[6]);
                DataPoint dataPoint = new DataPoint(vX, vY, vZ, sdX, sdY, sdZ, Category.values()[category]);
                listDataPointOriginal.add(new DataPoint(dataPoint));
                listDataPoint.add(dataPoint);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        classifier.setDistanceAlgorithm(distanceAlgorithms[0]);

        classifier.setK(11);
        classifier.setListDataPoint(listDataPoint);
        classifier.addTrainData();
        listDataPoint.clear();
        listDataPoint.addAll(classifier.getListTrainData());

    }

    void runClassifier() {
        classifier.reset();
        classifier.setDistanceAlgorithm(distanceAlgorithms[0]);
        classifier.setK(11);
        classifier.setSplitRatio(0.8);
        classifier.setListDataPoint(listDataPoint);
        classifier.splitData();
        listDataPoint.clear();
        listDataPoint.addAll(classifier.getListTestData());
        listDataPoint.addAll(classifier.getListTrainData());
        classifier.classify();
        activity.setText("Accuracy = " + classifier.getAccuracy());
    }

    public void start() {
        //Log.d(TAG, "olga::start");
        super.onStart();
        if (mSensorAccelero != null) {
            mSensorManager.registerListener(this, mSensorAccelero, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (mAccelero != null) {
            mSensorManager.registerListener(this, mAccelero, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (magneto != null) {
            mSensorManager.registerListener(this, magneto, SensorManager.SENSOR_DELAY_NORMAL);
        }
        Toast.makeText(getBaseContext(), "Data Recording Started", Toast.LENGTH_LONG).show();
    }


    public void stop() {

        super.onStop();
        mSensorManager.unregisterListener(this);
        Toast.makeText(getBaseContext(), "Data Recording Stopped", Toast.LENGTH_LONG).show();
    }

    public void processsData() {

        avgX = calculations.findAverage(dataX);
        avgY = calculations.findAverage(dataY);
        avgZ = calculations.findAverage(dataZ);

        varX = calculations.findVariance(dataX, avgX);
        varY = calculations.findVariance(dataY, avgY);
        varZ = calculations.findVariance(dataY, avgZ);

        sdX = calculations.findStandardDeviation(varX);
        sdY = calculations.findStandardDeviation(varY);
        sdZ = calculations.findStandardDeviation(varZ);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dataX.size() > 25) {
                Category category = classifier.predictNew(varX, varY, varZ, sdX, sdY, sdZ);
                String cat = category.toString();
                state = cat;
                activity.setText(cat); }
            }
        });
    }

    static long StartTime;
    static long StopTime;

    class movementDetector implements Runnable {
        @Override
        public void run() {
            while (1 < 2) {
                updateTextView();
            }
        }
    }

    protected void updateTextView() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                    if (state == "Idle") {
                        StartTime = System.nanoTime(); //set sta rt time 0
                        lastDirection = mAzimuth;
                        orient = currentDirection - lastDirection;
                        //activity.setText(state + "  " + String.valueOf(orient));
                        if (stepCount > 0) {
                            try {
                                particleFilter.move(orient, stepCount);
                                particleFilter.eliminate();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Log.d("test", String.valueOf(particleFilter.eliminateCount));
                            Particle[] newParticles = new Particle[NUM_PARTICLES - particleFilter.eliminateCount];
                            int count = 0;
                            for (int i = 0; i < ParticleFilter.particles.length; i++) {
                                if (particleFilter.particles[i].probability > 0) {
                                    newParticles[count] = particleFilter.particles[i];
                                    count += 1;
                                }
                            }

                            try {
                                particleFilter.reSamp(newParticles);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //Particle best = particleFilter.getBestParticle();//process of finding the next best particle
                            drawParticles.cleanView(image);
                            drawParticles.DrawParticleView(image, ParticleFilter.particles);
                        }
                        stepCount = 0;
                    }

                    if (state == "Walk") {
                       // activity.setText("threadrunning");
                        StopTime = System.nanoTime();
                        stepCount = Math.round((StopTime - StartTime) / 1000000000) * walkingFrequency;
                        currentDirection = mAzimuth;
                    }
//                    Log.d("test", "run method finished");

                }

        });
    }
}












    /*
    public class particleUpdate implements Runnable{

        private float orient;
        //private int stepCount;
        public particleUpdate(){
        }
        //public void update ( float orient, int stepCount ){
         //   this.orient=orient;
          //  this.stepCount=stepCount;
        //}

        @Override
        public void run() {
            try {
                particleFilter.move(orient,stepCount);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
*/



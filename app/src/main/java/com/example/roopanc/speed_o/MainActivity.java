package com.example.roopanc.speed_o;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.example.roopanc.speed_o.receiver.LocResultReceiver;
import com.example.roopanc.speed_o.service.LocationService;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SensorEventListener {

    private static final String TAG = MainActivity.class.getName();
    public static int REQ_CODE = 101;
    private static final long MAX_INTERVAL = 1000 * 2;
    private static final long MIN_INTERVAL = 1000 * 1;
    public static int appStatus = 1; // 1-resume 0-pause
    private String ACTION = "requestLocation";
    private String STOP = "stopService";
    private String RECEIVER = "locResultReceiver";
    LocationRequest mLocationRequest;
    Toolbar toolbar;
    TextView startTimeTV, endTimeTV;
    Chronometer durationTV;
    long elapsedSeconds, minutes, seconds, hours, elapsedTime;
    public TextView distanceTV, altitudeTV, headingTV, avgSpeedTV, maxSpeedTV, speedTV;
    private Button startBT, stopBT;
    public static int status = 0; //0-not started 1-started 2-paused 3-stopped
    private SensorManager sensorManager;
    private Sensor compassSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setting a toolbar
        toolbar = findViewById(R.id.toolbarhome);
        toolbar.setTitle(getString(R.string.app_name));
        setSupportActionBar(toolbar);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        compassSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        //showing logo screen
        showLogo();

        //check Permissions
        checkPermission();

        //initializing all widgets
        initializeViews();

        //
        reset();

    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_CODE);
            return;
        }else{
            // permission granted
            Toast.makeText(this, "Permission Already Granted", Toast.LENGTH_SHORT).show();
            checkLocationSettings();
        }
    }

    private void checkLocationSettings() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(MAX_INTERVAL);
        mLocationRequest.setFastestInterval(MIN_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder settingsBuilder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        settingsBuilder.setAlwaysShow(true);

        Task<LocationSettingsResponse> result = LocationServices.getSettingsClient(this)
                .checkLocationSettings(settingsBuilder.build());

        result.addOnCompleteListener(new OnCompleteListener<LocationSettingsResponse>() {
            @Override
            public void onComplete(@NonNull Task<LocationSettingsResponse> task) {
                try {
                    LocationSettingsResponse response =
                            task.getResult(ApiException.class);
                } catch (ApiException ex) {
                    switch (ex.getStatusCode()) {
                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                            try {
                                ResolvableApiException resolvableApiException =
                                        (ResolvableApiException) ex;
                                resolvableApiException
                                        .startResolutionForResult(MainActivity.this, 1);
                            } catch (IntentSender.SendIntentException e) {

                            }
                            break;
                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:

                            break;
                    }
                }
            }
        });
    }

    private void initializeViews() {

        startTimeTV = findViewById(R.id.starttimetv);
        endTimeTV = findViewById(R.id.endtimetv);
        distanceTV = findViewById(R.id.distancetv);
        altitudeTV = findViewById(R.id.altitudetv);
        headingTV = findViewById(R.id.headingtv);
        avgSpeedTV = findViewById(R.id.avgspeedtv);
        maxSpeedTV = findViewById(R.id.maxspeedtv);

        speedTV = findViewById(R.id.speedTV);

        durationTV = findViewById(R.id.durationtv);
        durationTV.setText("00:00:00");
        durationTV.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
            @Override
            public void onChronometerTick(Chronometer chronometer) {

                if (status == 1)
                {
                    elapsedSeconds++;

                    long time = SystemClock.elapsedRealtime() - chronometer.getBase();
                    int h   = (int)(time /3600000);
                    int m = (int)(time - h*3600000)/60000;
                    int s= (int)(time - h*3600000- m*60000)/1000 ;
                    String t = (h < 10 ? "0"+h: h)+":"+(m < 10 ? "0"+m: m)+":"+ (s < 10 ? "0"+s: s);
                    chronometer.setText(t);
                }

            }
        });

        startBT = findViewById(R.id.startbt);
        startBT.setOnClickListener(this);
        stopBT = findViewById(R.id.stopbt);
        stopBT.setOnClickListener(this);
    }

    private void displayResult(String result[]) {
        String _dist = result[0] + " KM";
        String _speed = result[1] + " KM/H";
        String _maxSpeed = result[2] + " KM/H";
        String _altitude = result[3] + " M";
        String _gpsProvider = result[5];

        if (_gpsProvider != null && _gpsProvider.equalsIgnoreCase("false"))
        {
            checkLocationSettings();
        }
        else {
            distanceTV.setText(_dist);
            avgSpeedTV.setText(_speed);
            maxSpeedTV.setText(_maxSpeed);
            altitudeTV.setText(_altitude);
            Log.d(TAG, "displayResult: "+_dist+"-"+_speed+"-"+_maxSpeed);
        }
    }

    private String calculateSpeed() {

        double distance = Double.parseDouble(distanceTV.getText().toString().trim().split(" ")[0]);
        String _duration = durationTV.getText().toString().trim();
        String split[] = _duration.split(":");
        if (split.length == 3)
        {
            int hour = Integer.parseInt(split[0]);
            int minute = Integer.parseInt(split[1]);
            int seconds = Integer.parseInt(split[2]);

            distance = distance * 1000;
            long totalSeconds = seconds + (minute*60) + (hour*60*60);
            double speed = distance / totalSeconds;
            speed = speed * 18 / 5;

            return new DecimalFormat("#.##").format(speed) + " KM/H";
        }
        return "-";
    }

    @Override
    protected void onPause() {
        super.onPause();
        appStatus = 0;
        sensorManager.unregisterListener(this, compassSensor);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appStatus = 1;
        if (status == 1) {
            sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.restore:
                reset();
                break;
        }
        return true;
    }

    private void reset() {

        startBT.setText("Start");
        status = 0;
        elapsedTime = durationTV.getBase() - SystemClock.elapsedRealtime();
        durationTV.stop();

        durationTV.setText("-");
        distanceTV.setText("-");
        startTimeTV.setText("-");
        endTimeTV.setText("-");
        maxSpeedTV.setText("-");
        avgSpeedTV.setText("-");
        altitudeTV.setText("-");
        headingTV.setText("-");

        speedTV.setText(getString(R.string.speed));
        sensorManager.unregisterListener(this, compassSensor);
    }

    private void showLogo() {
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new LogoFragment()).addToBackStack("logo").commit();
    }

    @Override
    public void onClick(View view) {

        switch (view.getId())
        {
            case R.id.startbt:
                if (startBT.getText().toString().trim().equalsIgnoreCase("Start"))
                {
                    //change text to pause & set status = 1

                    if (status == 2)
                    {
                        //resuming from pause state
                        durationTV.setBase(SystemClock.elapsedRealtime()+elapsedTime);
                        durationTV.start();
                    }
                    else {
                        //running for first time
                        durationTV.setBase(SystemClock.elapsedRealtime());
                        durationTV.start();
                        startServize(new LocationResultReceiver(this));

                        sensorManager.registerListener(this, compassSensor, SensorManager.SENSOR_DELAY_NORMAL);
                    }

                    startBT.setText("Pause");
                    status = 1;
                    String time = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());
                    startTimeTV.setText(time);
                    endTimeTV.setText("-");

                }
                else {
                    //change text to pause & set status = 2
                    startBT.setText("Start");
                    status = 2;
                    elapsedTime = durationTV.getBase() - SystemClock.elapsedRealtime();
                    durationTV.stop();
                }
                break;
            case R.id.stopbt:
                if (status == 1) {
                    String speed = calculateSpeed();
                    if (!speed.equalsIgnoreCase("-"))
                    {
                        speedTV.setText(getString(R.string.app_name));
                        avgSpeedTV.setText(speed);
                    }

                    startBT.setText("Start");
                    status = 3;
                    String time = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH).format(new Date());
                    endTimeTV.setText(time);
                    durationTV.stop();
                    elapsedTime = 0;
                    stopServize();

                    sensorManager.unregisterListener(this, compassSensor);
                }
                break;
        }
    }

    private void stopServize() {
        Intent intent = new Intent(this, LocationService.class);
        stopService(intent);
    }

    private void startServize(LocResultReceiver.ResultReceiverCallBack resultReceiverCallBack) {
        LocResultReceiver locResultReceiver = new LocResultReceiver(new Handler(getMainLooper()));
        locResultReceiver.setReceiver(resultReceiverCallBack);
        Intent intent = new Intent(this, LocationService.class);
        intent.setAction(ACTION);
        intent.putExtra(RECEIVER, locResultReceiver);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        }
        else {
            startService(intent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQ_CODE)
        {
            // If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            } else {
                // permission denied, boo! Disable the
                // functionality that depends on this permission.
                Toast.makeText(this, "App can't function without Location services, Please Allow", Toast.LENGTH_SHORT).show();
                checkPermission();
            }
            return;
        }

    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        float result = 0.0f;
        if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION)
        {
            float degree = Math.round(sensorEvent.values[0]);
            String direction = null;
            if (degree > 0 && degree < 90)
            {
                direction = "N";
                if (degree > 30 && degree < 60)
                {
                    direction = "NE";
                }
            }
            else if (degree > 90 && degree <180)
            {
                direction = "E";
                if (degree > 120 && degree < 150)
                {
                    direction = "SE";
                }
            }
            else if ((degree > 180 && degree <270))
            {
                direction = "S";
                if (degree > 210 && degree < 240)
                {
                    direction = "SW";
                }
            }
            else {
                direction = "W";
                if (degree > 300 && degree < 330)
                {
                    direction = "NW";
                }
            }
            headingTV.setText(degree + " \u00b0" + " " + direction);
        }
        if (sensorEvent.sensor.getType() == Sensor.TYPE_PRESSURE)
        {
            result = sensorEvent.values[0];
            altitudeTV.setText(result+"");
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private static class LocationResultReceiver implements LocResultReceiver.ResultReceiverCallBack<String[]>{

        private WeakReference<MainActivity> activityRef;
        String[] result;

        public LocationResultReceiver(MainActivity activity) {
            activityRef = new WeakReference<MainActivity>(activity);
        }

        @Override
        public void onSuccess(String[] data) {
            if(activityRef != null && activityRef.get() != null) {
                activityRef.get().displayResult(data);
            }
        }

        @Override
        public void onError(Exception exception) {
            Log.d(TAG, exception.toString());
        }
    }


}

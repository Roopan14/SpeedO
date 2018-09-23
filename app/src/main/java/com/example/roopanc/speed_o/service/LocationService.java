package com.example.roopanc.speed_o.service;

import android.Manifest;
import android.app.IntentService;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.roopanc.speed_o.MainActivity;
import com.example.roopanc.speed_o.receiver.LocResultReceiver;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.DecimalFormat;

/**
 * Created by Roopan C on 9/21/2018.
 */

public class LocationService extends Service implements LocationListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    public static final String TAG = LocationService.class.getSimpleName();

    private static String ACTION = "requestLocation";
    private static String STOP = "stopService";
    private static String RECEIVER = "locResultReceiver";
    private static final long MAX_INTERVAL = 1000 * 2;
    private static final long MIN_INTERVAL = 1000 * 1;
    String _gpsProvider;
    ResultReceiver resultReceiver;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    LocationCallback locationCallback;
    LocationManager locationManager;
    Location mCurrentLocation, startLocation, endLocation;
    static double distance = 0;
    double altitude, heading, speed, maxSpeed = 0.0;

    public LocationService() {
    }


    @Override
    public void onCreate() {
        super.onCreate();

        int NOTIFICATION_ID = (int) (System.currentTimeMillis()%10000);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(NOTIFICATION_ID, new Notification.Builder(this).build());
        }
    }

    private void initializeRequest() {

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(MAX_INTERVAL);
        mLocationRequest.setFastestInterval(MIN_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String _action = intent.getAction();
            if (_action.equalsIgnoreCase(ACTION))
            {
                Log.d("handleIntent", _action);
                resultReceiver = intent.getParcelableExtra(RECEIVER);
                initializeRequest();
            }
            else {
                stopSelf();
            }

        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
        if (mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        startLocation = null;
        endLocation = null;
        distance = 0;
        speed = 0.0;
        maxSpeed = 0.0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {

        if (MainActivity.status == 1) {
            Log.d(TAG, "locchanged");
            mCurrentLocation = location;
            altitude = mCurrentLocation.getAltitude();
            heading = mCurrentLocation.getBearing();
            if (startLocation == null) {
                startLocation = mCurrentLocation;
                endLocation = mCurrentLocation;
            } else {
                endLocation = mCurrentLocation;
            }

            //calculating the speed with getSpeed method it returns speed in m/s so we are converting it into kmph
            speed = location.getSpeed() * 18 / 5;
            if (speed > maxSpeed) {
                maxSpeed = speed;
            }
            distance = distance + (startLocation.distanceTo(endLocation) / 1000.00);
            startLocation = endLocation;

            //publish the result to ResultReceiver
            publishResult(resultReceiver, distance, speed, maxSpeed, altitude, heading);
        }
    }

    private void publishResult(ResultReceiver resultReceiver, double distance, double speed, double maxSpeed, double altitude, double heading) {

        if (MainActivity.appStatus == 1 && MainActivity.status == 1)
        {
            String _dist = new DecimalFormat("#.##").format(distance);
            String _speed = new DecimalFormat("#.##").format(speed);
            String _maxspeed = new DecimalFormat("#.##").format(maxSpeed);
            String _altitude = new DecimalFormat("#.##").format(altitude);
            String _heading = new DecimalFormat("#.##").format(heading);
            String result[] = {_dist, _speed, _maxspeed, _altitude, _heading, _gpsProvider};
            callReceiverResult(resultReceiver, result);
        }
    }

    private void callReceiverResult(ResultReceiver resultReceiver, String[] result) {
        Bundle bundle = new Bundle();
        int code = LocResultReceiver.RESULT_CODE_OK;
        bundle.putSerializable(LocResultReceiver.PARAM_RESULT, result);
        if (resultReceiver != null)
        {
            resultReceiver.send(code, bundle);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {
        _gpsProvider = "true";
    }

    @Override
    public void onProviderDisabled(String s) {
        _gpsProvider = "false";
        String result[] = {null, null, null, null, null, _gpsProvider};
        callReceiverResult(resultReceiver, result);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Toast.makeText(this, "Locations - Permission Not Granted", Toast.LENGTH_SHORT).show();
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAX_INTERVAL, 1, this);

        /*LocationServices.getFusedLocationProviderClient(getApplicationContext()).requestLocationUpdates(mLocationRequest, new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
            }
        }, null);*/

        /*try {
            LocationServices.FusedLocationApi
                    .requestLocationUpdates(mGoogleApiClient, mLocationRequest, (com.google.android.gms.location.LocationListener) this);
        } catch (SecurityException e) {

        }*/

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    protected void stopLocationUpdates() {
        /*LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, (com.google.android.gms.location.LocationListener) this);
        distance = 0;*/
        locationManager.removeUpdates(this);
    }


}

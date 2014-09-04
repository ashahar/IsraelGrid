package com.example.teamb.israelgrid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;


public class GPSItmActivity extends Activity {

    private TextView mTextViewLatitude;
    private TextView mTextViewLongitude;
    private TextView mTextViewSpeed;
    private TextView mTextViewITM;
    private TextView mTextViewAccuracy;
    private ImageView mCompassImage;

    static final int MIN_UPDATE_TIME = 500;
    static final int MIN_UPDATE_DISTANCE = 0;
    static final String TAG="GEOITM";
    private float mDegrees=0;
    private LocationManager mLocationManager;
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagneticField;
    private boolean hasCompass = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_goeitm);

        mTextViewLatitude = (TextView)findViewById(R.id.TextView_Latitude);
        mTextViewLongitude = (TextView)findViewById(R.id.TextView_Longitude);
        mTextViewSpeed = (TextView)findViewById(R.id.TextView_Speed);
        mTextViewITM = (TextView)findViewById(R.id.TextView_ITM);
        mTextViewAccuracy = (TextView)findViewById(R.id.TextView_Accuracy);
        mCompassImage = (ImageView)findViewById(R.id.ImageView_Compass);

        mLocationManager = (LocationManager)this.getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (mMagneticField!=null && mAccelerometer!=null) {
            hasCompass = true;
        } else {
            hasCompass = false;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.goeitm, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.removeUpdates(mLocationListener);
        if (hasCompass) {
            mSensorManager.unregisterListener(mSensorEventListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                MIN_UPDATE_TIME ,
                MIN_UPDATE_DISTANCE,
                mLocationListener);
        if (hasCompass) {
            mSensorManager.registerListener(mSensorEventListener, mMagneticField, SensorManager.SENSOR_DELAY_UI);
            mSensorManager.registerListener(mSensorEventListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        resetFields();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetFields() {
        mTextViewLatitude.setText(R.string.location_unavailable);
        mTextViewLongitude.setText(R.string.location_unavailable);
        mTextViewSpeed.setText(R.string.location_unavailable);
        mTextViewITM.setText(R.string.location_unavailable);
        mTextViewAccuracy.setText(R.string.location_unavailable);
        rotateImage(0);
    }

    private void updateLocation(Location location) {
        double lat = location.getLatitude();
        double lng = location.getLongitude();

        mTextViewLatitude.setText(Location.convert(lat, Location.FORMAT_SECONDS));
        mTextViewLongitude.setText(Location.convert(lng, Location.FORMAT_SECONDS));

        long[] itm = new long[2];
        GeoUtils.GeoToITM(location.getLatitude(), location.getLongitude(), itm);
        mTextViewITM.setText(itm[1] + "N " + itm[0]+ "E");

        if (location.hasAccuracy()) {
            mTextViewAccuracy.setText(String.format("%.2fm", location.getAccuracy()));
        } else {
            mTextViewAccuracy.setText(R.string.location_unavailable);
        }

        String speed = getResources().getText(R.string.location_unavailable).toString();
        if (location.hasSpeed()) {
            speed = String.format("%dkm/h",Math.round(location.getSpeed()*3.6));
        }

        if (location.hasBearing()) {
            if (!hasCompass) {
                rotateImage(location.getBearing());
            }
        }
        mTextViewSpeed.setText(speed);
    }

    private void rotateImage(float degrees) {
        RotateAnimation ra = new RotateAnimation(-mDegrees, -degrees,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        ra.setDuration(250);
        ra.setFillAfter(true);
        mCompassImage.startAnimation(ra);
        mDegrees = degrees;
    }

    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            updateLocation(location);
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };


    SensorEventListener mSensorEventListener = new SensorEventListener() {

        private float[] mGravity = null;
        private float[] mGeomagnetic = null;

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                mGravity = sensorEvent.values;
            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagnetic = sensorEvent.values;
            }

            if (mGravity != null && mGeomagnetic != null) {
                float R[] = new float[9];
                float I[] = new float[9];
                boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
                if (success) {
                    float orientation[] = new float[3];
                    // orientation contains: azimuth, pitch and roll
                    SensorManager.getOrientation(R, orientation);
                    rotateImage((float) Math.toDegrees(orientation[0]));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

}

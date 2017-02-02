package com.example.zyr.project_demo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Service class
 */

public class PostService extends Service {

    private static float sensorValue;
    //private static final String url_yue = "http://104.236.126.112/api/user/yue";
    private static final String url_yiran = "http://104.236.126.112/api/user/yiran";
    private NetworkUtility mNetworkUtility = new NetworkUtility();
    private SensorEventListener msensorEventListener;
    private SensorManager msensorManager;
    private Sensor mlight;

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }
    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("PostService", "onCreate excuted");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Log.d("PostService", "onStartCommand excuted");

        final String readData = readData();
        final String url_user = "http://" + readData;
        new Thread(new Runnable() {
            @Override
            public void run() {
                //get sensor data
                msensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
                mlight = msensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                msensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        float value = event.values[0];
                        sensorValue = value;
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                };
                msensorManager.registerListener(msensorEventListener,mlight,SensorManager.SENSOR_DELAY_GAME);
                //Post data
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss", Locale.ENGLISH);
                String currentTime = dateFormat.format(new Date());
                mNetworkUtility.sendPostHttpRequest(url_user, currentTime, sensorValue, new HttpCallbackListener() {
                    @Override
                    public void onFinish(String response, Message message) {

                    }
                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });
                stopSelf();
            }
        }).start();
        //Using Alarm to make sure the service runs continuously
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int doEveryTime = 5000; //3000ms
        long triggerAtTime = SystemClock.elapsedRealtime() + doEveryTime;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i ,0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        msensorManager.unregisterListener(msensorEventListener,mlight);
        msensorEventListener = null;
        Log.d("PostService", "onDestroy excuted");
    }
    //read data from sharedpreference file
    private String readData(){
        SharedPreferences pref = getSharedPreferences("UserName", MODE_PRIVATE);
        String data = pref.getString("username","");
        return data;
    }
}

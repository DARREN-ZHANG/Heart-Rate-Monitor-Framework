package com.example.zyr.project_demo;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Service class
 */

public class PostService extends Service {

    private static float sensorValue;
    private static float thresholdInService;
    private static int flag = 0;
    private static String url_user;
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
    public int onStartCommand(Intent intent, final int flags, int startId){
        Log.d("PostService", "onStartCommand excuted");
        MyDatabaseHelper myDB = new MyDatabaseHelper(this, "yiran.db", null, 1);
        final SQLiteDatabase myDBInstance = myDB.getWritableDatabase();
        final String readData = readData();
        url_user = "http://" + readData;
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
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
                        if(thresholdInService <=  value){
                            flag = 1;
                        }
                        else{
                            flag = 0;
                        }
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                };
                msensorManager.registerListener(msensorEventListener,mlight,SensorManager.SENSOR_DELAY_GAME);
                switch (flag){
                    case 1:
                        postData();
                        insert("userData", myDBInstance, sensorValue);
                        break;
                    case 0:
                        insert("userData", myDBInstance, sensorValue);
                        break;
                    default:
                        break;
                }
               stopSelf();
            }
        },1000);
        setAlarm();
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
        return pref.getString("username","");
    }

    private void setAlarm(){
        //Using Alarm to make sure the service runs continuously
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int doEveryTime = 5000; //5000ms
        long triggerAtTime = SystemClock.elapsedRealtime() + doEveryTime;
        Intent i = new Intent(this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(this, 0, i ,0);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
    }

    private void postData(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ENGLISH);
        String currentTime = dateFormat.format(Calendar.getInstance().getTime());
        mNetworkUtility.sendPostHttpRequest(url_user, currentTime, sensorValue, new HttpCallbackListener() {
            @Override
            public void onFinish(String response, Message message) {

            }
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void insert(String TableName, SQLiteDatabase db, float value) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
        String currentTime = dateFormat.format(Calendar.getInstance().getTime());
        ContentValues values = new ContentValues();
        values.put("time", currentTime);
        values.put("value", value);
        db.insert(TableName, null, values);
    }

    public void setThresholdInService(float value){
        thresholdInService = value;
        System.out.println("thresholdInService is :" + thresholdInService);
    }

}

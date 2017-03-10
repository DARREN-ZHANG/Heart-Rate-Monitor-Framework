package com.example.zyr.project_demo;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Service class. Upload the data immediately if value is larger than a threshold,
 * Otherwise save it into the local database.
 *
 * A more reasonable structure to realize the uploading function could be use this class
 * as a monitor service(at low sampling rate) and start another service for uploading
 * (Since there is not a perfect way to upload real time data for a long time,
 * this implementation only supports about 30 mins[determined by the loop condition i < ?] uploading at 5 HZ theoretically,
 * and less time when the sampling rate raises)
 */

public class PostService extends Service {

    private static float sensorValue;
    private static float thresholdInService;
    private static int flag = 0;
    private static String url_user_temp;
    private static String setHour;
    private static String setMin;
    private NetworkUtility mNetworkUtility = new NetworkUtility();
    private SensorEventListener msensorEventListener;
    private SensorManager msensorManager;
    private Sensor mlight;

    private static JSONArray jsonArray;
    private static int count;

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
    public int onStartCommand(Intent intent, final int flags, int startId) {
        Log.d("PostService", "onStartCommand excuted");
        //Set the service as a foreground service
        Intent notificationIntent = new Intent(this, GraphActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new Notification.Builder(this)
                .setContentTitle("Heart Rate Monitoring")
                .setContentText("View your data")
                .setTicker("Heart Rate Monitor")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);

        jsonArray = new JSONArray();
        final String readData = readData();
        String DBName = readData + ".db";
        final MyDatabaseHelper myDB = new MyDatabaseHelper(this, DBName, null, 1);
        url_user_temp = "http://104.236.126.112/api/user/" + readData + "/temp";

        final int Hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        final int Minute = Calendar.getInstance().get(Calendar.MINUTE);
        final Handler handler = new Handler();
        msensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
        msensorManager.registerListener(msensorEventListener, mlight, SensorManager.SENSOR_DELAY_NORMAL);
        for(int i = 0; i < 12000; i++ ) {//Current Set is 5Hz, set by the delay time of each execution
            //if the delay time is 200ms, frequency is 5Hz. The Frequency is an uploading frequency and has noting to do with the
            //light sensor accuracy(which is also 5Hz at the value of SENSOR_DELAY_NORMAL below)
            //if using while loop, the uploading rate can not be controlled
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (thresholdInService <= sensorValue) {
                        flag = 1;
                    } else {
                        flag = 0;
                    }
                    switch (flag) {
                        case 1:
                            postData(url_user_temp);
                            insert("userData", myDB, sensorValue);
                            System.out.println("case 1 executed");
                            break;
                        case 0:
                            insert("userData", myDB, sensorValue);
                            addDataToJson();
                            System.out.println("case 0 executed " + count++ + " times");
                            break;
                        default:
                            break;
                    }
                    //stopSelf();
                    if (Hour == getHour() && Minute == getMin()) {
                        askUserToUpload();
                    }
                }
            }, 100 * i);
        }
        return super.onStartCommand(intent, flags, startId);

        /*
        //this is the solution for the highest sampling rate for any device, it may varies from devices

        msensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
                    msensorManager.registerListener(msensorEventListener, mlight, SensorManager.SENSOR_DELAY_GAME);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    //get sensor data
                    if (thresholdInService <= sensorValue) {
                                flag = 1;
                            } else {
                                flag = 0;
                            }
                    switch (flag) {
                        case 1:
                            postData(url_user_temp);
                            insert("userData", myDB, sensorValue);
                            //System.out.println("case 1 executed");
                            break;
                        case 0:
                            insert("userData", myDB, sensorValue);
                            //System.out.println("case 0 executed");
                            break;
                        default:
                            break;
                    }
                    //stopSelf();
                    if (Hour == getHour() && Minute == getMin()) {
                        askUserToUpload();
                    }
                }
            }
        }).start();
        //setAlarm();
         return super.onStartCommand(intent, flags, startId);
        */
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopForeground(true);
        msensorManager.unregisterListener(msensorEventListener,mlight);
        msensorEventListener = null;
        Log.d("PostService", "onDestroy excuted");
    }

    private String getCurrentTimeMilli(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.US);
        String currentTime = dateFormat.format(Calendar.getInstance().getTime());
        return currentTime;
    }
    private String getCurrentTimeMicro(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",Locale.ENGLISH);//millisecond accuracy
        String currentTime = dateFormat.format(Calendar.getInstance().getTime()) + "000";//fake microsecond, to satisfied server side formation requirement
        return currentTime;
    }

    private void addDataToJson(){
        String time = getCurrentTimeMicro();
        float value = sensorValue;
        JSONObject obj = new JSONObject();
        try {
            obj.put("time", time);
            obj.put("value", value);
            jsonArray.put(obj);
            //System.out.println("in addDataToJson : " + jsonArray);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public JSONObject getJsonData(){
        JSONObject data = new JSONObject();
        try {
            data.put("data", jsonArray);
        }catch (Exception e){
            e.printStackTrace();
        }
        return data;
    }

    public void clearJsonArray(){
        /*
        for(int i = 0; i< jsonArray.length(); i++){
            try{
                JSONObject obj = jsonArray.getJSONObject(i);
                obj.remove("time");
                obj.remove("value");
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        */
        jsonArray = new JSONArray();
        //System.out.println("in clearJsonArray : " + jsonArray);
    }

    //read data from sharedpreference file
    private String readData(){
        SharedPreferences pref = getSharedPreferences("UserName", MODE_PRIVATE);
        return pref.getString("username","");
    }

    public void setThresholdInService(float value){
        thresholdInService = value;
        System.out.println("thresholdInService is :" + thresholdInService);
    }

    public void setSetHour(String hour){
        setHour = hour;
    }
    public void setSetMin(String min){
        setMin = min;
    }

    private int getHour(){
        return Integer.valueOf(setHour);
    }
    private int getMin(){
        return Integer.valueOf(setMin);
    }

    //method is used for immediately upload
    private void postData(String url){
        String currentTime = getCurrentTimeMicro();
        mNetworkUtility.sendPostHttpRequest(url, currentTime, sensorValue, new HttpCallbackListener() {
            @Override
            public void onFinish(String response, Message message) {

            }
            @Override
            public void onError(Exception e) {
                e.printStackTrace();
            }
        });
    }

    //insert a record into table
    private void insert(String TableName, MyDatabaseHelper dbHelper, float value) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String currentTime = getCurrentTimeMilli();
        ContentValues values = new ContentValues();
        values.put("time", currentTime);
        values.put("value", value);
        db.insert(TableName, null, values);
        db.close();
    }

    private void askUserToUpload(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Do you want to Upload today's data to the server now?");
        builder.setMessage("Continue Upload");
        builder.setPositiveButton("Upload", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Uploading", Toast.LENGTH_SHORT).show();
                //check network state and upload
                Intent intent = new Intent(PostService.this, ShowDBDataActivity.class);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Upload canceled", Toast.LENGTH_SHORT).show();
            }
        });
        builder.create();
        builder.show();
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
    //This method is for testing
    public void setCount(int data){
        count = data;
    }
}

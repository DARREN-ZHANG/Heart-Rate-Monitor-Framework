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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Service class
 * Upload the data in every 5s if the value is larger than a threshold,
 * otherwise save it into the local database.
 */

public class PostService extends Service {

    private static float sensorValue;
    private static float thresholdInService;
    private static String hourOfDay;
    private static String minOfHour;
    private static int flag = 0;
    private static String url_user_temp;
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
    public int onStartCommand(Intent intent, final int flags, int startId) {
        Log.d("PostService", "onStartCommand excuted");

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

        final String readData = readData();
        String DBName = readData + ".db";
        final MyDatabaseHelper myDB = new MyDatabaseHelper(this, DBName, null, 1);
        url_user_temp= "http://104.236.126.112/api/user/" + readData + "/temp";
        url_user = "http://104.236.126.112/api/user/" + readData;
        //System.out.println("url_user in PostService is : " + url_user_temp);

        final int Hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        final int Minute = Calendar.getInstance().get(Calendar.MINUTE);
        final Handler handler = new Handler();
        for (int i = 1; i <= 10000; i++){
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //get sensor data
                    msensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                    mlight = msensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
                    msensorEventListener = new SensorEventListener() {
                        @Override
                        public void onSensorChanged(SensorEvent event) {
                            float value = event.values[0];
                            sensorValue = value;
                            if (thresholdInService <= value) {
                                flag = 1;
                            } else {
                                flag = 0;
                            }
                        }

                        @Override
                        public void onAccuracyChanged(Sensor sensor, int accuracy) {

                        }
                    };
                    msensorManager.registerListener(msensorEventListener, mlight, SensorManager.SENSOR_DELAY_GAME);
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
                    if (Hour == 23 && Minute == 59) {
                        askUserToUpload();
                    }
                }
            }, 10000 * i);
        }

        //setAlarm();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopForeground(true);
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

    //method is used for immediately upload
    private void postData(String url){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.ENGLISH);
        String currentTime = dateFormat.format(Calendar.getInstance().getTime());
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

    private void insert(String TableName, MyDatabaseHelper dbHelper, float value) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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


    public void setHourOfDay(String hour){
        hourOfDay = hour;
    }

    public void setMinOfHour(String minute){
        minOfHour = minute;
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

}

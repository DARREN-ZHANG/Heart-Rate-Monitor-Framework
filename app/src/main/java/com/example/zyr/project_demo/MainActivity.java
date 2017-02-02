package com.example.zyr.project_demo;

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
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    //Network related vars
    private NetworkUtility mNetworkUtility;
    private static final String url_yue = "http://104.236.126.112/api/user/yue";
    private static final String url_yiran = "http://104.236.126.112/api/user/yiran";
    private static String url_user;
    //Sensor related vars
    private static final int SHOW_RESPONSE = 0;
    private SensorManager msensorManager;
    private Sensor mlight;
    private SensorEventListener msensorEventListener;
    private StringBuffer viewBuffer;
    public float sensorValue;
    //other vars
    private MyDatabaseHelper myDatabaseHelper;
    private static float threshold;
    //UI related vars
    private TextView showData;
    private EditText thresholdText;
    private TextView showResponse;
    public Handler handler = new Handler(){
        public void handleMessage(Message msg){
            switch (msg.what){
                case SHOW_RESPONSE:
                    String response = (String) msg.obj;
                    showResponse = (TextView)findViewById(R.id.network_response);
                    showResponse.setText(response);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNetworkUtility = new NetworkUtility();
        threshold = 500;

        Intent intent = getIntent();
        url_user = intent.getStringExtra("URL_user");
        saveData(url_user);
        final String url_data = readData();

        myDatabaseHelper = new MyDatabaseHelper(this, "yiran.db", null, 1);
        myDatabaseHelper.getWritableDatabase();
        /*
        String databaseName = url_data + ".db";
        myDatabaseHelper = new MyDatabaseHelper(this,databaseName,null,1);
        SQLiteDatabase myDB= myDatabaseHelper.getWritableDatabase();
        */

        //layout init
        Button mDrawButton;
        Button mGetButton;
        Button mPostButton;
        Button mApplyButton;

        mDrawButton = (Button)findViewById(R.id.draw_button);
        mGetButton = (Button)findViewById(R.id.get_button);
        mPostButton = (Button)findViewById(R.id.post_button);
        mApplyButton = (Button)findViewById(R.id.apply_button);

        showData = (TextView)findViewById(R.id.data_tv);
        thresholdText = (EditText)findViewById(R.id.threshold_EditText);

        viewBuffer = new StringBuffer();
        //sensor init
        msensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        mlight = msensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        msensorManager.registerListener(msensorEventListener,mlight,SensorManager.SENSOR_DELAY_NORMAL);
        msensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float acc = event.accuracy;
                float value = event.values[0];

                viewBuffer.append("accurancy:" + acc);
                viewBuffer.append("\n");
                viewBuffer.append("light level:" + value);
                viewBuffer.append("\n");
                showData.setText(viewBuffer.toString());

                sensorValue = value;

                //always show new data in the bottom of textview
                showData.setMovementMethod(ScrollingMovementMethod.getInstance());
                int offset = showData.getLineCount() * showData.getLineHeight();
                if (offset > showData.getHeight()) {
                    showData.scrollTo(0, offset - showData.getHeight());
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        //Button responses
        mDrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent drawIntent = new Intent(MainActivity.this, GraphActivity.class);
                startActivity(drawIntent);
            }
        });
        mGetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNetworkUtility.sendGetHttpRequest(url_yue, new HttpCallbackListener() {
                    @Override
                    public void onFinish(String response,Message message) {
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });

            }
        });
        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent startIntent = new Intent(MainActivity.this, PostService.class);
                startIntent.putExtra("URL_user_to_service",url_data);
                startService(startIntent);
                Toast.makeText(getApplicationContext(), "Post Service Initiated", Toast.LENGTH_SHORT).show();
            }
        });

        mApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tmp = thresholdText.getText().toString();
                threshold = Float.valueOf(tmp);
                Toast.makeText(getApplicationContext(),"Threshold is been set as" + tmp, Toast.LENGTH_SHORT).show();
            }
        });

        if (threshold > sensorValue){
            addtoDB("userData", myDatabaseHelper, sensorValue);
            //stop post service first,stop the alarm and the service
            //save for post later
        }
        else {
            addtoDB("userData", myDatabaseHelper, sensorValue);
            //post directly
            /*
            Intent startIntent = new Intent(MainActivity.this, PostService.class);
            startIntent.putExtra("URL_user_to_service",url_data);
            startService(startIntent);
            System.out.println("Post Start");
            */
        }
    }

    @Override
    protected void onResume() {
        // Register a listener for the sensor.
        super.onResume();
        msensorManager.registerListener(msensorEventListener, mlight, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        // Be sure to unregister the sensor when the activity pauses.
        super.onPause();
        msensorManager.unregisterListener(msensorEventListener, mlight);
    }

    @Override
    protected void onDestroy() {
        MainActivity.this.stopService(new Intent(MainActivity.this, PostService.class));
        super.onDestroy();
    }

    private void saveData(String data){
        SharedPreferences.Editor editor = getSharedPreferences("UserName", MODE_PRIVATE).edit();
        editor.putString("username", data);
        editor.commit();
    }
    //read data from sharedpreference file
    private String readData(){
        SharedPreferences pref = getSharedPreferences("UserName", MODE_PRIVATE);
        String data = pref.getString("username","");
        return data;
    }
    private void addtoDB(String TableName, MyDatabaseHelper dbHelper, float value){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try{
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss", Locale.ENGLISH);
            String currentTime = dateFormat.format(new Date());
            ContentValues values = new ContentValues();
            values.put("time", currentTime);
            values.put("value", value);
            db.insert(TableName, null, values);
            values.clear();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {
            db.endTransaction();
        }
    }

}

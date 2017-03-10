package com.example.zyr.project_demo;

import android.app.AlarmManager;
import android.app.PendingIntent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

public class MainActivity extends AppCompatActivity {
    //Network related vars
    private NetworkUtility mNetworkUtility;
    private static String user_name;
    private static String url_user;
    private PostService mService;
    //Sensor related vars
    private static final int SHOW_RESPONSE = 0;
    private SensorManager msensorManager;
    private Sensor mlight;
    private SensorEventListener msensorEventListener;
    private StringBuffer viewBuffer;
    //other vars
    private static float threshold;
    //UI related vars
    private TextView showData;
    private EditText thresholdText;
    private TextView showResponse;
    public Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case SHOW_RESPONSE:
                    String response = (String) msg.obj;
                    showResponse = (TextView) findViewById(R.id.network_response);
                    showResponse.setText(response);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        threshold = 500;
        mNetworkUtility = new NetworkUtility();
        mService =new PostService();
        //setup default value for the service
        mService.setThresholdInService(threshold);
        mService.setSetHour("23");
        mService.setSetMin("00");

        Intent intent = getIntent();
        user_name = intent.getStringExtra("UserName");
        saveData(user_name);
        url_user = "http://104.236.126.112/api/user/" + user_name;

        //layout init
        Button mDrawButton;
        Button mApplyButton;
        //Button mStartPollingButton;
        //Button mStopPollingButton;

        mDrawButton = (Button) findViewById(R.id.draw_button);
        mApplyButton = (Button) findViewById(R.id.apply_button);
        //mStartPollingButton = (Button) findViewById(R.id.start_polling_button);
        //mStopPollingButton = (Button) findViewById(R.id.stop_polling_button);

        showData = (TextView) findViewById(R.id.data_tv);
        thresholdText = (EditText) findViewById(R.id.threshold_EditText);

        viewBuffer = new StringBuffer();

        //sensor init
        msensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mlight = msensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        msensorManager.registerListener(msensorEventListener, mlight, SensorManager.SENSOR_DELAY_NORMAL);
        msensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float acc = event.accuracy;
                float value = event.values[0];

                viewBuffer.append("accurancy:" + acc);
                viewBuffer.append("   ");
                viewBuffer.append("light level:" + value);
                viewBuffer.append("\n");
                showData.setText(viewBuffer.toString());

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

        getPostService();

        //Button responses
        mDrawButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent drawIntent = new Intent(MainActivity.this, GraphActivity.class);
                startActivity(drawIntent);
            }
        });

        mApplyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tmp = thresholdText.getText().toString();
                threshold = Float.valueOf(tmp);
                mService.setThresholdInService(threshold);
                Toast.makeText(getApplicationContext(), "Threshold is been set as " + tmp, Toast.LENGTH_SHORT).show();
            }
        });

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

    //save URL in a SharedPreferences file
    private void saveData(String data) {
        SharedPreferences.Editor editor = getSharedPreferences("UserName", MODE_PRIVATE).edit();
        editor.putString("username", data);
        editor.commit();
    }

    //read data from the SharedPreferences file
    private String readData() {
        SharedPreferences pref = getSharedPreferences("UserName", MODE_PRIVATE);
        String data = pref.getString("username", "");
        return data;
    }

    private void getPostService(){
        final String url_data = readData();
        Intent startIntent = new Intent(MainActivity.this, PostService.class);
        startIntent.putExtra("URL_user_to_service", url_data);
        System.out.println("url_user is : " + url_user);
        startService(startIntent);
    }
    //Stop the PostService manually, not used for now,could be used in a test
    private void stopPostService(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent i = new Intent(MainActivity.this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 0, i, 0);
        if (null != pi) {
            alarmManager.cancel(pi);
        }
        stopService(new Intent(MainActivity.this, PostService.class));
    }
}


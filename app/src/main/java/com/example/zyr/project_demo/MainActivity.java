package com.example.zyr.project_demo;

import com.example.zyr.project_demo.NetworkUtility;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import org.w3c.dom.Text;

import static java.lang.Integer.valueOf;

public class MainActivity extends AppCompatActivity {
    private NetworkUtility mNetworkUtility;
    private static final String url_yue = "http://104.236.126.112/api/user/yue";
    private static final String url_yiran = "http://104.236.126.112/api/user/yiran";

    private TextView showData;
    private TextView showResponse;

    private static final int SHOW_RESPONSE = 0;
    private SensorManager msensorManager;
    private Sensor mlight;
    private SensorEventListener msensorEventListener;
    private StringBuffer viewBuffer;
    public float sensorValue;

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

        //layout init
        Button mDrawButton;
        Button mGetButton;
        Button mPostButton;
        Button mStopButton;
        mDrawButton = (Button)findViewById(R.id.draw_button);
        mGetButton = (Button)findViewById(R.id.get_button);
        mPostButton = (Button)findViewById(R.id.post_button);
        mStopButton = (Button)findViewById(R.id.stop_button);
        showData = (TextView)findViewById(R.id.data_tv);

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
                Intent intent = new Intent(MainActivity.this, GraphActivity.class);
                startActivity(intent);
            }
        });
        mGetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mNetworkUtility.sendGetHttpRequest(url_yue, new HttpCallbackListener() {
                    @Override
                    public void onFinish(String response,Message message) {
                        handler.sendMessage(message);
                        //mNetworkUtility.parseJsonwithGson(response);
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
                    /*
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-DD HH:mm:ss", Locale.ENGLISH);
                    String currentTime = dateFormat.format(new Date());
                    mNetworkUtility.sendPostHttpRequest(url_yiran, currentTime, sensorValue, new HttpCallbackListener() {
                    @Override
                    public void onFinish(String response, Message message) {
                        handler.sendMessage(message);
                    }
                    @Override
                    public void onError(Exception e) {
                        e.printStackTrace();
                    }
                });
                    */
                Intent startIntent = new Intent(MainActivity.this, MyService.class);
                startService(startIntent);
            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent stopIntent = new Intent(MainActivity.this, MyService.class);
                stopService(stopIntent);
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

}

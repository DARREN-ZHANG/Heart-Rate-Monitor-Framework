package com.example.zyr.project_demo;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {
    private Button mDrawButton;
    private Button mGetButton;
    private Button mPostButton;
    private Button mStopButton;
    private TextView showData;

    private SensorManager msensorManager;
    private Sensor mlight;
    private SensorEventListener msensorEventListener;
    private StringBuffer viewBuffer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //layout init
        viewBuffer = new StringBuffer();
        mDrawButton = (Button)findViewById(R.id.draw_button);
        mGetButton = (Button)findViewById(R.id.get_button);
        mPostButton = (Button)findViewById(R.id.post_button);
        mStopButton = (Button)findViewById(R.id.stop_button);
        showData = (TextView)findViewById(R.id.data_tv);
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

            }
        });
        mPostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                msensorManager.unregisterListener(msensorEventListener,mlight);
                msensorEventListener = null;
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

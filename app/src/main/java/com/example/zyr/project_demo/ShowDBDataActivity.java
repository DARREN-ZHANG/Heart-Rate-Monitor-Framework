package com.example.zyr.project_demo;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ShowDBDataActivity extends AppCompatActivity {
    private TextView showDataTV;
    private StringBuffer showBuffer;
    private static String url_user;
    private static String DBname;
    private static String startTime ;
    private static String endTime ;
    private static String startValue;
    private static String endValue;
    private static NetworkUtility mNetwork;

    private static EditText startTimeView;
    private static EditText endTimeView;
    private static EditText startValueView;
    private static EditText endValueView;

    private Handler uploadFinishHandler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(),"Upload Completed.",Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_dbdata);

        startTimeView = (EditText)findViewById(R.id.start_time);
        endTimeView = (EditText)findViewById(R.id.stop_time);
        startValueView = (EditText)findViewById(R.id.start_value);
        endValueView = (EditText)findViewById(R.id.end_value);

        Button mqueryViaTimeButton = (Button)findViewById(R.id.query_from_button);
        Button mqueryAllButton = (Button)findViewById(R.id.query_all_button);
        Button muploadButton = (Button)findViewById(R.id.upload_button);
        Button mqueryViaValueButton = (Button)findViewById(R.id.query_via_value);

        showDataTV = (TextView)findViewById(R.id.show_database_data);
        showDataTV.setMovementMethod(ScrollingMovementMethod.getInstance());

        String username = readData();
        url_user = "http://104.236.126.112/api/user/" + username;
        mNetwork = new NetworkUtility();
        DBname = username + ".db";
        final MyDatabaseHelper myDB = new MyDatabaseHelper(ShowDBDataActivity.this, DBname, null, 1);
        showBuffer = new StringBuffer();

        mqueryViaTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTime = startTimeView.getText().toString();
                endTime = endTimeView.getText().toString();
                queryBetweenDate("userData", myDB, startTime, endTime);
            }
        });

        mqueryViaValueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startValue = startValueView.getText().toString();
                endValue = endValueView.getText().toString();
                queryFromValue("userData", myDB, startValue, endValue);
            }
        });

        mqueryAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryall("userData",myDB);
            }
        });

        muploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isNetworkAvailable()){
                    if (isWiFi()){
                        System.out.println("Upload will be initiated immediately using WiFi");
                        //Upload directly
                        //initiateUplaod(myDB);
                    }
                    else if(isMobile()){
                        System.out.println("Mobile Data Connected.");
                        ifUseMobileToUpload(myDB);
                        //Ask user if he/she want to use data to upload
                    }
                }else {
                    //setNetwork
                    setNetwork();
                }
            }
        });
    }

    private void queryBetweenDate(String TableName, MyDatabaseHelper dbHelper, String startTime, String endTime){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        showBuffer.delete(0,showBuffer.length());
        String[] columns = new String[]{
                "time",
                "value"
        };
        String whereClause ="time >= ? AND time <= ?";
        String[] whereArgs = new String[]{
                startTime,
                endTime
        };
        String orderBy = "time";
        Cursor cursor = db.query(TableName,columns,whereClause,whereArgs,null,null,orderBy);
        if (cursor.moveToFirst()){
            do {
                String time = cursor.getString(cursor.getColumnIndex("time"));
                String value = cursor.getString(cursor.getColumnIndex("value"));
                showBuffer.append("Time: "+ time);
                showBuffer.append("  Value: " + value);
                showBuffer.append("\n");
                showDataTV.setText(showBuffer.toString());
            }while(cursor.moveToNext());
        }
        cursor.close();
    }

    private void queryFromValue(String TableName, MyDatabaseHelper dbHelper, String startValue, String endValue){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        showBuffer.delete(0,showBuffer.length());
        String[] columns = new String[]{
                "time",
                "value"
        };
        String whereClause ="value >= ? AND value <= ?";
        String[] whereArgs = new String[]{
                startValue,
                endValue
        };
        String orderBy = "value";
        Cursor cursor = db.query(TableName,columns,whereClause,whereArgs,null,null,orderBy);
        if (cursor.moveToFirst()){
            do {
                String time = cursor.getString(cursor.getColumnIndex("time"));
                String value = cursor.getString(cursor.getColumnIndex("value"));
                showBuffer.append("Time: "+ time);
                showBuffer.append("  Value: " + value);
                showBuffer.append("\n");
                showDataTV.setText(showBuffer.toString());
            }while(cursor.moveToNext());
        }
        cursor.close();
    }

    private void queryall(String TableName, MyDatabaseHelper dbHelper){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        showBuffer.delete(0,showBuffer.length());
        Cursor cursor = db.query(TableName, null, null, null, null, null, null);
        if (cursor.moveToFirst()){
            do {
                String time = cursor.getString(cursor.getColumnIndex("time"));
                float value = cursor.getFloat(cursor.getColumnIndex("value"));
                showBuffer.append("Time: "+ time);
                showBuffer.append("  Value: " + value);
                showBuffer.append("\n");
                showDataTV.setText(showBuffer.toString());
            }while(cursor.moveToNext());
        }
        cursor.close();
    }

    private JSONArray wrapDBDataToJson(String TableName, MyDatabaseHelper dbHelper){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(TableName, null, null, null, null, null, null);
        JSONObject data = new JSONObject();
        JSONArray dataArray = new JSONArray();
        if (cursor.moveToFirst()){
            do {
                String time = cursor.getString(cursor.getColumnIndex("time"));
                //System.out.println("time: " + time);
                float value = cursor.getFloat(cursor.getColumnIndex("value"));
                //System.out.println("value: " + value);
                try{
                    data.put("time", time);
                    data.put("value",value);
                    dataArray.put(data.toString());
                }catch (Exception e){
                    e.printStackTrace();
                }
            }while(cursor.moveToNext());
        }
        cursor.close();
        System.out.println(dataArray.toString());
        return dataArray;
    }

    private void ifUseMobileToUpload(final MyDatabaseHelper dbHelper){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Do you want to use mobile data to upload ?");
        builder.setMessage("Continue upload?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Uploading via Data", Toast.LENGTH_SHORT).show();
                //initiateUplaod(dbHelper);
            }
        });

        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Upload canceled", Toast.LENGTH_SHORT).show();
            }
        });
        builder.create();
        builder.show();
    }

    private void setNetwork(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Please Set Your Network");
        builder.setMessage("Continue?");
        builder.setPositiveButton("Setup", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = null;
                intent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
                startActivity(intent);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Network is not available now", Toast.LENGTH_SHORT).show();
            }
        });
        builder.create();
        builder.show();
    }

    private void initiateUplaod(MyDatabaseHelper dbHelper){
        JSONArray mData = wrapDBDataToJson("userData", dbHelper);
        mNetwork.postWholeTable(url_user, mData, new HttpCallbackListener() {
            @Override
            public void onFinish(String response, Message message) {
                uploadFinishHandler.sendMessage(message);
                //empty database
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }

    private boolean isNetworkAvailable(){
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }

    private boolean isWiFi(){
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isWiFi = activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
        return isWiFi;
    }

    private boolean isMobile(){
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(this.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isMobile = activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE;
        return isMobile;
    }

    private String readData(){
        SharedPreferences pref = getSharedPreferences("UserName", MODE_PRIVATE);
        return pref.getString("username","");
    }


}

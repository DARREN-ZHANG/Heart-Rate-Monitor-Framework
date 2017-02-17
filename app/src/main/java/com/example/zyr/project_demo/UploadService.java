package com.example.zyr.project_demo;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class UploadService extends Service {
    private NetworkUtility mNetwork;
    private String url_user;
    private String DBname;
    private int startIndex = 0;
    private int maxCount = 1000;

    private Handler uploadFinishHandler = new Handler() {
        public void handleMessage(Message msg) {
            Toast.makeText(getApplicationContext(),"Upload Completed.",Toast.LENGTH_SHORT).show();
        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate(){
        super.onCreate();
        Log.d("UploadService", "onCreate excuted");
    }

    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {
        mNetwork = new NetworkUtility();
        String username = readData();
        url_user = "http://104.236.126.112/api/user/" + username;
        DBname = username + ".db";
        final MyDatabaseHelper dpHelper = new MyDatabaseHelper(this, DBname, null, 1);

        new Thread(new Runnable() {
            @Override
            public void run() {
                initiateUpload(dpHelper);
                stopSelf();
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    private JSONObject wrapDBDataToJson(MyDatabaseHelper dbHelper, int startIndex, int maxCount){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //get the total number of the records
        long totalNum = getCount(dbHelper);
        System.out.println("Total Number of Records is : " + totalNum);
        JSONArray dataArray = new JSONArray();
        //do a query for 1000 records at a time and put the data in to the dataArray
        //too many records in one query may cause cursor memory problem
        do {
            Cursor cursor = db.rawQuery("select time,value from userData order by time limit ? offset ?"
                    , new String[]{String.valueOf(maxCount), String.valueOf(startIndex)});
            if (cursor.moveToFirst()){
                do {
                    JSONObject onePeaceOfData = new JSONObject();
                    String time = cursor.getString(cursor.getColumnIndex("time"));
                    float value = cursor.getFloat(cursor.getColumnIndex("value"));
                    try{
                        onePeaceOfData.put("time", time);
                        onePeaceOfData.put("value",value);
                        onePeaceOfData.put("value2", value);
                        dataArray.put(onePeaceOfData);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }while(cursor.moveToNext());
            }
            cursor.close();
            startIndex += maxCount;
        }while(startIndex < totalNum );

        //wrap the dataArray into a jsonObject again for uploading
        JSONObject allData = new JSONObject();
        try{
            allData.put("data", dataArray);
        }catch (Exception ee){
            ee.printStackTrace();
        }
        //System.out.println(allData);
        return allData;
    }

    private void initiateUpload(final MyDatabaseHelper dbHelper){
        JSONObject mData = wrapDBDataToJson(dbHelper, startIndex, maxCount);
        mNetwork.postWholeTable(url_user, mData, new HttpCallbackListener() {
            @Override
            public void onFinish(String response, Message message) {
                uploadFinishHandler.sendMessage(message);
                //empty database after a post success by delete and recreate table userData
                reCreateuserDataTable(dbHelper);
            }

            @Override
            public void onError(Exception e) {

            }
        });
    }
    private void reCreateuserDataTable(MyDatabaseHelper dpHelper){
        SQLiteDatabase db = dpHelper.getWritableDatabase();
        db.delete("userData", null, null);
        final String CREATE_TABLE = "create table userData ("
                + "_id integer primary key autoincrement, "
                + "time text, "
                + "value real)";
        db.execSQL(CREATE_TABLE);
    }

    private Long getCount(MyDatabaseHelper dpHelper){
        SQLiteDatabase db = dpHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("select count(*) from userData", null);
        cursor.moveToNext();
        Long count = cursor.getLong(0);
        cursor.close();
        return count;
    }

    private String readData(){
        SharedPreferences pref = getSharedPreferences("UserName", MODE_PRIVATE);
        return pref.getString("username","");
    }
}

package com.example.zyr.project_demo;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ShowDBDataActivity extends AppCompatActivity {
    //layout related vars
    private StringBuffer showBuffer;
    private static String url_user;
    private static String url_user_temp;
    private static String url_user_search;
    private static String DBname;
    private static String startTime ;
    private static String endTime ;
    private static String startValue;
    private static String endValue;
    private static String setHour;
    private static String setMin;
    //other vars
    private static PostService mService;
    private static NetworkUtility mNetwork;
    private static int flag = 0; //used in calling different loadMore method depends on querying from time or value
    private final static int queryFromTime = 1;
    private final static int queryFromValue = 2;
    private final static int queryFromServer = 3;

    //ListView related vars
    private ArrayAdapter<String> mAdapter;
    private LinearLayout mProgressLayout;
    private ListView mLoadView;
    //Adapter's data source
    private List<String> mDataList;
    //next batch of data
    private List<String> mMoreData;
    private int mStartIndex = 0;
    //numbers of a batch of data
    private int mMaxCount = 10;
    private static int mTotalDataNum = -1;

    //Handle UI About Data Form Server
    private Handler queryFromServerHandler = new Handler(){
        public void handleMessage(Message msg) {
            System.out.println("message is : " + msg);
            MyDatabaseHelper dpHelper = new MyDatabaseHelper(ShowDBDataActivity.this, DBname, null, 1);
            parseJsonAndSaveData(msg.obj.toString() , dpHelper);
            displayDataFromServer();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_dbdata);
        //Basic layout init (except the ListView)
        final EditText startTimeView = (EditText)findViewById(R.id.start_time);
        final EditText endTimeView = (EditText)findViewById(R.id.stop_time);
        final EditText startValueView = (EditText)findViewById(R.id.start_value);
        final EditText endValueView = (EditText)findViewById(R.id.end_value);
        final EditText hourSetView = (EditText)findViewById(R.id.hour_set_text);
        final EditText minSetView = (EditText)findViewById(R.id.min_set_text);

        Button mqueryViaTimeButton = (Button)findViewById(R.id.query_from_button);
        Button mqueryFromServerButton = (Button)findViewById(R.id.query_from_server_button);
        Button muploadButton = (Button)findViewById(R.id.upload_button);
        Button mqueryViaValueButton = (Button)findViewById(R.id.query_via_value);
        Button msetClockButton = (Button)findViewById(R.id.alarm_set_button);

        String username = readData();
        url_user = "http://104.236.126.112/api/user/" + username;
        url_user_temp = url_user + "/temp";
        url_user_search = url_user + "/search";
        mNetwork = new NetworkUtility();
        mService = new PostService();
        DBname = username + ".db";
        final MyDatabaseHelper myDB = new MyDatabaseHelper(ShowDBDataActivity.this, DBname, null, 1);
        showBuffer = new StringBuffer();

        //Button Responses
        mqueryViaTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set mAdapter = null, so every time button's clicked, the listview would be recreated
                mAdapter = null;
                flag = queryFromTime;
                startTime = startTimeView.getText().toString();
                endTime = endTimeView.getText().toString();
                mTotalDataNum = getTotalNumOfDataViaDate("userData", myDB, startTime, endTime);
                initListView();
                initData();
            }
        });

        mqueryViaValueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAdapter = null;
                flag = queryFromValue;
                startValue = startValueView.getText().toString();
                endValue = endValueView.getText().toString();
               mTotalDataNum = getTotalNumOfDataViaValue("userData", myDB,startValue, endValue);
                initListView();
                initData();
            }
        });

        mqueryFromServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createserverDataTable(myDB);
                startTime = startTimeView.getText().toString();
                endTime = endTimeView.getText().toString();
               mNetwork.queryFromServer(url_user_search, startTime, endTime, new HttpCallbackListener() {
                   @Override
                   public void onFinish(String response, Message message) {
                       queryFromServerHandler.sendMessage(message);

                   }

                   @Override
                   public void onError(Exception e) {

                   }
               });
            }
        });

        muploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isNetworkAvailable()){
                    if (isWiFi()){
                        System.out.println("Upload will be initiated immediately using WiFi");
                        //Upload directly
                        Toast.makeText(getApplicationContext(), "Uploading initiated, please do not exit before its been completed", Toast.LENGTH_SHORT).show();
                        initiateUploadService();
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

        msetClockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setHour = hourSetView.getText().toString();
                setMin = minSetView.getText().toString();
                setClock(setHour,setMin);
                Toast.makeText(getApplicationContext(),"Clock has been set at " + setHour + ":" + setMin, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int getTotalNumOfDataViaDate(String TableName, MyDatabaseHelper dbHelper, String startTime, String endTime){
        int TotalDataNum = -1;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //set argus for db.query()method, 7 needed
        String[] columns = new String[]{
                "time",
                "value"
        };
        String whereClause ="time >= ? AND time <= ?";
        //give value to "?" in whereClause
        String[] whereArgs = new String[]{
                startTime,
                endTime
        };
        String orderBy = "time";
        Cursor cursor = db.query(TableName,columns,whereClause,whereArgs,null,null,orderBy);
        //Table Traversal
        if (cursor.moveToFirst()){
            do {
                TotalDataNum +=1;
            }while(cursor.moveToNext());
        }
        cursor.close();
        return TotalDataNum;
    }

    private int getTotalNumOfDataViaValue(String TableName, MyDatabaseHelper dbHelper, String startValue, String endValue){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int TotalNum = -1;
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
                TotalNum += 1;
            }while(cursor.moveToNext());
        }
        cursor.close();
        return TotalNum;
    }
    //Init ListView
    private void initListView(){
        //ListView's efficiency can still be optimized with further work
        mProgressLayout = (LinearLayout)findViewById(R.id.ll_progress);
        mLoadView = (ListView) findViewById(R.id.show_database_data);

        mLoadView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                switch (scrollState){
                    case SCROLL_STATE_IDLE://when scrolling stopped
                        int position = mLoadView.getLastVisiblePosition();
                        if(position == mDataList.size() - 1 && position != mTotalDataNum -1){
                            mStartIndex += mMaxCount;
                            LoadDataTask mLoadDataTask = new LoadDataTask();
                            mLoadDataTask.execute();
                        }else if(position == mDataList.size() - 1 && position == mTotalDataNum - 1){
                            Toast.makeText(getApplicationContext(),"No more Data",Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case SCROLL_STATE_TOUCH_SCROLL://scrolling with finger
                        break;
                    case SCROLL_STATE_FLING://scrolling without finger

                        break;
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

            }
        });
    }

    //Init Data for the very first time when the query button's been pressed
    private void initData(){
        mDataList = new ArrayList<>();
        mMoreData = new ArrayList<>();
        LoadDataTask mLoadDataTask = new LoadDataTask();
        mLoadDataTask.execute();
    }
    //Wrap database's data into a jsonObject for upload

    private void initiateUploadService(){
        Intent intent = new Intent(this, UploadService.class);
        startService(intent);
    }

    private void parseJsonAndSaveData(String jsonData, MyDatabaseHelper dpHelper){
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            SQLiteDatabase db = dpHelper.getWritableDatabase();
            for (int i = 0; i < jsonArray.length(); i++) {
                //Parse JsonArray
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                //String Id = jsonObject.getString("No");
                String value = jsonObject.getString("value");
                String time = jsonObject.getString("time");
                //String value2 = jsonObject.getString("value2");

                //Save Data into a new Table serverData
                ContentValues values = new ContentValues();
                values.put("time", time);
                values.put("value", value);
                db.insert("serverData", null, values);
            }
            db.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayDataFromServer(){
        mAdapter = null;
        flag = queryFromServer;
        initListView();
        initData();
    }

    private void ifUseMobileToUpload(final MyDatabaseHelper dbHelper){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Do you want to use mobile data to upload ?");
        builder.setMessage("Continue upload?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(getApplicationContext(), "Uploading via Data", Toast.LENGTH_SHORT).show();
                initiateUploadService();
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

    private void setClock(String hour, String minute){
        mService.setSetHour(hour);
        mService.setSetMin(minute);
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

    private void createserverDataTable(MyDatabaseHelper dpHelper){
        SQLiteDatabase db = dpHelper.getWritableDatabase();
        db.delete("serverData", null, null);
        final String CREATE_TABLE = "create table serverData("
                + "_id integer primary key autoincrement, "
                + "time text, "
                + "value real)";
        db.execSQL(CREATE_TABLE);
    }

    class LoadDataTask extends AsyncTask<Void, Void, List<String>> {
        private MyDatabaseHelper dbHelper = new MyDatabaseHelper(ShowDBDataActivity.this, DBname, null, 1);

        @Override
        protected void onPreExecute(){
            mProgressLayout.setVisibility(View.VISIBLE);
        }

        @Override
        protected List<String> doInBackground(Void... params){
            switch (flag){
                case queryFromTime:
                    mMoreData = loadMoreViaDate(dbHelper, "userData", mStartIndex, mMaxCount);
                    break;
                case queryFromValue:
                    mMoreData = loadMoreViaValue(dbHelper, "userData", mStartIndex, mMaxCount);
                    break;
                case queryFromServer:
                    mMoreData = loadMorefromServerData(dbHelper, mStartIndex, mMaxCount);
                    break;
            }
            return  mMoreData;
        }

        @Override
        protected void onPostExecute(List<String> strings){
            //Hide the ProgressBar
            mProgressLayout.setVisibility(View.INVISIBLE);
            //Add new data into adapter's data source
            mDataList.addAll(mMoreData);
            if(mAdapter == null){
                mAdapter = new ArrayAdapter<String>(ShowDBDataActivity.this, android.R.layout.simple_list_item_1, mDataList);
                mLoadView.setAdapter(mAdapter);
            }else{
                mAdapter.notifyDataSetChanged();
            }
        }

        private List<String> loadMoreViaDate(MyDatabaseHelper dpHelper, String TableName, int startIndex, int maxCount){
            SQLiteDatabase db = dpHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("select time,value from "+ TableName + " where time >= ? AND time <= ? order by time limit ? offset ?"
                    , new String[]{startTime, endTime, String.valueOf(maxCount), String.valueOf(startIndex)});
            List<String> moreDataList = new ArrayList<>();
            if (cursor.moveToFirst())
                do {
                    String data = cursor.getString(cursor.getColumnIndex("time")) + "  Value: "
                            + cursor.getString(cursor.getColumnIndex("value"));
                    moreDataList.add(data);
                }while(cursor.moveToNext());
            cursor.close();
            db.close();
            return moreDataList;
        }
        private List<String> loadMoreViaValue(MyDatabaseHelper dpHelper, String TableName, int startIndex, int maxCount){
            SQLiteDatabase db = dpHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("select time,value from "+ TableName + " where value >= ? AND value <= ? order by value limit ? offset ?"
                    , new String[]{startValue, endValue, String.valueOf(maxCount), String.valueOf(startIndex)});
            List<String> moreDataList = new ArrayList<>();
            if (cursor.moveToFirst())
                do {
                    String data = cursor.getString(cursor.getColumnIndex("time")) + "  Value:"
                            + cursor.getString(cursor.getColumnIndex("value"));
                    moreDataList.add(data);
                }while(cursor.moveToNext());
            cursor.close();
            db.close();
            return moreDataList;
        }

        private List<String> loadMorefromServerData(MyDatabaseHelper dpHelper, int startIndex, int maxCount){
            SQLiteDatabase db = dpHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("select time,value from serverData order by time limit ? offset ?"
                    , new String[]{String.valueOf(maxCount), String.valueOf(startIndex)});
            List<String> moreDataList = new ArrayList<>();
            if (cursor.moveToFirst())
                do {
                    String data = cursor.getString(cursor.getColumnIndex("time")) + "  Value:"
                            + cursor.getString(cursor.getColumnIndex("value"));
                    moreDataList.add(data);
                }while(cursor.moveToNext());
            cursor.close();
            db.close();
            return moreDataList;
        }
    }


}

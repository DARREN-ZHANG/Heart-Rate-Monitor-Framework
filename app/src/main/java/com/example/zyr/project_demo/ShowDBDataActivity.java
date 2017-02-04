package com.example.zyr.project_demo;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ShowDBDataActivity extends AppCompatActivity {
    private TextView showDataTV;
    private StringBuffer showBuffer;
    private static String startTime ;
    private static String endTime ;

    private static EditText startTimeView;
    private static EditText endTimeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_dbdata);

        startTimeView = (EditText)findViewById(R.id.start_time);
        endTimeView = (EditText)findViewById(R.id.stop_time);

        Button mqueryButton = (Button)findViewById(R.id.query_from_button);
        Button mqueryAllButton = (Button)findViewById(R.id.query_all_button);
        Button mqueryServerButton = (Button)findViewById(R.id.query_from_server_button);
        Button minsertButton = (Button)findViewById(R.id.insert_button);

        showDataTV = (TextView)findViewById(R.id.show_database_data);
        showDataTV.setMovementMethod(ScrollingMovementMethod.getInstance());

        MyDatabaseHelper myDB = new MyDatabaseHelper(ShowDBDataActivity.this, "yiran.db", null, 1);
        final SQLiteDatabase myDBInstance = myDB.getWritableDatabase();

        showBuffer = new StringBuffer();



        mqueryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTime = startTimeView.getText().toString();
                endTime = endTimeView.getText().toString();
                queryBetweenDate("userData", myDBInstance, startTime, endTime);
            }
        });

        mqueryAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryall("userData",myDBInstance);
            }
        });

        mqueryServerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDataTV.setText("");
            }
        });

        minsertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                insert("userData", myDBInstance, 188);
            }
        });
    }

    private void queryBetweenDate(String TableName, SQLiteDatabase db, String startTime, String endTime){
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
                showBuffer.append("time: "+ time);
                showBuffer.append("  value" + value);
                showBuffer.append("\n");
                showDataTV.setText(showBuffer.toString());
            }while(cursor.moveToNext());
        }
        cursor.close();
    }

    private void queryall(String TableName, SQLiteDatabase db){
        showBuffer.delete(0,showBuffer.length());
        Cursor cursor = db.query(TableName, null, null, null, null, null, null);
        if (cursor.moveToFirst()){
            do {
                String time = cursor.getString(cursor.getColumnIndex("time"));
                float value = cursor.getFloat(cursor.getColumnIndex("value"));
                showBuffer.append("time: "+ time);
                showBuffer.append("  value" + value);
                showBuffer.append("\n");
                showDataTV.setText(showBuffer.toString());
            }while(cursor.moveToNext());
        }
        cursor.close();
    }

    private void insert(String TableName, SQLiteDatabase db, float value) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.US);
        String currentTime = dateFormat.format(Calendar.getInstance().getTime());
        ContentValues values = new ContentValues();
        values.put("time", currentTime);
        values.put("value", value);
        db.insert(TableName, null, values);
    }
}

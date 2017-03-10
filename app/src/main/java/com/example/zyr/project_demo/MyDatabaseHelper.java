package com.example.zyr.project_demo;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * This Class is used for creating a Database
 * if further requirements needs the Database to be updated, it would be implemented here
 */

public class MyDatabaseHelper extends SQLiteOpenHelper {
    private Context mContext;
    private static final String CREATE_TABLE = "create table userData ("
            + "_id integer primary key autoincrement, "
            + "time text, "
            + "value real)";
    //Constructor
    public MyDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
        this.mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion){
            case 1:
            default:
        }
    }
}

package com.example.zyr.project_demo;

/**
 * This class is used for Gson.
 */

public class UserData {
    private String time;
    private float value;

    public void setValue(float value){
        this.value = value;
    }
    public float getValue(){
        return value;
    }
    public void setTime(String time){
        this.time = time;
    }
    public String getTime(){
        return time;
    }
}

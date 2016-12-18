package com.example.zyr.project_demo;

/**
 * Created by ZYR on 2016/12/17.
 */

public interface HttpCallbackListener {
    void onFinish(String response);
    void onError(Exception e);
}

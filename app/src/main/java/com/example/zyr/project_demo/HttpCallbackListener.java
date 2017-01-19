package com.example.zyr.project_demo;

import android.os.Message;

/**
 * Created by ZYR on 2016/12/17.
 */

public interface HttpCallbackListener {
    void onFinish(String response,Message message);
    void onError(Exception e);
}

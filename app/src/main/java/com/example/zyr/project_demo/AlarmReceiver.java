package com.example.zyr.project_demo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * This class is a AlarmReceiver extends a BroadcastReceiver, used for exec the PostService
 * (as a background service) every certain seconds, along with the method 'setclock();'in PostService
 * The class is currently not useful since the PostService has been set as a foreground service
 */
public class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        Intent i = new Intent(context, PostService.class);
        context.startService(i);
    }
}

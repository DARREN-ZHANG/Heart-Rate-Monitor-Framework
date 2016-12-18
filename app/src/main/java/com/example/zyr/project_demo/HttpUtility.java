package com.example.zyr.project_demo;

import android.os.Message;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * This class is used to realize http get/post request
 */

public class HttpUtility {
    public void sendGetHttpRequest(final String address, final HttpCallbackListener listener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                try {
                    //establish and set up a connection
                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setDoInput(true);
                    connection.setConnectTimeout(8000);
                    connection.setReadTimeout(8000);
                    //get response stream from the server
                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Message message = new Message();
                    message.what = 0;
                    message.obj = response.toString();

                    //handler.sendMessage(message);

                    if (listener != null) {
                        //callback onFinish(String response) method,
                        //overwrite when the methods been called
                        listener.onFinish(response.toString());
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onError(e);
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    public void sendPostHttpRequest(final String address, final HttpCallbackListener listener){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection connection = null;
                JSONObject json= new JSONObject();
                try {
                    json.put("heartRate","200");
                    json.put("time","2016-12-06 15:00:05");
                    //establish and set up a connection

                    URL url = new URL(address);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("POST");
                    connection.setReadTimeout(8000);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/json");
                    //The data to be posted

                    DataOutputStream out = new DataOutputStream(connection.getOutputStream());
                    out.writeBytes(json.toString());
                    out.flush();
                    out.close();

                    //get response stream from the server
                    InputStream in = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    Message message = new Message();
                    message.what = 0;
                    message.obj = response.toString();
                    //handler.sendMessage(message);

                    if (listener != null) {
                        //callback onFinish(String response); method
                        listener.onFinish(response.toString());
                    }
                } catch (Exception e) {
                    if (listener != null) {
                        listener.onError(e);
                    }
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }



}

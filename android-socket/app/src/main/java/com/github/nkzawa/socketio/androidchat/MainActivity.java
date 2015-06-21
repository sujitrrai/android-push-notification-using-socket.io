package com.github.nkzawa.socketio.androidchat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class MainActivity extends ActionBarActivity {

    Socket mSocket;
    Button b;
    int button_id;
    int oid;
    static SharedPreferences.Editor configEditor;
    SharedPreferences prefs;
    private Boolean selected = false;
    boolean[] arr = new boolean[10];

    {
        try {
            mSocket = IO.socket("https://enigmatic-brushlands-5771.herokuapp.com/");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        Context ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        configEditor = prefs.edit();
        configEditor.putBoolean("activityStarted",true);
        configEditor.commit();
        Boolean status = prefs.getBoolean("serviceStopped",false);

        Log.d("service exists ?", status + "");

        if(status){

            Log.d("service","not running");
            Log.d("activity running",prefs.getBoolean("activityStarted",false)+"");

        }
        else{
            Log.d("service running","true");
            Log.d("activity running", prefs.getBoolean("activityStarted", false) + "");
            stopService(new Intent(getBaseContext(), BService.class));
            mSocket.disconnect();
        }
        setContentView(R.layout.activity_main);
        Log.d("activity", "connecting to server");

        mSocket.connect();
        mSocket.emit("type", "activity");
        Log.d("activity", "connected");
        mSocket.on("ack", onAck);
        mSocket.on("occupied", onOccupied);
        mSocket.on("preOccupied", onPreOccupied);
        mSocket.on("available", onAvailable);
    }

    private Emitter.Listener onAvailable = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d("inside call","recieved something");

            int num;
            try {
                num = data.getInt("av");
                Log.d("onAvailable",num+"");
            } catch (JSONException e) {
                return;
            }

            arr[num] = false;
            Set<String> occupied = prefs.getStringSet("occupied",new HashSet<String>());
            occupied.remove(num+"");
            configEditor = prefs.edit();
            configEditor.putStringSet("occupied",occupied);
            configEditor.commit();
            changeColor(num, Color.GRAY);
        }
    };



    @Override
    protected void onStart(){
        super.onStart();
        Log.d("activity","inside onstart");
        Log.d("onStart","getOccupied");
        getOccupied();
        getNotOccupied();
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d("activity", "inside onPause");

    }

    @Override
    protected void onStop(){
        super.onStop();
        Log.d("activity","inside onstop");

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("activity","inside onDestroy");

        mSocket.off("ack", onAck);
        mSocket.off("occupied", onOccupied);
        mSocket.off("preOccupied", onPreOccupied);
        mSocket.off("available", onAvailable);
        mSocket.disconnect();
        configEditor = prefs.edit();
        configEditor.putBoolean("activityStarted", false);
        configEditor.commit();
        startService(new Intent(getBaseContext(), BService.class));
    }

    private Emitter.Listener onPreOccupied = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONArray data = (JSONArray) args[0];
            Log.d("inside onPreOccupied", "got event");
            List list = new ArrayList();

            JSONArray nums = data;

            for(int j=1;j<=9;j++){
                arr[j] = false;
            }

            for (int i=0; i<nums.length(); i++) {
                try{

                    JSONObject actor = nums.getJSONObject(i);
                    int num = actor.getInt("num");
                    Log.d("value of num", num + "");
                    list.add(num + "");
                    arr[num] = true;
                }
                catch(JSONException e) {
                    return;
                }
            }
            Set<String> occupied = new HashSet(list);
            configEditor = prefs.edit();
            configEditor.putStringSet("occupied", occupied);
            configEditor.commit();

            getOccupied();
            getNotOccupied();

        }
    };

    public void getNotOccupied(){
        for(int j = 1;j<=9;j++){
            if(!arr[j]){
                changeColor(j,Color.GRAY);
            }
        }
    }

    public void getOccupied(){
        Set<String> click = prefs.getStringSet("occupied", new HashSet<String>());
        if(click != null){

            Iterator<String> iterator = click.iterator();

            while(iterator.hasNext()){
                String id = iterator.next();
                Log.d("inside while id",id);
                changeColor(Integer.parseInt(id), Color.RED);
                arr[Integer.parseInt(id)] = true;
            }

        }
    }

    private Emitter.Listener onAck = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d("inside call","recieved something");

            int ack;
            int num;
            try {
                ack = data.getInt("ack");
                Log.d("inside call",ack+"");
            } catch (JSONException e) {
                return;
            }

            if(ack == 0){
                selected = false;

            }
            else if(ack == 1){
                try{
                    num = data.getInt("num");
                }
                catch(JSONException e){
                    return;
                }
                changeColor(num,Color.BLUE);
                arr[num] = true;
            }
        }
    };

    private Emitter.Listener onOccupied = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d("Inside onOccupied","got num");
            int num;
            try {
                num = data.getInt("num");
            } catch (JSONException e) {
                return;
            }

            arr[num] = true;



            Set<String> occupied = prefs.getStringSet("occupied", new HashSet<String>());
            occupied.add(num+"");
            configEditor = prefs.edit();
            configEditor.putStringSet("occupied", occupied);
            configEditor.commit();



            Log.d("value of num", num + "");

            changeColor(num, Color.RED);

        }
    };

    public void changeColor(int num,int c){
        Log.d("inside changeToRed",num+"");
        final int color = c;
        switch(num){
            case 1 : oid = R.id.b1;
                break;
            case 2 : oid = R.id.b2;
                break;
            case 3 : oid = R.id.b3;
                break;
            case 4 : oid = R.id.b4;
                break;
            case 5 : oid = R.id.b5;
                break;
            case 6 : oid = R.id.b6;
                break;
            case 7 : oid = R.id.b7;
                break;
            case 8 : oid = R.id.b8;
                break;
            case 9 : oid = R.id.b9;
                break;
        }
        Log.d("value of oid",oid+"");

        final int finalDid = oid;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                    if(color == Color.GRAY){
                        b = (Button) findViewById(finalDid);
                        b.setBackgroundResource(android.R.drawable.btn_default);
                    }

                    else{
                        Log.d("inside UIthread", "value of id");
                        Log.d("value of id", finalDid + "");
                        b = (Button) findViewById(finalDid);
                        b.setBackgroundColor(color);
                    }



            }
        });
    }

    public void changeBackground(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                b = (Button) findViewById(button_id);
                arr[Integer.parseInt(b.getText().toString())] = true;
                b.setBackgroundColor(Color.BLUE);

            }
        });
    }

    public void buttonClicked(View view) {
        if(!selected){
            b = (Button) view;
            button_id = b.getId();
            String buttonText = b.getText().toString();
            mSocket.emit("pressedButton",buttonText);
        }

    }
}

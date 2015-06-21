package com.github.nkzawa.socketio.androidchat;


import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;



public class BService extends Service {

    public Socket mSocket;

    public BService that = this;
    SharedPreferences prefs;
    Context ctx;
    static SharedPreferences.Editor configEditor;

    public BService() {
    }

    @Override
    public void onCreate() {


        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Boolean status = prefs.getBoolean("activityStarted",true);
        Log.d("inside onCreate",status+"");

        if(!status){
            {
                try {
                    Log.d("inside if","oncreate");
                    configEditor = prefs.edit();
                    configEditor.putBoolean("serviceStopped", false);
                    configEditor.commit();
                    Log.d("connecting to server","inside oncreate");
                    mSocket = IO.socket("https://enigmatic-brushlands-5771.herokuapp.com/");
                    mSocket.connect();
                    mSocket.emit("type","service");
                    mSocket.on("oc",onNm);
                    mSocket.on("av",onAv);


                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        else {
            Log.d("inside else","oncreate");
            this.onDestroy();
        }

    }

    public void createNotification(String title,String text){

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.icon)
                        .setContentTitle(title)
                        .setContentText(text);
// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// mId allows you to update the notification later on.
        mNotificationManager.notify(1, mBuilder.build());

    }

    @Override
    public int onStartCommand(Intent intent,int flags, int startId) {


        ctx = getApplicationContext();
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        Boolean status = prefs.getBoolean("activityStarted",true);
        Log.d("inside onStartCommand",status+"");

        if(!status){
            {
                Log.d("inside if","onstartCommand");
                Log.d("service started",status+"");
            }
        } else {
            Log.d("inside else","onstartcommand");
            this.onDestroy();
        }



        return super.onStartCommand(intent,flags,startId);
    }

    private Emitter.Listener onNm = new Emitter.Listener() {
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


            Set<String> occupied = prefs.getStringSet("occupied", new HashSet<String>());
            occupied.add(num+"");
            configEditor = prefs.edit();
            configEditor.putStringSet("occupied", occupied);
            configEditor.commit();



            Log.d("value of num", num + "");

            that.createNotification("Button Pressed",""+num+" Pressed");


        }
    };

    private Emitter.Listener onAv = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            JSONObject data = (JSONObject) args[0];
            Log.d("onAv","recieved something");

            int num;
            try {
                num = data.getInt("av");
                Log.d("onAvailable",num+"");
            } catch (JSONException e) {
                return;
            }

            Set<String> occupied = prefs.getStringSet("occupied",new HashSet<String>());
            if(occupied.contains(num+"")){
                Log.d("inside if",num+"");
                occupied.remove(num + "");
            }
            configEditor = prefs.edit();
            configEditor.putStringSet("occupied",occupied);
            configEditor.commit();
            Log.d("inside onAv", prefs.getStringSet("occupied", new HashSet<String>()).toString());

            that.createNotification("Button Available", "" + num + " available");
        }
    };


    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy(){
        Log.d("inside service destroy","destroy service");
        Log.d("service","disconnecting");

        Log.d("service","disconnected");
        mSocket.off("oc", onNm);
        mSocket.off("av", onAv);
        configEditor = prefs.edit();
        configEditor.putBoolean("serviceStopped",true);
        configEditor.commit();

    }
}

package com.xstudio.infernopinger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import android.content.Intent;
import android.app.IntentService;
import android.support.v4.content.LocalBroadcastManager;
import android.app.Notification;
import android.support.v4.app.NotificationCompat;
import java.io.IOException;
import android.app.PendingIntent;

public class PingerService extends IntentService {

    public static final String ACTION_FILTER = "com.xstudio.inferno.action.filter";
    public static final String ACTION_START = "com.xstudio.inferno.action.start";
    public static final String ACTION_STOP = "com.xstudio.inferno.action.stop";
    public static final String TARGET = "target";
    public static final String RESPONSE = "response";
    public static final int NOTIF_ID = 1337;
    
    public static final AtomicBoolean isRunning = new AtomicBoolean(false);
    
    public PingerService() {
        super(PingerService.class.getName());
        setIntentRedelivery(true);
    }
    
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(ACTION_START)) {
            start(intent.getStringExtra(TARGET));
        }
    }
    
    private void start(String target) {
        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent mainPendingIntent = PendingIntent.getActivity(this, 0, mainIntent, 0);

        Notification notif = new NotificationCompat.Builder(this)
            .setContentIntent(mainPendingIntent)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("Inferno is running ...")
            .build();

        startForeground(NOTIF_ID, notif);
        new Pinger(target).run();
    }
    
    private void stop() {
        stopForeground(true);
        stopSelf();
    }
    
    private void sendResponse(String response) {
        Intent intent = new Intent();
        intent.setAction(ACTION_FILTER);
        intent.putExtra(RESPONSE, response);
        LocalBroadcastManager.getInstance(PingerService.this).sendBroadcast(intent);
    }
    
    class Pinger implements Runnable {

        private final String target;
        
        Pinger(String target) {
            this.target = target;
        }
        
        @Override
        public void run() {
            try {
                Process process = Runtime.getRuntime().exec("ping " + target);
                
                BufferedReader stdin = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                
                String line = null;
                while ((line = stdin.readLine()) != null && isRunning.get()) {
                    sendResponse(line);
                }
                while((line = stderr.readLine()) != null && isRunning.get()) {
                    sendResponse(line);
                }
                
                process.destroy();
                PingerService.this.stop();
            } catch (IOException err) {
                
            }
        }
        
    };
    
}

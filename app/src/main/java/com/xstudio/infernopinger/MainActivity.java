package com.xstudio.infernopinger;

import android.app.ActivityManager;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import android.support.v7.app.AppCompatActivity;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import android.widget.Button;
import android.content.Context;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.KeyEvent;
import android.support.v4.content.LocalBroadcastManager;
import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.ads.MobileAds;
import com.xstudio.infernopinger.PingerService;
import com.xstudio.infernopinger.R;
import android.content.SharedPreferences;
import android.widget.TextView;
import android.view.View;

public class MainActivity extends AppCompatActivity {
    
    private AdView bannerAdView;
    private AdRequest.Builder adRequestBuilder;
    private Button startButton;
    
    private EditText target;
    private EditText output;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MobileAds.initialize(this, getString(R.string.app_unit_ad_id));
        
        setContentView(R.layout.main);
        
        target = (EditText) findViewById(R.id.ip_address);
        target.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_GO:
                        startPinger();
                        return true;
                }
                return false;
            }
        });
        
        output = (EditText) findViewById(R.id.output);
        startButton = (Button) findViewById(R.id.start);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PingerService.isRunning.get()) {
                    stopPinger();
                } else {
                    startPinger();
                }
            }
        });
        
        loadState();
        
        bannerAdView = (AdView) findViewById(R.id.bannerAd);
        adRequestBuilder = new AdRequest.Builder();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        adRequestBuilder.setLocation(location);
        loadBannerAd();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if (isMyServiceRunning(PingerService.class)) {
            startButton.setText("Stop");
        } else {
            startButton.setText("Start");
        }
        
        IntentFilter pingerIntent = new IntentFilter(PingerService.ACTION_FILTER);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, pingerIntent);
        
        loadBannerAd();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        saveState();
        super.onPause();
    }

    @Override
    protected void onStop() {
        saveState();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        saveState();
        super.onDestroy();
    }
    
    private void loadState() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.class.getName(), Context.MODE_PRIVATE);
        String targetHost = prefs.getString("target", "www.example.com");
        target.setText(targetHost);
    }
    
    private void loadBannerAd() {
        bannerAdView.loadAd(adRequestBuilder.build());
    }
    
    private void saveState() {
        SharedPreferences prefs = getSharedPreferences(MainActivity.class.getName(), Context.MODE_PRIVATE);
        prefs.edit().putString("target", target.getText().toString())
                    .commit();
    }
    
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    
    private void startPinger() {
        target.clearFocus();
        output.getEditableText().clear();
        
        String targetAddress = target.getText().toString();
        
        if (targetAddress != null && targetAddress.length() > 0) {
            PingerService.isRunning.set(true);
            Intent intent = new Intent(this, PingerService.class);
            intent.setAction(PingerService.ACTION_START);
            intent.putExtra(PingerService.TARGET, targetAddress);
            startService(intent);
            startButton.setText("Stop");
        } else {
            output.getEditableText().append("Please enter the target address");
        }
    }
    
    private void stopPinger() {
        Intent intent = new Intent(this, PingerService.class);
        intent.setAction(PingerService.ACTION_STOP);
        stopService(intent);
        
        PingerService.isRunning.set(false);
        startButton.setText("Start");
    }
    
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String response = intent.getStringExtra(PingerService.RESPONSE);
            if (response.contains("ping: unknown host")) {
                output.getEditableText().append("Target not found, please check the address or your network connection");
                stopPinger();
            } else {
                output.getEditableText().append(response);
                output.getEditableText().append("\n");
            }
            if (output.getEditableText().length() > 5000) {
                output.getEditableText().delete(0, 1000);
            }
        }
        
    };
    
}

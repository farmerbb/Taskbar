package com.farmerbb.taskbar.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.farmerbb.taskbar.MainActivity;
import com.farmerbb.taskbar.R;

public class ImportSettingsActivity extends Activity {

    boolean broadcastSent = false;

    private BroadcastReceiver settingsReceivedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            startActivity(new Intent(ImportSettingsActivity.this, MainActivity.class));
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.import_settings);

        LocalBroadcastManager.getInstance(this).registerReceiver(settingsReceivedReceiver, new IntentFilter("com.farmerbb.taskbar.IMPORT_FINISHED"));

        if(broadcastSent) {
            sendBroadcast(new Intent("com.farmerbb.taskbar.SEND_SETTINGS"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(settingsReceivedReceiver);
    }
}

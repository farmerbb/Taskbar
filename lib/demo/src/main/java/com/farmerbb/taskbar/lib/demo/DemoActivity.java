package com.farmerbb.taskbar.lib.demo;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.farmerbb.taskbar.lib.Taskbar;

public class DemoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onBackPressed() {}

    public void openDeveloperOptions(View v) {
        startActivitySafely(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
    }

    public void setDefaultHome(View v) {
        startActivitySafely(Settings.ACTION_HOME_SETTINGS);
    }

    public void openSettings(View v) {
        Taskbar.openSettings(this);
    }

    private void startActivitySafely(String action) {
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
    }
}

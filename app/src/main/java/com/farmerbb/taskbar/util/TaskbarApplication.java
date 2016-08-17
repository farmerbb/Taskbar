package com.farmerbb.taskbar.util;

import android.app.Application;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatDelegate;

public class TaskbarApplication extends Application {
    public void onCreate() {
        super.onCreate();

        int theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

        SharedPreferences pref = U.getSharedPreferences(getApplicationContext());
        switch(pref.getString("theme", "light")) {
            case "light":
                theme = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                theme = AppCompatDelegate.MODE_NIGHT_YES;
                break;
        }

        AppCompatDelegate.setDefaultNightMode(theme);
    }
}

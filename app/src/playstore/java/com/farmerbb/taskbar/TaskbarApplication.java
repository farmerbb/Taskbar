package com.farmerbb.taskbar;

import android.app.Application;
import android.content.Context;

import com.farmerbb.taskbar.util.U;

import me.weishu.reflection.Reflection;

public class TaskbarApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        if(U.getCurrentApiVersion() > 29.0f)
            Reflection.unseal(base);
    }
}
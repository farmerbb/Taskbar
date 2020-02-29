package com.farmerbb.taskbar.activity;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.ApplicationType;
import com.farmerbb.taskbar.util.U;

public class PersistentShortcutLaunchActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String packageName = getIntent().getStringExtra("package_name");
        String componentName = getIntent().getStringExtra("component_name");
        String windowSize = getIntent().getStringExtra("window_size");

        if(!U.canDrawOverlays(this) && windowSize != null) {
            Intent intent = new Intent(this, DummyActivity.class);
            intent.putExtra("show_permission_dialog", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(intent);
        } else if(packageName != null && componentName != null) {
            final AppEntry entry = new AppEntry(packageName, componentName, null, null, false);

            U.launchApp(this, entry, windowSize, () -> {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));

                try {
                    startActivity(intent, U.getActivityOptionsBundle(this, ApplicationType.APP_PORTRAIT, null));
                } catch (ActivityNotFoundException | IllegalArgumentException e1) { /* Gracefully fail */ }
            });
        } else
            U.showToast(this, R.string.tb_invalid_shortcut);

        finish();
    }
}
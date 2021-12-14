/* Copyright 2021 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.activity;

import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.IBinder;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuBinderWrapper;
import rikka.shizuku.ShizukuProvider;
import rikka.shizuku.SystemServiceHelper;

public abstract class EnableAdditionalSettingsActivityBase extends AppCompatActivity implements Shizuku.OnRequestPermissionResultListener {

    private boolean isUsingShizuku = false;
    private boolean shouldFinish = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Shizuku.pingBinder()) {
            isUsingShizuku = true;

            boolean isGranted;
            if(Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                isGranted = checkSelfPermission(ShizukuProvider.PERMISSION) == PackageManager.PERMISSION_GRANTED;
            } else {
                isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            }

            if(isGranted) {
                grantWriteSecureSettingsPermission();
            } else {
                int SHIZUKU_CODE = 123;
                if(Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
                    requestPermissions(new String[] { ShizukuProvider.PERMISSION }, SHIZUKU_CODE);
                } else {
                    Shizuku.requestPermission(SHIZUKU_CODE);
                }
            }
        } else {
            proceedWithOnCreate();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(shouldFinish) finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        shouldFinish = isUsingShizuku;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for(int i = 0; i < permissions.length; i++) {
            String permission = permissions[i];
            int result = grantResults[i];

            if(permission.equals(ShizukuProvider.PERMISSION)) {
                onRequestPermissionResult(requestCode, result);
            }
        }
    }

    @Override
    public void onRequestPermissionResult(int requestCode, int grantResult) {
        boolean isGranted = grantResult == PackageManager.PERMISSION_GRANTED;
        if(isGranted) {
            grantWriteSecureSettingsPermission();
        } else {
            isUsingShizuku = false;
            proceedWithOnCreate();
        }
    }

    @SuppressLint("PrivateApi")
    private void grantWriteSecureSettingsPermission() {
        try {
            Class<?> iPmClass = Class.forName("android.content.pm.IPackageManager");
            Class<?> iPmStub = Class.forName("android.content.pm.IPackageManager$Stub");
            Method asInterfaceMethod = iPmStub.getMethod("asInterface", IBinder.class);
            Method grantRuntimePermissionMethod = iPmClass.getMethod("grantRuntimePermission", String.class, String.class, int.class);

            Object iPmInstance = asInterfaceMethod.invoke(null, new ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package")));
            grantRuntimePermissionMethod.invoke(iPmInstance, getPackageName(), android.Manifest.permission.WRITE_SECURE_SETTINGS, 0);

            U.showToast(this, R.string.tb_shizuku_successful);
        } catch (Exception ignored) {}

        finish();
    }

    protected abstract void proceedWithOnCreate();
}
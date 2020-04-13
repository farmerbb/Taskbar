/* Copyright 2020 Braden Farmer
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

package com.farmerbb.taskbar.service;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.PersistentShortcutLaunchActivity;
import com.farmerbb.taskbar.activity.PersistentShortcutSelectAppActivity;
import com.farmerbb.taskbar.util.TaskbarIntent;
import com.farmerbb.taskbar.util.U;

@TargetApi(Build.VERSION_CODES.N)
public abstract class FavoriteAppTileService extends TileService {

    protected abstract int tileNumber();

    private String prefix = "qs_tile_" + tileNumber() + "_";

    private BroadcastReceiver tileUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        U.registerReceiver(this, tileUpdateReceiver, TaskbarIntent.ACTION_UPDATE_FAVORITE_APP_TILE);
    }

    @Override
    public void onDestroy() {
        U.unregisterReceiver(this, tileUpdateReceiver);
        super.onDestroy();
    }

    @Override
    public void onStartListening() {
        updateState();
    }

    @Override
    public void onTileRemoved() {
        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean(prefix + "added", false).apply();
    }

    @Override
    public void onClick() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(!pref.getBoolean(prefix + "added", false)) {
            selectApp();
            return;
        }

        if(isLocked())
            unlockAndRun(this::launchApp);
        else
            launchApp();
    }

    private void selectApp() {
        Intent intent = new Intent(this, PersistentShortcutSelectAppActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("qs_tile", tileNumber());
        startActivityAndCollapse(intent);
    }

    private void launchApp() {
        SharedPreferences pref = U.getSharedPreferences(this);

        Intent shortcutIntent = new Intent(this, PersistentShortcutLaunchActivity.class);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra("package_name", pref.getString(prefix + "package_name", null));
        shortcutIntent.putExtra("component_name", pref.getString(prefix + "component_name", null));
        shortcutIntent.putExtra("window_size", pref.getString(prefix + "window_size", null));

        startActivityAndCollapse(shortcutIntent);
    }

    private void updateState() {
        Tile tile = getQsTile();
        if(tile == null) return;

        SharedPreferences pref = U.getSharedPreferences(this);
        tile.setIcon(Icon.createWithResource(this, R.drawable.tb_favorite_app_tile));

        if(pref.getBoolean(prefix + "added", false)) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel(pref.getString(prefix + "label", getString(R.string.tb_new_shortcut)));
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tb_new_shortcut));
        }

        tile.updateTile();
    }
}
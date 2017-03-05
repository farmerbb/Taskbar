/* Copyright 2017 Braden Farmer
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.U;

@TargetApi(Build.VERSION_CODES.N)
public class QuickSettingsTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateState();
    }

    @Override
    public void onClick() {
        super.onClick();

        sendBroadcast(new Intent("com.farmerbb.taskbar."
                + (U.isServiceRunning(this, NotificationService.class) ? "QUIT" : "START")));

        new Handler().postDelayed(this::updateState, 100);
    }

    private void updateState() {
        Tile tile = getQsTile();
        if(tile != null) {
            SharedPreferences pref = U.getSharedPreferences(this);
            tile.setIcon(Icon.createWithResource(this, pref.getBoolean("app_drawer_icon", false)
                    ? R.drawable.ic_system
                    : R.drawable.ic_allapps));

            if(U.canDrawOverlays(this))
                tile.setState(U.isServiceRunning(this, NotificationService.class)
                        ? Tile.STATE_ACTIVE
                        : Tile.STATE_INACTIVE);
            else
                tile.setState(Tile.STATE_UNAVAILABLE);

            tile.updateTile();
        }
    }
}
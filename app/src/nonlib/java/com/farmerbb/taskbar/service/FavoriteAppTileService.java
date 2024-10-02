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
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.VisibleForTesting;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.PersistentShortcutLaunchActivity;
import com.farmerbb.taskbar.activity.PersistentShortcutSelectAppActivity;
import com.farmerbb.taskbar.util.CompatUtils;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

@TargetApi(Build.VERSION_CODES.N)
public abstract class FavoriteAppTileService extends TileService {

    protected abstract int tileNumber();

    private final String prefix = PREF_QS_TILE + "_" + tileNumber() + "_";

    @Override
    public void onStartListening() {
        updateState();
    }

    @Override
    public void onTileRemoved() {
        SharedPreferences pref = U.getSharedPreferences(this);
        pref.edit().putBoolean(prefix + PREF_ADDED_SUFFIX, false).apply();
    }

    @Override
    public void onClick() {
        SharedPreferences pref = U.getSharedPreferences(this);
        if(!pref.getBoolean(prefix + PREF_ADDED_SUFFIX, false)) {
            selectApp();
            return;
        }

        if(isLocked()) {
            unlockAndRun(this::launchApp);
        } else {
            launchApp();
        }
    }

    @VisibleForTesting
    String getPrefix() {
        return prefix;
    }

    private void selectApp() {
        Intent intent = U.getThemedIntent(this, PersistentShortcutSelectAppActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(PREF_QS_TILE, tileNumber());

        if(!CompatUtils.startActivityAndCollapse(this, intent)) {
            startActivityAndCollapse(intent);
        }
    }

    private void launchApp() {
        SharedPreferences pref = U.getSharedPreferences(this);
        UserManager userManager = (UserManager) getSystemService(USER_SERVICE);

        Intent shortcutIntent = new Intent(this, PersistentShortcutLaunchActivity.class);
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra(
                EXTRA_PACKAGE_NAME,
                pref.getString(prefix + PREF_PACKAGE_NAME_SUFFIX, null)
        );
        shortcutIntent.putExtra(
                EXTRA_COMPONENT_NAME,
                pref.getString(prefix + PREF_COMPONENT_NAME_SUFFIX, null)
        );
        shortcutIntent.putExtra(
                EXTRA_WINDOW_SIZE,
                pref.getString(prefix + PREF_WINDOW_SIZE_SUFFIX, null)
        );
        shortcutIntent.putExtra(
                EXTRA_USER_ID,
                pref.getLong(
                        prefix + PREF_USER_ID_SUFFIX,
                        userManager.getSerialNumberForUser(Process.myUserHandle())
                )
        );

        if(!CompatUtils.startActivityAndCollapse(this, shortcutIntent)) {
            startActivityAndCollapse(shortcutIntent);
        }
    }

    private void updateState() {
        Tile tile = getQsTile();
        if(tile == null) return;

        SharedPreferences pref = U.getSharedPreferences(this);
        if(pref.getBoolean(prefix + PREF_ADDED_SUFFIX, false)) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.setLabel(pref.getString(prefix + PREF_LABEL_SUFFIX, getString(R.string.tb_new_shortcut)));

            String componentName = pref.getString(prefix + PREF_COMPONENT_NAME_SUFFIX, null);
            float threshold = pref.getFloat(prefix + PREF_ICON_THRESHOLD_SUFFIX, -1);

            if(componentName != null && threshold >= 0) {
                UserManager userManager = (UserManager) getSystemService(USER_SERVICE);
                LauncherApps launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);
                long userId = pref.getLong(
                        prefix + PREF_USER_ID_SUFFIX,
                        userManager.getSerialNumberForUser(Process.myUserHandle())
                );

                Intent intent = new Intent();
                intent.setComponent(ComponentName.unflattenFromString(componentName));
                LauncherActivityInfo info = launcherApps.resolveActivity(intent, userManager.getUserForSerialNumber(userId));

                IconCache cache = IconCache.getInstance(this);
                BitmapDrawable icon = U.convertToMonochrome(this, cache.getIcon(this, info), threshold);

                tile.setIcon(Icon.createWithBitmap(icon.getBitmap()));
            } else {
                tile.setIcon(Icon.createWithResource(this, R.drawable.tb_favorite_app_tile));
            }
        } else {
            tile.setState(Tile.STATE_INACTIVE);
            tile.setLabel(getString(R.string.tb_new_shortcut));
            tile.setIcon(Icon.createWithResource(this, R.drawable.tb_favorite_app_tile));
        }

        tile.updateTile();
    }
}
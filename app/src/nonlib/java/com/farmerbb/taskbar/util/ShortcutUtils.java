/* Copyright 2019 Braden Farmer
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

package com.farmerbb.taskbar.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.ShortcutActivity;
import com.farmerbb.taskbar.activity.StartTaskbarActivity;
import com.farmerbb.taskbar.service.FavoriteApp1;
import com.farmerbb.taskbar.service.FavoriteApp2;
import com.farmerbb.taskbar.service.FavoriteApp3;
import com.farmerbb.taskbar.service.FavoriteApp4;
import com.farmerbb.taskbar.service.FavoriteApp5;

import static com.farmerbb.taskbar.util.Constants.*;

public class ShortcutUtils {

    private ShortcutUtils() {}

    public static Intent getShortcutIntent(Context context) {
        Intent shortcutIntent = new Intent(context, ShortcutActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra(EXTRA_IS_LAUNCHING_SHORTCUT, true);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.tb_freeform_mode));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.tb_pref_header_freeform));

        return intent;
    }

    public static Intent getStartStopIntent(Context context) {
        Intent shortcutIntent = new Intent(context, StartTaskbarActivity.class);
        shortcutIntent.setAction(Intent.ACTION_MAIN);
        shortcutIntent.putExtra(EXTRA_IS_LAUNCHING_SHORTCUT, true);

        Intent intent = new Intent();
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(context, R.mipmap.tb_launcher));
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, context.getString(R.string.tb_start_taskbar));

        return intent;
    }

    public static void initFavoriteAppTiles(Context context) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return;

        Class<?>[] tiles = new Class<?>[] {
                FavoriteApp1.class,
                FavoriteApp2.class,
                FavoriteApp3.class,
                FavoriteApp4.class,
                FavoriteApp5.class
        };

        for(Class<?> tile : tiles) {
            TileService.requestListeningState(context, new ComponentName(context, tile));
        }
    }
}

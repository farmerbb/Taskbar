/* Copyright 2016 Braden Farmer
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

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.ShortcutUtils;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.TopApps;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public class ClearDataActivity extends AppCompatActivity {

    CheckBox pba;
    CheckBox hiddenApps;
    CheckBox topApps;
    CheckBox savedWindowSizes;
    CheckBox desktopIcons;
    CheckBox qsShortcuts;

    Button button;
    
    CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(pba.isChecked()
                    || hiddenApps.isChecked()
                    || topApps.isChecked()
                    || savedWindowSizes.isChecked()
                    || desktopIcons.isChecked()
                    || qsShortcuts.isChecked())
                button.setText(getResources().getString(R.string.tb_action_reset).toUpperCase());
            else
                button.setText(getResources().getString(R.string.tb_action_close).toUpperCase());
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tb_clear_data);
        setTitle(R.string.tb_clear_pinned_apps);

        pba = findViewById(R.id.clear_pba);
        pba.setOnCheckedChangeListener(listener);
        
        hiddenApps = findViewById(R.id.clear_hidden_apps);
        hiddenApps.setOnCheckedChangeListener(listener);
        
        topApps = findViewById(R.id.clear_top_apps);
        topApps.setOnCheckedChangeListener(listener);
        
        savedWindowSizes = findViewById(R.id.clear_window_sizes);
        if(U.canEnableFreeform(this))
            savedWindowSizes.setOnCheckedChangeListener(listener);
        else
            savedWindowSizes.setVisibility(View.GONE);

        desktopIcons = findViewById(R.id.clear_desktop_icons);
        if(U.isDesktopIconsEnabled(this))
            desktopIcons.setOnCheckedChangeListener(listener);
        else
            desktopIcons.setVisibility(View.GONE);

        qsShortcuts = findViewById(R.id.clear_qs_shortcuts);
        if(U.isFavoriteAppTilesEnabled(this))
            qsShortcuts.setOnCheckedChangeListener(listener);
        else
            qsShortcuts.setVisibility(View.GONE);

        button = findViewById(R.id.button);
        button.setText(getResources().getString(R.string.tb_action_close).toUpperCase());
        button.setOnClickListener(view -> {
            if(pba.isChecked())
                PinnedBlockedApps.getInstance(this).clear(this);

            if(hiddenApps.isChecked())
                Blacklist.getInstance(this).clear(this);

            if(topApps.isChecked())
                TopApps.getInstance(this).clear(this);

            if(savedWindowSizes.isChecked())
                SavedWindowSizes.getInstance(this).clear(this);

            if(desktopIcons.isChecked()) {
                SharedPreferences pref = U.getSharedPreferences(this);
                pref.edit().remove(PREF_DESKTOP_ICONS).apply();
                U.sendBroadcast(this, ACTION_REFRESH_DESKTOP_ICONS);
            }

            if(qsShortcuts.isChecked()) {
                SharedPreferences pref = U.getSharedPreferences(this);
                pref.edit()
                        .remove("qs_tile_1_added")
                        .remove("qs_tile_2_added")
                        .remove("qs_tile_3_added")
                        .remove("qs_tile_4_added")
                        .remove("qs_tile_5_added")
                        .apply();

                ShortcutUtils.initFavoriteAppTiles(this);
            }

            finish();
        });
    }
}

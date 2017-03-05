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

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.Blacklist;
import com.farmerbb.taskbar.util.PinnedBlockedApps;
import com.farmerbb.taskbar.util.SavedWindowSizes;
import com.farmerbb.taskbar.util.TopApps;

public class ClearDataActivity extends AppCompatActivity {

    CheckBox pba;
    CheckBox hiddenApps;
    CheckBox topApps;
    CheckBox savedWindowSizes;
    
    Button button;
    
    CompoundButton.OnCheckedChangeListener listener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(pba.isChecked() || hiddenApps.isChecked() || topApps.isChecked() || savedWindowSizes.isChecked())
                button.setText(getResources().getString(R.string.action_reset).toUpperCase());
            else
                button.setText(getResources().getString(R.string.action_close).toUpperCase());
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.clear_data);
        setTitle(R.string.clear_pinned_apps);

        pba = (CheckBox) findViewById(R.id.clear_pba);
        pba.setOnCheckedChangeListener(listener);
        
        hiddenApps = (CheckBox) findViewById(R.id.clear_hidden_apps);
        hiddenApps.setOnCheckedChangeListener(listener);
        
        topApps = (CheckBox) findViewById(R.id.clear_top_apps);
        topApps.setOnCheckedChangeListener(listener);
        
        savedWindowSizes = (CheckBox) findViewById(R.id.clear_window_sizes);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            savedWindowSizes.setOnCheckedChangeListener(listener);
        else
            savedWindowSizes.setVisibility(View.GONE);
        
        button = (Button) findViewById(R.id.button);
        button.setText(getResources().getString(R.string.action_close).toUpperCase());
        button.setOnClickListener(view -> {
            if(pba.isChecked())
                PinnedBlockedApps.getInstance(ClearDataActivity.this).clear(ClearDataActivity.this);

            if(hiddenApps.isChecked())
                Blacklist.getInstance(ClearDataActivity.this).clear(ClearDataActivity.this);

            if(topApps.isChecked())
                TopApps.getInstance(ClearDataActivity.this).clear(ClearDataActivity.this);

            if(savedWindowSizes.isChecked())
                SavedWindowSizes.getInstance(ClearDataActivity.this).clear(ClearDataActivity.this);

            finish();
        });
    }
}

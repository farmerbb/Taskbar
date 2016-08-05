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

package com.farmerbb.taskbar.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.widget.GridView;

import com.farmerbb.taskbar.R;

public class ThemedGridView extends GridView {
    private boolean enabled = true;

    public ThemedGridView(Context context, AttributeSet attrs) {
        super(new ContextThemeWrapper(context, R.style.AppTheme), attrs);
    }

    @Override
    public boolean isEnabled() {
        return enabled && super.isEnabled();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
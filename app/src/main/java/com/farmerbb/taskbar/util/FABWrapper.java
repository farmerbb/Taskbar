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

import android.content.Context;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.core.content.ContextCompat;
import android.view.View;
import android.widget.ImageView;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;

public class FABWrapper {
    public View view;

    public FABWrapper(Context context) {
        view = context.getPackageName().equals(BuildConfig.ANDROIDX86_APPLICATION_ID)
                ? new ImageView(context)
                : new FloatingActionButton(context);

        if(view instanceof ImageView) {
            view.setBackground(ContextCompat.getDrawable(context, R.drawable.tb_circle));

            int padding = context.getResources().getDimensionPixelSize(R.dimen.tb_fake_fab_padding);
            view.setPadding(padding, padding, padding, padding);
        }
    }

    public void setImageResource(int resource) {
        if(view instanceof FloatingActionButton)
            ((FloatingActionButton) view).setImageResource(resource);
        else if(view instanceof ImageView)
            ((ImageView) view).setImageResource(resource);
    }

    public void show() {
        if(view instanceof FloatingActionButton)
            ((FloatingActionButton) view).show();
        else if(view instanceof ImageView)
            view.setVisibility(View.VISIBLE);
    }

    public void hide() {
        if(view instanceof FloatingActionButton)
            ((FloatingActionButton) view).hide();
        else if(view instanceof ImageView)
            view.setVisibility(View.GONE);
    }
}

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

package com.farmerbb.taskbar.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.view.View;

import com.farmerbb.taskbar.BuildConfig;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.SecondaryHomeActivity;
import com.farmerbb.taskbar.helper.LauncherHelper;
import com.farmerbb.taskbar.util.CompatUtils;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.U;

import static com.farmerbb.taskbar.util.Constants.*;

public abstract class UIController {
    protected Context context;
    private boolean prevImeVisibility;

    public UIController(Context context) {
        this.context = U.getDisplayContext(context);
    }

    abstract void onCreateHost(UIHost host);
    abstract void onRecreateHost(UIHost host);
    abstract void onDestroyHost(UIHost host);

    protected void init(Context context, UIHost host, Runnable runnable) {
        SharedPreferences pref = U.getSharedPreferences(context);
        LauncherHelper helper = LauncherHelper.getInstance();

        boolean shouldProceed;
        if(helper.isOnSecondaryHomeScreen(context))
            shouldProceed = host instanceof SecondaryHomeActivity;
        else
            shouldProceed = true;

        if(shouldProceed && (pref.getBoolean(PREF_TASKBAR_ACTIVE, false)
                || helper.isOnHomeScreen(context))) {
            if(U.canDrawOverlays(context))
                runnable.run();
            else {
                pref.edit().putBoolean(PREF_TASKBAR_ACTIVE, false).apply();
                host.terminate();
            }
        } else
            host.terminate();
    }

    protected int getBottomMargin(Context context) {
        return U.getDisplayInfo(context).displayDefaultsToFreeform
                && U.getCurrentApiVersion() < 33.0f
                && !U.isChromeOs(context)
                && !U.isShowHideNavbarSupported()
                && TaskbarPosition.isBottom(context) ? U.getNavbarHeight(context) : -1;
    }

    protected void applyMarginFix(UIHost host, View layout, ViewParams params) {
        if(U.getCurrentApiVersion() <= 29.0 || !U.getDisplayInfo(context).displayDefaultsToFreeform)
            return;

        layout.setOnApplyWindowInsetsListener((v, insets) -> {
            boolean isImeVisible = CompatUtils.isImeVisible(v);
            if(isImeVisible != prevImeVisibility) {
                prevImeVisibility = isImeVisible;

                ViewParams newParams = isImeVisible ? params.updateBottomMargin(0) : params;
                host.updateViewLayout(layout, newParams);

                if(isImeFixDisabled() && !U.isLibrary(context)) {
                    SharedPreferences pref = U.getSharedPreferences(context);
                    if(!pref.getBoolean(PREF_DESKTOP_MODE_IME_FIX, false)) {
                        pref.edit().putBoolean(PREF_DESKTOP_MODE_IME_FIX, true).apply();
                        U.showToastLong(context, R.string.tb_desktop_mode_ime_fix_toast);
                    }
                }
            }

            return insets;
        });
    }

    protected boolean isImeFixDisabled() {
        String ime = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return !ime.startsWith(BuildConfig.BASE_APPLICATION_ID) && !ime.startsWith("com.farmerbb.secondscreen");
    }
}
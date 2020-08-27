package com.farmerbb.taskbar;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.os.UserHandle;

import org.robolectric.util.ReflectionHelpers;

import static org.robolectric.util.ReflectionHelpers.ClassParameter.from;

public class LauncherAppsHelper {
    public static LauncherActivityInfo generateTestLauncherActivityInfo(Context context,
                                                                        ActivityInfo activityInfo,
                                                                        int userHandleId) {
        return
                ReflectionHelpers.callConstructor(
                        LauncherActivityInfo.class,
                        from(Context.class, context),
                        from(ActivityInfo.class, activityInfo),
                        from(UserHandle.class, UserHandle.getUserHandleForUid(userHandleId))
                );
    }
}

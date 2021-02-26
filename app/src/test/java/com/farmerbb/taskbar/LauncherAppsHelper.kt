package com.farmerbb.taskbar

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.LauncherActivityInfo
import android.os.UserHandle
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

object LauncherAppsHelper {
    @JvmStatic
    fun generateTestLauncherActivityInfo(
        context: Context,
        activityInfo: ActivityInfo,
        userHandleId: Int
    ): LauncherActivityInfo {
        return ReflectionHelpers.callConstructor(
                LauncherActivityInfo::class.java,
                ClassParameter.from(Context::class.java, context),
                ClassParameter.from(ActivityInfo::class.java, activityInfo),
                ClassParameter.from(
                        UserHandle::class.java,
                        UserHandle.getUserHandleForUid(userHandleId)
                )
        )
    }
}

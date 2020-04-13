package com.farmerbb.taskbar.util;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Process;
import android.os.UserManager;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.activity.MainActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class AppEntryTest {
    private Context context;
    private AppEntry appEntry;
    private ComponentName componentName;
    private Drawable icon;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        componentName = new ComponentName(context, MainActivity.class);
        icon = context.getResources().getDrawable(R.drawable.tb_apps);
        appEntry = new AppEntry(
                context.getPackageName(),
                componentName.flattenToString(),
                context.getPackageName(),
                icon,
                true
        );
    }

    @Test
    public void testGetComponentName() {
        assertEquals(componentName.flattenToString(), appEntry.getComponentName());
    }

    @Test
    public void testGetPackageName() {
        assertEquals(context.getPackageName(), appEntry.getPackageName());
    }

    @Test
    public void testGetLabel() {
        assertEquals(context.getPackageName(), appEntry.getLabel());
    }

    @Test
    public void testGetUserId() {
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        long currentUser = userManager.getSerialNumberForUser(Process.myUserHandle());
        assertEquals(currentUser, appEntry.getUserId(context));
        appEntry.setUserId(currentUser + 1);
        assertEquals(currentUser + 1, appEntry.getUserId(context));
        appEntry.setUserId(currentUser);
    }

    @Test
    public void testGetIcon() {
        assertEquals(icon, appEntry.getIcon(context));
    }

    @Test
    public void testSetLastTimeUsed() {
        assertEquals(0, appEntry.getLastTimeUsed());
        appEntry.setLastTimeUsed(100);
        assertEquals(100, appEntry.getLastTimeUsed());
        appEntry.setLastTimeUsed(0);
    }

    @Test
    public void testSetTotalTimeInForeground() {
        assertEquals(0, appEntry.getTotalTimeInForeground());
        appEntry.setTotalTimeInForeground(100);
        assertEquals(100, appEntry.getTotalTimeInForeground());
        appEntry.setTotalTimeInForeground(0);
    }
}

package com.farmerbb.taskbar.ui;

import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.UserManager;
import android.view.Gravity;

import androidx.test.core.app.ApplicationProvider;

import com.farmerbb.taskbar.Constants;
import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.shadow.TaskbarShadowLauncherApps;
import com.farmerbb.taskbar.util.AppEntry;
import com.farmerbb.taskbar.util.IconCache;
import com.farmerbb.taskbar.util.TaskbarPosition;
import com.farmerbb.taskbar.util.U;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;

import java.util.ArrayList;
import java.util.List;

import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_BOTTOM_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_RIGHT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_LEFT;
import static com.farmerbb.taskbar.util.Constants.POSITION_TOP_VERTICAL_RIGHT;
import static com.farmerbb.taskbar.util.Constants.PREF_SHOW_SEARCH_BAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*",
        "android.*", "androidx.*", "com.farmerbb.taskbar.shadow.*"})
@PrepareForTest(value = {U.class, TaskbarPosition.class})
public class StartMenuControllerTest {
    private static final String NON_URL_QUERY = "test-query";

    @Rule
    public PowerMockRule rule = new PowerMockRule();

    private StartMenuController uiController;
    private Context context;
    private SharedPreferences prefs;
    private final UIHost host = new MockUIHost();

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        uiController = new StartMenuController(context);
        prefs = U.getSharedPreferences(context);

        uiController.onCreateHost(host);
    }

    @After
    public void tearDown() {
        prefs.edit().remove(PREF_SHOW_SEARCH_BAR).apply();
        uiController.onDestroyHost(host);
    }

    @Test
    public void testShouldShowSearchBox() {
        prefs.edit().remove(PREF_SHOW_SEARCH_BAR).apply();
        assertTrue(uiController.shouldShowSearchBox(prefs, false));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, "always").apply();
        assertTrue(uiController.shouldShowSearchBox(prefs, false));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, "keyboard").apply();
        assertFalse(uiController.shouldShowSearchBox(prefs, false));
        assertTrue(uiController.shouldShowSearchBox(prefs, true));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, "never").apply();
        assertFalse(uiController.shouldShowSearchBox(prefs, true));

        prefs.edit().putString(PREF_SHOW_SEARCH_BAR, Constants.UNSUPPORTED).apply();
        assertFalse(uiController.shouldShowSearchBox(prefs, true));
    }

    @Test
    public void testGetStartMenuLayoutId() {
        assertEquals(
                R.layout.tb_start_menu_left,
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_LEFT)
        );
        assertEquals(
                R.layout.tb_start_menu_right,
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_RIGHT)
        );
        assertEquals(
                R.layout.tb_start_menu_top_left,
                uiController.getStartMenuLayoutId(POSITION_TOP_LEFT)
        );
        assertEquals(
                R.layout.tb_start_menu_vertical_left,
                uiController.getStartMenuLayoutId(POSITION_TOP_VERTICAL_LEFT)
        );
        assertEquals(
                R.layout.tb_start_menu_vertical_left,
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_VERTICAL_LEFT)
        );
        assertEquals(
                R.layout.tb_start_menu_top_right,
                uiController.getStartMenuLayoutId(POSITION_TOP_RIGHT)
        );
        assertEquals(
                R.layout.tb_start_menu_vertical_right,
                uiController.getStartMenuLayoutId(POSITION_TOP_VERTICAL_RIGHT)
        );
        assertEquals(
                R.layout.tb_start_menu_vertical_right,
                uiController.getStartMenuLayoutId(POSITION_BOTTOM_VERTICAL_RIGHT)
        );
    }

    @Test
    public void testGetStartMenuGravity() {
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                uiController.getStartMenuGravity(POSITION_BOTTOM_LEFT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.LEFT,
                uiController.getStartMenuGravity(POSITION_BOTTOM_VERTICAL_LEFT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.RIGHT,
                uiController.getStartMenuGravity(POSITION_BOTTOM_RIGHT)
        );
        assertEquals(
                Gravity.BOTTOM | Gravity.RIGHT,
                uiController.getStartMenuGravity(POSITION_BOTTOM_VERTICAL_RIGHT)
        );
        assertEquals(
                Gravity.TOP | Gravity.LEFT,
                uiController.getStartMenuGravity(POSITION_TOP_LEFT)
        );
        assertEquals(
                Gravity.TOP | Gravity.LEFT,
                uiController.getStartMenuGravity(POSITION_TOP_VERTICAL_LEFT)
        );
        assertEquals(
                Gravity.TOP | Gravity.RIGHT,
                uiController.getStartMenuGravity(POSITION_TOP_RIGHT)
        );
        assertEquals(
                Gravity.TOP | Gravity.RIGHT,
                uiController.getStartMenuGravity(POSITION_TOP_VERTICAL_RIGHT)
        );
    }

    @Test
    public void testGenerateQueryWebSearchIntent() {
        Intent intent = uiController.generateQueryWebSearchIntent(NON_URL_QUERY);
        assertEquals(Intent.ACTION_WEB_SEARCH, intent.getAction());
        assertEquals(NON_URL_QUERY, intent.getStringExtra(SearchManager.QUERY));
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());

        String urlQuery = "https://github.com/farmerbb/Taskbar";
        intent = uiController.generateQueryWebSearchIntent(urlQuery);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(Uri.parse(urlQuery), intent.getData());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
    }

    @Test
    public void testGenerateQueryGoogleIntent() {
        Intent intent = uiController.generateQueryGoogleIntent(NON_URL_QUERY);
        assertEquals(Intent.ACTION_VIEW, intent.getAction());
        assertEquals(Intent.FLAG_ACTIVITY_NEW_TASK, intent.getFlags());
        Uri uri = intent.getData();
        assertEquals("https", uri.getScheme());
        assertEquals("www.google.com", uri.getAuthority());
        assertEquals("/search", uri.getPath());
        assertEquals(NON_URL_QUERY, uri.getQueryParameter("q"));
    }

    @Test
    public void testGenerateAppEntries() {
        List<LauncherActivityInfo> queryList = new ArrayList<>();
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        PackageManager packageManager = context.getPackageManager();

        List<AppEntry> appEntries =
                uiController.generateAppEntries(context, userManager, packageManager, queryList);
        assertEquals(0, appEntries.size());

        ActivityInfo activityInfo = new ActivityInfo();
        activityInfo.packageName = Constants.ENTRY_TEST_PACKAGE;
        activityInfo.name = Constants.ENTRY_TEST_LABEL;
        activityInfo.nonLocalizedLabel = activityInfo.name;
        activityInfo.applicationInfo = new ApplicationInfo();
        activityInfo.applicationInfo.packageName = activityInfo.packageName;
        LauncherActivityInfo launcherActivityInfo =
                TaskbarShadowLauncherApps
                        .generateTestLauncherActivityInfo(
                                context, activityInfo, Constants.DEFAULT_TEST_USER_ID
                        );
        queryList.add(launcherActivityInfo);
        appEntries =
                uiController.generateAppEntries(context, userManager, packageManager, queryList);
        assertEquals(1, appEntries.size());
        verifyAppEntryContent(activityInfo, appEntries.get(0));

        queryList.add(launcherActivityInfo);
        appEntries =
                uiController.generateAppEntries(context, userManager, packageManager, queryList);
        assertEquals(2, appEntries.size());

        verifyAppEntryContent(activityInfo, appEntries.get(0));
        verifyAppEntryContent(activityInfo, appEntries.get(1));
    }

    private void verifyAppEntryContent(ActivityInfo activityInfo, AppEntry appEntry) {
        assertEquals(activityInfo.nonLocalizedLabel, appEntry.getLabel());
        assertEquals(activityInfo.packageName, appEntry.getPackageName());
        ComponentName componentNameOne =
                new ComponentName(activityInfo.packageName, activityInfo.name);
        assertEquals(componentNameOne.flattenToString(), appEntry.getComponentName());
        assertEquals(Constants.DEFAULT_TEST_USER_ID, appEntry.getUserId(context));
    }
}

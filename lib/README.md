## libtaskbar

![Bintray](https://img.shields.io/bintray/v/farmerbb/libtaskbar/libtaskbar)

**libtaskbar allows you to quickly and easily add support for Android 10’s Desktop Mode to any third-party launcher, powered by the Taskbar app.**

It's a plug-and-play solution that is lightweight (less than 0.5 MB) and doesn't require Taskbar to already be installed.  libtaskbar gives your users on Android 10+ a fully-featured desktop-style experience with a taskbar, start menu, desktop icons, and more, while being unobtrusive to your launcher's existing phone or tablet experience.

### Setup

Adding Desktop Mode support to your existing launcher is as easy as including these lines in your build.gradle file:

```
repositories {
    jcenter()
}

android {
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation 'com.farmerbb:libtaskbar:2.1.0' // see badge above for latest version
}
```

That's it!  As long as your launcher is set as the system default, and the user has enabled the "Enable freeform windows" and "Force desktop mode" developer options, then Taskbar will appear whenever the user plugs in their HDMI-enabled phone into an external display.

### Additional configuration

You may wish to include a link to the Taskbar settings inside your launcher's settings UI.  To open Taskbar's settings, simply call this function:

    Taskbar.openSettings(context)

If desired, you can also supply a title to show in the top level of the Taskbar settings page, as well as a theme to apply to the activity:

    Taskbar.openSettings(context, "Desktop Mode Settings", R.style.AppTheme)

Finally, while Taskbar's desktop mode functionality is enabled out-of-the-box, it can be programmatically enabled and disabled by calling:

    Taskbar.setEnabled(context, true) // or false

### Things to consider

* libtaskbar doesn't include any UI for informing the user to enable the "Enable freeform windows" and "Force desktop mode" developer options.  You may wish to include a setup flow inside your launcher to guide the user with enabling these options.  Note that a reboot is required for the option to take effect.

* libtaskbar will add the `SYSTEM_ALERT_WINDOW` and `PACKAGE_USAGE_STATS` permissions to your app's manifest, as well as a small number of non-runtime permissions for additional functionality such as displaying a status area on the taskbar.  As a result, your app will appear inside the "Display over other apps" and "Usage access" sections of the "Special app access" page in Android's settings.

* libtaskbar's only transitive dependencies are `androidx.legacy:legacy-support-v4`, `androidx.appcompat:appcompat`, and `com.google.android.material:material`.  As of version 1.0.1, libtaskbar uses AndroidX and therefore requires an AndroidX-based project.  If you're still using the older support libraries, use version 1.0.0 of libtaskbar instead.

* If aapt complains about any resource conflicts, you may need to exclude the `com.google.android.material:material` transitive dependency from libtaskbar inside your build.gradle file.

* libtaskbar currently does not support launchers with a targetSdkVersion of 30 or higher.  For now, please set your targetSdkVersion back to 29 in order to use libtaskbar.

### Example implementation

An example implementation of libtaskbar using Lawnchair is available at https://github.com/farmerbb/libtaskbar-Lawnchair-Example.  

You can also download a prebuilt APK here: https://github.com/farmerbb/libtaskbar-Lawnchair-Example/releases

### Changelog

**v2.1.0**
* Includes all changes from Taskbar 6.1
* Prompts for enabling system alert window and usage access permissions now use the actual name of the app

_Known issues:_
* libtaskbar currently does not support launchers with a targetSdkVersion of 30 or higher

**v2.0.0**
* Includes all changes from Taskbar 6.0
* libtaskbar components now run in a separate process from the rest of the app
* The "Enable freeform windows" developer option is now required, in addition to "Force desktop mode"

**v1.0.1**
* Includes all changes from Taskbar 5.0.1
* Migrated to AndroidX

**v1.0.0**
* Initial release, based off of Taskbar 5.0

<!-- Don't forget to bump the latestChangelogVersion inside MainActivity -->

## Changelog for Taskbar 6.2

This is mostly a behind-the-scenes update, containing the following changes and fixes:

* Many stability fixes for Android 11 and Android 12 devices, as well as Chrome OS devices running ARCVM

* Hide freeform mode settings on most devices running Android 12, where launching freeform apps via Taskbar is non-functional

* Fixed a crash that occurred when using Taskbar's Tasker plugin and a state condition is met

* Hide divider when centering app icons

* Support for enabling additional desktop mode settings via Shizuku

* Themed start button support for Project Sakura PC builds

## Older Changelogs

<details>
<summary>New features in Taskbar 6.1</summary>

### Desktop mode fixes for Android 11

Taskbar's [desktop mode feature](https://www.xda-developers.com/taskbar-samsung-dex-desktop-mode-android-10/) now works much better under Android 11.  Several bugs were fixed in order to make desktop mode a usable experience on Android 11 devices.

There are a couple things to keep in mind, however, when using desktop mode on Android 11:

* The option to hide the navbar has been removed.  This is due to Google's [removal of overscan support](https://www.xda-developers.com/google-confirms-overscan-gone-android-11-crippling-third-party-gesture-apps/) from the Android framework, and unfortunately, nothing can be done about this at the moment.

* Due to changes in how window insets are handled on Android 11, you may experience visual glitches or incorrect placement of Taskbar's UI elements when interacting with text fields while using desktop mode.  To mitigate this, you can temporarily set Taskbar as your input method, which will suppress the on-screen keyboard (IME) for the duration of your desktop mode session.

### Minor visual improvements

When the system tray is enabled, space is no longer taken up for icons that are not currently showing, allowing more space for your recent apps.  Also, the option to center recent app icons now takes into account your device's entire screen width, instead of the width of the recent apps area itself.

On lower resolution devices, the start menu dimensions and font size have been tweaked to make the start menu easier to use.

### Miscellaneous

As usual, bugs have been fixed and top crashes have been dealt with.  Also, German and Chinese translations have been updated, and a Spanish translation has now been added.  Thank you very much to all who have contributed!

</details>

<details>
<summary>New features in Taskbar 6.0</summary>

### Android 10 Desktop Mode support

###### _XDA-Developers article: [Taskbar 6.0 enables a Samsung DeX-like desktop mode experience on some Android 10+ devices](https://www.xda-developers.com/taskbar-samsung-dex-desktop-mode-android-10/)_

Taskbar now has support for Android 10's built-in desktop mode functionality.  You can connect your compatible Android 10+ device to an external display and run apps in resizable windows, with Taskbar's interface running on your external display and your existing launcher still running on your phone.

Desktop mode requires a USB-to-HDMI adapter (or a lapdock), and a compatible device that supports video output.  Additionally, certain settings require granting a special permission via adb.

To get started, open up the Taskbar app and click "Desktop mode".  Then, just tick the checkbox and the app will guide you through the setup process.  For more information, click the (?) icon in the upper-right hand corner of the screen.

### Favorite app shortcuts

You can now launch your favorite apps in a floating window directly from your quick settings drawer (or a home screen shortcut), without needing to keep Taskbar running in the background.  This feature is available on Android 9+ devices.  To get started, simply add one of the new "Launch app" tiles to your quick settings drawer.

The first time you tap the "Launch app" tile, you'll be asked to select an app, choose if you want to launch the app in freeform window mode, and fine-tune the generated icon.  You'll then have a tile in your quick settings drawer with the app's icon and name, ready for you to launch at any time.

You can also choose to put these shortcuts directly on your home screen via the "Launch app" item under Taskbar in your launcher's widgets drawer.

### Backup and restore

Taskbar now allows you to backup and restore your app settings, allowing you to transfer settings more easily between devices, or to keep a copy of your Taskbar settings for archival purposes.  Look for the new "Manage app data" section under "Advanced features" to use the backup and restore functionality.

### System theme support

On Android 9+ devices, Taskbar now respects the system theme setting, automatically switching between light and dark mode along with the rest of the system.  For Android 10 and higher, the "Dark theme" system setting is followed; on Android 9, the "Night mode" developer option is used instead.

### Improved support for Android 11, Android TV, and Chrome OS

Taskbar is now able to launch apps in freeform mode on the Android 11 Developer Preview.

On Android TV 9+ devices like the Nvidia Shield, Taskbar will now guide you to the Android TV settings app for enabling crucial settings like displaying over other apps and usage access.  (Note that Taskbar still needs to be sideloaded on Android TV devices)

Certain app settings that weren't supported on Android TV or Chrome OS were removed on those devices.  The "Replace home screen" feature has also been improved for Chrome OS (though it still can't actually replace your home screen on those devices)

### New and updated translations

A Polish translation has been added, along with updated Chinese and German translations.  Huge thanks to everyone that has contributed to translating the app!

### Miscellaneous features and improvements

A new setting has been added to display a notification count in the status icon area (system tray).  There is also a new option to hide app labels on the start menu and desktop.  Apps that launch in a phone-sized freeform window will now launch in landscape if requested by the app's manifest.

Finally, many bug fixes and under-the-hood improvements have been made, keeping Taskbar well maintained for the future!

</details>

<details>
<summary>New features in Taskbar 5.0</summary>

### Desktop icons

Taskbar now allows you to place icons on your desktop when set as your home screen!

Simply long-press the wallpaper on your home screen to get started.  Select "Add icon to desktop" from the context menu, then select an app from the list to add it to your desktop.

Long-pressing the desktop and selecting "Move icons" will allow you to rearrange your icons.  Press the checkmark button in the lower-right hand corner to lock the desktop back into place.  You can also select "Sort icons by name" to auto-arrange your icons for you.

* PLEASE NOTE: If freeform mode is enabled, desktop icons will only be available if your device is running Android 10 or later.  This is due to technical limitations in how Google has implemented freeform mode on earlier Android versions.

### Status icons & clock on the recent apps tray

Taskbar now includes a status area and clock on the recent apps tray!  Now you can quickly check your battery, network status, and the time and date, all on the corner of your screen.

You can enable this option by going to the "Recent apps" section of Taskbar's settings, and enabling the "Expand area with whitespace" and "Show status icons and clock" options.

Note that this area will not display while Taskbar is in the vertical orientation.

### Custom start button image

You can now customize the start button image beyond the previous "App drawer" and "Taskbar logo" options.  Want to display the Windows logo on your start button instead?  No problem!

Change your start button image by going to the "Appearance" section of Taskbar's settings, and setting the "Start button image" option to "Custom".  The system file picker will appear; just select the image you want to use and you're set!

### Larger start menu on tablets

When the start menu layout is set to "Grid", Taskbar's start menu will now expand to fill either three, four, or five columns, depending on the size of your device's screen, taking better advantage of screen real-estate and helping you locate the apps you want to launch faster.

### New transitions when launching apps

Taskbar will now scale apps onto the screen from their icons as it launches them, for some visual pizazz!

### Better default options

On a fresh Taskbar install, a few of the default settings have now changed.  For example, Taskbar now ships out-of-the-box with a more contrasty color scheme, as well as a faster refresh rate for the recent apps tray.

#### Plus, lots of bug fixes and miscellaneous improvements!
</details>

![Taskbar](http://i.imgur.com/gttRian.png)

Taskbar puts a start menu and recent apps tray on top of your screen that's accessible at any time, increasing your productivity and turning your Android tablet (or phone) into a real multitasking machine!

On devices running Android 7.0 through 8.1, Taskbar can also launch apps in freeform windows for a PC-like experience!  No root required!  (see below for instructions)

Taskbar is also fully supported on Chrome OS - use Taskbar as a secondary Android app launcher on your Chromebook!

## Features
* Start menu - shows you all applications installed on the device, configurable as a list or as a grid
* Recent apps tray - shows your most recently used apps and lets you easily switch between them
* Collapsible and hideable - show it when you need it, hide it when you don't
* Many different configuration options - customize Taskbar however you want
* Pin favorite apps or block the ones you don't want to see
* Designed with keyboard and mouse in mind
* 100% free, open source, and no ads

#### Freeform window mode (Android 7.0 through 8.1)

Taskbar lets you launch apps in freeform floating windows on Android 7.0 and 7.1 (Nougat).  No root access is required.  Android 8.0 and 8.1 (Oreo) are also supported via an adb shell command.  Versions earlier than 7.0 and later than 8.1 are not supported.

Simply follow these steps to configure your device for launching apps in freeform mode:

1. Check the box for "Freeform window support" inside the Taskbar app
2. Follow the directions that appear in the pop-up to enable the proper settings on your device (one-time setup)
3. Go to your device's recent apps page and clear all recent apps
4. Select an app using Taskbar to launch it in a freeform window

For more information and detailed instructions, click "Help & instructions for freeform mode" inside the Taskbar app.

## Download
Taskbar can be downloaded as a standalone Android app from:

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Google Play"
      height="80"
      align="middle">](https://play.google.com/store/apps/details?id=com.farmerbb.taskbar)
[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="F-Droid"
      height="80"
      align="middle">](https://f-droid.org/packages/com.farmerbb.taskbar/)

Taskbar is also included as part of the following Android distributions for PCs:

* Android-x86 (7.1-rc2 and later) (http://www.android-x86.org)
* Bliss OS (x86 builds) (https://blissroms.com)

## How to Build
Prerequisites:
* Windows/MacOS/Linux
* JDK 8
* Android SDK
* Internet connection (to download dependencies)

Once all the prerequisites are met, simply cd to the base directory of the project and run "./gradlew assembleFreeDebug" to start the build.  Dependencies will download and the build will run.  After the build completes, cd to "app/build/outputs/apk" where you will end up with the APK file "app-free-debug.apk", ready to install on your Android device.

## Icon Pack Support
Taskbar includes support for ADW-style icon packs.  If you are an icon pack developer and would like to include support for applying the icon pack from within your app, simply use the following code:

    Intent intent = new Intent("com.farmerbb.taskbar.APPLY_ICON_PACK");
    intent.putExtra("android.intent.extra.PACKAGE_NAME", "com.iconpack.name");
    startActivity(intent);

## Contributors
* Mark Morilla (app logo) (https://plus.google.com/106169552593075739372)
* naofum (Japanese translation)
* HardSer (Russian translation)
* OfficialMITX (German translation)
* Whale Majida (Chinese translation)
* Mesut Han (Turkish translation)

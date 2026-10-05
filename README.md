# Android Taskbar

**Turn your Android device into a desktop.**

Open the app and your phone or tablet becomes a desktop: wallpaper, desktop icons, widgets, a taskbar and a start menu. Close it and everything stops. It behaves like an app you open and quit, not an overlay that sits on top of everything else.

> **Status:** early. It builds and passes its automated tests, but it has had limited testing on real devices. Expect rough edges, and please [open an issue](../../issues) if you hit one.

## What makes this different

This project started from [Taskbar](https://github.com/farmerbb/Taskbar) by Braden Farmer, which draws a taskbar over your existing launcher. This version changes how you use it:

* **One icon, one desktop.** Tapping the app opens a full desktop instead of a settings screen.
* **Start and stop like an app.** The desktop session runs while you use it and ends when you exit it. Taskbar's services stop when you exit.
* **Widgets on the desktop.** Add, move, resize and remove home-screen style widgets.
* **A visible way out.** "Exit desktop" is in the start menu, with a confirmation so you don't close it by accident.
* **Keyboard friendly.** The Windows/Meta key toggles the start menu, and Alt+F4 exits.

## Using it

| To do this | Do this |
| --- | --- |
| Start the desktop | Open the app from your launcher |
| Add a widget | Long-press an empty spot on the desktop, then choose **Add widget** |
| Move, resize or remove a widget | Long-press the widget and choose from the menu |
| Open the start menu | Tap the start button, or press the Windows/Meta key |
| Change settings | Start menu, then the ⋮ menu, then **Open settings** (also available from the app's system info page or its notification) |
| Exit | Start menu, then the ⋮ menu, then **Exit desktop**, or press Alt+F4, or swipe the app away from recents |

On first launch the app asks for the "Display over other apps" permission, which the taskbar needs. It may also ask for usage access so it can show your recent apps.

## Also included

Everything that Taskbar already did still works:

* Start menu as a list or a grid, with search
* Recent apps tray, plus pinned and blocked apps
* Desktop icons, wallpaper and ADW-style icon packs
* Freeform window mode on Android 7.0+ (some versions need a one-time `adb` step)
* Android 10+ desktop mode on an external display
* Designed with a keyboard and mouse in mind

## Get it

There are no store releases yet. To try it, download the latest debug build from this repository's **Actions** tab:

1. Open the [Actions tab](../../actions) and click the most recent successful **CI Check** run.
2. Under **Artifacts**, download `taskbar-debug-apk` and unzip it.
3. Copy the `.apk` to your device and install it (you'll need to allow installs from unknown sources).

The debug build installs as a separate app (`com.farmerbb.taskbar.debug`), so it won't replace any Taskbar you already have.

## Building from source

You need:

* JDK 21
* The Android SDK, with the `ANDROID_HOME` environment variable pointing at it
* An internet connection to download dependencies

Then run:

```
./gradlew assembleFreeDebug
```

The APK ends up in `app/build/outputs/apk/free/debug/`.

### Tests and checks

```
./gradlew testFreeDebug      # unit tests (Robolectric)
./gradlew spotlessCheck      # code style
```

GitHub Actions runs the build, style check and tests on every pull request to `master`.

## Contributing

Pull requests are welcome. If you change behavior, please add or update a test alongside it. The existing tests use Robolectric and live under `app/src/test`.

## Credits and license

This project is based on [Taskbar](https://github.com/farmerbb/Taskbar) by Braden Farmer and its contributors, and is released under the same [Apache License 2.0](LICENSE). See [NOTICE](NOTICE) for attribution details.

Thanks to the people who contributed to the original project, including Mark Morilla (app logo), naofum, HardSer, OfficialMITX, Whale Majida, Mesut Han, Zbigniew Zienko, utzcoz, RaspberryPiFan, Diego Sangunietti, Tommy He, Aaron Dewes and Ingo Brückl (translations, code cleanup and testing), and to Mishaal Rahman, Jon West and Chih-Wei Huang for their early support.

This is an independent fork and is not affiliated with or endorsed by the original author.

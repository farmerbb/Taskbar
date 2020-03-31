## Contributing

Pull requests of any type are welcome!  However, I do ask that contributors keep the following guidelines in mind before submitting a PR:

### Translations

Feel free to add new translations, or update an existing one!  The following applies to PRs that involve translations:

* Please base your translations off of `app/src/main/res/values/strings.xml` as found in this repo, instead of decompiling the APK and translating the strings from there.  (Note that there are a handful of strings found inside `values-land`, `values-port`, and `values-v28` that should be translated as well)
* Please be mindful of any adb shell commands found inside the strings, which should remain untranslated.
* Please also add the country code to the `resConfigs` list found inside `app/build.gradle`

### AOSP compatibility

Taskbar must retain the ability to be built completely from AOSP source.  This is due to its inclusion as part of Android-x86.  Due to this:

* Please try to refrain from adding any new third-party libraries.  If third-party libraries are added, please confine all code that references this library to the `app/src/playstore` directory, and add an alternative (stub?) implementation in the corresponding `app/src/nonplaystore` directory. 
* Please use the Java programming language for your contributions.  PRs written in Kotlin are allowed ONLY if the corresponding logic to allow Kotlin code to be built with AOSP is also added.  PRs written in other programming languages will not be accepted.

### Style guidelines

In order to keep PRs clean, please only include changes that are relevant to the overall feature that you are wanting to add.

* Please make a good effort to stick with the overall code style and structure of the project, unless the primary purpose of the PR involves improving / refactoring said code structure.
* Please try not to change any whitespace, bracket placement, etc. unless it is on a line of code that you are directly modifying as part of the PR.

### Other notes

I reserve the right to request any changes that may be made in order for a PR to be accepted, or to deny the PR altogether.

When contributing, feel free to add yourself to the Contributors list at the bottom of the README.

In lieu of adopting a formal Code of Conduct, please assume the best from people, treat each other like adults, and be courteous and kind to others.  Happy contributing!

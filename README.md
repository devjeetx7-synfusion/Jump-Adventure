# Jump-Adventure

[![Jump Adventure - Build APK](https://github.com/devjeetx7-synfusion/Jump-Adventure/actions/workflows/build-apk.yml/badge.svg)](https://github.com/devjeetx7-synfusion/Jump-Adventure/actions/workflows/build-apk.yml)

Jump Adventure is an offline 2D portrait platformer Android game built with Kotlin and native Android Views.

## Package

`com.synfusion.jump`

## GitHub APK Build

The APK is built manually using GitHub Actions.

Steps:

1. Open GitHub Actions
2. Select "Jump Adventure - Build APK"
3. Click "Run workflow"
4. Wait for the build
5. Open the successful workflow run
6. Download `JumpAdventure-APK`

## Installation & Update Behavior

- **Package ID:** The package ID remains fixed as `com.synfusion.jump`.
- **Signing Key:** The exact same existing TEST signing keystore (`app/jump-adventure-test.jks`) is reused across every GitHub Actions build.
- **Version Code:** Each manual build automatically increments the `versionCode` using the GitHub Actions run number.
- **In-Place Update:** Because the package ID, signing certificate, and data structure remain unchanged and the `versionCode` increases, new builds can be directly installed as an update over previous TEST builds without requiring the user to uninstall the application.

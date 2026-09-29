# Jump-Adventure

[![Jump Adventure - Android CI](https://github.com/devjeetx7-synfusion/Jump-Adventure/actions/workflows/build-apk.yml/badge.svg)](https://github.com/devjeetx7-synfusion/Jump-Adventure/actions/workflows/build-apk.yml)

Jump Adventure is an offline 2D portrait platformer Android game built with Kotlin and native Android Views.

## Package

`com.synfusion.jump`

## CI

Pull requests, `main`, and `fix/**` branch pushes run:

1. `testDebugUnitTest`
2. `lintDebug`
3. `assembleDebug`

This keeps gameplay/code changes build-tested before merge.

## Signed release APK

Signed release builds are manual (`workflow_dispatch`) and require private GitHub Actions secrets. Signing keys and passwords are **not stored in the repository**.

Required secrets:

- `RELEASE_KEYSTORE_BASE64` — base64-encoded release keystore
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

To create a signed APK:

1. Open **Actions**.
2. Select **Jump Adventure - Android CI**.
3. Select **Run workflow**.
4. Wait for both the verification and release jobs to pass.
5. Download the `JumpAdventure-APK` artifact.

## Signing / update behavior

- **Package ID:** `com.synfusion.jump`
- Android in-place updates require the same trusted signing certificate and an increasing `versionCode`.
- The workflow uses the GitHub Actions run number as the release `versionCode`.
- The old repository-committed TEST keystore must not be used for production distribution. Treat any signing material that was publicly committed as compromised and create a new private release key for production.

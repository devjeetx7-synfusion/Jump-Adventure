# Jump-Adventure

[![Jump Adventure - Build APK](https://github.com/devjeetx7-synfusion/Jump-Adventure/actions/workflows/build-apk.yml/badge.svg)](https://github.com/devjeetx7-synfusion/Jump-Adventure/actions/workflows/build-apk.yml)

Jump Adventure is an offline 2D portrait platformer Android game built with Kotlin and native Android Views.

## GitHub Actions APK Build

1. Open GitHub repository in your browser.
2. Open **Actions** tab.
3. Select **Jump Adventure - Build APK** workflow from the left sidebar.
4. Click **Run workflow** button.
5. Wait for tests, build, keystore generation, release signing, and verification to finish.
6. Open the successful workflow run.
7. Scroll down to the **Artifacts** section.
8. Download the generated artifacts:
   - `JumpAdventure-debug-apk`
   - `JumpAdventure-test-release-apk`
   - `JumpAdventure-test-keystore`

## Test Keystore

This repository workflow generates a TEST-ONLY Android signing key during the build process.

- **Keystore file:** `jump-adventure-test.jks`
- **Alias:** `jump_adventure_test`
- **Store Password:** `JumpAdventureTest2026!`
- **Key Password:** `JumpAdventureTest2026!`

> **IMPORTANT: TEST BUILD ONLY — NOT FOR PRODUCTION**
>
> This key is intentionally public/reproducible and MUST NOT be used for a production release.
> For Google Play production publishing, create and securely store a separate production signing key.
> Never commit the production keystore or production passwords to GitHub.

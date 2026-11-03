# PROJECT_AUDIT.md — Jump Adventure
### Complete Read-Only Source Audit

| | |
|---|---|
| **Repository** | `devjeetx7-synfusion/Jump-Adventure` |
| **Branch audited** | `arena/9e41e7be-jump-adventure` (from `main` @ `2890796`) |
| **Audit date** | 2026-11-03 |
| **Audit type** | Read-only. **No source file was modified, added, or deleted.** This document is the only file created. |
| **Files inspected** | **94 / 94** tracked files (26 Kotlin, 30 XML/resource, 18 binaries, 4 Gradle, 16 misc) |
| **Kotlin LOC** | 9,038 (main) + 143 (test) |
| **Build verification** | **Static only.** No JDK, Android SDK, or Gradle cache exists in the audit sandbox (`java: command not found`, `ANDROID_HOME` empty). No APK was built. All "build-blocking" findings below are proven by source/resource cross-reference, not by a compiler run. |
| **Git history available** | 1 commit (squashed) — historical recovery/regression analysis was **not possible**. |

---

## 0. EXECUTIVE SUMMARY

Jump Adventure is a well-structured, single-activity **2D portrait platformer** built with Kotlin + native Android Views (no game engine, no Compose). The architecture is cleanly layered (`model` / `data` / `level` / `engine` / `graphics` / `audio`), the procedural level generator is genuinely deterministic and unit-tested, and all art and audio are produced procedurally at runtime — the APK has **zero third-party game/audio libraries** and only 5 AndroidX dependencies.

**However, the app cannot currently be compiled.** Three resource and symbol references point at files that do not exist anywhere in the repository or in its history, and the README advertises a CI pipeline that is not present. This is the single most important finding.

### Headline results

| Metric | Value |
|---|---|
| Features assessed | 42 |
| **WORKING** | 17 |
| **PARTIALLY WORKING** | 12 |
| **NOT WORKING** | 6 |
| **NOT IMPLEMENTED** | 7 |
| Bugs found | 48 (4 Critical, 9 High, 17 Medium, 18 Low) |
| Compile-blocking defects | 3 |
| Runtime crash risks | 6 |
| Dead/unused assets | 11 drawables + 1 duplicated image (204 KB) + 2 whole classes |
| Dead data fields in save model | 9 |

### The three facts that matter most

1. **`CharacterCardView` does not exist.** `MainActivity.kt:818` and `:1123` instantiate `com.jumpadventure.game.graphics.CharacterCardView`, but no such class (or file) exists — not in the repo, not in git history. → **compile error**.
2. **Three drawables referenced by name do not exist.** `bg_card_glossy` (7 sites), `bg_card_selected` (4 sites), `bg_pill_badge` (2 layout sites) are absent. → **AAPT link error**.
3. **`app/proguard-rules.pro` is referenced but missing**, and **the CI workflow that the README documents does not exist**. → release build failure + no verification gate.

Detail, severity, and file/line for every issue is in §9.

---

## 1. COMPLETE PROJECT STRUCTURE

```
Jump-Adventure/
├── .gitignore                          (20 lines — ignores *.jks, *.keystore, *.apk, build/)
├── README.md                           (45 lines — documents CI + signing; CI is absent, see BUG-04)
├── build.gradle.kts                    (8 lines — plugin declarations only)
├── settings.gradle.kts                 (20 lines — repos: google, mavenCentral, google-maven mirror)
├── gradle.properties                   (4 lines — AndroidX on, nonTransitiveRClass, 2048m heap)
├── gradlew / gradlew.bat               (Gradle 8.8 wrapper scripts)
├── gradle/
│   ├── libs.versions.toml              (version catalog)
│   └── wrapper/{gradle-wrapper.jar, gradle-wrapper.properties}   → Gradle 8.8
└── app/
    ├── build.gradle.kts                (66 lines — AGP app module, signing, deps)
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml     (2 activities, portrait-locked)
        │   ├── java/com/jumpadventure/game/
        │   │   ├── MainActivity.kt                   2216 ★  the entire UI/nav/shop layer
        │   │   ├── CrashActivity.kt                    97      custom crash reporter screen
        │   │   ├── JumpAdventureApplication.kt         23      global uncaught-exception hook
        │   │   ├── engine/
        │   │   │   └── GameView.kt                   1387 ★  SurfaceView game loop + physics
        │   │   ├── audio/
        │   │   │   └── SoundManager.kt                304      procedural AudioTrack synth
        │   │   ├── data/
        │   │   │   └── SaveManager.kt                 325      SharedPreferences + JSON save
        │   │   ├── level/
        │   │   │   ├── LevelGenerator.kt              261      deterministic procedural levels
        │   │   │   └── WorldRepository.kt              72      14-theme infinite world mapping
        │   │   ├── model/
        │   │   │   └── GameModels.kt                  117      6 data classes (save schema)
        │   │   ├── util/
        │   │   │   └── InsetsManager.kt                95      edge-to-edge inset application
        │   │   └── graphics/                          (16 custom View classes)
        │   │       ├── CharacterRenderer.kt           376      procedural character drawing
        │   │       ├── LevelMapView.kt                421      sinusoidal level-select map
        │   │       ├── ControlCustomizationView.kt    384      drag/resize control editor
        │   │       ├── GameBottomNavView.kt           332      4-tab custom bottom nav
        │   │       ├── Stars3DView.kt                 301      winner-dialog 3D stars
        │   │       ├── PlayerSpeedControlView.kt      301      speed slider w/ live preview
        │   │       ├── GamePrimaryButton.kt           290      8-variant 3D button
        │   │       ├── GameCurrencyBadge.kt           284      coin/gem pill + "+" button
        │   │       ├── CharacterPreviewView.kt        279      home-screen animated character
        │   │       ├── RewardSummaryView.kt           198      coins/stars/time chips
        │   │       ├── GameDialogBoardView.kt         193      dialog board background
        │   │       ├── HomeEnvironmentView.kt         184      ⚠️ NEVER REFERENCED
        │   │       ├── GamePowerUpButton.kt           183      ⚠️ NEVER REFERENCED
        │   │       ├── SparkleView.kt                 169      particle celebration
        │   │       ├── CurvedTitleView.kt             114      arched title text
        │   │       └── GameDialogView.kt              132      modal dialog panel
        │   └── res/
        │       ├── layout/                    (7 files)
        │       │   ├── activity_main.xml             36  6 stacked <include> layers
        │       │   ├── layout_main_menu.xml          116  home screen
        │       │   ├── layout_level_map.xml           86  map + floating HUD
        │       │   ├── layout_gameplay.xml           131  GameView container + HUD
        │       │   ├── layout_secondary_screen.xml    87  shared scrollable screen
        │       │   ├── layout_control_customization.xml 132 control editor
        │       │   └── layout_overlay.xml            142  pause/win/dialog overlay
        │       ├── drawable/                  (23 XML + 1 PNG)
        │       ├── drawable-nodpi/            (7 WebP — 1.96 MB total)
        │       ├── mipmap-*/                  (10 PNG launcher icons, 5 densities)
        │       ├── mipmap-anydpi-v26/         (2 adaptive icons)
        │       └── values/                    (colors.xml 54, strings.xml 5, styles.xml 28)
        └── test/java/com/jumpadventure/game/
            ├── LevelGeneratorTest.kt          (47 lines — 3 tests)
            ├── LevelSafetyTest.kt             (60 lines — 3 tests)
            └── SaveSanitizationTest.kt        (36 lines — 2 tests)
```

**No `androidTest/` directory exists. No instrumented tests.**

### Resource inventory

| Type | Count | Notes |
|---|---|---|
| Layout XML | 7 | All screens are `<include>`d into `activity_main.xml` |
| Vector/bg drawable XML | 23 | 11 are unused (see §10) |
| WebP backgrounds | 7 | `home_bg` is byte-identical to `green_forest` (md5 `17394c4d…`) |
| Launcher PNGs | 10 | 5 densities × (square + round) |
| `values/` files | 3 | **Only 1 string resource exists** (`app_name`) |

---

## 2. ARCHITECTURE

### 2.1 Layering (as actually implemented)

```
┌──────────────────────────────────────────────────────────────────────┐
│  MainActivity  (2,216 lines)  ← GOD OBJECT                           │
│  · all navigation (6 mutually-exclusive <include> layers)            │
│  · 6 secondary screens built entirely in imperative Kotlin            │
│  · shop (5 categories), achievements, worlds, settings, controls     │
│  · pause / win / game-over / modal dialogs                           │
│  · owns SaveManager, SoundManager, GameSaveData, GameView            │
└───────┬────────────────┬──────────────┬──────────────┬───────────────┘
        │                │              │              │
   ┌────▼─────┐   ┌──────▼──────┐  ┌────▼─────┐  ┌─────▼──────┐
   │ GameView │   │ SaveManager │  │ SoundMgr │  │ Graphics/* │
   │ (engine) │   │  (persist)  │  │ (audio)  │  │ 16 Views   │
   └────┬─────┘   └──────┬──────┘  └──────────┘  └─────┬──────┘
        │                │                             │
   ┌────▼──────────────┐ │                        ┌────▼──────────┐
   │ LevelGenerator    │ │                        │ CharacterRenderer│
   │ WorldRepository   │ │                        │ (shared by both) │
   └────┬──────────────┘ │                        └─────────────────┘
        │                │
   ┌────▼────────────────▼────┐
   │ GameModels (save schema) │
   └──────────────────────────┘
```

**Strengths:** clean package boundaries, zero magic globals (except `WorldRepository`/`LevelGenerator` objects, which are pure), no network code, no reflection, no persistent threads outside the game/audio loops.

**Central weakness:** `MainActivity` is a 2,216-line god object. Six screens are built by imperatively constructing views in Kotlin rather than inflating XML, which produces heavy duplication (see BUG-17). It is also the only `ViewModel`-less screen owner — all state is `private lateinit var saveData` inside the Activity.

### 2.2 Navigation model

There is **no `Fragment`, no `NavController`, no multiple-Activity flow**. Navigation is a single `showScreen(String)` function (`MainActivity.kt:330`) that flips `visibility` on six stacked includes inside `activity_main.xml`:

```kotlin
private fun showScreen(screenName: String) {
    currentScreenName = screenName
    incMainMenu.visibility = View.GONE
    incLevelMap.visibility = View.GONE
    incGameplay.visibility = View.GONE
    incSecondary.visibility = View.GONE
    incControlCustomization.visibility = View.GONE
    incOverlay.visibility = View.GONE            // ← also hides the overlay
    …
    when (screenName) {
        "MAIN_MENU" -> …;  "LEVEL_MAP" -> …;  "GAMEPLAY" -> …
        "SECONDARY" -> …;  "CUSTOM_CONTROLS" -> …;  "OVERLAY" -> …
    }
}
```

`"SECONDARY"` is a **single** shared screen whose *content* is swapped by whichever `openXxxScreen()` ran last. `layout_secondary_screen.xml` provides one `ScrollView` + `LinearLayout`; the caller wipes and repopulates it. This means Hero, Shop, Worlds, Trophies and Settings all share one container and one back button.

**Important consequence:** `showScreen()` sets `incOverlay.visibility = GONE` on **every** navigation call. Any flow that navigates while a dialog is open silently dismisses the dialog (verified: `resetProgress → showScreen("MAIN_MENU")` closes the reset dialog, which is the intended behaviour here, but the same mechanism makes dialogs un-stackable).

**Navigation graph (verified against source):**

```
                     ┌──────────────┐
        ┌───────────►│  MAIN_MENU   │◄──────────────┐
        │            └──┬────┬───┬──┘               │
        │        Play   │    │   │  Settings        │
        │               │    │   └────────────┐     │
        │               ▼    │                ▼     │
        │        ┌─────────┐ │         ┌────────────┐
        │        │LEVEL_MAP│ │         │  SETTINGS  │──► CUSTOM_CONTROLS
        │        └────┬────┘ │         └────────────┘        │
        │             │ node │                              │
        │             ▼      │ BottomNav: SHOP / HEROES /   │
        │        ┌─────────┐ │  WORLDS / TROPHIES          │
        │        │GAMEPLAY │─┘        │                    │
        │        └────┬────┘          ▼                    │
        │             │        ┌──────────────┐            │
        │   pause/win/│        │  SECONDARY   │────────────┘
        │   game-over │        │ (Hero/Shop/  │
        │             ▼        │ Worlds/      │
        │        ┌─────────┐   │ Trophies)    │
        └────────┤ OVERLAY │   └──────────────┘
                 └─────────┘
```

---

## 3. SCREEN-BY-SCREEN BREAKDOWN

Legend: **W** = WORKING, **P** = PARTIALLY WORKING, **N** = NOT WORKING, **NI** = NOT IMPLEMENTED

### 3.1 MAIN MENU — `layout_main_menu.xml` + `MainActivity.kt:159–322` — **W**

| Element | ID | Behaviour | Status |
|---|---|---|---|
| Background art | `ivHomeBg` | static `home_bg.webp` | **W** |
| Logo | `ivHomeLogo` | `jumpadventure_home.webp`, responsively resized | **W** |
| Mountain platform | `ivMountainPlatform` | `mountain.webp` (2,857×3,264) | **W** |
| Character preview | `charPreviewView` | animated idle walk, tap-to-hop | **W** |
| PLAY NOW | `btnPlay` | shows `"LEVEL N"` subtext, opens level map | **W** |
| Coin badge | `badgeCoins` | shows `saveData.coins`, `+` → shop | **W** |
| Gem badge | `badgeGems` | shows `saveData.gems`, `+` → shop | **W** |
| Settings | `btnSettings` | opens settings | **W** |
| Bottom nav | `bottomNavView` | 4 tabs → shop / heroes / worlds / trophies | **W** |

`setupResponsiveHomeLayout()` (`:159`) recomputes logo/button/mountain/character layout in a `addOnLayoutChangeListener`, including a hard-coded aspect ratio `3264f / 2857f` matching `mountain.webp`. Responsive behaviour is genuinely implemented and guarded with `runCatching`.

**Note:** `incMainMenu` is a `RelativeLayout` whose **root id `homeRootLayout` is never used**, and `ivHomeBg`, `currencyContainer` are also never referenced from Kotlin — harmless but dead.

### 3.2 LEVEL MAP — `layout_level_map.xml` + `LevelMapView.kt` — **P**

- Custom `View` draws an infinitely-scrolling sinusoidal path of level nodes, world banner backgrounds, star ratings, boss nodes (`lvl % 5 == 0 || lvl % 25 == 0` — the second condition is redundant), and a lock glyph.
- `maxRenderLevel = maxOf(currentHighestLevel + 35, 50)` — renders 35 levels ahead so upward scrolling reveals upcoming levels.
- `getScrollYForLevel()` centres the highest level on open.
- Tapping a node calls `onNodeClicked` **only if `node.isUnlocked`** (`:412`).

**Defects found (see §9):**
- The header always says `"WORLD ${currentWorld.id}"` computed from `saveData.highestLevel` (`MainActivity.kt:359–361`) but the map renders/pans across **all** worlds, and it **never updates** while scrolling. So while scrolling through World 2 the header still reads "WORLD 1". → **P**
- The snake animation `animTime += 0.05f` in `onDraw` (`:177`) is used for the current-node pulse — but `invalidate()` is only called from `setupMap()` (`:123`), never from `onDraw`. The pulse therefore **freezes after the first frame**. → **N (animation dead)**
- `visibleNodes` is computed with `nodes.filter { … }` then `nodes.indexOf(visibleNodes.first())` — O(n) scans per frame inside `onDraw`, plus a `RectF` allocation per node per frame.
- `isBoss` is calculated and drawn larger/red but **has no gameplay meaning** — no boss level exists in `LevelGenerator`.

### 3.3 GAMEPLAY — `layout_gameplay.xml` + `GameView.kt` — **P**

| Element | Behaviour | Status |
|---|---|---|
| Level title | `tvHudLevel` = "LEVEL n" | **W** |
| Progress bar | `pbLevelProgress` from `playerX / totalWidth` | **W** |
| Coin counter | `tvHudCoins` (level-local, not bank) | **W** |
| Star counter | `tvHudStars` | **W** |
| Pause button | `btnPause` → `showPauseOverlay()` | **W** |
| Game canvas | `gameViewContainer` (SurfaceView) | **P — see HUD occlusion below** |
| Lifetime/lives HUD | — | **NI (no lives display)** |

**Critical UI/engine disconnect:** `layout_gameplay.xml` puts `gameViewContainer` at `match_parent` **first**, then draws the HUD `RelativeLayout` (`hudTopBar`) over it. The HUD is fully opaque-ish and is **not** touch-transparent in the areas it covers, but more importantly `hudTopBar` is `wrap_content` at the top while the on-canvas control circles are positioned bottom-anchored by `updateControlLayouts` — that part is fine. The real issue is the **HUD sits on top of the game canvas but the canvas does not know the HUD height**, so `playerY`/`cameraY` are never adjusted and the play field starts under the HUD. Combined with the **absent vertical camera** (BUG-07) this means tall-platform sections are drawn off-screen upward.

Also: there is **no lives/hearts display anywhere** even though `GameView.lives = 3` and lives are decremented — the player gets no feedback on remaining lives.

### 3.4 SECONDARY: HEROES — `MainActivity.kt:768–894` — **P**

- Title "HEROES", coin badge (no `+`).
- 25 characters in a 2-per-row grid built by `getFullCharacterList()` (`:1060`).
- Card shows name, description, and an action button: `EQUIPPED` / `EQUIP` / `UNLOCK <price> COINS`.
- Unlock path here goes through `showCustomCharacterPreviewDialog()` (`:895`) → confirm dialog → deduct coins.

**Defects:** crashes at compile time due to `CharacterCardView` (BUG-01). Card background also references the missing `bg_card_glossy`/`bg_card_selected` (BUG-02). The character's `skillName`/`skillDesc` fields are **never displayed** — the UI shows `item.description` only, so the player cannot see that e.g. Galaxy Hero has Double Jump.

### 3.5 SECONDARY: SHOP — `MainActivity.kt:987–1660` — **P**

Five categories via a horizontal `GamePrimaryButton` row: `CHARACTERS`, `SKINS`, `TRAILS`, `POWER-UPS`, `CURRENCY`.

| Category | Renderer | Items | Status |
|---|---|---|---|
| CHARACTERS | `renderShopCharacters` `:1090` | 25 | **P** — same crash/missing-drawable issues; buys directly (no confirm) |
| SKINS | `renderShopSkins` `:1196` | 7 | **P** — colour-swatch only; skin has **no visible effect in gameplay beyond tint** (works, but the "skin" is a palette swap) |
| TRAILS | `renderShopTrails` `:1321` | 12 | **W** — trails genuinely render (`GameView.updateParticles` + `trailColor`) |
| POWER-UPS | `renderShopPowerUps` `:1451` | 6 upgrades, max L5 | **W** — `powerUpLevels` genuinely drive engine values |
| CURRENCY | `renderShopCurrency` `:1592` | 12 "packs" with `CLAIM` | **N** — see below |

**CURRENCY is completely non-functional.** `renderShopCurrency` creates packs labelled "1000 Coin Pack" … "10,000 Gem Pack" each with a **`CLAIM`** button that unconditionally does:

```kotlin
setOnClickListener {
    soundManager.playCoin()
    if (isGem) saveData.gems += amount else saveData.coins += amount
    saveManager.saveData(saveData)
    secondaryBadgeCoins.amount = saveData.coins
    showCustomGameDialog(title = "REWARD CLAIMED!", …)
}
```

There is **no payment, no billing, no ad, no cooldown, no limit**. It is free money. `SaveManager.rejectUnsupportedCurrencyGrants()` (`:236–275`) was clearly written as a *guard* against exactly this: it rolls back any coin/gem increase that isn't accompanied by a "trusted progression signal". The net effect is a **silent no-op** — the dialog says "REWARD CLAIMED! +1,000,000 Coins" while the wallet is silently reverted. Verified logic path:

```
claim → coins += 1_000_000 → saveData()
      → rejectUnsupportedCurrencyGrants(): data.coins > oldCoins && !progressionSignal
      → data.coins = oldCoins   (Log.w "Rejected unsupported coin grant")
      → dialog still says "REWARD CLAIMED!"
      → secondaryBadgeCoins.amount set from the *mutated in-memory* saveData (stale)
```

So the button both **lies to the user** and **does not work**, and the in-memory `saveData.coins` is left inflated relative to disk until the next save. Severity: **High** (see BUG-05).

Also `activeShopCategory` (`:987`) is a plain Activity field — it **resets to `"CHARACTERS"` on every `openShopScreen()` call from outside**, so leaving the shop from the TRAILS tab and returning via the nav bar lands you back on CHARACTERS. Minor UX.

### 3.6 SECONDARY: WORLDS — `MainActivity.kt:1661–1859` — **P**

- Renders `WorldRepository.worlds` — that is **50 world cards**, each with a 214 dp `FrameLayout` holding a background image, a gradient veil, title, level range, two reward badges and an `EXPLORE`/`LOCKED` button.
- 50 cards × (2 `GradientDrawable` + 1 `ImageView` + 3 `TextView` + 2 badges each with `GradientDrawable` + `ImageView` + `TextView` + a `GamePrimaryButton`) ≈ **600+ views built synchronously on the UI thread** on every open. This is a measurable jank source (BUG-11).

**Unlock rule is wrong and inverted:**
```kotlin
val isUnlocked = saveData.unlockedWorlds.contains(world.id)
    || totalStars >= world.requiredStarsToUnlock
    || saveData.highestLevel >= world.startLevel
```
`requiredStarsToUnlock = (worldId - 1) * 15` and `world.startLevel = (worldId-1)*25 + 1`. Because `highestLevel >= startLevel` alone unlocks a world, **the star requirement is entirely bypassed** — reaching level 26 unlocks World 2 with zero stars. Worse, `|| totalStars >= …` at world 1 gives `requiredStarsToUnlock = 0`, so **every world with `requiredStarsToUnlock == 0` is unlocked**, i.e. World 1. Combined: the "N STARS REQUIRED" label shown on locked cards is **decorative and false**. → **P/N**

`saveData.unlockedWorlds` is **never written to by gameplay** — only read. It starts as `{1}` and never grows (grep confirms no `unlockedWorlds.add` outside `SaveManager` load/sanitise). So the "world progression reward" mentioned in `SaveManager`'s doc comment has no implementation.

**`EXPLORE` mutates progress:** it does `saveData.currentLevel = world.startLevel` and persists — meaning a player can jump to World 40's level 976 by clicking EXPLORE on an unlocked card, *without having the stars*. Since unlocking keys off `highestLevel`, this then cascades: reaching level 976 sets `highestLevel` to 977 which unlocks every world up to 40. **Progression is trivially skippable.** → **High (BUG-06)**

The coin/gem badges (`+${world.id * 100}` coins, `+${world.id * 2}` gems) are **purely decorative** — no code ever awards them. → **NI**

### 3.7 SECONDARY: TROPHIES (Achievements) — `MainActivity.kt:1861–2015` — **W (for claiming), P (for icons)**

30 `AchievementItem`s across 5 groups (Progress, Coins, Stars, Gameplay, Infinite Worlds), each with a lambda predicate on `GameSaveData`. Claim flow: if predicate true and not already claimed → `coins += rewardCoins`, add to `unlockedAchievements`, save, re-render. `CLAIMED` state is correctly persisted and disables the button.

- Achievements are evaluated **only when the screen is opened** — no toast/notification when one becomes claimable. → **P**
- `AchievementItem.icon` (the `String` field) is **never used**; every card hardcodes `R.drawable.ic_trophy` (`:1966`). All 30 rows look identical. → **P / cosmetic**
- `unlockedAchievements.size` growth is used as a `progressionSignal` in `SaveManager`, so claiming works and is not rolled back. ✔

### 3.8 SECONDARY: SETTINGS — `MainActivity.kt:2016–2085` — **P**

| Control | Behaviour | Status |
|---|---|---|
| Speed slider | `PlayerSpeedControlView`, 0.75×–1.50×, live preview | **W** |
| SOUND EFFECTS | toggles `soundManager.soundEnabled`, persists | **W** |
| BACKGROUND MUSIC | toggles `musicEnabled` → setter stops/starts music | **W** |
| CUSTOMIZE CONTROLS | → control editor | **W** |
| RESET ALL PROGRESS | confirm dialog → `resetProgress()` → reload | **W** |
| Vibration toggle | field exists in save model | **NI** |
| Language selector | field exists in save model | **NI** |

**Defect:** toggling SOUND or MUSIC updates `subText` **only for the button that was tapped** — the sibling button's label is not refreshed, but since each is recreated on screen open this is fine. However **neither toggle re-renders**, so the "STATE: ON/OFF" text is correct. ✔

**Bug (Medium):** `PlayerSpeedControlView.onSpeedChangedListener` fires **on every `ACTION_MOVE`** (`:278`) and the listener does `saveManager.saveData(saveData)` — a full SharedPreferences + JSON serialisation per touch-move event. That is a `synchronized` disk-backed write at ~100 Hz during a drag. → **BUG-12**

### 3.9 CUSTOM CONTROLS — `layout_control_customization.xml` + `ControlCustomizationView.kt` — **W**

Drag to move, ±10% scale via toolbar, SAVE LAYOUT, RESET DEFAULT. Persists all 6 controls (`left/right/jump/magnet/speed/shield`: X, Y, Scale) into the save. `GameView.updateControlLayouts()` (`:362`) reads them back and clamps to screen bounds. **This feature is fully round-tripped and genuinely works.**

Minor: `controlCustomizationRoot`, `ivCustomBg`, `tvCustomTitle`, `sizeToolbar`, `bottomActionToolbar` ids are declared but never referenced from Kotlin (styling only). OK.

### 3.10 OVERLAY (pause / win / game-over / dialogs) — `layout_overlay.xml` + `MainActivity.kt:456–766, 921–985` — **P**

- **PAUSE** (`showPauseOverlay` `:684`): RESUME / RESTART / HOME. `currentGameView?.pauseGame()` sets `isPaused`. ✔
- **WINNER** (`handleLevelCompleted` `:561`): curved title, 3D stars with staggered pop + sound, reward summary (coins/stars/time), NEXT LEVEL / RESTART / HOME, sparkle celebration + victory fanfare. This is the most polished flow in the app. ✔
- **GAME OVER** (`handleGameOver` `:667`): simple `showCustomGameDialog` with RETRY / HOME. ✔
- **Generic dialog** (`showCustomGameDialog` `:921`): primary + optional secondary button.

**Defect (High):** `onPause()` (`:99`) calls `dismissWinnerOverlay()` unconditionally, which sets `incOverlay.visibility = GONE` and `overlayHandler.removeCallbacksAndMessages(null)`. Any *modal decision* the player was mid-way through (reset progress confirm, layout-saved confirm, insufficient funds) is **silently destroyed** by backgrounding the app — and the pending `onConfirm` lambda is lost. Worse: `onPause` does **not** call `currentGameView?.pauseGame()`, so **backgrounding during gameplay does not pause the game loop** (see BUG-08).

**Dead views in the overlay:** `tvOverlayHeader`, `rewardsContainer`, `dialogButtonLayout`, `rowSecondaryButtons`, `btnOverlayExtra`, `ivOverlayCrown` are never used by Kotlin. `ivOverlayCrown` is 1×1 dp and force-hidden twice (`:663`, `:711`).

### 3.11 CRASH REPORTER — `CrashActivity.kt` — **W**

`JumpAdventureApplication` installs a `Thread.setDefaultUncaughtExceptionHandler` that launches `CrashActivity` with thread name, stack trace, exception name+message. The user can COPY ERROR (clipboard) and RESTART (relaunch intent + `CLEAR_TASK`). Good defensive practice.

**Defect (Medium):** the handler does **not** delegate to the previous handler on the *happy* path, so Android's normal "app keeps crashing" behaviour and Play Store crash reporting are bypassed. It also `startActivity` from a crashing process, which is not guaranteed to succeed. If it fails, it correctly falls back to `previousHandler`.

---

## 4. GAMEPLAY ENGINE AND GAME LOOP

### 4.1 Loop — `GameView.run()` (`:420–475`)

- `GameView` extends `SurfaceView` and implements `SurfaceHolder.Callback` + `Runnable`.
- Dedicated `Thread("JumpAdventure-GameLoop")`, started in `surfaceCreated()` and `resumeGame()`.
- **Fixed timestep 1/60 s**, accumulator pattern, **max 5 catch-up steps** (then accumulator is dropped — correct spiral-of-death protection).
- Frame delta clamped to `0.10f` s.
- Sleeps `16 ms − frameTime`; when paused, sleeps 16 ms in the loop instead of exiting.
- Whole iteration wrapped in `try/catch (Throwable)` → logs to `JUMP_DEBUG` and **continues**. Robust against a single-frame exception, but it also **masks bugs permanently**.

`surfaceDestroyed` → `stopGameLoop()` (interrupt + `join(600)`). `onDetachedFromWindow` → `stopGameLoop()` + `cachedBgBitmap.recycle()`. Lifecycle is handled correctly here. ✔

### 4.2 `update(deltaTime)` — `:477–507`

```
dtFactor = deltaTime * 60
animTick += dt * (2.5 if moving else 1.5)
updateTimers(dt)
effectiveSpeed = 11 * settingsSpeed(0.75..1.50) * characterSpeed * (1.50 if speed powerup)
targetVelocityX = ±effectiveSpeed or 0
velocityX += (targetVX - velocityX) * (response*dtFactor)     // response .24 idle / .32 moving
velocityY = min(velocityY + 0.75*dtFactor, 25)
coyote / jump-buffer handling
updateParticles(dtFactor)
updateMovingElements(dtFactor)
applyAttraction(dtFactor)
moveAndCollide(dtFactor)
handleCollectiblesAndHazards()
if (playerY > 1200) handlePlayerHit()                        // pit death
cameraX += (targetCameraX - cameraX) * min(0.18*dtFactor, 1)
HUD throttle: onProgressUpdated every 0.10 s
```

**Assessment:** the loop and the input smoothing are competently written. `dtFactor`-scaled acceleration, coyote time (`0.10 s`), jump buffering (`0.12 s`), variable jump height (`cutJumpShort` halves upward velocity on release) — all textbook platformer feel features and all genuinely implemented.

### 4.3 Physics constants (`:65–83`)

| Constant | Value | Notes |
|---|---|---|
| `baseMoveSpeed` | 11 units/frame@60 | |
| `baseJumpStrength` | −17 | |
| `gravityPerFrame` | 0.75 | |
| `maxFallVelocity` | 25 | |
| `coyoteTimeSeconds` | 0.10 | |
| `jumpBufferSeconds` | 0.12 | |
| `damageInvulnerabilitySeconds` | 1.0 | |
| `lives` | 3 | no HUD |
| `visualWidth/Height` | 92 / 124 | drawn at **1.28× scale** |
| `colliderWidth/Height` | 54 / 104 | narrower than visual ✔ |

**Physics are frame-rate independent** — every velocity integration multiplies by `dtFactor` and gravity is `dtFactor`-scaled. ✔ Good.

Jump apex check: `v²/2g = 17²/(2·0.75) = 192.7` units of height. `groundY = 800`, `SECTION_B` platforms are at `groundY − 100 = 700`, `SECTION_F` steps are `−70` each. Generator gaps are 80–200 units horizontally. A 192-unit jump height with 11-unit/frame horizontal speed gives a comfortable but fair reach — the generator's section spacing is consistent with these constants. ✔

### 4.4 Collision — `moveAndCollide()` (`:657–720`)

Two-phase, axis-separated:
1. **X axis:** move `playerX`, then for each solid, if vertically overlapping (`curBottom > top+8 && curTop < bottom-8`) and the player has crossed the edge in the direction of travel, snap `playerX` to the platform edge and zero `velocityX`.
2. **Y axis:** move `playerY`, then if horizontally overlapping (`curRight > left+4 && curLeft < right-4`) and `velocityY >= 0` and previous feet were above the platform top, snap `playerY = elemTop − visualHeight`, zero `velocityY`, set `isGrounded`, reset mid-air jumps, refresh coyote time, and **carry the player with moving platforms** (`playerX += elem.speed * direction * dtFactor`). Ceiling collision handled symmetrically.

**Assessment: WORKING and reasonable.** Caveats:
- `isSolid` = `PLATFORM | MOVING_PLATFORM | BOX`. Correct.
- The Y-snap uses `playerY = elemTop - visualHeight`, i.e. it aligns the **visual** box, while the sweep test uses the **collider**. The collider has `colliderOffsetY = visualHeight − colliderHeight = 20`. So after landing the collider bottom is `elemTop - 20`, leaving a **20-unit visual/collider gap** — the character appears to float 20 px above every platform. Cosmetic but visible. → BUG-19
- No wall-slide, no one-way platforms, no step-up assist.
- O(n) scan of **all** active elements twice per frame, no spatial partitioning. At level 28 sections × ~15 elements ≈ 400 elements → 800 rectangle tests/frame. Acceptable now, will degrade on very long levels.

### 4.5 Hazards — `handleHazard()` (`:790`)

```
if (damageInvulnerability > 0) return
if (isPowerActive) { consume enemy, playHit, return }         // Power = invincible + kill
if (spike && isFireResistant) return                          // Lava Knight immunity
if (isShieldActive && shieldHits > 0) { shieldHits--; 0.25s invuln; play; … }
handlePlayerHit()
```
`handlePlayerHit()` — if `lives <= 0` → `onGameOver()`; else respawn at `checkpointX/Y`, zero velocity, 1 s invulnerability, **blinks the sprite** while invulnerable (`drawPlayer` `:1191`).

**Note:** enemies are only destroyed while `isPowerActive`; otherwise they are permanent. `active.isCollected` is set for enemies only in the Power branch.

---

## 5. PLAYER CHARACTER AND ANIMATIONS

### 5.1 Rendering — `CharacterRenderer.drawCharacter()` — **W**

A single `object` with a `drawCharacter(canvas, bounds, characterId, skinId, facingRight, animState, animTime)` API. It draws head, eyes, brows, cheeks, hoodie/outfit, zip, arms, legs, shoes and highlights as vector primitives — no bitmaps at all.

- **4 animation states:** `IDLE` (gentle leg/arm sway via `sin(animTime*3.5)`), `RUN` (`sin(animTime*12)` leg swing ±30°, arm ±35°, vertical bounce `height*0.04`), `JUMP` (scaleY 1.15 / scaleX 0.9, leg −22/+25, arm −50/+35), `FALL` (scaleY 0.95 / scaleX 1.05, arm −60/−40).
- **Facing:** `canvas.scale(-1f, 1f, cx, centerY)` when `facingRight == false`. ✔
- **24 character palettes** mapped by `characterId` (with legacy aliases), plus a **7-skin palette override** for outfit / hair / accent.

**Verified:** `canvas.save()` count = 5, `canvas.restore()` count = 5 — balanced. ✔

**Performance defect (High):** `drawCharacter` is called **every frame** and allocates **~25 `Paint`/`Path`/`RectF` objects per call on the heap** (`Paint(Paint.ANTI_ALIAS_FLAG)` at lines 72, 74, 141–145, 199 + `RectF(...)` + `Paint(outfitPaint)` + `Color.parseColor(...)` string parsing). Worse, the file **declares 13 cached `private val` paints at the top (lines 9–41) and then shadows all of them with locals inside the function** — `skinPaint`, `darkOutlinePaint`, `outfitPaint`, `hairHatPaint`, `accentPaint`, `shoePaint`, `shoeSolePaint`. The cached fields are **never used**. At 60 fps this is ~1,500 allocations/sec plus repeated hex-string parsing, guaranteeing GC churn and frame drops. → **BUG-09**

`Color.parseColor("#…")` is called with **string literals** dozens of times per frame across `GameView` and `CharacterRenderer`; each call parses the hex string. → BUG-10

### 5.2 Home preview — `CharacterPreviewView.kt` — **W**

Self-driving `Runnable` re-posted at 60 fps (`onAttachedToWindow` → `post(animRunnable)`, `onDetachedFromWindow` → `removeCallbacks`). Implements a 5-state jump state machine (`IDLE_WALK → JUMP_ANTICIPATION → JUMP_RISING → JUMP_FALLING → JUMP_LANDING`) with a **0.28 s** anticipation phase, driven by `playerSpeedMultiplier`. Tap triggers a hop and plays the jump sound. Handles the trail colour via `selectedTrailId`.

**Defect (Low):** the runnable continues to run at 60 fps whenever the home screen is *attached*, even when `incMainMenu.visibility == GONE` (it isn't detached when hidden). `onVisibilityChanged` is not overridden here. → BUG-13

---

## 6. CAMERA, BACKGROUNDS AND PARALLAX

### 6.1 Horizontal camera — **W**

```kotlin
val targetCameraX = (playerX - screenWidth * 0.35f).coerceAtLeast(0f)
cameraX += (targetCameraX - cameraX) * (0.18f * dtFactor).coerceAtMost(1f)
```
Player sits at 35 % of screen width with smooth lerp. `drawFrame` does `canvas.translate(-cameraX, 0f)` for the world layer only; controls and HUD are drawn in screen space. ✔

### 6.2 Vertical camera — **NOT IMPLEMENTED** (High severity)

There is **no `cameraY`** anywhere (grep: only `cameraX`). The world is translated on X only. But:

- `groundY = 800f` (fixed world coordinate),
- `SECTION_F` builds **3 stacked platforms, each `−70`**: `730, 660, 590`,
- `SECTION_B` platforms at `700`,
- `findSafeSpawnY()` returns `support.y - visualHeight`.

On a typical device the `SurfaceView` height in world units is roughly the screen height (no scaling is applied between world and view coordinates — the engine works in raw pixels). On a 1080×2400 device the playfield is 2400 units tall, so `groundY=800` is in the **top third** of the screen and everything below it is empty. On a small 1080×1920 device it is worse. Conversely on a 720×1280 device, `groundY=800` sits at 62 % — closer to right, but then `playerY > 1200` (the pit-death threshold) is **above** the bottom of the 1280-tall screen.

**Net effect:** the vertical framing is entirely resolution-dependent and there is no scrolling to compensate. Tall sections (SECTION_F) and any upward jump move content off the top of the screen with no way to follow. This is a **structural gameplay defect**, not a polish issue. → **BUG-07**

### 6.3 Background / parallax — **P**

```kotlin
val scale  = max(w/bgW, h/bgH)                       // cover-scale
val maxOffset = (scaledW - w).coerceAtLeast(0f)
val progress  = (cameraX / totalWidth).coerceIn(0f, 1f)
val parallaxX = -(progress * maxOffset * 0.35f)
canvas.drawBitmap(bg, srcRect, dstRect, bgPaint)
// then fills the exposed left/right edge with a sampled edge pixel
```

- **One background bitmap per world**, decoded with `BitmapFactory.decodeResource(resources, resId)` — **no `inSampleSize`**, no downscale.
- Only **4 of 14 themes have art**: `green_forest`, `desert_land`, `snow_mountain`, `lava_cave`, chosen by `(worldId − 1) % 4`. Worlds 5–14 (Jungle, Ocean, Sky Islands, Ancient Ruins, Crystal Cave, Dark Forest, Volcano, Cyber World, Galaxy Realm, Cloud Kingdom) all reuse those same four images. The `WorldInfo.skyColorHex`/`platformColorHex` do differ, so platforms change colour but the backdrop does not. → **P**
- The "parallax" is a `0.35` factor on leftover overflow — a single-plane slide, not multi-layer parallax. Genuinely implemented, but minimal.
- Edge-fill uses `bg.getPixel(0, h/2)` / `bg.getPixel(w−1, h/2)` **on the UI/game thread every frame** — `getPixel` on a hardware bitmap can force a readback. Cheap on ARGB_8888 but unnecessary; should be cached. → BUG-14
- The old bitmap is correctly `recycle()`d on level change and detach. ✔

### 6.4 Level map background — **P**

`LevelMapView.onDraw` paints a full-map gradient plus per-world background sections with colour-transition blending, culled by `visibleTop/visibleBottom`. `getWorldForLevel(...)` is called inside `onDraw` in a loop and twice more to compute `minVisibleWorldId`/`maxVisibleWorldId` using `nodes.firstOrNull{…}` / `nodes.lastOrNull{…}` — **two more O(n) linear scans per frame**, and `WorldRepository` allocates a new `WorldInfo` each call. → BUG-13b

---

## 7. POWER-UPS

### 7.1 The five in-level pickups

`ElementType` (`LevelGenerator.kt:14–18`): `POWERUP_MAGNET`, `POWERUP_SHIELD`, `POWERUP_SPEED`, `POWERUP_HIGH_JUMP`, `POWERUP_POWER`.

Pickup semantics differ — **this is the single most confusing part of the game's design**:

| Pickup | On collect | Effect | Status |
|---|---|---|---|
| **MAGNET** | `magnetCharges = min(+1, 9)` (`:748`) | Stores a charge; the on-screen magnet button (`tryUseMagnetCharge`) consumes it to grant `magnetDuration()` s of attraction. | **W** |
| **SHIELD** | `shieldCharges = min(+1, 9)` (`:753`) | Charge → button spends it for `shieldDuration()` s and `shieldHitCapacity()` hits. | **W** |
| **SPEED** | `speedCharges = min(+1, 9)` (`:758`) | Charge → button spends it for `speedDuration()` s at `speedPowerMultiplier()`. | **W** |
| **HIGH JUMP** | `activateHighJumpPowerUp()` **immediately** (`:754`) | `isHighJumpActive` for `highJumpDuration()` s; jump ×1.35 | **W (inconsistent)** |
| **POWER** | `activatePowerPowerUp()` **immediately** (`:758`) | 8 s invincibility + enemies destroyed on contact | **W (inconsistent, undocumented)** |

So three are "ammo" and two are "instant". Nothing in the UI or level design explains the difference, and the HUD only has buttons for magnet/speed/shield — **HIGH JUMP and POWER have no button and no HUD indicator at all**. Their state is only visible indirectly (bigger jump / golden aura on the sprite). → **P**

### 7.2 Duration / strength formulas (all genuinely used)

| Power-up | Duration | Strength | Formula site |
|---|---|---|---|
| MAGNET | `8 + (L−1)*1.5` s | radius `420 + (L−1)*45` | `:214–215` |
| SHIELD | `10 + (L−1)*1.5` s | hits `1 + (L−1)/2` | `:216–217` |
| SPEED | `8 + (L−1)` s | ×`1.50 + (L−1)*0.05` | `:218–219` |
| HIGH JUMP | `8 + (L−1)` s | jump ×`1 + (L−1)*0.08`, ×1.35 while active | `:220–221` |
| POWER | **fixed 8 s** | invincibility | `:912` |
| DOUBLE_COIN (passive) | always | coin ×`1 + (L−1)*0.15` | `:222` |
| STAR_BOOST (passive) | always | star attraction radius `(L−1)*70` | `:223` |

Shop upgrades therefore **do** affect gameplay — this is a real, connected system. ✔

### 7.3 Attraction — `applyAttraction()` — **W**

```kotlin
val magnetOn = isMagnetActive || isPassiveMagnet      // passive = Forest Guardian etc.
allowedRange = magnetOn ? magnetRange() : (STAR if starRange>0 else 0)
if (hypot(dx,dy) < allowedRange) { pull = min(0.18*dtFactor, 0.5); lerp toward player }
```
Applies to COIN, STAR, GEM and **all POWERUP_ types** (so the magnet also slurps power-ups — arguably a bug, arguably a feature). Uses `active.currentX/currentY`, which is the **top-left** of the element, compared against the player's **top-left**, not centres — so the effective radius is skewed. → BUG-20

### 7.4 Passive character skills — **W (verified)**

| Skill | Characters | Implementation |
|---|---|---|
| Double jump | GALAXY_HERO, GALAXY, SPACE_RUNNER, CRYSTAL_MAGE, VOID_WALKER | `maxMidAirJumps = 1` (`:293`) |
| Passive magnet | FOREST_GUARDIAN, FOREST, JUNGLE_FIGHTER | `isPassiveMagnet = true` (`:296`) |
| Fire resistance | LAVA_KNIGHT, LAVA | `isFireResistant = true`, ignored in `handleHazard` (`:296, :797`) |
| +50 % coins | GOLDEN_WARRIOR, PIRATE, EXPLORER | `isCoinBonusActive` → `1.5f` multiplier (`:297, :723`) |
| Slow enemies | ICE_WARRIOR, ICE, ARCTIC_RANGER | `enemySpeedMultiplier = 0.5f` (`:298`) |
| +speed | STORM_RIDER, THUNDER_HERO, NEON_RUNNER, NEON (+20 %), NINJA/CYBER_NINJA/SHADOW_HUNTER/DESERT_RUNNER (+15 %), GIRL/COWBOY (+8 %) | `characterSpeedMultiplier` (`:300–306`) |
| Start shielded | ROBOT, ROBOT_X, CYBER_BOT, SAMURAI | `startsWithShield` (`:260`) |

Reachability check: every id in the table above is a **buyable** character id in `getFullCharacterList()`. `CYBER_BOT` and `GALAXY`/`ICE`/`LAVA`/`NEON`/`DESERT`/`FOREST` are legacy aliases with no matching shop entry — harmless.

**Confirmed broken/misleading:**
- `CharacterItem.skillName` / `skillDesc` are **never rendered** anywhere. The player buys "Galaxy Hero" for 8,000 coins and is never told it has double jump (the `description` field says "Cosmic space jumper"). → **BUG-15**
- The `SAMURAI` id appears in the start-shield list, and it **is** in the shop — good.
- `characterJumpMultiplier` only affects GIRL/Cowboy — but the shop advertises "Agility Boost / Lightweight jumper" and "Quick Draw / Enhanced movement precision", neither of which is jump-related. Mislabelled. → Low

---

## 8. COINS, GEMS, STARS, SCORING AND REWARDS

| Collectible | In-level effect | Persisted? | Status |
|---|---|---|---|
| **COIN** | `+10 * (1.5 if coin-skill) * coinUpgradeMultiplier()` to `coinsCollectedInLevel` | On level complete: `saveData.coins += coins`; `totalCoinsCollected += coins` | **W** |
| **STAR** | `starsCollectedInLevel++` | On complete: `levelStars[level] = max(old, stars)` | **W** |
| **GEM** | `gemsCollectedInLevel++` | On complete: `saveData.gems += gems` (**inside `GameView.completeLevel()`**) | **W** |
| **Score** | — | — | **NI (no score concept)** |

### 8.1 Stars: guaranteed exactly 3 per level

`LevelGenerator` tracks `totalStarsPlaced` and, after the section loop, back-fills to exactly 3 stars on "star-safe" (static, width ≥ 90) platforms, choosing platforms spread across the level (`desiredFraction = (totalStarsPlaced+1)/4`) and avoiding clustering within 70 units. Stars are placed at `platform.y − 72` so they are reachable from a `−72` jump. This is genuinely well engineered, and `LevelSafetyTest` asserts both invariants. ✔

**But:** `handleLevelCompleted` does **not** convert `starsCollectedInLevel` into a currency or a "stars required to unlock" gate that actually works (see §3.6). Stars are *only* used for: node star icons on the map, the Trophies predicates, and the (bypassed) world unlock rule.

### 8.2 The gem double-count / save-order hazard — **P**

`GameView.completeLevel()` (`:840`) mutates `saveData.gems` directly:
```kotlin
saveData.gems += gemsCollectedInLevel
onProgressUpdated(…1f)
onLevelCompleted(coins, stars, timeSec)     → MainActivity.handleLevelCompleted → saveManager.saveData(saveData)
```
It works because the Activity saves immediately afterwards. But it means **engine code writes to the save model and relies on the Activity to persist it** — a fragile, undocumented contract (the code even carries a comment admitting it). If `onLevelCompleted` ever fails to persist, gems are lost; if the user quits via HOME from the pause menu without completing, gems are silently discarded (correct), but coins/stars are also discarded (correct).

**Also:** `MainActivity.handleLevelCompleted` (`:562`) does `saveData.coins += coinsEarned` **and** `totalCoinsCollected += coinsEarned`, then saves. This is the *only* legitimate coin source, and it is what makes `rejectUnsupportedCurrencyGrants()` permit the increase. ✔

### 8.3 Reward summary — **W**

`RewardSummaryView.setRewardData(coins, stars, timeSec)` renders three custom-drawn chips (coin icon, star icon, timer icon) with formatted values. The level timer is `System.currentTimeMillis() - levelStartTime` and is displayed. ✔

**Note:** the elapsed time is computed **after** `isPaused = true` was set on completion, but it is **wall-clock**, so time spent in the pause overlay is included in "time taken". → Low

---

## 9. AUDIO SYSTEM

### 9.1 Architecture — `SoundManager.kt` — **W (works, but everything is synthesised)**

There are **no audio assets at all**. No `.mp3`, `.ogg`, `.wav` anywhere in the project. Every sound is generated as a PCM sine sweep at runtime and played through `AudioTrack` in `MODE_STATIC`.

- **SFX:** single-thread `ExecutorService("JumpAdventure-SFX")`, daemon. `submitSfx {}` no-ops when `soundEnabled == false`. Each call constructs a `ByteArray(numSamples*2)`, fills it with a **phase-accumulated linear frequency sweep** with attack/release envelope, builds an `AudioTrack`, `write`s, `play`s, `Thread.sleep(duration + 15)`, then `stop()`/`release()`.
- **Music:** a separate daemon `Thread("JumpAdventure-Music")` loops an 8-note melody (`0,4,7,12,7,4,2,5` semitones) over a per-world base pitch (261.63 / 293.66 / 329.63 / 220.00 for `worldId % 4`), 145 ms notes with 90 ms gaps.

**Sounds exposed:** `playJump`, `playCoin`, `playStar`, `playButtonClick`, `playHit`, `playPowerUp`, `playPowerUpActivation(type)`, `playShieldBreak`, `playLevelComplete`, `playStarImpact(isCenter)`, `playVictory` (6-note fanfare).

### 9.2 Assessment: **PARTIALLY WORKING**

What works: the synth is correct, `released`/`soundEnabled` guards are in place, `@Synchronized` on music control, `AtomicBoolean` release, executor shutdown on `release()`. ✔

Problems:
1. **No music in the main menu / shop / map.** `startMusic` is only called from `GameView.loadLevel()` and `MainActivity.onResume()`. Menu music does exist via `onResume` → `startMusic(1)`. OK.
2. **A fresh `AudioTrack` for every single note.** The melody creates and releases an `AudioTrack` ~7 times per second (`145 + 90 = 235 ms` per note). `AudioTrack` creation is a heavyweight IPC-backed operation. This will cause audible stutter and battery drain on low-end devices, and can exhaust audio HAL resources. → **BUG-16 (High on low-end)**
3. **Blocking `Thread.sleep` inside the tone player** means SFX are strictly serialised — a long `playVictory()` (≈1 s of notes + sleeps) blocks all subsequent SFX. Since `submitSfx` is a *single-thread* executor, **victory fanfare swallows the "click" that follows it**. → Medium
4. **Music pitch is keyed on `(worldId − 1) % 4`** but `WorldRepository` has 14 themes — so worlds 1, 5, 9, 13 share a melody. Acceptable, but inconsistent with the 14-theme art claim.
5. **`playToneBlocking` allocates a new `ByteArray` per note** — at 235 ms/note that is ~4 arrays/sec of ~6 KB. Minor.
6. **No audio focus handling.** `AudioManager.requestAudioFocus` is never called, so the game's music will play over the user's music app and won't pause on phone calls. → Medium (BUG-21)
7. `playVictory()` is called on **every** level completion; there is no rate limiting, and the loop checks `soundEnabled` but **not** `musicEnabled` (music is separate by design — documented in code).

---

## 10. LEVELS, WORLDS AND GENERATION

### 10.1 `WorldRepository` — **W**

- **Infinite worlds:** `worldId = ((level − 1) / 25) + 1`; `startLevel = (worldId−1)*25+1`; `endLevel = worldId*25`.
- **14 theme templates** cycle: `(worldId − 1) % 14`, with a Roman-numeral suffix after the first cycle (`cycle = (worldId−1)/14` → "Green Forest II"). `toRoman()` supports 1–10 only, then falls back to the digit. So world 15×14 = 210 → cycle 14 → "Green Forest 15". Fine.
- `requiredStarsToUnlock = (worldId − 1) * 15`.
- `getWorldBackgroundRes(worldId)` maps to only **4** images (`% 4`), not 14. → see §6.3.
- `worlds` property enumerates **50** worlds (`(1..50).map`) — so the Worlds screen tops out at level 1,250 while the map supports far beyond. Inconsistent with "infinite". → Medium

### 10.2 `LevelGenerator` — **W** (the most solid subsystem)

- **Deterministic:** `seed = (level * 31337L) xor 0x5DEECE66DL`, `Random(seed)`. Same level always yields the same layout. Asserted by `LevelGeneratorTest.testLevelGenerationIsDeterministic`. ✔
- **Section count:** `≤5 → 4`, `≤10 → 6`, `≤25 → 8`, `≤50 → 12`, `≤100 → 16`, else `18 + (level−100)/50` capped at **28**.
- **Section pool by level:** A/B/G → +C → +D/H → +E/F. All 8 sections (`SECTION_A`…`SECTION_H`) are implemented with distinct geometry.
- **Difficulty scaling:** `diffScale = (1 + level/100 * 0.15).coerceAtMost(1.8)`; gap widths grow to 200 units by level ~250.
- **Guaranteed 3 stars** with back-fill and spatial spread (§8.1).
- **Finish door:** always appended on a 250-wide platform at the end, plus `totalWidth = currentX + 250`.
- **Moving platforms:** `MOVING_PLATFORM` with `moveDistanceX = 120 + rand(80)`, speed `(2 + rand*1.5)*diffScale` capped at 5.
- **Enemies:** speed `1.5*diffScale` capped at 3.5, patrol `moveDistanceX = 70`.
- **Checkpoints:** `SECTION_H` places one every ~180 units of level length.

**Weaknesses:**
- Only **8 hand-written section templates** recycled endlessly — a level at 5,000 feels structurally identical to one at 500, just longer and with wider gaps. No new mechanics ever appear.
- **No actual "boss"** despite `LevelMapView` drawing boss nodes every 5 levels.
- **No enemies after SECTION_E** in early levels — first 50 levels literally cannot contain an `ENEMY` because `SECTION_E` only enters the pool at level > 50. Combined with `SECTION_D`/`H` also being >50-only, **levels 1–50 consist of only A, B, C, G** — i.e. coins, one spike box, and a speed power-up. → **P (difficulty curve)**
- Gap of up to **200 units** vs. a max jump distance of roughly `11 * 34 frames ≈ 370` units — still jumpable, but only with a running start.
- `SECTION_F` stacks `−70` thrice; with no vertical camera (§6.2) this section is partly off-screen.

### 10.3 Save schema versioning — **P**

`CURRENT_SCHEMA_VERSION = 2`; `migrateIfNeeded()` compares and **only writes the version number** — no actual migration logic, because "existing keys remain backwards compatible". Reasonable, but there is no `defaults` fill-in beyond `GameSaveData()`'s field defaults, and `GameSaveData()` itself ships **non-zero defaults** (`coins = 1250`, `gems = 35`, `playerSpeedMultiplier = 1.15f`) that also act as the `prefs.getInt(...)` fallbacks. Consequence: **a fresh install starts with 1,250 coins and 35 gems**, and `resetProgress()` returns you to 1,250 coins. Whether that is intended is undocumented. → Medium

---

## 11. SAVE SYSTEM AND PLAYER PROGRESS

`SaveManager` (singleton, `SharedPreferences("jump_adventure_save")`) — **W**

| Aspect | Implementation | Assessment |
|---|---|---|
| Serialisation | primitives + `JSONObject` for `levelStars` and `powerUpLevels`, `JSONArray` for `unlockedWorlds`, `StringSet` for unlocks | **W** |
| Thread safety | `@Synchronized saveData`, `@Volatile INSTANCE`, double-checked locking | **W** |
| Corrupt-data resilience | every JSON parse wrapped in `runCatching { }.onFailure { Log.w }` | **W** |
| Sanitisation | `sanitizeSelections()` clamps coins/gems ≥ 0, speed 0.75–1.50, power-up levels 1–5, re-adds `DEFAULT`/`NONE`, reverts selection if not unlocked | **W** |
| Anti-cheat | `rejectUnsupportedCurrencyGrants()` — see §3.5 | **P (works, but breaks the shop)**, and trivially bypassed by any rooted user editing XML |
| Backup | `android:allowBackup="true"` in the manifest | **Security/cheat concern** — the save is extractable via `adb backup` / cloud backup |
| Reset | `prefs.edit().clear().apply()` | **W**, but see the default-values note in §10.3 |

**Fields that exist in the schema but have NO implementation** (verified by grep — only read/written by `SaveManager`, never consumed by gameplay or UI):

| Field | Status |
|---|---|
| `dailyRewardLastClaimTimestamp`, `dailyRewardStreak` | **NOT IMPLEMENTED** (no daily reward UI at all) |
| `selectedPet`, `unlockedPets` | **NOT IMPLEMENTED** (no pet UI, no renderer) |
| `language` | **NOT IMPLEMENTED** (no localisation, no selector; app is English-hardcoded) |
| `vibrationEnabled` | **NOT IMPLEMENTED** (no `Vibrator` usage anywhere) |
| `unlockedPets`/`unlockedWorlds` are never grown by gameplay | **NOT IMPLEMENTED** (see §3.6) |
| `hasCustomControls` | written by `ControlCustomizationView` but **never read** by anything |

**Save write frequency problem:** `saveData()` is called from `PlayerSpeedControlView`'s scroll listener on every `ACTION_MOVE` (§3.8) and from every shop purchase / equip tap, and serialises ~60 keys plus two JSON blobs each time. `apply()` is async, so it won't ANR, but it is excessive.

---

## 12. UI COMPONENTS AND RESPONSIVE LAYOUTS

### 12.1 Custom View inventory

| View | Purpose | Status |
|---|---|---|
| `GamePrimaryButton` | 8 colour variants × 6 icon types, 3D gradient + shadow + press state, `onMeasure` defaults to 320×64 dp | **W** |
| `GameCurrencyBadge` | coin/gem pill with attached circular "+" button; `onPlusClickListener` | **W** |
| `GameBottomNavView` | 4 custom-drawn tabs with per-tab colour theming and active capsule | **W** |
| `CurvedTitleView` | arched text on a drawn arc | **W** |
| `Stars3DView` | 3 stars, staggered `ValueAnimator` entrance pop + infinite shimmer, `onStarImpactListener` for per-star SFX | **W** |
| `RewardSummaryView` | 3 drawn chips (coins / stars / timer) | **W** |
| `GameDialogBoardView` | drawn board background, `BoardType.PAUSE`/`WINNER` | **W** |
| `GameDialogView` | modal panel with `titleView`/`messageView`/`buttonContainer` | **W** (but `applyResponsiveConstraints()` is **never called** → BUG-18) |
| `SparkleView` | 100 % procedural particles, `postInvalidateDelayed(33)`, stops on detach/hide | **W** |
| `CharacterPreviewView` | animated home-screen hero | **P** (runs while hidden) |
| `CharacterRenderer` | shared procedural character drawing | **P** (allocation storm) |
| `LevelMapView` | infinite snake map | **P** (frozen anim, per-frame allocs) |
| `ControlCustomizationView` | drag/resize editor | **W** |
| `PlayerSpeedControlView` | speed slider + live preview + quick presets | **P** (saves on every move) |
| `GamePowerUpButton` | — | **DEAD** |
| `HomeEnvironmentView` | — | **DEAD** |

### 12.2 Responsiveness

**Genuinely implemented:**
- `InsetsManager.applySystemWindowInsets()` — edge-to-edge via `WindowCompat.setDecorFitsSystemWindows(false)`, then translates `systemBars()` + `displayCutout()` insets into **top margins** for the 5 top bars, **bottom margins** for the nav, **bottom padding** for `mapScrollView`, and **all-round padding** for the overlay. Original values are captured up front so repeated inset dispatches don't accumulate. This is correct, non-trivial and **WORKING**. ✔
- `MainActivity.setupResponsiveHomeLayout()` — recomputes on every layout change, clamped with `coerceIn`, guarded by `runCatching`. ✔
- `applyOverlayResponsiveSizing()` — computes a compact mode below `760 dp` usable height, resizes the board to `86 %`/`80 %` of window width clamped to 280–520 dp, and re-lays-out title/stars/rewards/buttons. ✔
- `GameView.updateControlLayouts()` — control sizes are `%` of width with dp clamps (68–88 dp sides, 84–106 dp jump, 46–62 dp power-ups), margins in dp. ✔

**Not responsive:**
- **Hard-coded pixel world coordinates** (`groundY = 800`) with no world-to-screen scaling and no vertical camera → §6.2, the single biggest responsive-layout failure.
- `layout_overlay.xml` hardcodes `340dp` board and `320dp` dialog widths (the latter is partly corrected by `applyOverlayResponsiveSizing`, the former by nothing).
- `activity_main.xml` stacks 6 full-screen includes; all 6 **inflate on startup** even though 5 are `gone`. With 50-world/25-character lists this inflates the startup cost.
- `textSize` in all custom views is **raw pixels** (`textSize = 32f`, `textSize = 14f`) rather than `sp`/density-scaled for text, so fonts do not scale with the user's system font-size setting (accessibility failure).

### 12.3 Accessibility — **N**

- All UI text is hard-coded English string literals in Kotlin — **1 string resource exists** (`app_name`). No localisation possible.
- `contentDescription` present on some `ImageButton`s (good) but most `ImageView`s created in Kotlin have none.
- `GamePrimaryButton`/`GameBottomNavView` are custom `View`s that draw text on canvas — they expose **no `contentDescription` and no accessibility node**, so TalkBack sees an unlabelled clickable region.
- Touch targets are generally ≥ 44 dp ✔.

---

## 13. DEPENDENCIES, APIS AND EXTERNAL LIBRARIES

```toml
agp = "8.4.2"        kotlin = "1.9.24"     gradle = 8.8
compileSdk 34   minSdk 24   targetSdk 34   JVM target 17
```

| Dependency | Version | Used? |
|---|---|---|
| `androidx.core:core-ktx` | 1.13.1 | ✔ `ViewCompat`, `WindowInsetsCompat` |
| `androidx.appcompat:appcompat` | 1.7.0 | ✔ `AppCompatActivity` |
| `com.google.android.material` | 1.12.0 | ✔ **only** for `Theme.MaterialComponents.DayNight.NoActionBar` |
| `androidx.activity:activity` | 1.9.0 | ✖ **not referenced in any source file** |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.1 | ✖ **not referenced in any source file** |
| `junit:junit` | 4.13.2 | ✔ 8 unit tests |

- **No game engine** (no libGDX, Unity, Godot), **no Compose**, **no networking** (no OkHttp/Retrofit, no `INTERNET` permission), **no billing**, **no analytics**, **no ads**, **no Room**, **no Hilt/Koin**, **no Coil/Glide**, **no Firebase**.
- **Zero permissions declared** in the manifest. Consistent with a fully offline game. ✔
- Two unused dependencies (`activity`, `lifecycle-runtime-ktx`) bloat the APK.

**External "APIs":** none. All art is procedurally drawn or shipped as WebP; all audio is synthesised. This is genuinely impressive and is the project's strongest engineering property.

### Version catalog vs. actual IDs

- `applicationId = "com.synfusion.jump"` but `namespace = "com.jumpadventure.game"` and all source lives in `com.jumpadventure.game.*`. This is legal but confusing; `README.md` only documents the `applicationId`.
- `versionName = "1.0"` hardcoded; `versionCode` comes from a Gradle property (or 1).

---

## 14. BUILD CONFIGURATION

| Item | Finding | Severity |
|---|---|---|
| `app/proguard-rules.pro` | **Referenced** in `app/build.gradle.kts:50` but the file **does not exist**. `isMinifyEnabled = false`, so it is only read for the release variant → the release build path is broken/dependent on AGP tolerance. | **High** |
| Missing drawables | `bg_card_glossy`, `bg_card_selected`, `bg_pill_badge` | **Critical (AAPT)** |
| Missing class | `CharacterCardView` | **Critical (Kotlin)** |
| CI workflow | `README.md` badges link to `.github/workflows/build-apk.yml`; **`.github/` does not exist in the repo** and never has (1 commit). No `testDebugUnitTest` / `lintDebug` / `assembleDebug` gate exists. | **High** |
| Signing | Env-driven (`KEYSTORE_FILE` etc.), correctly absent from the repo, `*.jks`/`*.keystore` gitignored, `hasReleaseSigning` guard so unsigned builds still work | **W — good practice** ✔ |
| Repository declarations | `RepositoriesMode.FAIL_ON_PROJECT_REPOS` in `settings.gradle.kts` | **W** ✔ |
| `gradle.properties` | `-Xmx2048m`, `android.useAndroidX=true`, `nonTransitiveRClass=true` | **W** ✔ |
| No `baselineProfiles` / no R8 rules / no `resConfigs` | APK ships all language resources and unshrunk code | Low |
| `allowBackup="true"` | save file extractable | Medium |

**Build status: CANNOT BE CONFIRMED TO BUILD.** Three source/resource symbols are unresolved. With `AGP 8.4.2`:
- `R.drawable.bg_card_glossy` / `bg_card_selected` → *"Unresolved reference: bg_card_glossy"* in `MainActivity.kt` (7 + 4 sites).
- `@drawable/bg_pill_badge` in `layout_gameplay.xml:78,105` → AAPT2 error *"resource drawable/bg_pill_badge not found"*.
- `com.jumpadventure.game.graphics.CharacterCardView` → *"Unresolved reference"* at `MainActivity.kt:818,1123`.
- `app/proguard-rules.pro` → *"ProGuard configuration file … was not found"* on `assembleRelease`.

---

## 15. TESTING STATUS

| Test file | Tests | What it actually verifies | Value |
|---|---|---|---|
| `LevelGeneratorTest.kt` | 3 | determinism; levels 1000 & 5000 are non-empty with `totalWidth > 1000`/`>1500`; world mapping for levels 1/101/2000 | **High** ✔ |
| `LevelSafetyTest.kt` | 3 | **exactly 3 stars** on 13 level numbers; **every star has static support below it** (horizontal overlap + vertical gap 0–140); finish door always supported | **High** ✔ |
| `SaveSanitizationTest.kt` | 2 | asserts `coerceAtLeast(0)` / `coerceIn(1,5)` on a `GameSaveData` — **it never touches `SaveManager`**; it re-implements the clamping inline and asserts its own arithmetic | **Near-zero** ✖ |

**Coverage reality:** the tests cover the *pure* level generator (genuinely useful, and `LevelSafetyTest` is a good invariant test). They cover **none** of: `GameView` physics, collision, power-up timers, `SoundManager`, `SaveManager` I/O or `rejectUnsupportedCurrencyGrants`, any view, any screen, or the Activity.

**No instrumented tests** (`androidTest/` absent), so **nothing would have caught the three compile-blockers**, and nothing verifies that any screen actually renders.

`SaveSanitizationTest` is effectively a tautology — this is worth calling out because it gives false confidence.

---

## 16. BUG LIST

Severity: **Critical** = blocks shipping · **High** = major feature broken / crash / data loss · **Medium** = incorrect behaviour, jank, or degraded UX · **Low** = cosmetic / dead code.

### CRITICAL

| ID | Location | Issue |
|---|---|---|
| **BUG-01** | `MainActivity.kt:818`, `MainActivity.kt:1123` | `com.jumpadventure.game.graphics.CharacterCardView` is instantiated twice but **no such class exists** in the repository or its git history. **Kotlin compile error — the app cannot be built.** The Heroes screen and Shop→Characters are both unreachable until this class is authored. |
| **BUG-02** | `MainActivity.kt:809, 1114, 1228, 1358, 1483, 1593, 1938` | `R.drawable.bg_card_glossy` and `R.drawable.bg_card_selected` are referenced 9 times; **neither drawable exists**. **Unresolved reference / AAPT error.** |
| **BUG-03** | `layout_gameplay.xml:78, 105` | `android:background="@drawable/bg_pill_badge"` — **drawable does not exist**. **AAPT2 link failure.** |
| **BUG-04** | `README.md:5`, repo root | README advertises CI (`build-apk.yml`, `testDebugUnitTest`/`lintDebug`/`assembleDebug`) and a build badge; **`.github/` does not exist**. There is no build/test gate, which is precisely why BUG-01–03 shipped. |

### HIGH

| ID | Location | Issue |
|---|---|---|
| **BUG-05** | `MainActivity.kt:1629–1653` + `SaveManager.kt:236–275` | **Shop → CURRENCY `CLAIM` buttons.** 12 packs from 1,000 to 1,000,000 coins/gems with **zero payment, cooldown or limit**. `SaveManager.rejectUnsupportedCurrencyGrants()` silently rolls every grant back, so the button shows "REWARD CLAIMED! +1,000,000 Coins" while the balance is unchanged, **and** leaves the in-memory `saveData` inflated relative to disk. Dead feature + misleading UI + state desync. |
| **BUG-06** | `MainActivity.kt:1709`, `:1826–1838` | **World progression is trivially bypassed.** `isUnlocked` includes `saveData.highestLevel >= world.startLevel`, which nullifies `requiredStarsToUnlock`. `EXPLORE` then sets `currentLevel = world.startLevel` and persists, so one tap on any unlocked world card jumps the player hundreds of levels ahead and cascades `highestLevel` upward, unlocking everything. The "N STARS REQUIRED" label is false. |
| **BUG-07** | `GameView.kt` (all of), `WorldRepository/LevelGenerator` `groundY = 800` | **No vertical camera.** Only `cameraX` exists; the world is translated on X only. With hard-coded pixel world coordinates and no world→screen scaling, the vertical framing of every level depends entirely on the device's pixel height. Tall sections (`SECTION_F`: 3× −70 dp stacks) and any upward jump render off-screen with no way to follow. Pit-death threshold `playerY > 1200` is above the screen bottom on short devices → the player can fall off-screen before dying. |
| **BUG-08** | `MainActivity.kt:98–106` | **`onPause()` does not pause the game.** It calls `dismissWinnerOverlay()` (destroying any open modal) and `soundManager.pauseMusic()`, but never `currentGameView?.pauseGame()`. Backgrounding the app mid-level leaves the `GameLoop` thread running at 60 fps, draining battery and advancing the simulation (and `Thread.sleep` in the engine is unaffected). It also **discards the pending `onConfirm` lambda** of an open dialog. |
| **BUG-09** | `CharacterRenderer.kt:9–41` + `:43–330` | **~25 heap allocations per character per frame.** All 13 cached `private val` `Paint`s declared at lines 9–41 are **shadowed by locals** inside `drawCharacter` (`skinPaint`, `darkOutlinePaint`, `outfitPaint`, `hairHatPaint`, `accentPaint`, `shoePaint`, `shoeSolePaint`). Combined with `RectF()`, `Path()` and `Color.parseColor("…")` string parsing, this is ~1,500 allocs/sec at 60 fps → sustained GC pressure and frame drops. |
| **BUG-10** | `GameView.kt:960–1180` (draw*), `CharacterRenderer.kt` | **Hex strings re-parsed every frame.** `Color.parseColor("#…")` appears dozens of times inside `onDraw`-path functions (`drawPlatform`, `drawCoin`, `drawStar`, `drawGem`, `drawPowerUp`, `drawWorld`), plus `Paint(...)`/`Path()`/`RectF(...)` constructed inside `drawWorld`'s per-element loop. Colours and paints should be constants/fields. |
| **BUG-11** | `MainActivity.kt:1661–1859` | **Worlds screen builds 50 fully-inflated cards (~600+ views) synchronously on the UI thread**, each with 2 `GradientDrawable`s and a background `ImageView` decode. Guaranteed multi-hundred-ms jank/jank-freeze on open, and repeated on every return to the screen. |
| **BUG-12** | `PlayerSpeedControlView.kt:278` → `MainActivity.kt:2032` | **Full save on every touch-move.** `onSpeedChangedListener` fires from `ACTION_MOVE`; the listener runs `saveManager.saveData(saveData)` — a `@Synchronized` serialisation of ~60 keys + 2 JSON blobs at drag-event rate (~100 Hz). |
| **BUG-16** | `SoundManager.kt:62–119, 236–265` | **A new `AudioTrack` is constructed and released for every note** (~7/sec for music, once per SFX). `AudioTrack` is an IPC-backed heavyweight resource; this causes audible stutter, wake-lock-like battery drain, and risks HAL resource exhaustion. Music also runs on its own thread with `Thread.sleep`, so any SFX queued behind it stalls. |

### MEDIUM

| ID | Location | Issue |
|---|---|---|
| **BUG-13** | `LevelMapView.kt:177, 123` | The current-node **pulse animation is frozen**: `animTime += 0.05f` runs in `onDraw` but `invalidate()` is only called from `setupMap()`, and `onDraw` never re-schedules itself. |
| **BUG-13b** | `LevelMapView.kt:199–213, 251–256` | **O(n) linear scans + allocations inside `onDraw`**: `nodes.firstOrNull{}`, `nodes.lastOrNull{}`, `nodes.filter{}`, `nodes.indexOf(...)` ×2 — on a 50–1000 node list. Plus a `RectF` allocation per visible node and `WorldRepository.getWorldForLevel()` (allocating `WorldInfo`) in a loop. |
| **BUG-14** | `GameView.kt:975–980` | `bg.getPixel(0, h/2)` / `bg.getPixel(w−1, h/2)` are called from the render path to compute the parallax edge-fill colour. Should be sampled once at decode time. |
| **BUG-15** | `GameModels.kt:65–66`, `MainActivity.kt:1060–1086` | `CharacterItem.skillName` and `skillDesc` are **populated but never displayed**. Players cannot see that Galaxy Hero has Double Jump, Lava Knight is spike-immune, etc. The Heroes card shows only `description` ("Cosmic space jumper"). |
| **BUG-17** | `MainActivity.kt` passim | **Massive duplication.** `charCardView/tvName/actionBtn/card` construction is copy-pasted across `openCharactersScreen`, `renderShopCharacters`, `renderShopSkins`, `renderShopTrails`, `renderShopPowerUps`, `renderShopCurrency`, `openAchievementsScreen`, `openWorldsScreen` — ~1,500 of the file's 2,216 lines are near-identical imperative view-building. Also two independent character-unlock paths (dialog-confirm vs. instant-buy) with different UX. |
| **BUG-18** | `GameDialogView.kt:96` | `applyResponsiveConstraints(windowWidth, usableWindowHeight)` is **never called** — the dialog has a hardcoded `320dp` width in `layout_overlay.xml` and its compact mode never activates. |
| **BUG-19** | `GameView.kt:700` | `playerY = elemTop - visualHeight` uses the **visual** height while the sweep test uses the **collider** (`colliderOffsetY = 20`). After landing, the collider bottom sits 20 units above the platform → the character visibly floats above every surface. |
| **BUG-20** | `GameView.kt:613–640` | `applyAttraction` measures `hypot(active.currentX − playerX, …)` — **top-left vs top-left**, not centres, so the effective magnet radius is offset by half the element and half the player. Also magnetises POWERUP elements (probably unintended). |
| **BUG-21** | `SoundManager.kt` (whole file) | **No audio-focus handling.** `AudioManager.requestAudioFocus` is never called: music plays over the user's own audio and does not duck/pause on calls or other apps. |
| **BUG-22** | `SaveManager.kt:79–80` + `GameModels.kt:4–5` | Fresh install and `resetProgress()` both start the player at **1,250 coins / 35 gems / 1.15× speed**, because the data-class defaults double as the `prefs` fallbacks. Undocumented; effectively a permanent free-currency grant that also makes `rejectUnsupportedCurrencyGrants`'s baseline meaningless. |
| **BUG-23** | `MainActivity.kt:88–96` | `onResume` tests `currentScreenName == "GAME"` — **that string is never assigned**; the real value is `"GAMEPLAY"` (`:348`). The branch is dead, so `resumeMusic()` is never taken and `startMusic(1)` always runs, **restarting music at world 1's pitch** even when resuming inside world 5. |
| **BUG-24** | `MainActivity.kt:358–361`, `LevelMapView.kt` | The level-map header text (`"WORLD n"` / world name) is computed **once** from `saveData.highestLevel` on open and **never updated while scrolling** — so after scrolling into an adjacent world the header is wrong. |
| **BUG-25** | `LevelGenerator.kt:150, 252–259` | **Levels 1–50 contain no enemies, no moving platforms and no checkpoints.** `SECTION_E`/`F` are only in the pool when `level > 50`; `D`/`H` when `level > 25`. Levels 1–25 are only A/B/G (platforms + coins + one speed pickup). The difficulty curve is essentially flat for the first ~50 levels. |
| **BUG-26** | `MainActivity.kt:1629–1653` + `secondaryBadgeCoins` | After a rejected currency grant the UI badge is set from the **mutated in-memory** `saveData`, so the on-screen balance disagrees with the persisted balance until the next screen refresh. |

### LOW

| ID | Location | Issue |
|---|---|---|
| **BUG-27** | `graphics/GamePowerUpButton.kt`, `graphics/HomeEnvironmentView.kt` | Two complete classes (183 + 184 lines) are **never referenced** anywhere. |
| **BUG-28** | `res/drawable/` | **11 unused drawables**: `bg_bottom_nav_container`, `bg_button_game_danger`, `bg_button_game_primary`, `bg_home_sky`, `bg_nav_active_capsule`, `ic_hero`, `ic_launcher_legacy`, `ic_lock`, `ic_play`, `ic_shop`, `ic_world`. |
| **BUG-29** | `res/drawable-nodpi/home_bg.webp` vs `green_forest.webp` | Byte-identical files (md5 `17394c4d…`) — 204 KB of pure duplication. |
| **BUG-30** | `res/values/colors.xml` | **18 of 31 colours unused**: `game_sky_top/bottom/deep`, `game_success_*` (3), `game_gold_*` (3 of 4), `game_panel_border/shadow`, `game_text_navy/muted`, `game_nav_bg/active/inactive`, `game_locked_*` (3). |
| **BUG-31** | `res/layout/*` | **13 declared-but-unused ids**: `bottomActionToolbar`, `btnOverlayExtra`, `controlCustomizationRoot`, `currencyContainer`, `dialogButtonLayout`, `homeRootLayout`, `ivCustomBg`, `ivHomeBg`, `rewardsContainer`, `rowSecondaryButtons`, `sizeToolbar`, `tvCustomTitle`, `tvOverlayHeader`. |
| **BUG-32** | `MainActivity.kt:21,118,339` | `overlayHandler` is created and cleared but **never posted to** — dead machinery. |
| **BUG-33** | `MainActivity.kt` (all screens) | **No string resources** — every label is a hard-coded Kotlin literal. `strings.xml` contains exactly one entry (`app_name`). Localisation and font-scaling accessibility are impossible. |
| **BUG-34** | `graphics/*.kt` | `Paint.textSize = 32f` etc. are **raw pixels**, not `sp` — text ignores the user's font-size preference. |
| **BUG-35** | `graphics/*.kt` (custom-drawn buttons/nav) | `GamePrimaryButton` and `GameBottomNavView` draw their labels on canvas, so they expose **no accessibility node or `contentDescription`**; TalkBack announces unlabelled clickables. |
| **BUG-36** | `GameModels.kt:19`, `:20–21`, `:17–18`, `:25` | Nine save fields are **dead**: `dailyRewardLastClaimTimestamp`, `dailyRewardStreak`, `selectedPet`, `unlockedPets`, `language`, `vibrationEnabled`, `hasCustomControls`, `unlockedWorlds` (never grown), `unlockedPets`. Written and read by `SaveManager` only. |
| **BUG-37** | `app/build.gradle.kts:57–60` | `androidx.activity` and `androidx.lifecycle-runtime-ktx` are declared but **never imported** by any source file. |
| **BUG-38** | `res/drawable-nodpi/` (all 7) | Backgrounds are decoded with `BitmapFactory.decodeResource` and **no `inSampleSize`**; `mountain.webp` alone is 2,857 × 3,264. Peak bitmap memory is device-resolution-dependent (see §17). |
| **BUG-39** | `LevelMapView.kt:150` | `isBoss = (lvl % 5 == 0) \|\| (lvl % 25 == 0)` — the second clause is **subsumed** by the first; boss nodes are drawn red and large but **have no gameplay effect** (no boss exists in `LevelGenerator`). |
| **BUG-40** | `LevelGenerator.kt:242` + `LevelMapView` | `isBoss`/`WorldRepository.worlds` cap at **50 worlds (1,250 levels)** while the generator and map are "infinite" — the Worlds screen and the map disagree about how far the game goes. |
| **BUG-41** | `MainActivity.kt:577–583` | `handleLevelCompleted` calls `saveManager.saveData(saveData)` **before** the overlay is set up, but `GameView` already mutated `saveData.gems` — the persist-then-mutate ordering relies on the caller, an undocumented contract (see §8.2). |
| **BUG-42** | `MainActivity.kt:562–570` | `handleLevelCompleted` does **not** guard against double-invocation. `GameView.levelCompletionHandled` guards the engine side, but a re-entrant `onLevelCompleted` (e.g. finish door touched on the same frame as a respawn) would double-credit coins and `totalLevelsCompleted`. |
| **BUG-43** | `GameView.kt:112` / `:225` | `saveData` is mutated directly by the engine (`saveData.totalJumps++`, `saveData.gems += …`) from the **game thread**, while `MainActivity` also reads/writes it from the **UI thread** with no synchronisation. `levelStars`/`powerUpLevels` are `MutableMap`s. Race conditions are possible (torn reads, `ConcurrentModificationException` on `powerUpLevels` if it were iterated). |
| **BUG-44** | `MainActivity.kt:1661` | `WorldRepository.worlds` is a `get()` property that rebuilds the 50-item list **on every access**, and `getWorldForLevel` allocates a fresh `WorldInfo` per call. |
| **BUG-45** | `GameView.kt:814–816` | `findSafeSpawnY()` scans **all** `levelLayout.elements` (not `activeElements`) and picks `minByOrNull { it.y }` — the **highest** platform overlapping the X range, which may be far above the checkpoint. Used for checkpoint respawn Y and initial spawn. |
| **BUG-46** | `SaveManager.kt:236–275` | The anti-cheat is a **client-side heuristic on SharedPreferences** — trivially bypassed by editing the XML or via `adb backup` (`allowBackup="true"`). It provides cheat-resistance theatre, not security. |
| **BUG-47** | `GameView.kt:436` | The whole `run()` iteration catches `Throwable` and only logs — a permanently broken game state (e.g. `lateinit` `levelLayout`) becomes an infinite loop of swallowed exceptions at 60 fps rather than a visible crash. |
| **BUG-48** | `MainActivity.kt:69–85` | `onBackPressed` is `@Suppress("DEPRECATION")` and does not use `OnBackPressedDispatcher`; on API 33+ with predictive back this behaves inconsistently. No predictive-back callback is registered. |

---

## 17. PERFORMANCE AND SECURITY CONCERNS

### 17.1 Performance

| Concern | Evidence | Impact |
|---|---|---|
| **Render-path allocations** | `CharacterRenderer.drawCharacter` ~25 objects/frame; `GameView.drawWorld` allocates a `Paint`/`Path`/`RectF` per solid element per frame; `Color.parseColor` on hex literals throughout | **High** — GC churn, frame drops, especially on low-end devices |
| **Un-scaled large bitmaps** | 7 WebP backgrounds at native resolution, decoded with no `inSampleSize`; `mountain.webp` is 2,857×3,264 | `mountain` alone ≈ 37 MB as ARGB_8888 (2,857×3,264×4). Cached per-level bitmap is recycled correctly ✔, but a single decode is a large transient spike → **OOM risk on low-memory devices** |
| **Bitmap `getPixel` in render loop** | `GameView.kt:975–980` | forces readback each frame |
| **ViewModel-free 2,216-line Activity** | all screens built imperatively | measured 600+ view constructions for Worlds; duplication makes every fix 7× |
| **`AudioTrack` per note** | `SoundManager` | audio stutter + battery; ~7 track creations/sec during music |
| **Save-on-touch-move** | `PlayerSpeedControlView` → `saveManager.saveData` | repeated JSON + prefs serialisation at drag rate |
| **Per-frame O(n) scans** | `LevelMapView.onDraw` (`filter`/`indexOf`/`firstOrNull`/`lastOrNull`), `GameView.moveAndCollide` (2× full element scan), `applyAttraction` (full scan) | scales poorly on long levels / deep map positions |
| **All 6 screen includes inflated at startup** | `activity_main.xml` | slower cold start |
| **Always-on 60 fps runnables** | `CharacterPreviewView` keeps ticking while `GONE` | wasted CPU on all non-home screens |
| **No `onTrimMemory` handling** | — | the recycled background is re-decoded on every level load |

### 17.2 Security

**Positives:** no `INTERNET` permission, no permissions at all, no network code, no WebView, no dynamic code loading, no exported components other than the launcher `MainActivity`, no secrets in the repo (signing via env vars, `*.jks`/`*.keystore` gitignored), `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, no `allowClearTextTraffic` concern.

**Concerns:**

| Concern | Severity |
|---|---|
| `android:allowBackup="true"` — the save file (coins, gems, unlocks) can be extracted/restored via `adb backup` or cloud backup, enabling trivial progression tampering and cloning | Medium |
| Client-side-only "anti-cheat" (`rejectUnsupportedCurrencyGrants`) is not security — it is a correctness guard that a rooted user bypasses with a text editor | Low (single-player, no economy) |
| `CrashActivity` renders a raw stack trace and offers **COPY ERROR** with the full trace to the clipboard — fine for a dev build, information-leaking in production | Low |
| `JumpAdventureApplication` replaces the default uncaught handler, which **suppresses Play Console crash reporting** | Low |
| No certificate-pinning / no back-end — not applicable | — |
| `CrashActivity` has `android:excludeFromRecents="true"` and `exported="false"` ✔ | — |

---

## 18. MISSING AND INCOMPLETE FEATURES

| Feature | Evidence | Status |
|---|---|---|
| **Daily rewards / streak** | `dailyRewardLastClaimTimestamp`, `dailyRewardStreak` exist in the model + prefs; **no UI, no claim logic, no timer** | **NOT IMPLEMENTED** |
| **Pets / companions** | `selectedPet`, `unlockedPets`; no renderer, no shop tab, no gameplay hook | **NOT IMPLEMENTED** |
| **Language / localisation** | `saveData.language`; `strings.xml` has **1** entry; no selector | **NOT IMPLEMENTED** |
| **Haptics / vibration** | `vibrationEnabled`; no `Vibrator`/`HapticFeedbackConstants` usage anywhere | **NOT IMPLEMENTED** |
| **Real currency economy (IAP)** | Shop CURRENCY tab is a fake `CLAIM` button; no billing dependency | **NOT IMPLEMENTED** |
| **World-unlock by stars** | `requiredStarsToUnlock` computed but bypassed by `highestLevel >= startLevel`; `unlockedWorlds` never grows | **NOT IMPLEMENTED / BROKEN** |
| **World completion rewards** | World cards show `+N coins` / `+N gems` badges; no code awards them | **NOT IMPLEMENTED** |
| **Boss levels** | `LevelMapView` draws boss nodes (red, larger, every 5th level); generator has no boss, no boss behaviour | **NOT IMPLEMENTED** |
| **Vertical camera / world scaling** | no `cameraY`; `groundY` hard-coded to 800 px | **NOT IMPLEMENTED (High)** |
| **Lives HUD** | `lives = 3` decremented, never displayed | **NOT IMPLEMENTED** |
| **HIGH JUMP / POWER HUD** | no button, no icon, no timer readout (unlike magnet/speed/shield) | **PARTIAL** |
| **Character skill display** | `skillName`/`skillDesc` populated, never rendered | **NOT IMPLEMENTED** |
| **Achievement icons** | `AchievementItem.icon` never used; all 30 rows use `ic_trophy` | **NOT IMPLEMENTED** |
| **Achievement unlock notification** | predicates only evaluated on screen open | **NOT IMPLEMENTED** |
| **Level-map header sync while scrolling** | static text set once on open | **BROKEN** |
| **Pause on app background** | `onPause()` doesn't call `pauseGame()` | **BROKEN (High)** |
| **Music world continuity** | `currentScreenName == "GAME"` never matches `"GAMEPLAY"` | **BROKEN** |
| **CI / build verification** | README documents it; `.github/` absent | **NOT IMPLEMENTED** |
| **`proguard-rules.pro`** | referenced in `app/build.gradle.kts:50`; file absent | **MISSING** |
| **`CharacterCardView`** | referenced twice; class absent | **MISSING** |
| **`bg_card_glossy` / `bg_card_selected` / `bg_pill_badge`** | 11 references; drawables absent | **MISSING** |

---

## 19. HOW THE MAJOR SYSTEMS INTERACT

```
 User tap ──► GameView.onTouchEvent (multi-pointer)
                │  jump/speed/magnet/shield rects hit-tested
                ▼
         triggerJump() / tryUseXCharge()
                ▼
         GameLoop thread  run()  ── fixed 1/60 s, ≤5 catch-up
                ▼
         update(dt)
           ├─ updateTimers(dt)   → magnet/shield/speed/highJump/power countdown
           ├─ velocity integration (dtFactor-scaled)
           ├─ updateMovingElements  → MOVING_PLATFORM + ENEMY patrol
           ├─ applyAttraction       → magnet radius vs coin/star/gem/powerup
           ├─ moveAndCollide        → X sweep, then Y sweep, O(n) over solids
           ├─ handleCollectiblesAndHazards
           │     ├─ COIN   → coinsCollectedInLevel += 10·1.5·upgrade
           │     ├─ STAR   → starsCollectedInLevel++
           │     ├─ GEM    → gemsCollectedInLevel++
           │     ├─ POWERUP_{MAGNET,SHIELD,SPEED} → +1 charge (button to spend)
           │     ├─ POWERUP_{HIGH_JUMP,POWER}     → activate immediately
           │     ├─ CHECKPOINT → checkpointX/Y = findSafeSpawnY()
           │     ├─ SPIKE/ENEMY → handleHazard → shield ▸ fire-resist ▸ power ▸ life--
           │     └─ FINISH_DOOR → completeLevel()
           └─ cameraX lerp; onProgressUpdated throttled to 10 Hz
                ▼
         drawFrame()  →  drawBackground(parallax, no Y)  →  translate(-cameraX)
                      →  drawWorld (culled) → drawParticles → drawPlayer
                      →  drawControls (screen space)
                ▼
    completeLevel()  ── saveData.gems += gemsCollected  (engine mutates save model!)
                     ── onLevelCompleted(coins, stars, time)
                            ▼
                    MainActivity.handleLevelCompleted
                      coins += earned; totalCoinsCollected += earned
                      totalLevelsCompleted++
                      levelStars[level] = max(old, stars)
                      highestLevel = max(highestLevel, level+1)
                            ▼
                    SaveManager.saveData()
                      sanitizeSelections()                    (clamp)
                      rejectUnsupportedCurrencyGrants()       (rollback if untrusted)
                      prefs.edit() { ~40 put + 2 JSON } .apply()
                            ▼
                    WINNER overlay: curved title, 3D stars (SFX per star),
                      RewardSummary, sparkle burst, victory fanfare
                            ▼
                    NEXT LEVEL → startLevelGameplay(currentLevel + 1)
                              → new GameView → init → loadLevel → world/layout/bg decode
```

**Cross-system couplings that are genuinely wired:**
1. **Shop → engine.** `powerUpLevels` → `upgradeLevel(id)` → all 10 duration/strength formulas in `GameView` (`:212–223`). Real and bidirectional.
2. **Heroes → engine.** `selectedCharacter` → `configureCharacterSkills()` → 7 distinct passive skills, all verified reachable from purchasable ids.
3. **Trails → renderer.** `selectedTrail` → `updateParticles` + `trailColor` + `CharacterPreviewView`.
4. **Skins → renderer.** `selectedSkin` → 3 palette overrides in `CharacterRenderer`.
5. **Controls → engine.** `ControlCustomizationView.saveToSaveData` → 18 fields → `GameView.updateControlLayouts` with clamping.
6. **World → background/palette/music.** `worldId` → `getWorldBackgroundRes` (4 images), `skyColorHex`/`platformColorHex` (14 themes), music base pitch (`% 4`).
7. **Settings → save → engine.** `playerSpeedMultiplier` feeds `effectiveSpeed`.

### Missing connections between UI and gameplay logic

| UI element | Reality |
|---|---|
| Shop → CURRENCY → **CLAIM** | No economy; calls `saveData` mutation that `SaveManager` reverts. **UI exists, feature absent.** |
| Worlds → **reward badges** (`+N coins`, `+N gems`) | Never awarded. Decorative only. |
| Worlds → **"N STARS REQUIRED"** | Star gate is bypassed by `highestLevel >= startLevel`. |
| Worlds → **EXPLORE** | Mutates `currentLevel` to jump hundreds of levels. |
| Heroes → **UNLOCK (price)** | Works via dialog path; the Shop path is instant-buy. Two inconsistent flows. |
| Heroes card → **skillName / skillDesc** | Never rendered. Player buys skills blind. |
| Trophies → **achievement icons** | Never rendered. |
| Settings → **vibration / language** | No UI controls at all despite model fields. |
| Gameplay → **lives** | Not displayed. |
| Gameplay → **HIGH JUMP / POWER** | No HUD button or indicator. |
| Level map → **boss nodes** | No boss gameplay. |
| Level map → **header** | Never updates while scrolling. |
| Overlay → **`ivOverlayCrown`** | 1×1 dp, force-hidden, unused. |

---

## 20. FEATURE STATUS MATRIX

| # | Feature | Status | Notes |
|---|---|---|---|
| 1 | App launch / Main Menu | **WORKING** | responsive, animated |
| 2 | Level map & node selection | **PARTIAL** | frozen pulse anim, stale header |
| 3 | Game loop (fixed 60 Hz) | **WORKING** | accumulator + catch-up cap |
| 4 | Horizontal camera | **WORKING** | lerp, 35 % anchor |
| 5 | Vertical camera | **NOT IMPLEMENTED** | no `cameraY` at all |
| 6 | Gravity / jump / coyote / buffer / variable jump | **WORKING** | frame-rate independent |
| 7 | Collision (X/Y sweep, moving platforms) | **WORKING** | 20 px visual float bug |
| 8 | Player rendering + 4 anim states | **WORKING** | allocation storm |
| 9 | Facing / skin recolour / trail | **WORKING** | |
| 10 | Coins | **WORKING** | collected → banked on complete |
| 11 | Gems | **WORKING** | engine mutates save; persists via Activity |
| 12 | Stars | **WORKING** | exactly 3/level, unit-tested |
| 13 | Scoring | **NOT IMPLEMENTED** | no score concept |
| 14 | Magnet power-up | **WORKING** | charge → button |
| 15 | Shield power-up | **WORKING** | charge → button, N hits |
| 16 | Speed power-up | **WORKING** | charge → button |
| 17 | High Jump power-up | **PARTIAL** | instant, no HUD |
| 18 | Power power-up | **PARTIAL** | instant, undocumented, no HUD |
| 19 | Passive hero skills (7) | **WORKING** | reachable from shop |
| 20 | Power-up upgrade shop | **WORKING** | drives engine formulas |
| 21 | Enemies | **PARTIAL** | none before level 51 |
| 22 | Spikes / hazards / respawn / checkpoints | **WORKING** | checkpoints only > level 50 |
| 23 | Lives (3) | **PARTIAL** | no HUD |
| 24 | Level completion / rewards | **WORKING** | best flow in app |
| 25 | Pause overlay | **PARTIAL** | doesn't pause on background |
| 26 | Game over / retry | **WORKING** | |
| 27 | Procedural level generation | **WORKING** | deterministic, tested |
| 28 | World theming (14 themes) | **PARTIAL** | only 4 backdrops |
| 29 | Infinite levels/worlds | **PARTIAL** | Worlds capped at 50 |
| 30 | Heroes screen | **NOT WORKING** | `CharacterCardView` missing |
| 31 | Shop → Characters/Skins | **NOT WORKING** | same missing class + drawables |
| 32 | Shop → Trails | **WORKING** | |
| 33 | Shop → Power-ups | **WORKING** | |
| 34 | Shop → Currency | **NOT WORKING** | fake claim, then rolled back |
| 35 | Worlds screen | **PARTIAL** | gate bypass, no rewards, 600-view jank |
| 36 | Trophies screen | **PARTIAL** | claiming works; icons/notifications don't |
| 37 | Settings (speed/sound/music/reset) | **WORKING** | |
| 38 | Control customization | **WORKING** | fully round-tripped |
| 39 | Save / load / sanitise / schema | **WORKING** | 9 dead fields |
| 40 | SFX (procedural) | **WORKING** | serialised, track-per-note |
| 41 | Music (procedural) | **PARTIAL** | wrong world on resume, no focus |
| 42 | Crash reporter | **WORKING** | suppresses Play reporting |

**Totals: 17 WORKING · 12 PARTIAL · 6 NOT WORKING · 7 NOT IMPLEMENTED.**

---

## 21. PRIORITIZED RECOMMENDATIONS

### P0 — Unblock the build (do first; the app cannot compile)

1. **Author `com.jumpadventure.game.graphics.CharacterCardView`** (BUG-01). Minimum viable: a `View` with `var characterId: String` that delegates to `CharacterRenderer.drawCharacter(canvas, bounds, characterId, "DEFAULT", true, AnimState.IDLE, t)` inside `onDraw`, plus a name label. It is instantiated with only `characterId` set (`MainActivity.kt:819, 1124`), so that is the entire required surface.
2. **Create the three missing drawables** (BUG-02, BUG-03): `bg_card_glossy.xml`, `bg_card_selected.xml` (rounded-rect gradient + stroke `GradientDrawable`s are sufficient), `bg_pill_badge.xml` (rounded pill, `@color/game_panel_bg`-ish fill for `game_text_dark` text contrast). All three are pure XML — no bitmaps needed.
3. **Add `app/proguard-rules.pro`** (empty or with standard keep rules) so the release build path resolves.
4. **Add the CI workflow** the README already promises: `.github/workflows/build-apk.yml` running `testDebugUnitTest`, `lintDebug`, `assembleDebug` on PRs and `main`. This is the systemic fix — it would have caught all three blockers.

### P1 — Correctness and data integrity

5. **Resolve the Shop → CURRENCY dead end** (BUG-05). Either remove the tab, or replace `CLAIM` with a cooldown-gated daily/rewarded flow that sets a trusted progression signal (e.g. bump `dailyRewardLastClaimTimestamp`) so `rejectUnsupportedCurrencyGrants` permits it. **Never show "REWARD CLAIMED!" when the grant was rejected** — surface the rejection to the UI.
6. **Fix world unlocking** (BUG-06) to honour `requiredStarsToUnlock` only, remove the `highestLevel >= startLevel` bypass, actually grow `saveData.unlockedWorlds` on unlock, and **remove `currentLevel` mutation from `EXPLORE`**.
7. **Pause the game in `onPause()`** (BUG-08): call `currentGameView?.pauseGame()` and set `incOverlay` to the PAUSE board instead of destroying the modal. Move to `ProcessLifecycleOwner` for robustness.
8. **Introduce a vertical camera + world→screen scale** (BUG-07). Pick a virtual world height (e.g. 1920), scale the canvas uniformly, and add a `cameraY` that follows the player with the same lerp used for X. This is the single change that makes the game playable on all screen sizes.
9. **Guard `handleLevelCompleted` against double-invocation** (BUG-42) and make gem crediting explicit in the Activity rather than the engine (BUG-41).

### P2 — Performance

10. **Fix the allocation storm** (BUG-09, BUG-10): delete the shadowing locals in `CharacterRenderer.drawCharacter` so the 13 cached fields are used, hoist all `Color.parseColor` hex literals to `const val` ints, and move the `Paint`/`Path`/`RectF` allocations in `GameView.drawWorld` out of the element loop into reusable fields.
11. **Downsample background bitmaps** (BUG-38): decode with `inSampleSize` computed from the view dimensions to ~1 screen height; cache the edge pixel at decode time (BUG-14). Consider converting `mountain.webp` (740 KB) to a smaller asset.
12. **Reuse one `AudioTrack`** (BUG-16) instead of constructing one per note — or move to `SoundPool` for SFX and a single streaming `AudioTrack` for music. Add `AudioManager.requestAudioFocus` (BUG-21).
13. **Replace the Worlds screen's 50-synchronous-cards loop** (BUG-11) with a `RecyclerView` (or at minimum lazy population / `ViewStub`).
14. **Debounce the speed-slider save** (BUG-12): persist on `ACTION_UP`, not `ACTION_MOVE`.
15. **Cache `WorldRepository.worlds`** and pre-compute `WorldInfo` (BUG-44); cache `minVisibleWorldId`/`maxVisibleWorldId` per scroll instead of scanning in `onDraw` (BUG-13b).
16. **Stop the 60 fps preview runnable when hidden** (BUG-13): override `onVisibilityChanged` in `CharacterPreviewView`.

### P3 — Design coherence and content

17. **Make all five power-ups consistent.** Either all become charges with HUD buttons, or all become instant. Add HUD presence for HIGH JUMP and POWER (BUG-17 area / §7.1).
18. **Show character skills on the Heroes card** (BUG-15) using `skillName`/`skillDesc`.
19. **Add a lives HUD** and a pit-death indicator.
20. **Rebalance early levels** (BUG-25): pull `SECTION_E`/`F` into the pool from level ~10 and `D`/`H` from ~5, so the first 25 levels have hazards and moving platforms.
21. **Add distinct backdrops for the 10 themes lacking one** (§6.3), or reduce `themeTemplates` to 4 and derive the rest by tint, so the UI stops promising art that doesn't exist.
22. **Remove the boss node styling or implement bosses** (BUG-39).
23. **Remove the dead World reward badges or implement them** (§3.6).
24. **Implement or delete the dead save fields** (BUG-36): daily reward, pets, language, vibration, `hasCustomControls`. Deleting is cheaper and honest.
25. **Fix the level-map header** to update from the visible node range during scroll (BUG-24).
26. **Fix `currentScreenName == "GAME"`** → `"GAMEPLAY"` so music resumes in the right world (BUG-23).
27. **Extract the duplicated card builders** (BUG-17) into a shared factory / custom view — this is the highest-leverage refactor in the codebase and would shrink `MainActivity` by ~60 %.
28. **Remove the dead code and assets** (BUG-27 … BUG-37): 2 classes, 11 drawables, 1 duplicated image, 18 colours, 13 ids, `overlayHandler`, 2 unused dependencies.

### P4 — Quality, accessibility, security

29. **Extract all strings to `strings.xml`** (BUG-33) and add a language selector backed by the existing `saveData.language`.
30. **Use `sp` for text** in custom views (BUG-34) and add `contentDescription`/accessibility nodes to `GamePrimaryButton` and `GameBottomNavView` (BUG-35).
31. **Set `android:allowBackup="false"`** (or provide `dataExtractionRules`) to stop save tampering (BUG-46).
32. **Replace `onBackPressed` with `OnBackPressedDispatcher`** + a predictive-back callback (BUG-48).
33. **Make `SaveSanitizationTest` test `SaveManager`** (or delete it as a tautology) and add tests for `rejectUnsupportedCurrencyGrants`, `LevelGenerator` difficulty bounds, and `GameView` physics (headless — extract `moveAndCollide` into a pure, testable function).
34. **Add an `androidTest` smoke test** that launches `MainActivity` and opens each screen — this would have caught BUG-01 and the missing drawables at runtime even if not at compile time.
35. **Align `CharacterRenderer`'s `isFireResistant`/etc. aliases** so legacy ids and shop ids are documented in one place.
36. **Reconcile the README** with reality: package (`com.synfusion.jump` vs `com.jumpadventure.game`), the CI that doesn't exist, and the signing flow.

---

## 22. APPENDIX — VERIFICATION METHOD

Every claim in this report was checked against source. Key verifications performed:

| Claim | How verified |
|---|---|
| `CharacterCardView` missing | `grep -rn "class CharacterCardView"` across working tree **and** all 1 commit via `git grep` → no definition; only 2 call sites |
| Missing drawables | `comm` of `@drawable/*` / `R.drawable.*` references against `ls res/drawable*`; also `git log --all -- '*bg_card_glossy*'` → never existed |
| `proguard-rules.pro` missing | `ls app/proguard-rules.pro` |
| CI missing | `ls -a .github`; `git ls-tree -r origin/main \| grep -i workflow` |
| No TODO/FIXME anywhere | `grep -rn "TODO\|FIXME\|XXX\|HACK\|…"` over `app/src` → **none** (so the incompleteness is silent, not annotated) |
| All `R.id` refs resolve | `comm -23 used_ids defined_ids` → **empty** (all 59 referenced ids exist) |
| All graphics classes referenced exist | `comm -23 referenced_classes real_classes` → **only `CharacterCardView`** |
| Unused classes | `GamePowerUpButton`, `HomeEnvironmentView` have zero references outside their own files |
| Dead save fields | per-field `grep -rn <field> app/src/main/java` → all hits confined to `SaveManager`/`GameModels` |
| `"GAME"` vs `"GAMEPLAY"` | `grep -n 'currentScreenName =='` → only `"GAME"` at `:91`; `showScreen` assigns `"GAMEPLAY"` at `:348` |
| No vertical camera | `grep -n "camera"` in `GameView.kt` → only `cameraX` |
| Duplicate image | `md5sum res/drawable-nodpi/*.webp` → `home_bg` ≡ `green_forest` |
| Image dimensions | parsed VP8/VP8X/VP8L headers directly (no tooling dependency) |
| `CharacterRenderer` paint shadowing | `awk` over lines 43–330 → 7 local `val …Paint = Paint(…)` shadowing the cached fields at 9–41 |
| Save/restore balance | `grep -c 'canvas.save()'` = 5, `'canvas.restore()'` = 5 |
| Level 1–50 has no enemies | pool selection at `LevelGenerator.kt:252–259` — `SECTION_E` only for `level > 50` |
| Star invariants | read `LevelSafetyTest.kt` + traced the generator back-fill at `:212–238` |
| Bundle size of assets | `ls -la res/drawable-nodpi` → 1.96 MB total |
| Build tooling absent | `which java javac gradle` → none; `ANDROID_HOME`/`ANDROID_SDK_ROOT` empty |

**Not verified (impossible in this environment):** actual compilation, lint output, APK size, runtime behaviour, frame timing, memory profiling. Those require a JDK + Android SDK, which were not available. Where a runtime consequence is asserted (e.g. "the app cannot be built"), it is derived from a proven missing symbol, not from an observed build failure.

---

*End of audit. No project file was modified. Only `PROJECT_AUDIT.md` was created.*

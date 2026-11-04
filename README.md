# JM Tube

<p align="center">
  <img src="assets/app_logo.svg" width="120" height="120" alt="JM Tube Night Cookie Logo" />
</p>

<p align="center">
  <strong>JM Tube</strong> — A privacy-first, feature-rich YouTube and YouTube Music client for Android, Android TV, and Android Auto, built with 100% Kotlin, Jetpack Compose, Material Design 3 Expressive, and AndroidX Media3.
</p>

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Requirements](#2-system-requirements)
3. [Complete Dependency Installation](#3-complete-dependency-installation)
4. [Project Setup from Scratch](#4-project-setup-from-scratch)
5. [Project Structure](#5-project-structure)
6. [Build Instructions (Debug & Release APKs)](#6-build-instructions-debug--release-apks)
7. [Troubleshooting](#7-troubleshooting)
8. [For AI Coding Assistants](#8-for-ai-coding-assistants)
9. [Final Build Verification Checklist](#9-final-build-verification-checklist)

---

## 1. Project Overview

### Purpose & Objectives

**JM Tube** (`com.jmtube.app`) is a modern, standalone Android media application that unifies full **YouTube** video streaming and **JM Music** (YouTube Music) audio streaming into a single privacy-respecting client. It requires no Google account sign-in or proprietary Google Play Services to browse, search, stream, or download content. All user personalization, watch history, playlists, subscriptions, and recommendation weights are stored locally on the device.

### Major Functionalities & Key Features

#### Video Streaming & Discovery (JM Tube)
- **Home & Personalized Feeds:** Powered by the on-device `FlowNeuroEngine` recommendation system, combining channel subscriptions, topic preferences, watch history, and trending shelves without external tracking.
- **Full-Featured Video Player:** Built on AndroidX Media3 (`ExoPlayer`) with adaptive DASH/HLS streaming, resolution & codec selection, playback speed controls, chapter navigation, captions/subtitles, double-tap seek gestures, and volume/brightness swipe controls.
- **Background Playback & Picture-in-Picture (PiP):** Seamless transition to background audio service or native Android Picture-in-Picture window (with fallback floating popup player support).
- **Shorts Player:** Vertical swipe reel player (`ShortsPlayerPool`) with preloading, background play options, and PiP support.
- **SponsorBlock & DeArrow Integration:** Automatic skipping of sponsor segments, intros, outros, and self-promotions, plus crowd-sourced original titles and thumbnails via `DeArrowRepository` and `OriginalTitleRepository`.
- **Subscriptions, Channel Groups & Watch History:** Local subscription management, custom channel grouping, watch-later queues, and searchable local watch/search history with configurable retention and auto-delete rules.

#### Music Streaming & Audio Hub (JM Music)
- **Dedicated Music Interface:** Authentic YouTube Music header and layout featuring quick picks, mood and genre shelves, top charts, albums, singles, and artist discographies.
- **Synced Lyrics & Romanization:** Real-time synced lyrics (`.lrc` import/export, multi-source lyrics lookup, translation, and automatic on-device script romanization).
- **Audio Recognition (Shazam-style):** Built-in microphone audio fingerprinting to identify songs playing nearby.
- **Equalizer & Scrobbling:** Built-in equalizer integration (`EqualizerScreen`) and scrobbling support for **Last.fm** and **Libre.fm**, plus **Discord Rich Presence** (`DiscordPresenceRuntime`).

#### Security, Customization & Multi-Form-Factor Support
- **Native Android App Lock:** Built-in security gate (`AppLockPreferences`, `NativeAuthHelper`, `AppLockOverlay`) supporting Android's native `BiometricPrompt` (Fingerprint / Face Unlock) and `KeyguardManager` device credentials (PIN, Pattern, Password), configurable auto-lock timeouts, and `FLAG_SECURE` Recents preview hiding.
- **YouTube Opening Animation & Smart Adaptive Icons:** Authentic four-stage YouTube startup animation (`YouTubeStartupAnimation` morphing from the YouTube Red Pill into an expanding timeline/seek bar and resolving into the classic YouTube logo lockup) and 16 custom adaptive launcher icons (`AppIcons`) with automatic Dark/Light mode icon synchronization.
- **Material 3 Expressive Theming:** Dynamic Material You colors, Light, Dark, and pure AMOLED Black themes, custom theme editor (`CustomThemeEditorScreen`), and custom typography (`AppFontPreferences`).
- **Multi-Profile & Peer-to-Peer Device Sync:** Isolated local user profiles (`ProfilesScreen`) and encrypted LAN/QR-code device-to-device synchronization (`FLOW-SYNC` via embedded Ktor WebSockets server & CameraX/ZXing QR scanner) plus Google Takeout / NewPipe / Metrolist backup import & export.
- **Android TV, Android Auto & Home-Screen Widgets:** Dedicated Leanback D-pad interface (`TvActivity` / `FlowTvApp`), Android Auto media browser service, and Jetpack Glance Material 3 home-screen widgets (`NowPlayingWidget`, `QuickActions`, `RecentlyPlayed`, `Downloads`).

### Architecture Overview

JM Tube follows **Clean Architecture** and **MVVM (Model-View-ViewModel)** principles:
- **UI Layer (`ui/`):** 100% declarative Jetpack Compose UI using Material 3 Expressive (`compose-bom-alpha`), adaptive list-detail scaffolds (`androidx.compose.material3.adaptive`), and unidirectional data flow (`StateFlow` + `collectAsStateWithLifecycle`).
- **Domain & Recommendation Layer (`data/recommendation/`, `player/`, `security/`):** Encapsulates playback policies, on-device recommendation engines (`FlowNeuroEngine`, `MusicBrainEngine`), and biometric/credential security state.
- **Data Layer (`data/local/`, `data/repository/`, `innertube/`):** Combines **Room** SQLite persistence (with schema exports in `app/schemas/`), **Jetpack DataStore** (`safePreferencesDataStore` scoped per profile or per device), **NewPipeExtractor**, and custom **InnerTube** Ktor/OkHttp clients.
- **Dependency Injection (`di/`):** Managed via **Dagger Hilt** (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`) with **KSP** code generation.

---

## 2. System Requirements

### Supported Development Operating Systems
- **Linux:** Ubuntu 22.04 LTS / 24.04 LTS, Debian 12+, Fedora 39+, or any modern 64-bit (`x86_64` / `arm64`) Linux distribution.
- **macOS:** macOS 13 (Ventura) or newer (Intel or Apple Silicon).
- **Windows:** Windows 10 (64-bit) or Windows 11 (64-bit).

### Minimum & Recommended Hardware
| Resource | Minimum | Recommended |
| :--- | :--- | :--- |
| **RAM** | 8 GB (Gradle JVM heap is configured to `-Xmx4g`) | 16 GB or more |
| **CPU** | 4 cores (`org.gradle.workers.max=4`) | 8+ cores |
| **Disk Space** | 15 GB free (SDK + Gradle caches + build outputs) | 30 GB+ SSD |

### Required Toolchain & Exact Versions

All versions below are strictly pinned in the repository's build configuration (`gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and `app/build.gradle.kts`). **Do not substitute arbitrary newer or older versions.**

| Component | Required Version | Configuration Source |
| :--- | :--- | :--- |
| **Java Development Kit (JDK)** | **JDK 21** (e.g., Eclipse Temurin 21 LTS; compiles to JVM Target 17 bytecode) | `app/build.gradle.kts` (`JavaVersion.VERSION_17`, `JvmTarget.JVM_17`) |
| **Gradle** | **9.3.1** | `gradle/wrapper/gradle-wrapper.properties` |
| **Android Gradle Plugin (AGP)** | **9.1.1** | `gradle/libs.versions.toml` (`agp = "9.1.1"`) |
| **Kotlin** | **2.4.10** | `gradle/libs.versions.toml` (`kotlin = "2.4.10"`) |
| **KSP (Kotlin Symbol Processing)** | **2.3.11** | `gradle/libs.versions.toml` (`ksp = "2.3.11"`) |
| **Compile SDK (`compileSdk`)** | **API 37** | `app/build.gradle.kts` (`compileSdk = 37`) |
| **Target SDK (`targetSdk`)** | **API 36** (Android 16) | `app/build.gradle.kts` (`targetSdk = 36`) |
| **Minimum SDK (`minSdk`)** | **API 26** (Android 8.0 Oreo) | `app/build.gradle.kts` (`minSdk = 26`) |
| **Android Studio** | **Android Studio Otter / Meerkat / Narwhal (2025.1+)** supporting AGP 9.1.1 | IDE compatibility requirement |

---

## 3. Complete Dependency Installation

### 3.1 Installing JDK 21

Gradle 9.3.1 and AGP 9.1.1 run best on **JDK 21 LTS** (while `app/build.gradle.kts` compiles source compatibility to Java 17).

- **Linux (Ubuntu/Debian):**
  ```bash
  sudo apt-get update
  sudo apt-get install -y temurin-21-jdk || sudo apt-get install -y openjdk-21-jdk
  ```
- **macOS (Homebrew):**
  ```bash
  brew install --cask temurin@21
  ```
- **Windows (Winget):**
  ```powershell
  winget install EclipseAdoptium.Temurin.21.JDK
  ```
- **Verify JDK Installation:**
  ```bash
  java -version
  javac -version
  ```
  Ensure `JAVA_HOME` points to your JDK 21 installation directory.

### 3.2 Installing the Android SDK & Command-Line Tools

You can install the required Android SDK packages either through **Android Studio -> SDK Manager** or via the **Android SDK Command-Line Tools (`sdkmanager`)**:

1. **Required SDK Components:**
   - `platform-tools` (ADB, fastboot)
   - `platforms;android-37` (Required by `compileSdk = 37`)
   - `platforms;android-36` (Target SDK 36)
   - `build-tools;36.0.0` (or `35.0.0`+)

2. **Install via `sdkmanager` (CLI):**
   ```bash
   export ANDROID_SDK_ROOT="$HOME/Android/Sdk" # Adjust path for your OS
   $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --install \
     "platform-tools" \
     "platforms;android-37" \
     "platforms;android-36" \
     "build-tools;36.0.0"
   $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses
   ```

### 3.3 Repository Configuration

Defined in `settings.gradle.kts`:
- **Plugin Repositories:**
  - `google()` (filtered for `com.android.*`, `com.google.*`, `androidx.*`)
  - `mavenCentral()`
  - `gradlePluginPortal()`
- **Dependency Repositories (`FAIL_ON_PROJECT_REPOS`):**
  - `google()`
  - `mavenCentral()`
  - `https://jitpack.io` (Required for `com.github.TeamNewPipe:NewPipeExtractor:v0.26.5`)
- **Toolchain Resolver:**
  - `org.gradle.toolchains.foojay-resolver-convention` version `1.0.0`

### 3.4 Complete Library & Plugin Catalog (`gradle/libs.versions.toml`)

All project libraries are resolved automatically by Gradle during synchronization or build. Below is the exact inventory configured in the project:

#### Gradle Plugins
| Plugin Alias | Plugin ID | Version | Purpose |
| :--- | :--- | :--- | :--- |
| `libs.plugins.android.application` | `com.android.application` | `9.1.1` | Android application build system |
| `libs.plugins.kotlin.compose` | `org.jetbrains.kotlin.plugin.compose` | `2.4.10` | Jetpack Compose compiler plugin |
| `libs.plugins.hilt.android` | `com.google.dagger.hilt.android` | `2.60.1` | Dagger Hilt dependency injection |
| `libs.plugins.kotlin.serialization` | `org.jetbrains.kotlin.plugin.serialization` | `2.4.10` | Kotlinx JSON & route serialization |
| `libs.plugins.ksp` | `com.google.devtools.ksp` | `2.3.11` | Kotlin Symbol Processing for Room & Hilt |
| `libs.plugins.room` | `androidx.room` | `2.8.4` | Room schema export & verification |

#### Core Libraries & Frameworks
| Category | Artifact(s) | Version |
| :--- | :--- | :--- |
| **Core & Lifecycle** | `androidx.core:core-ktx`, `androidx.core:core-splashscreen`, `androidx.activity:activity-compose`, `androidx.lifecycle:lifecycle-*` (`viewmodel-compose`, `runtime-compose`, `runtime-ktx`, `process`), `androidx.window:window` | `core-ktx 1.19.0`, `splashscreen 1.2.0`, `activity-compose 1.13.0`, `lifecycle 2.11.0`, `window 1.5.0` |
| **Jetpack Compose & Material 3** | `androidx.compose:compose-bom-alpha`, `ui`, `ui-graphics`, `ui-tooling-preview`, `material3`, `material-icons-extended`, `material3.adaptive:adaptive-layout`, `adaptive-navigation`, `constraintlayout-compose`, `graphics-shapes` | BOM `2026.08.00`, `constraintlayout 1.1.2`, `graphics-shapes 1.1.0` |
| **Navigation** | `androidx.navigation:navigation-compose` | `2.10.0` |
| **Dependency Injection** | `com.google.dagger:hilt-android`, `hilt-android-compiler` (KSP), `androidx.hilt:hilt-navigation-compose` | Hilt `2.60.1`, Hilt Nav `1.4.0` |
| **Media Playback** | `androidx.media3:media3-exoplayer`, `media3-ui`, `media3-common`, `media3-session`, `media3-exoplayer-hls`, `media3-exoplayer-dash`, `media3-datasource`, `media3-datasource-okhttp`, `media3-muxer`, `media3-inspector`, `androidx.media:media` | Media3 `1.11.0`, Media `1.8.0` |
| **Extraction & Networking** | `com.github.TeamNewPipe:NewPipeExtractor`, `com.squareup.okhttp3:okhttp`, `io.ktor:ktor-client-*` (`core`, `okhttp`, `content-negotiation`, `encoding`), `io.ktor:ktor-serialization-kotlinx-json`, `org.brotli:dec`, `com.google.re2j:re2j` | NewPipeExtractor `v0.26.5`, OkHttp `5.4.0`, Ktor `3.5.2`, Brotli `0.1.2`, RE2J `1.7` |
| **Device Sync & QR** | `io.ktor:ktor-server-core`, `ktor-server-cio`, `ktor-server-websockets`, `com.google.zxing:core`, `androidx.camera:camera-*` (`core`, `camera2`, `lifecycle`, `view`), `org.conscrypt:conscrypt-android`, `org.bouncycastle:bcprov-jdk18on`, `cash.z.ecc.android:kotlin-bip39` | Ktor `3.5.2`, ZXing `3.5.4`, CameraX `1.6.1`, Conscrypt `2.6.1`, BouncyCastle `1.86`, BIP39 `1.0.9` |
| **Database & Storage** | `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KSP), `androidx.datastore:datastore-preferences` | Room `2.8.4`, DataStore `1.1.7` |
| **Images & UI Reordering** | `io.coil-kt.coil3:coil-compose`, `coil-video`, `coil-network-okhttp`, `androidx.palette:palette-ktx`, `sh.calvin.reorderable:reorderable` | Coil `3.5.0`, Palette `1.0.0`, Reorderable `3.1.0` |
| **Widgets & Background Work** | `androidx.glance:glance-appwidget`, `glance-material3`, `androidx.work:work-runtime-ktx`, `androidx.paging:paging-runtime-ktx`, `paging-compose` | Glance `1.2.0`, WorkManager `2.11.2`, Paging `3.5.1` |
| **Desugaring & Performance** | `com.android.tools:desugar_jdk_libs_nio`, `androidx.profileinstaller:profileinstaller`, `androidx.tracing:tracing` | Desugar `2.1.5`, ProfileInstaller `1.4.1`, Tracing `1.3.0` |
| **Testing** | `junit:junit`, `org.robolectric:robolectric`, `androidx.test:core-ktx`, `androidx.test.ext:junit`, `io.mockk:mockk`, `com.google.truth:truth`, `app.cash.turbine:turbine`, `org.jetbrains.kotlinx:kotlinx-coroutines-test`, `com.ibm.icu:icu4j`, `net.sf.kxml:kxml2` | JUnit `4.13.2`, Robolectric `4.16.1`, MockK `1.14.11`, Truth `1.4.5`, Turbine `1.2.1`, Coroutines `1.11.0` |

---

## 4. Project Setup from Scratch

### 4.1 Clone the Repository

```bash
git clone <repository-url> JM-Tube
cd JM-Tube
```

### 4.2 Environment Variables & SDK Location

- **Cloud / CI / Container Environments:** Set `ANDROID_SDK_ROOT` (and optionally `ANDROID_HOME`) in your shell environment:
  ```bash
  export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
  export ANDROID_HOME="$ANDROID_SDK_ROOT"
  export PATH="$PATH:$ANDROID_SDK_ROOT/platform-tools"
  ```
  > **Note for AI Studio Cloud Environment:** `ANDROID_SDK_ROOT` is pre-configured in the container environment. **Never create or edit a `local.properties` file inside the AI Studio cloud container**, as doing so interferes with the container's SDK configuration.
- **Local Desktop Android Studio Users:** When opening the project on a local desktop machine, Android Studio automatically detects your SDK via `ANDROID_SDK_ROOT` or generates a local `local.properties` (which is ignored by `.gitignore`).

### 4.3 Mandatory vs. Optional API Keys & Secrets

**Mandatory Configuration:** **None.** JM Tube builds and runs out-of-the-box with zero API keys required.

**Optional Configuration (Last.fm Scrobbling):**
`app/build.gradle.kts` reads two optional secrets via `secretValue(...)` and injects them into `BuildConfig`:
- `LASTFM_API_KEY` (Gradle property `lastfmApiKey` or environment variable `LASTFM_API_KEY`)
- `LASTFM_API_SECRET` (Gradle property `lastfmApiSecret` or environment variable `LASTFM_API_SECRET`)

If omitted, both default cleanly to `""` (`empty string`), and users can still supply their own Last.fm API key at runtime inside **Settings -> Integrations -> Scrobbling**.

To provide them at build time:
- **In AI Studio:** Add `LASTFM_API_KEY` and `LASTFM_API_SECRET` in the **Secrets panel in AI Studio** (see `.env.example`).
- **In Command-Line / CI:** Export them as environment variables:
  ```bash
  export LASTFM_API_KEY="your_lastfm_api_key"
  export LASTFM_API_SECRET="your_lastfm_shared_secret"
  ```

### 4.4 Debug Keystore Decoding (First-Time Setup Check)

Look at `app/build.gradle.kts` lines 100–105:
```kotlin
create("debugConfig") {
    storeFile = file("${rootDir}/debug.keystore")
    storePassword = "android"
    keyAlias = "androiddebugkey"
    keyPassword = "android"
}
```
The repository includes `debug.keystore.base64` at the project root. In the AI Studio build container, `debug.keystore` is automatically materialized before builds. If you are building on a fresh local machine and `debug.keystore` does not yet exist in the root directory, generate it from `debug.keystore.base64` with one command:

```bash
base64 -d debug.keystore.base64 > debug.keystore
```
*(On macOS, use `base64 -D -i debug.keystore.base64 -o debug.keystore`.)*

### 4.5 Opening in Android Studio vs. Command-Line Setup

#### Option A: Android Studio Setup
1. Launch Android Studio (2025.1+ supporting AGP 9.1.1).
2. Click **Open** and select the root `JM-Tube` directory.
3. Verify under **Settings -> Build, Execution, Deployment -> Build Tools -> Gradle** that **Gradle JDK** is set to **JDK 21**.
4. Wait for Gradle Sync to complete.

#### Option B: Command-Line Setup
1. Ensure `java -version` shows JDK 21 and `ANDROID_SDK_ROOT` is set.
2. Ensure `debug.keystore` exists at the repository root (`ls -l debug.keystore`).
3. Run `gradle tasks` (or `./gradlew tasks` if a local wrapper script is present) to verify Gradle configuration.

---

## 5. Project Structure

```text
JM-Tube/
├── .env.example                      # Template for optional Last.fm API environment variables
├── build.gradle.kts                  # Root Gradle configuration (Kotlin 2.4.10 classpath & plugin aliases)
├── settings.gradle.kts               # Repositories (Google, MavenCentral, JitPack) & rootProject.name = "JM Tube"
├── gradle.properties                 # JVM args (-Xmx4g), parallel builds, caching, in-process Kotlin compiler
├── debug.keystore.base64             # Base64-encoded debug signing keystore for consistent APK signatures
├── metadata.json                     # Google AI Studio platform metadata
├── gradle/
│   ├── libs.versions.toml            # Centralized Gradle Version Catalog for all plugins and libraries
│   └── wrapper/
│       └── gradle-wrapper.properties # Pins Gradle 9.3.1 distribution
└── app/
    ├── build.gradle.kts              # App module configuration (namespace/appId com.jmtube.app, SDKs, signing, deps)
    ├── proguard-rules.pro            # R8/ProGuard keep rules for NewPipeExtractor, Rhino, Gson, Ktor, Media3
    ├── schemas/                      # Exported Room database JSON schemas for migration verification
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml   # Permissions, MainActivity, TvActivity, services, deep links & 16 icon aliases
        │   ├── assets/               # Changelogs and bundled static assets
        │   ├── generated/            # Baseline profiles (startup-prof.txt, baseline-prof.txt)
        │   ├── res/
        │   │   ├── drawable/         # Vector icons, adaptive icon foregrounds/backgrounds, splash icons
        │   │   ├── mipmap-anydpi-v26/# Adaptive launcher icon XML definitions for all 16 launcher aliases
        │   │   ├── values/           # strings.xml, themes.xml (Theme.Flow & Theme.Flow.Starting), colors.xml
        │   │   ├── values-*/         # Localized string resources (40+ languages; auto-detected by translatedLocaleTags())
        │   │   └── xml/              # Backup rules, data extraction rules, automotive app descriptor, widget metadata
        │   └── java/com/jmtube/app/
        │       ├── JMTubeApplication.kt  # @HiltAndroidApp entry point, crash handler & background warmups
        │       ├── MainActivity.kt       # Phone/tablet/foldable host Activity, PiP lifecycle & App Lock hooks
        │       ├── TvActivity.kt         # Android TV Leanback launcher Activity
        │       ├── StartupWarmups.kt     # Deferred non-blocking startup initializers
        │       ├── data/
        │       │   ├── backup/           # Android BackupAgent & local/cloud backup serialization
        │       │   ├── local/            # Room AppDatabase, DAOs, Entities, DataStore & migrations
        │       │   ├── lyrics/           # Multi-provider synced lyrics, translation & romanization
        │       │   ├── model/            # Core domain/data models
        │       │   ├── music/            # Music playlists, offline track metadata & download states
        │       │   ├── playlist/         # Playlist import/export (YouTube Takeout, CSV, HTML, M3U)
        │       │   ├── profile/          # Multi-profile file & DataStore isolation
        │       │   ├── recognition/      # Shazam audio recognition fingerprinting
        │       │   ├── recommendation/   # FlowNeuroEngine (video) & MusicBrainEngine (music) on-device AI
        │       │   ├── repository/       # Video, Music, SponsorBlock, DeArrow & OriginalTitle repositories
        │       │   ├── stats/            # Watch time ledgers & Recap aggregation
        │       │   └── update/           # Release version checking & APK update logic
        │       ├── di/                   # Dagger Hilt modules (Database, Network, Player, Repositories)
        │       ├── discord/              # Discord Rich Presence gateway runtime
        │       ├── extensions/           # Kotlin & Compose extension utilities
        │       ├── innertube/            # YouTube & YouTube Music InnerTube API client & parsers
        │       ├── network/              # OkHttp/Ktor clients, proxy & doh configuration
        │       ├── notification/         # Bedtime/break reminders, new video alerts & download notifications
        │       ├── platform/             # AppIconController, DeviceFormFactorDetector, keyboard shortcuts
        │       ├── player/               # EnhancedPlayerManager, EnhancedMusicPlayerManager, ShortsPlayerPool, PiP
        │       ├── security/             # AppLockPreferences & NativeAuthHelper (BiometricPrompt & KeyguardManager)
        │       ├── service/              # MediaSession services, background playback & Android Auto browser
        │       ├── sync/ & sync2/        # Encrypted LAN device sync (Ktor WebSockets + QR pairing)
        │       ├── ui/
        │       │   ├── FlowApp.kt        # Main adaptive navigation shell & bottom/rail navigation
        │       │   ├── IncomingIntents.kt# YouTube, YouTube Music, youtu.be & Piped deep-link router
        │       │   ├── components/       # Reusable M3 components, cards, mini-players & AppLockOverlay
        │       │   ├── screens/          # Home, Music, Shorts, Search, Subscriptions, Library, Onboarding, Settings
        │       │   ├── startup/          # FlowRoot, YouTubeStartupAnimation, SplashController, ThemeSettings
        │       │   ├── theme/            # Material 3 color schemes, dynamic colors, custom themes & typography
        │       │   └── tv/               # Android TV D-pad Compose screens & navigation
        │       ├── updater/              # In-app APK download & package installer helpers
        │       ├── util/ & utils/        # AppIcons, AppLanguageManager, FlowCrashHandler, PoTokenWebView
        │       └── widget/               # Jetpack Glance home-screen widgets
        └── debug/                        # Debug-specific manifest/resources
```

---

## 6. Build Instructions (Debug & Release APKs)

> **Important Note on Gradle Command Syntax:**
> - In the **AI Studio Cloud Environment** (or any system with Gradle 9.3.1 installed globally), invoke `gradle <task>` directly (the `./gradlew` wrapper script is not used in the container).
> - On a **local machine** where a Gradle wrapper script is present, you may use either `gradle <task>` (with Gradle 9.3.1 installed) or `./gradlew <task>`.

### 6.1 Building the Debug APK

1. Ensure `debug.keystore` exists in the project root:
   ```bash
   test -f debug.keystore || base64 -d debug.keystore.base64 > debug.keystore
   ```
2. Run the Debug assembly task:
   ```bash
   gradle :app:assembleDebug
   ```
3. **Expected Output Location & Filename:**
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```
   - **Application ID:** `com.jmtube.app`
   - **Version:** `1.0.0` (`versionCode = 1`)
   - **Signing Config:** Signed automatically with `debugConfig` (`debug.keystore`, alias `androiddebugkey`).

### 6.2 Installing and Testing the Debug APK on a Physical Device or Local Emulator

*(For local development environments with `adb` connected to a device or emulator)*:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.jmtube.app/com.jmtube.app.MainActivity
```

### 6.3 Building the Release APK

Look at how `release` signing is configured in `app/build.gradle.kts` (lines 92–127):

```kotlin
signingConfigs {
    create("release") {
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
        storeFile = file("${rootDir}/debug.keystore")
        storePassword = "android"
        keyAlias = "androiddebugkey"
        keyPassword = "android"
    }
}
```
And in `buildTypes.release`:
- If the release keystore file (`KEYSTORE_PATH` or `my-upload-key.jks`) exists, Gradle signs the Release APK using the `release` signing configuration.
- If no production keystore file is present, Gradle **automatically falls back to `debugConfig`** so that `gradle :app:assembleRelease` succeeds out-of-the-box without failing on missing signing credentials.

#### Option A: Build Release APK Immediately (Using Automatic Fallback Signing)

```bash
gradle :app:assembleRelease
```
- **Expected Output Location:**
  ```text
  app/build/outputs/apk/release/app-release.apk
  ```

#### Option B: Generate a Production Upload Keystore & Build a Production-Signed Release APK

1. **Generate a 2048-bit RSA keystore (`my-upload-key.jks`) using `keytool`:**
   ```bash
   keytool -genkeypair -v \
     -keystore my-upload-key.jks \
     -keyalg RSA \
     -keysize 2048 \
     -validity 10000 \
     -alias upload \
     -storepass "YourStrongStorePassword" \
     -keypass "YourStrongKeyPassword" \
     -dname "CN=JM Tube, OU=Mobile, O=JMTube, L=City, ST=State, C=US"
   ```
   > **Security Warning:** Never commit `my-upload-key.jks` or your passwords to Git. (`*.jks` should be kept outside version control or in `.gitignore`.)

2. **Export the Signing Environment Variables and Run `assembleRelease`:**
   ```bash
   export KEYSTORE_PATH="$(pwd)/my-upload-key.jks"
   export STORE_PASSWORD="YourStrongStorePassword"
   export KEY_PASSWORD="YourStrongKeyPassword"

   gradle :app:assembleRelease
   ```

3. **Verify the Signed Release APK Signature:**
   ```bash
   $ANDROID_SDK_ROOT/build-tools/36.0.0/apksigner verify --verbose --print-certs \
     app/build/outputs/apk/release/app-release.apk
   ```
   Or using `keytool`:
   ```bash
   keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk
   ```

---

## 7. Troubleshooting

### 1. `Keystore file '.../debug.keystore' not found for signing config 'debugConfig'`
- **Cause:** `debug.keystore` was not decoded from `debug.keystore.base64` on a fresh clone.
- **Solution:**
  ```bash
  base64 -d debug.keystore.base64 > debug.keystore
  ```

### 2. `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable`
- **Cause:** Neither `ANDROID_SDK_ROOT` nor `ANDROID_HOME` is set in your shell environment.
- **Solution:** Export `ANDROID_SDK_ROOT` pointing to your Android SDK installation directory:
  ```bash
  export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
  export ANDROID_HOME="$ANDROID_SDK_ROOT"
  ```

### 3. `Failed to find target with hash string 'android-37'`
- **Cause:** `app/build.gradle.kts` specifies `compileSdk = 37`, and Android API 37 platform is not installed in your local SDK.
- **Solution:** Install `platforms;android-37` via Android Studio SDK Manager (enable **Show Package Details** under SDK Platforms) or via CLI:
  ```bash
  $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --install "platforms;android-37" "platforms;android-36"
  ```

### 4. `OutOfMemoryError: Java heap space` or Kotlin Daemon Disconnection
- **Cause:** Insufficient system memory when compiling large Compose + KSP modules.
- **Solution:** `gradle.properties` already sets `org.gradle.jvmargs=-Xmx4g -Dfile.encoding=UTF-8`, `org.gradle.workers.max=4`, and `kotlin.compiler.execution.strategy=in-process`. If building on a low-memory machine (8 GB RAM), close other applications or run with `--max-workers=2`:
  ```bash
  gradle :app:assembleDebug --max-workers=2
  ```

### 5. App Icon Picker / Launcher Shortcut Changes
- **Cause:** `AndroidManifest.xml` defines 16 `<activity-alias>` entries (`.IconFlowRed`, `.IconFlowLight`, `.IconFlowPlay`, `.IconAmoled`, `.IconMonochrome`, `.IconGhost`, `.IconDynamic`, `.IconMaterialSky`, `.IconMaterialMint`, `.IconExpressiveSegmented`, `.IconExpressivePlay`, `.IconExpressiveSky`, `.IconExpressiveMint`, `.IconExpressiveCookie`, `.IconExpressiveOval`, `.IconExpressivePill`).
- **Solution:** Never delete or rename the default enabled alias (`.IconFlowRed`) without keeping `AppIcons.kt`, `SplashThemes.kt`, and `AndroidManifest.xml` synchronized. If an `<activity-alias>` is modified, every `<activity-alias>` in `AndroidManifest.xml` must match the list in `AppIcons.ALL_SUFFIXES`.

---

## 8. For AI Coding Assistants

If you are an AI coding agent (such as Google AI Studio Build, Gemini, Claude, Cursor, or GitHub Copilot) working on this repository, adhere strictly to the following rules:

1. **Inspect Before Modifying:**
   - Always inspect the actual files (`app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `FlowRoot.kt`, `MainActivity.kt`, and `res/values/strings.xml`) before proposing or applying edits.
   - Never invent file paths, package names, or dependencies. The root Kotlin package is `com.jmtube.app` under `app/src/main/java/com/jmtube/app/`.
2. **Preserve Build & Signing Configurations:**
   - **Never** modify `debug.keystore` or `debug.keystore.base64`.
   - **Never** remove or alter `signingConfigs` (`release` and `debugConfig`) or their assignments in `buildTypes` inside `app/build.gradle.kts`.
   - **Never** downgrade or arbitrarily upgrade `agp` (`9.1.1`), `kotlin` (`2.4.10`), `ksp` (`2.3.11`), `compileSdk` (`37`), `targetSdk` (`36`), or `minSdk` (`26`) in `gradle/libs.versions.toml` or `app/build.gradle.kts`.
   - **Never** create or use a `local.properties` file in the cloud container environment.
3. **Respect Existing Architecture & Patterns:**
   - **Settings System:** New settings pages must be added to `SettingsDestination.kt`, indexed in `DestinationIndex.kt` and `SettingsIndex.kt` (for searchability), given an icon in `SettingsHomeIcons.kt`, linked in `SettingsHomeContent.kt`, and routed in `SettingsDetail.kt`.
   - **Data Persistence:** Device-wide preferences (such as `AppLockPreferences`) belong in `context.devicePreferencesDataStore` (`StoreScope.DEVICE`), whereas profile-specific settings belong in profile-scoped stores (`StoreScope.PROFILE_SETTINGS`).
   - **Launcher Icons (`AppIcons.kt`):** Any change to launcher icon aliases must be kept in 1:1 sync across `AndroidManifest.xml`, `AppIcons.kt`, `SplashThemes.kt`, and `AppIconDialog.kt`.
   - **Lifecycle State Collection:** Note that with Lifecycle `2.11.0`, `Flow<T>.collectAsStateWithLifecycle(initialValue = ...)` requires the named parameter `initialValue` (while `StateFlow<T>.collectAsStateWithLifecycle()` requires no initial value parameter).
4. **Verify With Real Builds:**
   - Always verify changes using `compile_applet` (in AI Studio) or `gradle :app:assembleDebug` (via CLI).
   - Never claim a build or test succeeded without actually running the build tool and confirming a zero-error exit status.

---

## 9. Final Build Verification Checklist

Use this checklist to verify a complete setup and build from scratch:

- [ ] **JDK 21 Active:** `java -version` outputs version `21.x`.
- [ ] **Android SDK Configured:** `ANDROID_SDK_ROOT` points to a valid Android SDK with `platforms;android-37`, `platforms;android-36`, and `build-tools;36.0.0` installed.
- [ ] **Gradle Version Verified:** `gradle --version` outputs `Gradle 9.3.1`.
- [ ] **Debug Keystore Ready:** `debug.keystore` is present at the repository root.
- [ ] **Debug APK Compiled & Verified:**
  - Command: `gradle :app:assembleDebug`
  - Verified Output: `app/build/outputs/apk/debug/app-debug.apk`
- [ ] **Release APK Compiled & Verified:**
  - Command: `gradle :app:assembleRelease`
  - Verified Output: `app/build/outputs/apk/release/app-release.apk`
- [ ] **APK Signature Verified:** `keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk` (or `apksigner verify`) confirms a valid v2/v3 APK signature.
- [ ] **Unit / Robolectric Tests Verified (Optional):** `gradle :app:testDebugUnitTest` executes local JVM unit tests cleanly.

---

## License

JM Tube is licensed under the **GNU General Public License v3.0 (GPL-3.0)**.

# Build Guide

## Prerequisites

| Requirement | Version |
| --- | --- |
| JDK | 17 or newer |
| Android SDK | API 36 (compile), API 26 (min) |
| Android Studio | Ladybug or newer (optional — CLI builds work) |
| Gradle | Provided by the wrapper (do not install separately) |

Verify your JDK:

```bash
java -version
echo $JAVA_HOME
```

## First-time setup

1. Clone the repository:

   ```bash
   git clone https://github.com/zuher5/Zdon.git
   cd Zdon
   ```

2. Create `local.properties` pointing at your Android SDK. This file is
   intentionally gitignored and must be created locally:

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

   On Linux this is typically `/home/<user>/Android/Sdk`, on macOS
   `/Users/<user>/Library/Android/sdk`, and on Windows
   `C:\\Users\\<user>\\AppData\\Local\\Android\\Sdk`.

   Android Studio creates this file automatically when you open the project.

3. Verify the toolchain resolves:

   ```bash
   ./gradlew tasks
   ```

## Building

### Debug

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/`

Debug builds have minification disabled and use the `.debug` application ID
suffix, so a debug and release build can be installed side by side.

### Release

```bash
./gradlew assembleRelease
```

Output: `app/build/outputs/apk/release/`

Release builds enable R8 minification and resource shrinking.

### Both

```bash
./gradlew assembleDebug assembleRelease
```

## APK variants

ABI splits are enabled, so each build produces five APKs:

| APK | Approx. size | Target |
| --- | --- | --- |
| `app-arm64-v8a-release.apk` | ~54 MB | Modern 64-bit ARM devices (recommended) |
| `app-armeabi-v7a-release.apk` | ~48 MB | Older 32-bit ARM devices |
| `app-x86_64-release.apk` | ~57 MB | 64-bit emulators, x86 tablets |
| `app-x86-release.apk` | ~53 MB | 32-bit emulators |
| `app-universal-release.apk` | ~193 MB | All architectures in one file |

The size is dominated by the bundled Python runtime and FFmpeg binaries, which
are required by yt-dlp and are shipped per architecture. Distribute the
ABI-specific APKs where possible; the universal APK exists for convenience only.

## Signing

The release build type currently reuses the debug signing config:

```kotlin
release {
    signingConfig = signingConfigs.getByName("debug")
}
```

This is fine for local testing but **must be replaced before distributing
builds**. To use your own keystore:

1. Generate a keystore (keep it out of version control):

   ```bash
   keytool -genkey -v -keystore release.jks \
     -keyalg RSA -keysize 2048 -validity 10000 -alias zdon
   ```

2. Add the credentials to `local.properties` or environment variables — never to
   a tracked file:

   ```properties
   ZDON_KEYSTORE_FILE=/absolute/path/to/release.jks
   ZDON_KEYSTORE_PASSWORD=...
   ZDON_KEY_ALIAS=zdon
   ZDON_KEY_PASSWORD=...
   ```

3. Wire a `signingConfigs.create("release")` block in `app/build.gradle.kts`
   that reads those properties, and point the release build type at it.

`*.jks` and `*.keystore` are gitignored.

## Verification

Run these before opening a pull request:

```bash
./gradlew lint          # Android Lint across all modules
./gradlew test          # JVM unit tests
./gradlew assembleDebug # Compile check
```

Instrumented tests require a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

## Clean builds

```bash
./gradlew clean
```

If Gradle behaves unexpectedly after a dependency or plugin change, escalate
gradually:

```bash
./gradlew clean build --no-configuration-cache
./gradlew --stop                    # kill daemons
rm -rf .gradle .kotlin              # local caches
```

## Build performance

The project enables parallel execution, the build cache, and the configuration
cache in `gradle.properties`. The Gradle daemon is given 4 GB, which is what KSP
plus Compose compilation needs comfortably.

If your machine has less RAM available, lower `org.gradle.jvmargs` rather than
disabling the caches.

Note that the configuration cache is invalidated whenever a build script
changes, so the first build after editing `build.gradle.kts` will be slower.

## Common issues

**`SDK location not found`**
`local.properties` is missing or `sdk.dir` is wrong. See first-time setup.

**`Unable to strip the following libraries`**
Expected and harmless. The Python and FFmpeg payloads ship without debug
symbols to strip; AGP logs a warning and packages them as-is.

**`ExceptionInInitializerError` at runtime on release builds**
R8 stripped a class needed reflectively by yt-dlp's extraction path. Check that
`app/proguard-rules.pro` still keeps `org.apache.commons.compress.**` and
`com.yausername.**`.

**Configuration cache errors after changing build logic**
Run once with `--no-configuration-cache` to get a clean error message, then fix
the underlying problem rather than leaving the cache disabled.

## Continuous integration

`.github/workflows/build.yml` runs on every push and pull request:

1. Set up JDK 17
2. Restore the Gradle cache
3. `./gradlew lint`
4. `./gradlew test`
5. `./gradlew assembleDebug`
6. `./gradlew assembleRelease`

Any failing step fails the workflow. CI does not upload APKs or handle signing
secrets.

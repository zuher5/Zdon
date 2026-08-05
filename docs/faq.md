# FAQ

## General

**What is Zdon?**
An Android video downloader built on yt-dlp, with a Jetpack Compose / Material 3
interface.

**Does it need root?**
No. Zdon runs entirely within the normal app sandbox.

**Which Android versions are supported?**
Android 8.0 (API 26) and newer.

**Is there a Play Store release?**
No. Apps that download media from third-party sites generally violate Play
policy. Install from [Releases](https://github.com/zuher5/Zdon/releases).

**Does Zdon collect analytics?**
No. There is no telemetry, no crash reporting service, and no network traffic
other than what yt-dlp performs to fetch the media you request.

## Installation

**Which APK should I install?**
`app-arm64-v8a-release.apk` for essentially any phone sold in the last several
years. Use `app-armeabi-v7a-release.apk` for older 32-bit devices, and the
universal APK only if you are unsure.

**Why is the APK so large?**
Each APK bundles a full Python runtime plus FFmpeg for its architecture, because
yt-dlp is a Python program and needs FFmpeg for muxing and audio extraction.
The universal APK contains four copies of that payload, one per ABI, which is why
it is roughly four times larger.

**Can the size be reduced further?**
Not meaningfully without removing functionality. The Java/Kotlin code is already
minified with R8 and resource-shrunk; the remaining size is native payload.

**Android says the app is from an unknown source.**
Enable installation from unknown sources for your browser or file manager, then
install. This is expected for APKs distributed outside an app store.

## Usage

**Which sites work?**
Anything yt-dlp supports — over a thousand sites. See the
[yt-dlp supported sites list](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md).

**Can I download private or age-restricted videos?**
Not currently. Cookie and credential support is not implemented.

**Where do downloads go?**
A folder you choose in Settings via the system directory picker. Zdon writes
through the Storage Access Framework, so no broad storage permission is needed.

**Can I download only the audio?**
Yes. Enable audio extraction on the home screen and pick a target format.

**Do playlists work?**
Yes. Enable playlist download before starting; each entry is queued as its own
download.

**What happens if I close the app mid-download?**
Downloads run in a WorkManager worker with a foreground notification and
continue. Progress is persisted in the database, so the queue is restored even
after the process is killed.

**A download failed. What now?**
Use Retry on the download row. Persistent failures are usually an upstream site
change — check whether the same URL works with current yt-dlp on desktop.

## Technical

**Why is the first launch slow?**
The bundled Python runtime is extracted to the app's private directory on first
run. Subsequent launches skip this.

**Does Zdon bundle yt-dlp itself?**
It bundles the `youtubedl-android` library, which packages yt-dlp and its Python
runtime. Binary updates are scheduled through `BinaryUpdateWorker`.

**Why does the release build need special ProGuard rules?**
`ZipUtils` in youtubedl-common reaches Apache Commons Compress reflectively while
extracting the Python payload. Without keep rules, R8 renames those classes and
initialization fails with `ExceptionInInitializerError`. The rules live in
`app/proguard-rules.pro`.

**Why `useLegacyPackaging = true` for jniLibs?**
The Python and FFmpeg binaries are executed as child processes, which requires
them to exist as real, uncompressed files on disk.

**How is the project structured?**
Multi-module Clean Architecture. See [architecture.md](architecture.md).

## Contributing

**How do I build from source?**
See [build.md](build.md).

**How do I contribute?**
See [CONTRIBUTING.md](../CONTRIBUTING.md).

**Where do I report a bug?**
[Open an issue](https://github.com/zuher5/Zdon/issues) using the bug report
template. Include your Android version, device, app version, and reproduction
steps.

**Where do I report a security issue?**
Privately — see [SECURITY.md](../SECURITY.md). Do not open a public issue.

## Legal

**Is downloading videos legal?**
It depends on the content, the platform's terms of service, and your
jurisdiction. Zdon is a tool; how you use it is your responsibility. Respect
copyright and the terms of the sites you use.

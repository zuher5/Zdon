# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.1] - 2026-08-05

### Fixed
- Fixed R8 obfuscation issue causing `ExceptionInInitializerError` during YoutubeDL initialization
- Added ProGuard rules for Apache Commons Compress to prevent class stripping
- Resolved crash on release builds when extracting Python runtime

### Changed
- Optimized ProGuard rules for better release build stability
- Improved error handling in download initialization

## [1.0.0] - 2026-08-05

### Added
- Initial public release
- Modern Material 3 UI with dynamic color theming
- Video quality selection up to 8K
- Audio-only extraction with format selection
- Playlist download support
- Subtitle and thumbnail embedding
- Download queue management with pause/resume
- Download history with search functionality
- Custom output directory selection
- Proxy and custom headers support
- Dark and light theme support
- Adaptive layouts for tablets and foldables
- Per-ABI APK splits for optimized install size

### Technical
- Clean Architecture with modular design
- Jetpack Compose UI
- Hilt dependency injection
- Room database for persistence
- DataStore for preferences
- WorkManager for background downloads
- yt-dlp integration via youtubedl-android
- FFmpeg for media processing

---

## Release Notes

### v1.0.1
Critical bug fix for release builds. Users experiencing crashes on startup should update to this version.

### v1.0.0
First stable release. Tested on Android 8.0+ devices with various architectures.

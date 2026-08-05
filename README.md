# Zdon

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-blue.svg)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-1.7-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Material 3](https://img.shields.io/badge/Material%20You-3.0-orange.svg)](https://m3.material.io/)
[![yt-dlp](https://img.shields.io/badge/yt--dlp-powered-red.svg)](https://github.com/yt-dlp/yt-dlp)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A modern Android video downloader powered by yt-dlp, built with Jetpack Compose and Material 3.

## Features

✨ **Modern UI**
- Material 3 Design with dynamic color theming
- Adaptive layouts for tablets and foldables
- Edge-to-edge immersive experience
- Dark and light theme support

📥 **Powerful Downloads**
- Video quality selection up to 8K
- Audio-only extraction with format selection
- Playlist download support
- Subtitle embedding
- Thumbnail embedding
- Custom filename templates

⚡ **Advanced Features**
- Parallel downloads with queue management
- Download progress tracking with speed and ETA
- Pause, resume, and retry failed downloads
- Download history with search
- Custom output directory selection
- Proxy and custom headers support

🎯 **Developer-Friendly**
- Clean Architecture with modular design
- Jetpack Compose UI
- Hilt dependency injection
- Room database persistence
- DataStore preferences
- Kotlin Coroutines and Flow
- Comprehensive test coverage

## Screenshots

_Screenshots will be added soon_

## Architecture

Zdon follows Clean Architecture principles with a modular, multi-module structure:

```
Zdon/
├── app/                    # Application module
├── core/
│   ├── common/            # Common utilities and extensions
│   ├── data/              # Repository implementations
│   ├── database/          # Room database
│   ├── datastore/         # DataStore preferences
│   ├── designsystem/      # UI components and theming
│   ├── downloader/        # Download management
│   ├── engine/            # yt-dlp engine wrapper
│   └── model/             # Domain models
└── feature/
    ├── downloads/         # Downloads screen
    ├── history/           # History screen
    ├── home/              # Home screen with URL input
    └── settings/          # Settings screen
```

### Tech Stack

**UI Layer**
- Jetpack Compose
- Material 3 Components
- Compose Navigation
- Coil for image loading

**Domain & Data Layer**
- Kotlin Coroutines & Flow
- Hilt for dependency injection
- Room for local persistence
- DataStore for preferences

**Download Engine**
- yt-dlp (Python) via youtubedl-android
- FFmpeg for media processing
- WorkManager for background downloads

**Build & Tooling**
- Gradle with Version Catalogs
- Kotlin DSL
- Convention Plugins
- R8 with ProGuard rules

## Requirements

- **Android 8.0 (API 26)** or higher
- **64MB** free storage (minimum)
- **Internet connection** for downloads

## Installation

### From Releases

1. Download the appropriate APK from [Releases](https://github.com/zuher5/Zdon/releases)
   - `app-arm64-v8a-release.apk` - Modern 64-bit devices (recommended)
   - `app-armeabi-v7a-release.apk` - Older 32-bit devices
   - `app-universal-release.apk` - All architectures (larger size)

2. Enable "Install from Unknown Sources" in Android settings
3. Install the APK

### Build from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/zuher5/Zdon.git
   cd Zdon
   ```

2. **Open in Android Studio**
   - Android Studio Ladybug or newer
   - JDK 17 or higher

3. **Build**
   ```bash
   ./gradlew assembleRelease
   ```

4. **APKs location**
   ```
   app/build/outputs/apk/release/
   ```

See [docs/build.md](docs/build.md) for detailed build instructions.

## How to Use

1. **Enter URL**: Paste a video URL from supported sites
2. **Analyze**: Tap "Analyze" to fetch video information
3. **Configure**: Select quality, audio format, and other options
4. **Download**: Tap "Download" to start

### Supported Sites

Zdon supports 1000+ websites through yt-dlp, including:
- YouTube
- Twitter/X
- Instagram
- Facebook
- TikTok
- Vimeo
- And many more...

See [yt-dlp supported sites](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md) for the complete list.

## Contributing

Contributions are welcome! Please read [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Setup

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and lint
5. Submit a pull request

## Roadmap

- [ ] Download scheduling
- [ ] Download speed limiter
- [ ] Format presets
- [ ] Batch URL import
- [ ] Share target integration
- [ ] Export/import settings
- [ ] Video preview before download
- [ ] Cloud storage integration

See [open issues](https://github.com/zuher5/Zdon/issues) for more planned features.

## FAQ

**Q: Does Zdon require root?**  
A: No, Zdon works on non-rooted devices.

**Q: Why is the APK size large?**  
A: Zdon bundles Python runtime and FFmpeg for video processing. Use ABI-specific APKs to reduce size.

**Q: Which sites are supported?**  
A: Zdon supports all sites that yt-dlp supports (1000+).

**Q: Can I download private videos?**  
A: Currently, authentication is not implemented. Only public videos are supported.

See [docs/faq.md](docs/faq.md) for more questions.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- [yt-dlp](https://github.com/yt-dlp/yt-dlp) - The powerful media downloader
- [youtubedl-android](https://github.com/yausername/youtubedl-android) - Android wrapper for yt-dlp
- [Material 3](https://m3.material.io/) - Design system

## Disclaimer

This app is for educational purposes only. Please respect copyright laws and terms of service of content providers. The developers are not responsible for any misuse of this application.

---

**Made with ❤️ using Jetpack Compose**

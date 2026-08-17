# Security Policy

## Supported Versions

Only the latest release receives security updates.

| Version | Supported          |
| ------- | ------------------ |
| 1.0.x   | :white_check_mark: |
| < 1.0   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in Zdon, please report it privately.

**Do not open a public issue for security vulnerabilities.**

Instead:

1. Open a [GitHub Security Advisory](https://github.com/zuher5/Zdon/security/advisories/new)
2. Or contact the maintainer directly through GitHub

Please include:

- A description of the vulnerability
- Steps to reproduce
- Affected version(s)
- Potential impact
- Suggested fix, if you have one

### Response Timeline

- **Acknowledgement**: within 7 days
- **Initial assessment**: within 14 days
- **Fix or mitigation plan**: communicated after assessment

## Scope

The following are in scope:

- The Zdon Android application
- Build configuration and release pipeline
- Handling of user data (download history, preferences, output paths)

The following are **out of scope**:

- Vulnerabilities in [yt-dlp](https://github.com/yt-dlp/yt-dlp) — report upstream
- Vulnerabilities in [youtubedl-android](https://github.com/yausername/youtubedl-android) — report upstream
- Vulnerabilities in FFmpeg — report upstream
- Issues that require a rooted device or physical access with an unlocked bootloader

## Security Considerations

Zdon by design:

- Executes a bundled Python runtime in the app's private data directory
- Spawns child processes (`yt-dlp`, `ffmpeg`) confined to the app sandbox
- Stores no credentials or API keys
- Performs no telemetry or analytics collection
- Requests only the permissions required for downloading and notifications

Users should be aware that:

- Custom headers and proxy settings are stored locally in DataStore
- Downloaded files are written to a user-selected directory via the Storage
  Access Framework
- Release builds fall back to a debug key when no signing configuration is
  present; for distribution, build with the `ZDON_KEYSTORE_PATH`,
  `ZDON_STORE_PASSWORD`, `ZDON_KEY_ALIAS` and `ZDON_KEY_PASSWORD` properties or
  environment variables, and provide them as GitHub Secrets (`ZDON_KEYSTORE`
  base64-encoded) in the release workflow so every published APK shares one
  permanent signature
